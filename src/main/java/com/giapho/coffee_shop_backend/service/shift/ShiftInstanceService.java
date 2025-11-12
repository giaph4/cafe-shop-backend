package com.giapho.coffee_shop_backend.service.shift;

import com.giapho.coffee_shop_backend.domain.entity.ShiftAssignment;
import com.giapho.coffee_shop_backend.domain.entity.ShiftInstance;
import com.giapho.coffee_shop_backend.domain.entity.ShiftTemplate;
import com.giapho.coffee_shop_backend.domain.enums.ShiftStatus;
import com.giapho.coffee_shop_backend.domain.repository.ShiftAssignmentRepository;
import com.giapho.coffee_shop_backend.domain.repository.ShiftInstanceRepository;
import com.giapho.coffee_shop_backend.domain.repository.ShiftTemplateRepository;
import com.giapho.coffee_shop_backend.dto.shift.ShiftInstanceCreateRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftInstanceResponseDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftInstanceStatusUpdateRequestDTO;
import com.giapho.coffee_shop_backend.mapper.ShiftInstanceMapper;
import com.giapho.coffee_shop_backend.util.SecurityUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ShiftInstanceService {

    private static final String SYSTEM_USER = "SYSTEM";

    private final ShiftInstanceRepository shiftInstanceRepository;
    private final ShiftTemplateRepository shiftTemplateRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftInstanceMapper shiftInstanceMapper;

    @Transactional(readOnly = true)
    public Page<ShiftInstanceResponseDTO> listInstances(LocalDate from, LocalDate to, ShiftStatus status, Pageable pageable) {
        List<Specification<ShiftInstance>> specs = new ArrayList<>();

        if (from != null) {
            specs.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("shiftDate"), from));
        }
        if (to != null) {
            specs.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("shiftDate"), to));
        }
        if (status != null) {
            specs.add((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        if (specs.isEmpty()) {
            return shiftInstanceRepository.findAll(pageable)
                    .map(shiftInstanceMapper::toResponseDTO);
        }

        Specification<ShiftInstance> composed = Specification.allOf(specs);

        return shiftInstanceRepository.findAll(composed, pageable)
                .map(shiftInstanceMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public ShiftInstanceResponseDTO getInstance(Long id) {
        ShiftInstance instance = findById(id);
        return shiftInstanceMapper.toResponseDTO(instance);
    }

    public List<ShiftInstanceResponseDTO> createInstances(ShiftInstanceCreateRequestDTO request) {
        ShiftTemplate template = shiftTemplateRepository.findById(request.templateId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy template với ID: " + request.templateId()));

        LocalTime start = request.startTime() != null ? request.startTime() : template.getStartTime();
        LocalTime end = request.endTime() != null ? request.endTime() : template.getEndTime();
        validateTimeRange(start, end);

        List<LocalDate> dates = request.dates();
        if (dates == null || dates.isEmpty()) {
            dates = List.of(request.shiftDate());
        }

        List<ShiftInstanceResponseDTO> result = new ArrayList<>();
        for (LocalDate date : dates) {
            if (shiftInstanceRepository.existsByTemplateAndDate(template.getId(), date)) {
                continue;
            }
            ShiftInstance instance = new ShiftInstance();
            instance.setTemplate(template);
            instance.setShiftDate(date);
            instance.setStartTime(start);
            instance.setEndTime(end);
            instance.setNotes(request.notes());
            instance.setCreatedBy(SecurityUtil.getCurrentUsername().orElse(SYSTEM_USER));
            instance.setUpdatedBy(instance.getCreatedBy());

            ShiftInstance saved = shiftInstanceRepository.save(instance);
            result.add(shiftInstanceMapper.toResponseDTO(saved));
        }
        return result;
    }

    public ShiftInstanceResponseDTO updateInstance(Long id, ShiftInstanceCreateRequestDTO request) {
        ShiftInstance instance = findById(id);
        if (instance.getStatus() == ShiftStatus.LOCKED || instance.getStatus() == ShiftStatus.DONE) {
            throw new IllegalStateException("Không thể chỉnh sửa ca đã khóa hoặc hoàn thành");
        }

        LocalTime start = request.startTime() != null ? request.startTime() : instance.getStartTime();
        LocalTime end = request.endTime() != null ? request.endTime() : instance.getEndTime();
        validateTimeRange(start, end);

        shiftInstanceMapper.updateFromDto(request, instance);
        instance.setUpdatedBy(SecurityUtil.getCurrentUsername().orElse(SYSTEM_USER));

        ShiftInstance updated = shiftInstanceRepository.save(instance);
        return shiftInstanceMapper.toResponseDTO(updated);
    }

    public ShiftInstanceResponseDTO updateStatus(Long id, ShiftInstanceStatusUpdateRequestDTO request) {
        ShiftInstance instance = findById(id);
        ShiftStatus newStatus = request.status();

        if (newStatus == ShiftStatus.LOCKED) {
            instance.setLockedAt(java.time.LocalDateTime.now());
        }

        if (newStatus == ShiftStatus.CANCELLED && !instance.getAssignments().isEmpty()) {
            throw new IllegalStateException("Không thể hủy ca vì đã có nhân viên được phân");
        }

        instance.setStatus(newStatus);
        instance.setNotes(request.notes());
        instance.setUpdatedBy(SecurityUtil.getCurrentUsername().orElse(SYSTEM_USER));

        ShiftInstance saved = shiftInstanceRepository.save(instance);
        return shiftInstanceMapper.toResponseDTO(saved);
    }

    public void deleteInstance(Long id) {
        ShiftInstance instance = findById(id);
        if (!instance.getAssignments().isEmpty()) {
            throw new IllegalStateException("Không thể xóa ca vì đã có nhân viên được phân");
        }
        shiftInstanceRepository.delete(instance);
    }

    private ShiftInstance findById(Long id) {
        return shiftInstanceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy ca với ID: " + id));
    }

    private void validateTimeRange(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Giờ bắt đầu và kết thúc không được để trống");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Giờ bắt đầu phải trước giờ kết thúc");
        }
    }
}
