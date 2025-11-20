package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.shift.ShiftPerformanceAdjustmentRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftPerformanceAdjustmentResponseDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftPerformanceAdjustmentRevokeRequestDTO;
import com.giapho.coffee_shop_backend.service.ShiftPerformanceAdjustmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shifts/adjustments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
public class ShiftPerformanceAdjustmentController {

    private final ShiftPerformanceAdjustmentService adjustmentService;

    @GetMapping("/{adjustmentId}")
    public ResponseEntity<ShiftPerformanceAdjustmentResponseDTO> getAdjustment(
            @PathVariable Long adjustmentId
    ) {
        return ResponseEntity.ok(adjustmentService.getAdjustment(adjustmentId));
    }

    @GetMapping("/assignment/{assignmentId}")
    public ResponseEntity<List<ShiftPerformanceAdjustmentResponseDTO>> getByAssignment(
            @PathVariable Long assignmentId
    ) {
        List<ShiftPerformanceAdjustmentResponseDTO> adjustments = adjustmentService.getAdjustmentsForAssignment(assignmentId);
        return ResponseEntity.ok(adjustments);
    }

    @PostMapping
    public ResponseEntity<ShiftPerformanceAdjustmentResponseDTO> createAdjustment(
            @Valid @RequestBody ShiftPerformanceAdjustmentRequestDTO request
    ) {
        ShiftPerformanceAdjustmentResponseDTO created = adjustmentService.createAdjustment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{adjustmentId}/revoke")
    public ResponseEntity<ShiftPerformanceAdjustmentResponseDTO> revokeAdjustment(
            @PathVariable Long adjustmentId,
            @Valid @RequestBody ShiftPerformanceAdjustmentRevokeRequestDTO request
    ) {
        ShiftPerformanceAdjustmentResponseDTO revoked = adjustmentService.revokeAdjustment(adjustmentId, request);
        return ResponseEntity.ok(revoked);
    }

    @DeleteMapping("/{adjustmentId}")
    public ResponseEntity<Void> deleteAdjustment(@PathVariable Long adjustmentId) {
        adjustmentService.deleteAdjustment(adjustmentId);
        return ResponseEntity.noContent().build();
    }
}
