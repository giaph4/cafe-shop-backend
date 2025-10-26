package com.giapho.coffee_shop_backend.mapper;

import com.giapho.coffee_shop_backend.domain.entity.Supplier;
import com.giapho.coffee_shop_backend.dto.SupplierDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SupplierMapper {

    // DTO -> Entity (tạo mới)
    @Mapping(target = "id", ignore = true)
    Supplier toEntity(SupplierDTO dto);

    // Entity -> DTO (hiển thị)
    SupplierDTO toDto(Supplier entity);

    // List<Entity> -> List<DTO>
    List<SupplierDTO> toDtoList(List<Supplier> entities);

    // Cập nhật Entity từ DTO
    @Mapping(target = "id", ignore = true)
    void updateEntityFromDto(SupplierDTO dto, @MappingTarget Supplier entity);
}
