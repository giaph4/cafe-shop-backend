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

@Mapper(componentModel = "spring", uses = {RoleMapper.class}) // Sử dụng RoleMapper
public interface UserMapper {

    // Entity -> ResponseDTO
    @Mapping(source = "roles", target = "roles") // Tự động dùng RoleMapper.toDtoSet
    UserResponseDTO toUserResponseDto(User user);

    // UpdateRequestDTO -> Entity (Cập nhật)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true) // Không cập nhật username
    @Mapping(target = "password", ignore = true) // Không cập nhật password
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "authorities", ignore = true)
    @Mapping(source = "roleIds", target = "roles", qualifiedByName = "roleIdsToRoleSet") // Map ID -> Set<Role>

    void updateUserFromDto(UserUpdateRequestDTO dto, @MappingTarget User user);

    // Helper map từ Set<Long> sang Set<Role>
    @Named("roleIdsToRoleSet")
    default Set<Role> roleIdsToRoleSet(Set<Long> roleIds) {
        if (roleIds == null) {
            return null; // Hoặc trả về Set rỗng tùy logic
        }
        // Tạo Set<Role> chứa các proxy Role chỉ có ID
        return roleIds.stream()
                .map(id -> {
                    Role role = new Role();
                    role.setId(id);
                    return role;
                })
                .collect(Collectors.toSet());
    }
}