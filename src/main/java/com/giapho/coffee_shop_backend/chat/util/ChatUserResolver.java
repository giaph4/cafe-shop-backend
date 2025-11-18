package com.giapho.coffee_shop_backend.chat.util;

import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import com.giapho.coffee_shop_backend.exception.user.UserNotFoundException;
import com.giapho.coffee_shop_backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatUserResolver {

    private final UserRepository userRepository;

    public User requireCurrentUser() {
        String username = SecurityUtil.getCurrentUsername()
                .orElseThrow(() -> new UserNotFoundException("anonymous"));
        return userRepository.findWithRolesByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    public User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
