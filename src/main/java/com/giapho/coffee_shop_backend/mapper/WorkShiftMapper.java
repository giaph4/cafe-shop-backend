package com.giapho.coffee_shop_backend.mapper;

import com.giapho.coffee_shop_backend.domain.entity.WorkShift;
import com.giapho.coffee_shop_backend.dto.shift.WorkShiftRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.WorkShiftResponseDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class WorkShiftMapper {

    public WorkShift toEntity(WorkShiftRequestDTO request) {
        WorkShift workShift = new WorkShift();
        applyRequest(workShift, request);
        return workShift;
    }

    public void updateEntityFromRequest(WorkShiftRequestDTO request, WorkShift entity) {
        applyRequest(entity, request);
    }

    public WorkShiftResponseDTO toResponseDTO(WorkShift entity) {
        return new WorkShiftResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getStartAt(),
                entity.getEndAt(),
                entity.getMaxEmployees(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private void applyRequest(WorkShift target, WorkShiftRequestDTO request) {
        if (request == null) {
            return;
        }
        if (request.name() != null) {
            target.setName(request.name());
        }
        target.setDescription(request.description());
        if (request.startAt() != null) {
            target.setStartAt(request.startAt());
        }
        if (request.endAt() != null) {
            target.setEndAt(request.endAt());
        }
        if (request.maxEmployees() != null) {
            target.setMaxEmployees(request.maxEmployees());
        }
        if (request.active() != null) {
            target.setActive(request.active());
        }
        if (target.getCreatedAt() == null) {
            target.setCreatedAt(LocalDateTime.now());
        }
        if (target.getUpdatedAt() == null) {
            target.setUpdatedAt(LocalDateTime.now());
        }
    }
}
