package com.giapho.coffee_shop_backend.mapper;

import com.giapho.coffee_shop_backend.domain.entity.Voucher;
import com.giapho.coffee_shop_backend.dto.VoucherCheckResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VoucherMapper {
    VoucherCheckResponseDTO toDTO(Voucher voucher);
    Voucher toEntity(VoucherCheckResponseDTO voucherDTO);
    // Thêm các mapping cho Create/Update DTOs nếu cần
}