package com.giapho.coffee_shop_backend.mapper;

import com.giapho.coffee_shop_backend.domain.entity.ShiftSession;
import com.giapho.coffee_shop_backend.dto.shift.ShiftSessionResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShiftSessionMapper {

    @Mapping(source = "workShift.id", target = "workShiftId")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.fullName", target = "fullName")
    @Mapping(source = "forceBy.id", target = "forceByUserId")
    ShiftSessionResponseDTO toResponse(ShiftSession session);
}
