package com.giapho.coffee_shop_backend.service.impl;

import com.giapho.coffee_shop_backend.domain.entity.ShiftInstance;
import com.giapho.coffee_shop_backend.domain.entity.ShiftTemplate;
import com.giapho.coffee_shop_backend.domain.enums.ShiftStatus;
import com.giapho.coffee_shop_backend.domain.repository.ShiftAssignmentRepository;
import com.giapho.coffee_shop_backend.domain.repository.ShiftInstanceRepository;
import com.giapho.coffee_shop_backend.domain.repository.ShiftTemplateRepository;
import com.giapho.coffee_shop_backend.dto.shift.ShiftInstanceCreateRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftInstanceResponseDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftInstanceStatusUpdateRequestDTO;
import com.giapho.coffee_shop_backend.exception.shift.ShiftInstanceNotFoundException;
import com.giapho.coffee_shop_backend.exception.shift.ShiftInstanceStateException;
import com.giapho.coffee_shop_backend.exception.shift.ShiftInstanceValidationException;
import com.giapho.coffee_shop_backend.exception.shift.ShiftTemplateNotFoundException;
import com.giapho.coffee_shop_backend.mapper.ShiftInstanceMapper;
import com.giapho.coffee_shop_backend.service.ShiftInstanceService;
import com.giapho.coffee_shop_backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ShiftInstanceServiceImpl implements ShiftInstanceService {

    private static final String SYSTEM_USER = "SYSTEM";

    private final ShiftInstanceRepository shiftInstanceRepository;
    private final ShiftTemplateRepository shiftTemplateRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftInstanceMapper shiftInstanceMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ShiftInstanceResponseDTO> listInstances(LocalDate from, LocalDate to, ShiftStatus status, Pageable pageable) {
        Specification<ShiftInstance> specification = buildSpecification(from, to, status);
        Page<ShiftInstance> page = specification == null
                ? shiftInstanceRepository.findAll(pageable)
                : shiftInstanceRepository.findAll(specification, pageable);
        return page.map(shiftInstanceMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftInstanceResponseDTO getInstance(Long id) {
        ShiftInstance instance = findInstance(id);
        return shiftInstanceMapper.toResponseDTO(instance);
    }

    @Override
    public List<ShiftInstanceResponseDTO> createInstances(ShiftInstanceCreateRequestDTO request) {
        ShiftTemplate template = resolveTemplate(request.templateId());

        LocalTime startTime = request.startTime() != null ? request.startTime() : template.getStartTime();
        LocalTime endTime = request.endTime() != null ? request.endTime() : template.getEndTime();
        validateTimeRange(startTime, endTime);

        List<LocalDate> targetDates = resolveTargetDates(request);

        String actor = resolveActor();
        List<ShiftInstanceResponseDTO> createdInstances = new ArrayList<>();

        for (LocalDate date : targetDates) {
            if (Boolean.TRUE.equals(shiftInstanceRepository.existsByTemplateAndDate(template.getId(), date))) {
                log.debug("Skip creating shift instance for template {} on {} because it already exists", template.getId(), date);
                continue;
            }

            ShiftInstance instance = ShiftInstance.builder()
                    .template(template)
                    .shiftDate(date)
                    .startTime(startTime)
                    .endTime(endTime)
                    .status(ShiftStatus.PLANNED)
                    .notes(request.notes())
                    .createdBy(actor)
                    .updatedBy(actor)
                    .build();

            ShiftInstance saved = shiftInstanceRepository.save(instance);
            createdInstances.add(shiftInstanceMapper.toResponseDTO(saved));
            log.info("Created shift instance {} for template {} on {}", saved.getId(), template.getId(), date);
        }

        if (createdInstances.isEmpty()) {
            log.warn("No shift instances were created for template {} due to duplicates", template.getId());
        }

        return createdInstances;
    }

    @Override
    public ShiftInstanceResponseDTO updateInstance(Long id, ShiftInstanceCreateRequestDTO request) {
        ShiftInstance instance = findInstance(id);
        ensureInstanceEditable(instance);

        LocalTime startTime = request.startTime() != null ? request.startTime() : instance.getStartTime();
        LocalTime endTime = request.endTime() != null ? request.endTime() : instance.getEndTime();
        validateTimeRange(startTime, endTime);

        shiftInstanceMapper.updateFromDto(request, instance);
        instance.setStartTime(startTime);
        instance.setEndTime(endTime);
        if (request.shiftDate() != null) {
            instance.setShiftDate(request.shiftDate());
        }
        if (StringUtils.hasText(request.notes())) {
            instance.setNotes(request.notes());
        }
        instance.setUpdatedBy(resolveActor());

        ShiftInstance saved = shiftInstanceRepository.save(instance);
        log.info("Updated shift instance {}", id);
        return shiftInstanceMapper.toResponseDTO(saved);
    }

    @Override
    public ShiftInstanceResponseDTO updateStatus(Long id, ShiftInstanceStatusUpdateRequestDTO request) {
        ShiftInstance instance = findInstance(id);
        ShiftStatus newStatus = request.status();

        if (newStatus == ShiftStatus.CANCELLED && hasAssignments(instance.getId())) {
            throw new ShiftInstanceStateException("Không thể hủy ca vì đã có nhân viên được phân");
        }
        if (newStatus == ShiftStatus.LOCKED) {
            instance.setLockedAt(LocalDateTime.now());
        }

        instance.setStatus(newStatus);
        instance.setNotes(request.notes());
        instance.setUpdatedBy(resolveActor());

        ShiftInstance saved = shiftInstanceRepository.save(instance);
        log.info("Updated status for shift instance {} to {}", id, newStatus);
        return shiftInstanceMapper.toResponseDTO(saved);
    }

    @Override
    public void deleteInstance(Long id) {
        ShiftInstance instance = findInstance(id);
        if (hasAssignments(id)) {
            throw new ShiftInstanceStateException("Không thể xóa ca vì đã có nhân viên được phân");
        }
        shiftInstanceRepository.delete(instance);
        log.info("Deleted shift instance {}", id);
    }

    private Specification<ShiftInstance> buildSpecification(LocalDate from, LocalDate to, ShiftStatus status) {
        List<Specification<ShiftInstance>> predicates = new ArrayList<>();

        if (from != null) {
            predicates.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("shiftDate"), from));
        }
        if (to != null) {
            predicates.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("shiftDate"), to));
        }
        if (status != null) {
            predicates.add((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        return predicates.isEmpty() ? null : Specification.allOf(predicates);
    }

    private ShiftInstance findInstance(Long id) {
        return shiftInstanceRepository.findById(id)
                .orElseThrow(() -> new ShiftInstanceNotFoundException(id));
    }

    private ShiftTemplate resolveTemplate(Long templateId) {
        return shiftTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ShiftTemplateNotFoundException(templateId));
    }

    private List<LocalDate> resolveTargetDates(ShiftInstanceCreateRequestDTO request) {
        List<LocalDate> dates = request.dates();
        if (CollectionUtils.isEmpty(dates)) {
            if (request.shiftDate() == null) {
                throw new ShiftInstanceValidationException("Cần cung cấp ít nhất một ngày tạo ca");
            }
            dates = List.of(request.shiftDate());
        }
        return dates;
    }

    private void ensureInstanceEditable(ShiftInstance instance) {
        if (instance.getStatus() == ShiftStatus.LOCKED || instance.getStatus() == ShiftStatus.DONE) {
            throw new ShiftInstanceStateException("Không thể chỉnh sửa ca đã khóa hoặc hoàn thành");
        }
    }

    private boolean hasAssignments(Long instanceId) {
        return shiftAssignmentRepository.existsByShiftId(instanceId);
    }

    private void validateTimeRange(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            throw new ShiftInstanceValidationException("Giờ bắt đầu và kết thúc không được để trống");
        }
        if (!start.isBefore(end)) {
            throw new ShiftInstanceValidationException("Giờ bắt đầu phải trước giờ kết thúc");
        }
    }

    private String resolveActor() {
        return SecurityUtil.getCurrentUsername().orElse(SYSTEM_USER);
    }
}
