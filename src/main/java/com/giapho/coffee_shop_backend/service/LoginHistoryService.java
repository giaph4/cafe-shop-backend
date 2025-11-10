package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.LoginHistory;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.repository.LoginHistoryRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import com.giapho.coffee_shop_backend.dto.LoginHistoryResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;
    private final UserRepository userRepository;

    /**
     * Records a login attempt into the database.
     *
     * @param username The username of the user who attempted to login. If the username is null, the user is not found.
     * @param success Whether the login attempt was successful or not.
     * @param ipAddress The IP address of the user who attempted to login.
     * @param userAgent The user agent of the user who attempted to login.
     * @param message The message of the login attempt.
     */
    @Transactional
    public void recordLoginAttempt(String username,
                                   boolean success,
                                   String ipAddress,
                                   String userAgent,
                                   String message) {
        User user = null;
        if (StringUtils.hasText(username)) {
            // Try to find the user by the username
            user = userRepository.findByUsername(username).orElse(null);
        }

        LoginHistory loginHistory = LoginHistory.builder()
                // Set the user of the login history if the user is found
                .user(user)
                // Set the username of the login history
                .username(username)
                // Set the success of the login history
                .success(success)
                // Set the IP address of the login history
                .ipAddress(ipAddress)
                // Set the user agent of the login history
                .userAgent(userAgent)
                // Set the message of the login history
                .message(message)
                .build();

        // Save the login history into the database
        loginHistoryRepository.save(loginHistory);
    }

    @Transactional
    public void recordLoginAttempt(User user,
                                   boolean success,
                                   String ipAddress,
                                   String userAgent,
                                   String message) {
        LoginHistory loginHistory = LoginHistory.builder()
                .user(user)
                .username(user != null ? user.getUsername() : null)
                .success(success)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .message(message)
                .build();

        loginHistoryRepository.save(loginHistory);
    }

    @Transactional
    public void recordSuccessfulLogin(User user,
                                      String ipAddress,
                                      String userAgent) {
        recordLoginAttempt(user, true, ipAddress, userAgent, "Login successful");
    }

    @Transactional
    public void recordFailedLogin(String username,
                                  String ipAddress,
                                  String userAgent,
                                  String message) {
        recordLoginAttempt(username, false, ipAddress, userAgent, message);
    }

    @Transactional(readOnly = true)
    public Page<LoginHistoryResponseDTO> searchLoginHistory(String username,
                                                            Boolean success,
                                                            LocalDateTime startDate,
                                                            LocalDateTime endDate,
                                                            Pageable pageable) {
        List<Specification<LoginHistory>> specs = new ArrayList<>();

        if (StringUtils.hasText(username)) {
            specs.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), "%" + username.toLowerCase() + "%"));
        }

        if (success != null) {
            specs.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("success"), success));
        }

        if (startDate != null) {
            specs.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.greaterThanOrEqualTo(root.get("loginAt"), startDate));
        }

        if (endDate != null) {
            specs.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.lessThanOrEqualTo(root.get("loginAt"), endDate));
        }

        Specification<LoginHistory> specification = specs.stream()
                .reduce(Specification::and)
                .orElse((root, query, criteriaBuilder) -> criteriaBuilder.conjunction());

        Page<LoginHistory> histories = loginHistoryRepository.findAll(specification, pageable);
        return histories.map(this::mapToDto);
    }

    private LoginHistoryResponseDTO mapToDto(LoginHistory history) {
        User user = history.getUser();
        return LoginHistoryResponseDTO.builder()
                .id(history.getId())
                .userId(user != null ? user.getId() : null)
                .username(history.getUsername())
                .fullName(user != null ? user.getFullName() : null)
                .email(user != null ? user.getEmail() : null)
                .status(user != null ? user.getStatus() : null)
                .success(history.getSuccess())
                .loginAt(history.getLoginAt())
                .ipAddress(history.getIpAddress())
                .userAgent(history.getUserAgent())
                .message(history.getMessage())
                .build();
    }
}
