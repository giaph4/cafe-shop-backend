package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.shift.ShiftTemplateRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftTemplateResponseDTO;
import com.giapho.coffee_shop_backend.service.ShiftTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shifts/templates")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
public class ShiftTemplateController {

    private final ShiftTemplateService shiftTemplateService;

    @GetMapping
    public ResponseEntity<Page<ShiftTemplateResponseDTO>> listTemplates(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ShiftTemplateResponseDTO> templates = shiftTemplateService.getAllTemplates(pageable);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShiftTemplateResponseDTO> getTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(shiftTemplateService.getTemplate(id));
    }

    @PostMapping
    public ResponseEntity<ShiftTemplateResponseDTO> createTemplate(@Valid @RequestBody ShiftTemplateRequestDTO request) {
        ShiftTemplateResponseDTO created = shiftTemplateService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShiftTemplateResponseDTO> updateTemplate(@PathVariable Long id,
                                                                    @Valid @RequestBody ShiftTemplateRequestDTO request) {
        ShiftTemplateResponseDTO updated = shiftTemplateService.updateTemplate(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        shiftTemplateService.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }
}
