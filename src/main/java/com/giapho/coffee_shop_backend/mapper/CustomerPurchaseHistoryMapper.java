package com.giapho.coffee_shop_backend.mapper;

import com.giapho.coffee_shop_backend.domain.entity.Order;
import com.giapho.coffee_shop_backend.dto.CustomerPurchaseHistoryItemDTO;
import com.giapho.coffee_shop_backend.dto.CustomerPurchaseHistoryResponseDTO;
import com.giapho.coffee_shop_backend.domain.repository.CustomerPurchaseAggregate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Mapper(componentModel = "spring", uses = {OrderDetailMapper.class})
public interface CustomerPurchaseHistoryMapper {

    @Mappings({
            @Mapping(source = "id", target = "orderId"),
            @Mapping(source = "user.username", target = "staffUsername"),
            @Mapping(source = "cafeTable.name", target = "tableName"),
            @Mapping(source = "orderDetails", target = "orderDetails")
    })
    CustomerPurchaseHistoryItemDTO orderToHistoryItem(Order order);

    default CustomerPurchaseHistoryResponseDTO aggregateToResponse(
            Long customerId,
            String customerName,
            String customerPhone,
            CustomerPurchaseAggregate aggregate,
            List<CustomerPurchaseHistoryItemDTO> orders,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {
        BigDecimal totalAmount = aggregate != null && aggregate.getTotalAmount() != null
                ? aggregate.getTotalAmount()
                : BigDecimal.ZERO;
        long totalOrders = aggregate != null && aggregate.getTotalOrders() != null
                ? aggregate.getTotalOrders()
                : 0L;
        java.time.LocalDateTime lastPurchaseDate = aggregate != null
                ? aggregate.getLastPurchaseDate()
                : null;

        BigDecimal averageOrderValue = totalOrders > 0
                ? totalAmount.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return CustomerPurchaseHistoryResponseDTO.builder()
                .customerId(customerId)
                .customerName(customerName)
                .customerPhone(customerPhone)
                .totalOrders(totalOrders)
                .totalAmount(totalAmount)
                .averageOrderValue(averageOrderValue)
                .orders(orders)
                .lastPurchaseDate(lastPurchaseDate)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .build();
    }
}
