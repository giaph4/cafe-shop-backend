package com.giapho.coffee_shop_backend.service.impl;

import com.giapho.coffee_shop_backend.domain.entity.AuditLog;
import com.giapho.coffee_shop_backend.domain.repository.AuditLogRepository;
import com.giapho.coffee_shop_backend.dto.audit.AuditLogRequest;
import com.giapho.coffee_shop_backend.exception.audit.InvalidAuditLogException;
import com.giapho.coffee_shop_backend.service.AuditLogService;
import com.giapho.coffee_shop_backend.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AuditLogServiceImpl implements AuditLogService {

    private static final int MAX_LENGTH_DEFAULT = 500;
    private static final int MAX_LENGTH_RESOURCE_ID = 100;
    private static final int MAX_LENGTH_ACTION = 100;

    private final AuditLogRepository auditLogRepository;

    @Override
    public void recordAction(AuditLogRequest request) {
        validateMandatoryFields(request);
        AuditLog.AuditLogBuilder builder = AuditLog.builder()
                .eventTime(LocalDateTime.now())
                .action(truncate(request.getAction(), MAX_LENGTH_ACTION))
                .resourceType(truncate(request.getResourceType(), MAX_LENGTH_ACTION))
                .resourceId(truncate(request.getResourceId(), MAX_LENGTH_RESOURCE_ID))
                .summary(truncate(request.getSummary(), MAX_LENGTH_DEFAULT))
                .details(truncate(request.getDetails(), MAX_LENGTH_DEFAULT))
                .errorMessage(truncate(request.getErrorMessage(), MAX_LENGTH_DEFAULT))
                .success(Boolean.TRUE.equals(request.getSuccess()));

        applySecurityContext(builder);
        applyRequestContext(builder);

        auditLogRepository.save(builder.build());
    }

    @Override
    public void recordSuccess(AuditLogRequest request) {
        recordAction(ensureSuccessFlag(request, true));
    }

    @Override
    public void recordFailure(AuditLogRequest request) {
        recordAction(ensureSuccessFlag(request, false));
    }

    private void validateMandatoryFields(AuditLogRequest request) {
        if (!StringUtils.hasText(request.getAction())) {
            throw new InvalidAuditLogException("Audit action is required");
        }
        if (!StringUtils.hasText(request.getResourceType())) {
            throw new InvalidAuditLogException("Resource type is required");
        }
    }

    private AuditLogRequest ensureSuccessFlag(AuditLogRequest request, boolean success) {
        return request.toBuilder().success(success).build();
    }

    private void applySecurityContext(AuditLog.AuditLogBuilder builder) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            SecurityUtil.getCurrentUsername().ifPresent(builder::actorUsername);
            return;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof com.giapho.coffee_shop_backend.domain.entity.User user) {
            builder.actorId(user.getId());
            builder.actorUsername(user.getUsername());
        } else if (principal instanceof UserDetails userDetails) {
            builder.actorUsername(userDetails.getUsername());
        } else if (StringUtils.hasText(authentication.getName())) {
            builder.actorUsername(authentication.getName());
        }

        if (authentication.getAuthorities() != null) {
            Set<String> uniqueRoles = authentication.getAuthorities().stream()
                    .map(grantedAuthority -> grantedAuthority.getAuthority())
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toSet());
            if (!uniqueRoles.isEmpty()) {
                builder.actorRoles(String.join(",", uniqueRoles));
            }
        }
    }

    private void applyRequestContext(AuditLog.AuditLogBuilder builder) {
        resolveRequest().ifPresent(request -> {
            builder.requestUri(truncate(request.getRequestURI(), MAX_LENGTH_DEFAULT));
            builder.httpMethod(truncate(request.getMethod(), 10));
            builder.ipAddress(truncate(resolveClientIp(request), 64));
            builder.userAgent(truncate(request.getHeader("User-Agent"), MAX_LENGTH_DEFAULT));
        });
    }

    private Optional<HttpServletRequest> resolveRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return Optional.ofNullable(servletAttributes.getRequest());
        }
        return Optional.empty();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "X-Real-IP",
                "CF-Connecting-IP",
                "Forwarded",
                "Forwarded-For",
                "X-Forwarded",
                "X-Cluster-Client-IP",
                "Client-IP",
                "Fastly-Client-IP",
                "True-Client-IP"
        };

        for (String header : headers) {
            String value = request.getHeader(header);
            if (StringUtils.hasText(value)) {
                return value.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    private String truncate(String value, int max) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        String truncated = trimmed.substring(0, max);
        log.warn("Audit log field truncated to {} characters", max);
        return truncated;
    }
}
