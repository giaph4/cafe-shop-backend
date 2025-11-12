package com.giapho.coffee_shop_backend.mapper;

import com.giapho.coffee_shop_backend.domain.entity.Voucher;
import com.giapho.coffee_shop_backend.dto.VoucherCheckResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VoucherMapper {
    @Mappings({
            @Mapping(target = "message", ignore = true),
            @Mapping(target = "discountAmount", source = "discountValue"),
            @Mapping(target = "isValid", expression = "java(voucher != null && voucher.isActive())")
    })
    VoucherCheckResponseDTO toDTO(Voucher voucher);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "description", ignore = true),
            @Mapping(target = "discountValue", source = "discountAmount"),
            @Mapping(target = "minimumOrderAmount", ignore = true),
            @Mapping(target = "maximumDiscountAmount", ignore = true),
            @Mapping(target = "validFrom", ignore = true),
            @Mapping(target = "validTo", ignore = true),
            @Mapping(target = "usageLimit", ignore = true),
            @Mapping(target = "timesUsed", ignore = true),
            @Mapping(target = "active", expression = "java(Boolean.valueOf(voucherDTO.isValid()))"),
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "updatedAt", ignore = true)
    })
    Voucher toEntity(VoucherCheckResponseDTO voucherDTO);
}