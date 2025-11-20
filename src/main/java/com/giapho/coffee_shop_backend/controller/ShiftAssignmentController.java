package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.dto.shift.ShiftAssignmentRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftAssignmentResponseDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftAssignmentStatusUpdateRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftAssignmentUpdateRequestDTO;
import com.giapho.coffee_shop_backend.service.ShiftAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shifts/assignments")
@RequiredArgsConstructor
public class ShiftAssignmentController {

    private final ShiftAssignmentService shiftAssignmentService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    public ResponseEntity<List<ShiftAssignmentResponseDTO>> getAssignmentsForCurrentUser() {
        return ResponseEntity.ok(shiftAssignmentService.getAssignmentsForCurrentUser());
    }

    @GetMapping("/{assignmentId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ShiftAssignmentResponseDTO> getAssignment(@PathVariable Long assignmentId) {
        return ResponseEntity.ok(shiftAssignmentService.getAssignment(assignmentId));
    }

    @GetMapping("/shift/{shiftId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<List<ShiftAssignmentResponseDTO>> listAssignmentsForShift(@PathVariable Long shiftId) {
        List<ShiftAssignmentResponseDTO> assignments = shiftAssignmentService.getAssignmentsForShift(shiftId);
        return ResponseEntity.ok(assignments);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ShiftAssignmentResponseDTO> createAssignment(
            @Valid @RequestBody ShiftAssignmentRequestDTO request
    ) {
        ShiftAssignmentResponseDTO created = shiftAssignmentService.createAssignment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{assignmentId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ShiftAssignmentResponseDTO> updateAssignment(
            @PathVariable Long assignmentId,
            @Valid @RequestBody ShiftAssignmentUpdateRequestDTO request
    ) {
        ShiftAssignmentResponseDTO updated = shiftAssignmentService.updateAssignment(assignmentId, request);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{assignmentId}/status")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<ShiftAssignmentResponseDTO> updateStatus(
            @PathVariable Long assignmentId,
            @Valid @RequestBody ShiftAssignmentStatusUpdateRequestDTO request
    ) {
        ShiftAssignmentResponseDTO updated = shiftAssignmentService.updateStatus(assignmentId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{assignmentId}")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long assignmentId) {
        shiftAssignmentService.deleteAssignment(assignmentId);
        return ResponseEntity.noContent().build();
    }
}
