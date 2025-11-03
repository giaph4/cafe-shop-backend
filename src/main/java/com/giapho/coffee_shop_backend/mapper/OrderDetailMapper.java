package com.giapho.coffee_shop_backend.mapper;

import com.giapho.coffee_shop_backend.domain.entity.OrderDetail;
import com.giapho.coffee_shop_backend.domain.entity.Product;
import com.giapho.coffee_shop_backend.dto.OrderDetailRequestDTO;
import com.giapho.coffee_shop_backend.dto.OrderDetailResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface OrderDetailMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    @Mapping(source = "productId", target = "product", qualifiedByName = "productIdToProduct")
    @Mapping(target = "priceAtOrder", ignore = true)
    OrderDetail requestToEntity(OrderDetailRequestDTO dto);

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    OrderDetailResponseDTO entityToResponse(OrderDetail entity);

    Set<OrderDetailResponseDTO> entitySetToResponseSet(Set<OrderDetail> details);

    @Named("productIdToProduct")
    default Product productIdToProduct(Long productId) {
        if (productId == null) {
            return null;
        }
        Product product = new Product();
        product.setId(productId);
        return product;
    }
}
