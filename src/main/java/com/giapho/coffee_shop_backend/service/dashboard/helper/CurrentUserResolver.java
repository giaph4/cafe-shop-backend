package com.giapho.coffee_shop_backend.service.dashboard.helper;

import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import com.giapho.coffee_shop_backend.exception.dashboard.DashboardUserNotFoundException;
import com.giapho.coffee_shop_backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * Hỗ trợ xác định người dùng hiện tại cho các dashboard phụ thuộc user.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

    private final UserRepository userRepository;

    public Long resolveOrCurrent(@Nullable Long suppliedUserId) {
        if (suppliedUserId != null) {
            return validateUserExists(suppliedUserId);
        }
        return resolveCurrentUserId();
    }

    private Long resolveCurrentUserId() {
        return SecurityUtil.getCurrentUsername()
                .flatMap(userRepository::findByUsername)
                .map(User::getId)
                .orElseThrow(DashboardUserNotFoundException::new);
    }

    private Long validateUserExists(Long userId) {
        return userRepository.findById(userId)
                .map(User::getId)
                .orElseThrow(() -> new DashboardUserNotFoundException(userId));
    }
}
