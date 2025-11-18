package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.dto.shift.ShiftTemplateRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftTemplateResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ShiftTemplateService {

    Page<ShiftTemplateResponseDTO> getAllTemplates(Pageable pageable);

    ShiftTemplateResponseDTO getTemplate(Long id);

    ShiftTemplateResponseDTO createTemplate(ShiftTemplateRequestDTO request);

    ShiftTemplateResponseDTO updateTemplate(Long id, ShiftTemplateRequestDTO request);

    void deleteTemplate(Long id);
}
