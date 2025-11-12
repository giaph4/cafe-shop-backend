package com.giapho.coffee_shop_backend.mapper;

import com.giapho.coffee_shop_backend.domain.entity.ShiftAssignment;
import com.giapho.coffee_shop_backend.domain.entity.ShiftInstance;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.dto.shift.ShiftAssignmentRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftAssignmentResponseDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        uses = {AttendanceRecordMapper.class, ShiftPerformanceAdjustmentMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        builder = @Builder(disableBuilder = true))
public interface ShiftAssignmentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "shift", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "actualMinutes", ignore = true)
    @Mapping(target = "totalOrders", ignore = true)
    @Mapping(target = "totalRevenue", ignore = true)
    @Mapping(target = "bonusAmount", ignore = true)
    @Mapping(target = "penaltyAmount", ignore = true)
    @Mapping(target = "basePayroll", ignore = true)
    @Mapping(target = "adjustmentTotal", ignore = true)
    @Mapping(target = "calculatedPayroll", ignore = true)
    @Mapping(target = "notes", ignore = true)
    @Mapping(target = "attendanceRecords", ignore = true)
    @Mapping(target = "adjustments", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ShiftAssignment toEntity(ShiftAssignmentRequestDTO requestDTO);

    @Mapping(source = "shift", target = "shiftId", qualifiedByName = "shiftId")
    @Mapping(source = "user", target = "userId", qualifiedByName = "userId")
    @Mapping(source = "user", target = "username", qualifiedByName = "username")
    @Mapping(source = "user", target = "fullName", qualifiedByName = "fullName")
    @Mapping(source = "attendanceRecords", target = "attendanceRecords")
    @Mapping(source = "adjustments", target = "adjustments")
    ShiftAssignmentResponseDTO toResponseDTO(ShiftAssignment entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "shift", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "totalOrders", ignore = true)
    @Mapping(target = "totalRevenue", ignore = true)
    @Mapping(target = "bonusAmount", ignore = true)
    @Mapping(target = "penaltyAmount", ignore = true)
    @Mapping(target = "basePayroll", ignore = true)
    @Mapping(target = "adjustmentTotal", ignore = true)
    @Mapping(target = "calculatedPayroll", ignore = true)
    @Mapping(target = "actualMinutes", ignore = true)
    void updateEntityFromDto(ShiftAssignmentRequestDTO dto, @MappingTarget ShiftAssignment entity);

    @Named("shiftId")
    default Long mapShiftId(ShiftInstance shift) {
        return shift != null ? shift.getId() : null;
    }

    @Named("userId")
    default Long mapUserId(User user) {
        return user != null ? user.getId() : null;
    }

    @Named("username")
    default String mapUsername(User user) {
        return user != null ? user.getUsername() : null;
    }

    @Named("fullName")
    default String mapFullName(User user) {
        return user != null ? user.getFullName() : null;
    }
}
