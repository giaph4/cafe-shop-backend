package com.giapho.coffee_shop_backend.service;

import com.giapho.coffee_shop_backend.dto.audit.AuditLogRequest;

public interface AuditLogService {

    void recordAction(AuditLogRequest request);

    void recordSuccess(AuditLogRequest request);

    void recordFailure(AuditLogRequest request);
}
