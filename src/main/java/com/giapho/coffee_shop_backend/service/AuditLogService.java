package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.domain.entity.AuditLog;
import com.giapho.coffee_shop_backend.domain.entity.User;
import com.giapho.coffee_shop_backend.domain.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void recordAction(String action,
                              String resourceType,
                              String resourceId,
                              boolean success,
                              String summary,
                              String details,
                              String errorMessage) {
        AuditLog log = AuditLog.builder()
                .eventTime(LocalDateTime.now())
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .success(success)
                .summary(summary)
                .details(details)
                .errorMessage(errorMessage)
                .build();

        applySecurityContext(log);
        applyRequestContext(log);

        auditLogRepository.save(log);
    }

    private void applySecurityContext(AuditLog log) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            log.setActorId(user.getId());
            log.setActorUsername(user.getUsername());
        } else if (principal instanceof UserDetails userDetails) {
            log.setActorUsername(userDetails.getUsername());
        } else {
            log.setActorUsername(authentication.getName());
        }

        if (log.getActorRoles() == null && authentication.getAuthorities() != null) {
            String roles = authentication.getAuthorities().stream()
                    .map(grantedAuthority -> grantedAuthority.getAuthority())
                    .sorted()
                    .collect(Collectors.joining(","));
            log.setActorRoles(roles);
        }
    }

    private void applyRequestContext(AuditLog log) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return;
        }

        HttpServletRequest request = servletAttributes.getRequest();
        log.setRequestUri(request.getRequestURI());
        log.setHttpMethod(request.getMethod());
        log.setIpAddress(request.getRemoteAddr());
        log.setUserAgent(request.getHeader("User-Agent"));
    }
}
