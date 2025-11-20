package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.dto.shift.ShiftAssignmentRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftAssignmentResponseDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftAssignmentStatusUpdateRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftAssignmentUpdateRequestDTO;

import java.util.List;

public interface ShiftAssignmentService {

    ShiftAssignmentResponseDTO getAssignment(Long assignmentId);

    List<ShiftAssignmentResponseDTO> getAssignmentsForShift(Long shiftId);

    ShiftAssignmentResponseDTO createAssignment(ShiftAssignmentRequestDTO request);

    ShiftAssignmentResponseDTO updateAssignment(Long assignmentId, ShiftAssignmentUpdateRequestDTO request);

    ShiftAssignmentResponseDTO updateStatus(Long assignmentId, ShiftAssignmentStatusUpdateRequestDTO request);

    void deleteAssignment(Long assignmentId);

    void recalculateAssignment(Long assignmentId);

    List<ShiftAssignmentResponseDTO> getAssignmentsForCurrentUser();
}
