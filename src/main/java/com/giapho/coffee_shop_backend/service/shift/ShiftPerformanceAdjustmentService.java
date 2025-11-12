package com.giapho.coffee_shop_backend.service.shift;

import com.giapho.coffee_shop_backend.domain.entity.ShiftAssignment;
import com.giapho.coffee_shop_backend.domain.entity.ShiftPerformanceAdjustment;
import com.giapho.coffee_shop_backend.domain.repository.ShiftAssignmentRepository;
import com.giapho.coffee_shop_backend.domain.repository.ShiftPerformanceAdjustmentRepository;
import com.giapho.coffee_shop_backend.dto.shift.ShiftPerformanceAdjustmentRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftPerformanceAdjustmentResponseDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftPerformanceAdjustmentRevokeRequestDTO;
import com.giapho.coffee_shop_backend.mapper.ShiftPerformanceAdjustmentMapper;
import com.giapho.coffee_shop_backend.util.SecurityUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ShiftPerformanceAdjustmentService {

    private static final String SYSTEM_USER = "SYSTEM";

    private final ShiftPerformanceAdjustmentRepository adjustmentRepository;
    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final ShiftAssignmentService shiftAssignmentService;
    private final ShiftPerformanceAdjustmentMapper adjustmentMapper;

    @Transactional(readOnly = true)
    public ShiftPerformanceAdjustmentResponseDTO getAdjustment(Long id) {
        ShiftPerformanceAdjustment adjustment = findById(id);
        return adjustmentMapper.toResponseDTO(adjustment);
    }

    @Transactional(readOnly = true)
    public List<ShiftPerformanceAdjustmentResponseDTO> getAdjustmentsForAssignment(Long assignmentId) {
        return adjustmentRepository.findByAssignmentId(assignmentId).stream()
                .map(adjustmentMapper::toResponseDTO)
                .toList();
    }

    public ShiftPerformanceAdjustmentResponseDTO createAdjustment(ShiftPerformanceAdjustmentRequestDTO request) {
        ShiftAssignment assignment = shiftAssignmentRepository.findById(request.assignmentId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phân công với ID: " + request.assignmentId()));

        ShiftPerformanceAdjustment adjustment = adjustmentMapper.toEntity(request);
        adjustment.setAssignment(assignment);
        adjustment.setRevoked(false);
        adjustment.setCreatedBy(resolveActor());
        adjustment.setUpdatedBy(adjustment.getCreatedBy());

        ShiftPerformanceAdjustment saved = adjustmentRepository.save(adjustment);
        shiftAssignmentService.recalculateAssignment(assignment.getId());
        log.info("Created adjustment {} for assignment {}", saved.getId(), assignment.getId());
        return adjustmentMapper.toResponseDTO(saved);
    }

    public ShiftPerformanceAdjustmentResponseDTO revokeAdjustment(Long adjustmentId, ShiftPerformanceAdjustmentRevokeRequestDTO request) {
        ShiftPerformanceAdjustment adjustment = findById(adjustmentId);
        if (adjustment.isRevoked()) {
            throw new IllegalStateException("Điều chỉnh đã bị thu hồi trước đó");
        }

        adjustment.setRevoked(true);
        adjustment.setRevokedAt(LocalDateTime.now());
        adjustment.setRevokedBy(resolveActor());
        if (request != null && request.reason() != null && !request.reason().isBlank()) {
            adjustment.setReason(request.reason());
        }
        adjustment.setUpdatedBy(adjustment.getRevokedBy());

        ShiftPerformanceAdjustment saved = adjustmentRepository.save(adjustment);
        shiftAssignmentService.recalculateAssignment(saved.getAssignment().getId());
        log.info("Revoked adjustment {}", adjustmentId);
        return adjustmentMapper.toResponseDTO(saved);
    }

    public void deleteAdjustment(Long adjustmentId) {
        ShiftPerformanceAdjustment adjustment = findById(adjustmentId);
        Long assignmentId = adjustment.getAssignment().getId();
        adjustmentRepository.delete(adjustment);
        shiftAssignmentService.recalculateAssignment(assignmentId);
        log.info("Deleted adjustment {} for assignment {}", adjustmentId, assignmentId);
    }

    private ShiftPerformanceAdjustment findById(Long id) {
        return adjustmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy điều chỉnh với ID: " + id));
    }

    private String resolveActor() {
        return SecurityUtil.getCurrentUsername().orElse(SYSTEM_USER);
    }
}
