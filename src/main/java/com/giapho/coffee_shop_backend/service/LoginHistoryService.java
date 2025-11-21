package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.dto.LoginHistoryResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface LoginHistoryService {

    void recordLoginAttempt(String username,
                            boolean success,
                            String ipAddress,
                            String userAgent,
                            String message);

    void recordLoginAttempt(User user,
                            boolean success,
                            String ipAddress,
                            String userAgent,
                            String message);

    void recordSuccessfulLogin(User user,
                               String ipAddress,
                               String userAgent);

    void recordFailedLogin(String username,
                           String ipAddress,
                           String userAgent,
                           String message);

    Page<LoginHistoryResponseDTO> searchLoginHistory(String username,
                                                     Boolean success,
                                                     LocalDateTime startDate,
                                                     LocalDateTime endDate,
                                                     Pageable pageable);
}
