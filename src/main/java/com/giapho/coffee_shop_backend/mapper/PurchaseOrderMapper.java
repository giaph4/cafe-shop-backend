package com.giapho.coffee_shop_backend.mapper;

import com.giapho.coffee_shop_backend.domain.entity.PurchaseOrder;
import com.giapho.coffee_shop_backend.domain.entity.Supplier;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.dto.PurchaseOrderResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {PurchaseOrderDetailMapper.class})
public interface PurchaseOrderMapper {

    @Mapping(source = "supplier.id", target = "supplierId")
    @Mapping(source = "supplier.name", target = "supplierName")
    @Mapping(source = "user", target = "staffUsername", qualifiedByName = "userToUsername")
    @Mapping(source = "purchaseOrderDetails", target = "purchaseOrderDetails")
    PurchaseOrderResponseDTO entityToResponse(PurchaseOrder entity);

    @Named("userToUsername")
    default String userToUsername(User user) {
        return (user != null) ? user.getUsername() : null;
    }
}