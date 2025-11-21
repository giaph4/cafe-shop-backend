package com.giapho.coffee_shop_backend.service.impl;

import com.giapho.coffee_shop_backend.domain.entity.LoginHistory;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.repository.LoginHistoryRepository;
import com.giapho.coffee_shop_backend.domain.repository.UserRepository;
import com.giapho.coffee_shop_backend.dto.LoginHistoryResponseDTO;
import com.giapho.coffee_shop_backend.exception.login.LoginHistoryInvalidDateRangeException;
import com.giapho.coffee_shop_backend.service.LoginHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginHistoryServiceImpl implements LoginHistoryService {

    private static final int MAX_IP_LENGTH = 100;
    private static final int MAX_USER_AGENT_LENGTH = 500;
    private static final int MAX_MESSAGE_LENGTH = 255;
    private static final String SUCCESS_MESSAGE = "Đăng nhập thành công";

    private final LoginHistoryRepository loginHistoryRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void recordLoginAttempt(String username,
                                   boolean success,
                                   String ipAddress,
                                   String userAgent,
                                   String message) {
        String normalizedUsername = normalizeUsername(username);
        User user = resolveUser(normalizedUsername);
        persistLoginAttempt(user, normalizedUsername, success, ipAddress, userAgent, message);
    }

    @Override
    @Transactional
    public void recordLoginAttempt(User user,
                                   boolean success,
                                   String ipAddress,
                                   String userAgent,
                                   String message) {
        String username = Optional.ofNullable(user).map(User::getUsername).orElse(null);
        persistLoginAttempt(user, normalizeUsername(username), success, ipAddress, userAgent, message);
    }

    @Override
    @Transactional
    public void recordSuccessfulLogin(User user,
                                      String ipAddress,
                                      String userAgent) {
        recordLoginAttempt(user, true, ipAddress, userAgent, SUCCESS_MESSAGE);
    }

    @Override
    @Transactional
    public void recordFailedLogin(String username,
                                  String ipAddress,
                                  String userAgent,
                                  String message) {
        recordLoginAttempt(username, false, ipAddress, userAgent, message);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoginHistoryResponseDTO> searchLoginHistory(String username,
                                                            Boolean success,
                                                            LocalDateTime startDate,
                                                            LocalDateTime endDate,
                                                            Pageable pageable) {
        validateDateRange(startDate, endDate);

        Specification<LoginHistory> specification = buildSpecification(username, success, startDate, endDate);

        return loginHistoryRepository.findAll(specification, pageable)
                .map(this::toResponseDto);
    }

    private void persistLoginAttempt(User user,
                                     String username,
                                     boolean success,
                                     String ipAddress,
                                     String userAgent,
                                     String message) {
        LoginHistory history = LoginHistory.builder()
                .user(user)
                .username(username)
                .success(success)
                .ipAddress(safeTrim(ipAddress, MAX_IP_LENGTH))
                .userAgent(safeTrim(userAgent, MAX_USER_AGENT_LENGTH))
                .message(buildMessage(message))
                .build();

        loginHistoryRepository.save(history);
        log.debug("Recorded {} login attempt for user: {}", success ? "successful" : "failed", username);
    }

    private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new LoginHistoryInvalidDateRangeException();
        }
    }

    private Specification<LoginHistory> buildSpecification(String username,
                                                           Boolean success,
                                                           LocalDateTime startDate,
                                                           LocalDateTime endDate) {
        List<Specification<LoginHistory>> specs = new ArrayList<>();

        if (StringUtils.hasText(username)) {
            String keyword = username.trim().toLowerCase();
            specs.add((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), "%" + keyword + "%"));
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

        return specs.stream()
                .reduce(Specification::and)
                .orElse((root, query, criteriaBuilder) -> criteriaBuilder.conjunction());
    }

    private LoginHistoryResponseDTO toResponseDto(LoginHistory history) {
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

    private User resolveUser(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return userRepository.findByUsername(username).orElse(null);
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return username.trim();
    }

    private String safeTrim(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String buildMessage(String message) {
        String defaultMessage = StringUtils.hasText(message) ? message.trim() : SUCCESS_MESSAGE;
        if (defaultMessage.length() > MAX_MESSAGE_LENGTH) {
            return defaultMessage.substring(0, MAX_MESSAGE_LENGTH);
        }
        return defaultMessage;
    }
}
