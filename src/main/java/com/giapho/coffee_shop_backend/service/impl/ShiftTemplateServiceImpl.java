package com.giapho.coffee_shop_backend.service.impl;

import com.giapho.coffee_shop_backend.domain.entity.ShiftTemplate;
import com.giapho.coffee_shop_backend.domain.repository.ShiftTemplateRepository;
import com.giapho.coffee_shop_backend.dto.shift.ShiftTemplateRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftTemplateResponseDTO;
import com.giapho.coffee_shop_backend.exception.shift.ShiftTemplateNotFoundException;
import com.giapho.coffee_shop_backend.exception.shift.ShiftTemplateValidationException;
import com.giapho.coffee_shop_backend.mapper.ShiftTemplateMapper;
import com.giapho.coffee_shop_backend.service.ShiftTemplateService;
import com.giapho.coffee_shop_backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ShiftTemplateServiceImpl implements ShiftTemplateService {

    private static final String SYSTEM_USER = "SYSTEM";

    private final ShiftTemplateRepository shiftTemplateRepository;
    private final ShiftTemplateMapper shiftTemplateMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<ShiftTemplateResponseDTO> getAllTemplates(Pageable pageable) {
        return shiftTemplateRepository.findAll(pageable)
                .map(shiftTemplateMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public ShiftTemplateResponseDTO getTemplate(Long id) {
        ShiftTemplate template = findTemplate(id);
        return shiftTemplateMapper.toResponseDTO(template);
    }

    @Override
    public ShiftTemplateResponseDTO createTemplate(ShiftTemplateRequestDTO request) {
        validateTemplateRequest(request);
        ensureNameIsUnique(request.name(), null);

        ShiftTemplate template = shiftTemplateMapper.toEntity(request);
        String actor = resolveActor();
        template.setCreatedBy(actor);
        template.setUpdatedBy(actor);

        ShiftTemplate saved = shiftTemplateRepository.save(template);
        log.info("Created shift template {} with code {}", saved.getId(), saved.getName());
        return shiftTemplateMapper.toResponseDTO(saved);
    }

    @Override
    public ShiftTemplateResponseDTO updateTemplate(Long id, ShiftTemplateRequestDTO request) {
        ShiftTemplate template = findTemplate(id);
        validateTemplateRequest(request);
        ensureNameIsUnique(request.name(), template);

        shiftTemplateMapper.updateFromDto(request, template);
        template.setUpdatedBy(resolveActor());

        ShiftTemplate saved = shiftTemplateRepository.save(template);
        log.info("Updated shift template {}", id);
        return shiftTemplateMapper.toResponseDTO(saved);
    }

    @Override
    public void deleteTemplate(Long id) {
        ShiftTemplate template = findTemplate(id);
        shiftTemplateRepository.delete(template);
        log.info("Deleted shift template {}", id);
    }

    private ShiftTemplate findTemplate(Long id) {
        return shiftTemplateRepository.findById(id)
                .orElseThrow(() -> new ShiftTemplateNotFoundException(id));
    }

    private void validateTemplateRequest(ShiftTemplateRequestDTO request) {
        if (!StringUtils.hasText(request.name())) {
            throw new ShiftTemplateValidationException("Template name must not be blank");
        }
        validateTimeRange(request.startTime(), request.endTime());
        validateRates(request.defaultHourlyRate(), request.defaultFixedAllowance());
    }

    private void ensureNameIsUnique(String name, ShiftTemplate currentTemplate) {
        boolean exists = shiftTemplateRepository.existsByNameIgnoreCase(name);
        if (!exists) {
            return;
        }
        if (currentTemplate != null && name.equalsIgnoreCase(currentTemplate.getName())) {
            return;
        }
        throw new ShiftTemplateValidationException("Tên ca đã tồn tại: " + name);
    }

    private void validateTimeRange(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            throw new ShiftTemplateValidationException("Giờ bắt đầu và kết thúc không được để trống");
        }
        if (!start.isBefore(end)) {
            throw new ShiftTemplateValidationException("Giờ bắt đầu phải trước giờ kết thúc");
        }
    }

    private void validateRates(BigDecimal hourlyRate, BigDecimal fixedAllowance) {
        if (hourlyRate != null && hourlyRate.signum() < 0) {
            throw new ShiftTemplateValidationException("Lương theo giờ không được âm");
        }
        if (fixedAllowance != null && fixedAllowance.signum() < 0) {
            throw new ShiftTemplateValidationException("Phụ cấp cố định không được âm");
        }
    }

    private String resolveActor() {
        return SecurityUtil.getCurrentUsername().orElse(SYSTEM_USER);
    }
}
