package com.giapho.coffee_shop_backend.mapper;

import com.giapho.coffee_shop_backend.domain.entity.Expense;
import com.giapho.coffee_shop_backend.domain.entity.User; // Import User
import com.giapho.coffee_shop_backend.dto.ExpenseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named; // Import Named

@Mapper(componentModel = "spring")
public interface ExpenseMapper {

    // DTO -> Entity (tạo mới)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true) // User sẽ gán thủ công trong Service
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Expense toEntity(ExpenseDTO dto);

    // Entity -> DTO (hiển thị)
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "username")
    ExpenseDTO toDto(Expense entity);

    // Cập nhật Entity từ DTO
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true) // Không cho đổi người tạo
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true) // Sẽ được @PreUpdate xử lý
    void updateEntityFromDto(ExpenseDTO dto, @MappingTarget Expense entity);

    // Helper (có thể cần nếu UserMapper chưa có)
    @Named("userIdToUser")
    default User userIdToUser(Long userId) {
        if (userId == null) return null;
        User user = new User();
        user.setId(userId);
        return user;
    }
}