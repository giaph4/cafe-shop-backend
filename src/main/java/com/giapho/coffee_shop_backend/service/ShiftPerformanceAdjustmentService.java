package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.dto.shift.ShiftPerformanceAdjustmentRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftPerformanceAdjustmentResponseDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftPerformanceAdjustmentRevokeRequestDTO;

import java.util.List;

public interface ShiftPerformanceAdjustmentService {

    ShiftPerformanceAdjustmentResponseDTO getAdjustment(Long id);

    List<ShiftPerformanceAdjustmentResponseDTO> getAdjustmentsForAssignment(Long assignmentId);

    ShiftPerformanceAdjustmentResponseDTO createAdjustment(ShiftPerformanceAdjustmentRequestDTO request);

    ShiftPerformanceAdjustmentResponseDTO revokeAdjustment(Long adjustmentId, ShiftPerformanceAdjustmentRevokeRequestDTO request);

    void deleteAdjustment(Long adjustmentId);
}
