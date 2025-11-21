package com.giapho.coffee_shop_backend.dto.audit;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder(toBuilder = true)
@ToString
public class AuditLogRequest {

    private final String action;
    private final String resourceType;
    private final String resourceId;
    private final Boolean success;
    private final String summary;
    private final String details;
    private final String errorMessage;

    public boolean isSuccess() {
        return Boolean.TRUE.equals(success);
    }
}
