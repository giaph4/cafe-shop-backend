package com.giapho.coffee_shop_backend.mapper;

import com.giapho.coffee_shop_backend.domain.entity.Customer;
import com.giapho.coffee_shop_backend.dto.CustomerDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    // DTO -> Entity (tạo mới)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "loyaltyPoints", ignore = true) // Điểm bắt đầu từ 0
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Customer toEntity(CustomerDTO dto);

    // Entity -> DTO (hiển thị)
    CustomerDTO toDto(Customer entity);

    // Cập nhật Entity từ DTO
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "loyaltyPoints", ignore = true) // Không cho cập nhật điểm trực tiếp qua API này
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true) // Sẽ được @PreUpdate xử lý
    void updateEntityFromDto(CustomerDTO dto, @MappingTarget Customer entity);
}