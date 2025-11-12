package com.giapho.coffee_shop_backend.mapper;

import com.giapho.coffee_shop_backend.domain.entity.ShiftPerformanceAdjustment;
import com.giapho.coffee_shop_backend.dto.shift.ShiftPerformanceAdjustmentRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftPerformanceAdjustmentResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ShiftPerformanceAdjustmentMapper {

    @Mapping(target = "assignmentId", source = "assignment.id")
    ShiftPerformanceAdjustmentResponseDTO toResponseDTO(ShiftPerformanceAdjustment entity);

    @Mapping(target = "assignment", ignore = true)
    ShiftPerformanceAdjustment toEntity(ShiftPerformanceAdjustmentResponseDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "assignment", ignore = true)
    @Mapping(target = "revoked", ignore = true)
    @Mapping(target = "revokedAt", ignore = true)
    @Mapping(target = "revokedBy", ignore = true)
    @Mapping(target = "effectiveAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ShiftPerformanceAdjustment toEntity(ShiftPerformanceAdjustmentRequestDTO dto);

    @Mapping(target = "assignment", ignore = true)
    void updateEntityFromDto(ShiftPerformanceAdjustmentResponseDTO dto, @MappingTarget ShiftPerformanceAdjustment entity);
}
