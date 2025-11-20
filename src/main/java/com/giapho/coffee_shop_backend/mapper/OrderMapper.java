package com.giapho.coffee_shop_backend.mapper;

import com.giapho.coffee_shop_backend.domain.entity.CafeTable;
import com.giapho.coffee_shop_backend.domain.entity.Customer;
import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.repository.projection.OrderSummaryProjection;
import com.giapho.coffee_shop_backend.dto.OrderResponseDTO;
import com.giapho.coffee_shop_backend.dto.OrderSummaryDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {OrderDetailMapper.class})
public interface OrderMapper {

    @Mapping(source = "cafeTable", target = "tableName", qualifiedByName = "tableToTableName")
    @Mapping(source = "user", target = "staffUsername", qualifiedByName = "userToUsername")
    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer", target = "customerName", qualifiedByName = "customerToName")
    @Mapping(source = "customer.phone", target = "customerPhone")
    @Mapping(source = "orderDetails", target = "orderDetails")
    @Mapping(target = "type", expression = "java(orderTypeToString(order.getType()))")
    @Mapping(target = "status", expression = "java(orderStatusToString(order.getStatus()))")
    OrderResponseDTO entityToResponse(Order order);

    OrderSummaryDTO toSummary(OrderSummaryProjection projection);

    @Named("tableToTableName")
    default String tableToTableName(CafeTable table) {
        return (table != null) ? table.getName() : null;
    }

    @Named("userToUsername")
    default String userToUsername(User user) {
        return (user != null) ? user.getUsername() : null;
    }

    @Named("customerToName")
    default String customerToName(Customer customer) {
        return (customer != null) ? customer.getFullName() : null;
    }

    default String orderTypeToString(com.giapho.coffee_shop_backend.domain.enums.OrderType type) {
        return type != null ? type.name() : null;
    }

    default String orderStatusToString(com.giapho.coffee_shop_backend.domain.enums.OrderStatus status) {
        return status != null ? status.name() : null;
    }
}