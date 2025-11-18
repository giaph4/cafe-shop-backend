package com.giapho.coffee_shop_backend.service.impl;

import com.giapho.coffee_shop_backend.domain.entity.ShiftAssignment;
import com.giapho.coffee_shop_backend.domain.entity.ShiftPerformanceAdjustment;
import com.giapho.coffee_shop_backend.domain.enums.AdjustmentType;
import com.giapho.coffee_shop_backend.domain.repository.ShiftAssignmentRepository;
import com.giapho.coffee_shop_backend.domain.repository.ShiftPerformanceAdjustmentRepository;
import com.giapho.coffee_shop_backend.dto.shift.ShiftPerformanceAdjustmentRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftPerformanceAdjustmentResponseDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftPerformanceAdjustmentRevokeRequestDTO;
import com.giapho.coffee_shop_backend.exception.shift.ShiftAssignmentNotFoundException;
import com.giapho.coffee_shop_backend.exception.shift.ShiftPerformanceAdjustmentNotFoundException;
import com.giapho.coffee_shop_backend.exception.shift.ShiftPerformanceAdjustmentStateException;
import com.giapho.coffee_shop_backend.mapper.ShiftPerformanceAdjustmentMapper;
import com.giapho.coffee_shop_backend.service.ShiftPerformanceAdjustmentService;
import com.giapho.coffee_shop_backend.service.ShiftAssignmentService;
import com.giapho.coffee_shop_backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ShiftPerformanceAdjustmentServiceImpl implements ShiftPerformanceAdjustmentService {

    private static final String SYSTEM_USER = "SYSTEM";

    private final ShiftPerformanceAdjustmentRepository adjustmentRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftAssignmentService shiftAssignmentService;
    private final ShiftPerformanceAdjustmentMapper adjustmentMapper;

    @Override
    @Transactional(readOnly = true)
    public ShiftPerformanceAdjustmentResponseDTO getAdjustment(Long id) {
        ShiftPerformanceAdjustment adjustment = findAdjustment(id);
        return adjustmentMapper.toResponseDTO(adjustment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftPerformanceAdjustmentResponseDTO> getAdjustmentsForAssignment(Long assignmentId) {
        return adjustmentRepository.findByAssignmentId(assignmentId).stream()
                .map(adjustmentMapper::toResponseDTO)
                .toList();
    }

    @Override
    public ShiftPerformanceAdjustmentResponseDTO createAdjustment(ShiftPerformanceAdjustmentRequestDTO request) {
        ShiftAssignment assignment = resolveAssignment(request.assignmentId());
        validateAdjustmentType(request.type());

        ShiftPerformanceAdjustment adjustment = adjustmentMapper.toEntity(request);
        adjustment.setAssignment(assignment);
        adjustment.setRevoked(false);
        String actor = resolveActor();
        adjustment.setCreatedBy(actor);
        adjustment.setUpdatedBy(actor);

        ShiftPerformanceAdjustment saved = adjustmentRepository.save(adjustment);
        shiftAssignmentService.recalculateAssignment(assignment.getId());
        log.info("Created adjustment {} for assignment {}", saved.getId(), assignment.getId());
        return adjustmentMapper.toResponseDTO(saved);
    }

    @Override
    public ShiftPerformanceAdjustmentResponseDTO revokeAdjustment(Long adjustmentId, ShiftPerformanceAdjustmentRevokeRequestDTO request) {
        ShiftPerformanceAdjustment adjustment = findAdjustment(adjustmentId);
        if (adjustment.isRevoked()) {
            throw new ShiftPerformanceAdjustmentStateException("Điều chỉnh đã bị thu hồi trước đó");
        }

        adjustment.setRevoked(true);
        adjustment.setRevokedAt(LocalDateTime.now());
        String actor = resolveActor();
        adjustment.setRevokedBy(actor);
        if (request != null && StringUtils.hasText(request.reason())) {
            adjustment.setReason(request.reason());
        }
        adjustment.setUpdatedBy(actor);

        ShiftPerformanceAdjustment saved = adjustmentRepository.save(adjustment);
        shiftAssignmentService.recalculateAssignment(saved.getAssignment().getId());
        log.info("Revoked adjustment {}", adjustmentId);
        return adjustmentMapper.toResponseDTO(saved);
    }

    @Override
    public void deleteAdjustment(Long adjustmentId) {
        ShiftPerformanceAdjustment adjustment = findAdjustment(adjustmentId);
        Long assignmentId = adjustment.getAssignment().getId();
        adjustmentRepository.delete(adjustment);
        shiftAssignmentService.recalculateAssignment(assignmentId);
        log.info("Deleted adjustment {} for assignment {}", adjustmentId, assignmentId);
    }

    private ShiftAssignment resolveAssignment(Long assignmentId) {
        return shiftAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ShiftAssignmentNotFoundException(assignmentId));
    }

    private ShiftPerformanceAdjustment findAdjustment(Long id) {
        return adjustmentRepository.findById(id)
                .orElseThrow(() -> new ShiftPerformanceAdjustmentNotFoundException(id));
    }

    private void validateAdjustmentType(AdjustmentType type) {
        if (Objects.isNull(type)) {
            throw new ShiftPerformanceAdjustmentStateException("Loại điều chỉnh không hợp lệ");
        }
    }

    private String resolveActor() {
        return SecurityUtil.getCurrentUsername().orElse(SYSTEM_USER);
    }
}
