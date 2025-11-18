package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.domain.enums.ShiftStatus;
import com.giapho.coffee_shop_backend.dto.shift.ShiftInstanceCreateRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftInstanceResponseDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftInstanceStatusUpdateRequestDTO;
import com.giapho.coffee_shop_backend.service.ShiftInstanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/shifts/instances")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
public class ShiftInstanceController {

    private final ShiftInstanceService shiftInstanceService;

    @GetMapping
    public ResponseEntity<Page<ShiftInstanceResponseDTO>> listInstances(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) ShiftStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ShiftInstanceResponseDTO> response = shiftInstanceService.listInstances(from, to, status, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShiftInstanceResponseDTO> getInstance(@PathVariable Long id) {
        return ResponseEntity.ok(shiftInstanceService.getInstance(id));
    }

    @PostMapping
    public ResponseEntity<List<ShiftInstanceResponseDTO>> createInstances(
            @Valid @RequestBody ShiftInstanceCreateRequestDTO request
    ) {
        List<ShiftInstanceResponseDTO> created = shiftInstanceService.createInstances(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShiftInstanceResponseDTO> updateInstance(
            @PathVariable Long id,
            @Valid @RequestBody ShiftInstanceCreateRequestDTO request
    ) {
        ShiftInstanceResponseDTO updated = shiftInstanceService.updateInstance(id, request);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ShiftInstanceResponseDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody ShiftInstanceStatusUpdateRequestDTO request
    ) {
        ShiftInstanceResponseDTO updated = shiftInstanceService.updateStatus(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInstance(@PathVariable Long id) {
        shiftInstanceService.deleteInstance(id);
        return ResponseEntity.noContent().build();
    }
}
