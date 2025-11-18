package com.giapho.coffee_shop_backend.mapper;

import com.giapho.coffee_shop_backend.domain.entity.CafeTable;
import com.giapho.coffee_shop_backend.dto.CafeTableRequest;
import com.giapho.coffee_shop_backend.dto.CafeTableResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CafeTableMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    CafeTable requestToEntity(CafeTableRequest request);

    @Mapping(target = "status", expression = "java(entity.getStatus() != null ? entity.getStatus().name() : null)")
    CafeTableResponse entityToResponse(CafeTable entity);

    List<CafeTableResponse> entityListToResponseList(List<CafeTable> tables);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntityFromRequest(CafeTableRequest request, @MappingTarget CafeTable cafeTable);
}
