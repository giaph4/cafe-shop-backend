package com.giapho.coffee_shop_backend.mapper;

import com.giapho.coffee_shop_backend.domain.entity.ShiftInstance;
import com.giapho.coffee_shop_backend.domain.entity.ShiftTemplate;
import com.giapho.coffee_shop_backend.dto.shift.ShiftInstanceCreateRequestDTO;
import com.giapho.coffee_shop_backend.dto.shift.ShiftInstanceResponseDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", uses = {ShiftAssignmentMapper.class}, builder = @Builder(disableBuilder = true))
public interface ShiftInstanceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "template", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "lockedAt", ignore = true)
    @Mapping(target = "assignments", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ShiftInstance toEntity(ShiftInstanceCreateRequestDTO requestDTO);

    @Mapping(source = "template", target = "templateId", qualifiedByName = "mapTemplateId")
    @Mapping(source = "template", target = "templateName", qualifiedByName = "mapTemplateName")
    ShiftInstanceResponseDTO toResponseDTO(ShiftInstance entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "template", ignore = true)
    @Mapping(target = "assignments", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "lockedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFromDto(ShiftInstanceCreateRequestDTO requestDTO, @MappingTarget ShiftInstance entity);

    @Named("mapTemplateId")
    default Long mapTemplateId(ShiftTemplate template) {
        return template != null ? template.getId() : null;
    }

    @Named("mapTemplateName")
    default String mapTemplateName(ShiftTemplate template) {
        return template != null ? template.getName() : null;
    }
}
