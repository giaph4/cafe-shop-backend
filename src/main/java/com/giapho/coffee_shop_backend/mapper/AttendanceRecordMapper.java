package com.giapho.coffee_shop_backend.mapper;

import com.giapho.coffee_shop_backend.domain.entity.AttendanceRecord;
import com.giapho.coffee_shop_backend.dto.shift.AttendanceRecordResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AttendanceRecordMapper {

    @Mapping(target = "assignmentId", source = "assignment.id")
    AttendanceRecordResponseDTO toResponseDTO(AttendanceRecord entity);

    @Mapping(target = "assignment", ignore = true)
    AttendanceRecord toEntity(AttendanceRecordResponseDTO dto);

    @Mapping(target = "assignment", ignore = true)
    void updateEntityFromDto(AttendanceRecordResponseDTO dto, @MappingTarget AttendanceRecord entity);
}
