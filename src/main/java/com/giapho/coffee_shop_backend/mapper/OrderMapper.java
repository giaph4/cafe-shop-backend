package com.giapho.coffee_shop_backend.mapper;

import com.giapho.coffee_shop_backend.domain.entity.CafeTable;
import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.dto.OrderResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {OrderDetailMapper.class}) // Báo MapStruct sử dụng OrderDetailMapper
public interface OrderMapper {

    // Chuyển từ Entity sang Response DTO
    @Mapping(source = "cafeTable", target = "tableName", qualifiedByName = "tableToTableName")
    @Mapping(source = "user", target = "staffUsername", qualifiedByName = "userToUsername")
//    @Mapping(source = "paymentMethod", target = "paymentMethod", qualifiedByName = "userToUsername")
    @Mapping(source = "orderDetails", target = "orderDetails") // Tự động dùng OrderDetailMapper
    OrderResponseDTO entityToResponse(Order order);

    // --- Hàm Helpers ---
    // Lấy tên bàn
    @Named("tableToTableName")
    default String tableToTableName(CafeTable table) {
        return (table != null) ? table.getName() : null;
    }

    // Lấy username nhân viên
    @Named("userToUsername")
    default String userToUsername(User user) {
        return (user != null) ? user.getUsername() : null;
    }
}