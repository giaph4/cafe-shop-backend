package com.giapho.coffee_shop_backend.service.shift;

import com.giapho.coffee_shop_backend.domain.entity.ShiftTemplate;
import com.giapho.coffee_shop_backend.domain.repository.ShiftTemplateRepository;
import com.giapho.coffee_shop_backend.dto.shift.ShiftTemplateRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftTemplateResponseDTO;
import com.giapho.coffee_shop_backend.mapper.ShiftTemplateMapper;
import com.giapho.coffee_shop_backend.util.SecurityUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ShiftTemplateService {

    private static final String SYSTEM_USER = "SYSTEM";

    private final ShiftTemplateRepository shiftTemplateRepository;
    private final ShiftTemplateMapper shiftTemplateMapper;

    @Transactional(readOnly = true)
    public Page<ShiftTemplateResponseDTO> getAllTemplates(Pageable pageable) {
        return shiftTemplateRepository.findAll(pageable)
                .map(shiftTemplateMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public ShiftTemplateResponseDTO getTemplate(Long id) {
        ShiftTemplate template = findById(id);
        return shiftTemplateMapper.toResponseDTO(template);
    }

    public ShiftTemplateResponseDTO createTemplate(ShiftTemplateRequestDTO request) {
        validateTimeRange(request.startTime(), request.endTime());
        validateRates(request.defaultHourlyRate(), request.defaultFixedAllowance());

        if (shiftTemplateRepository.existsByNameIgnoreCase(request.name())) {
            throw new IllegalArgumentException("Tên ca đã tồn tại: " + request.name());
        }

        ShiftTemplate template = shiftTemplateMapper.toEntity(request);
        template.setCreatedBy(SecurityUtil.getCurrentUsername().orElse(SYSTEM_USER));
        template.setUpdatedBy(template.getCreatedBy());

        ShiftTemplate saved = shiftTemplateRepository.save(template);
        return shiftTemplateMapper.toResponseDTO(saved);
    }

    public ShiftTemplateResponseDTO updateTemplate(Long id, ShiftTemplateRequestDTO request) {
        ShiftTemplate template = findById(id);

        if (!template.getName().equalsIgnoreCase(request.name())
                && shiftTemplateRepository.existsByNameIgnoreCase(request.name())) {
            throw new IllegalArgumentException("Tên ca đã tồn tại: " + request.name());
        }

        validateTimeRange(request.startTime(), request.endTime());
        validateRates(request.defaultHourlyRate(), request.defaultFixedAllowance());

        shiftTemplateMapper.updateFromDto(request, template);
        template.setUpdatedBy(SecurityUtil.getCurrentUsername().orElse(SYSTEM_USER));

        ShiftTemplate updated = shiftTemplateRepository.save(template);
        return shiftTemplateMapper.toResponseDTO(updated);
    }

    public void deleteTemplate(Long id) {
        ShiftTemplate template = findById(id);
        shiftTemplateRepository.delete(template);
    }

    private ShiftTemplate findById(Long id) {
        return shiftTemplateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy template ca với ID: " + id));
    }

    private void validateTimeRange(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Giờ bắt đầu và kết thúc không được để trống");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("Giờ bắt đầu phải trước giờ kết thúc");
        }
    }

    private void validateRates(BigDecimal hourlyRate, BigDecimal fixedAllowance) {
        if (hourlyRate != null && hourlyRate.signum() < 0) {
            throw new IllegalArgumentException("Lương theo giờ không được âm");
        }
        if (fixedAllowance != null && fixedAllowance.signum() < 0) {
            throw new IllegalArgumentException("Phụ cấp cố định không được âm");
        }
    }
}
