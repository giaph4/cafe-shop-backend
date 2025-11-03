package com.giapho.coffee_shop_backend.mapper;

import com.giapho.coffee_shop_backend.domain.entity.Role;
import com.giapho.coffee_shop_backend.dto.RoleDTO;
import org.mapstruct.Mapper;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    RoleDTO toDto(Role entity);
    Set<RoleDTO> toDtoSet(Set<Role> entities);

    default Role idToRole(Long id) {
        if (id == null) return null;
        Role role = new Role();
        role.setId(id);
        return role;
    }
}