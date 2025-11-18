package com.giapho.coffee_shop_backend.mapper;

import com.giapho.coffee_shop_backend.domain.entity.Role; // Import Role
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.dto.UserResponseDTO;
import com.giapho.coffee_shop_backend.dto.UserUpdateRequestDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors; // Import Collectors

@Mapper(componentModel = "spring", uses = {RoleMapper.class})
public interface UserMapper {

    // Entity -> ResponseDTO
    @Mapping(source = "roles", target = "roles")
    UserResponseDTO toUserResponseDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    @Mapping(target = "roles", ignore = true) // Roles are handled manually in the service // Map ID -> Set<Role>
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "statusMessage", ignore = true)
    @Mapping(target = "lastSeenAt", ignore = true)

    void updateUserFromDto(UserUpdateRequestDTO dto, @MappingTarget User user);

    @Named("roleIdsToRoleSet")
    default Set<Role> roleIdsToRoleSet(Set<Long> roleIds) {
        if (roleIds == null) {
            return null;
        }
        return roleIds.stream()
                .map(id -> {
                    Role role = new Role();
                    role.setId(id);
                    return role;
                })
                .collect(Collectors.toSet());
    }
}