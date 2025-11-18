package com.giapho.coffee_shop_backend.service.impl;

import com.giapho.coffee_shop_backend.domain.enums.DashboardRange;
import com.giapho.coffee_shop_backend.dto.dashboard.AdminDashboardDTO;
import com.giapho.coffee_shop_backend.dto.dashboard.ManagerDashboardDTO;
import com.giapho.coffee_shop_backend.dto.dashboard.StaffDashboardDTO;
import com.giapho.coffee_shop_backend.service.RoleDashboardService;
import com.giapho.coffee_shop_backend.service.dashboard.helper.CurrentUserResolver;
import com.giapho.coffee_shop_backend.service.dashboard.helper.DashboardDateResolver;
import com.giapho.coffee_shop_backend.service.dashboard.provider.AdminDashboardProvider;
import com.giapho.coffee_shop_backend.service.dashboard.provider.ManagerDashboardProvider;
import com.giapho.coffee_shop_backend.service.dashboard.provider.StaffDashboardProvider;
import com.giapho.coffee_shop_backend.util.DateRange;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@CacheConfig(cacheNames = {
        "adminDashboard",
        "managerDashboard",
        "staffDashboard"
})
public class RoleDashboardServiceImpl implements RoleDashboardService {

    private final DashboardDateResolver dateResolver;
    private final CurrentUserResolver currentUserResolver;
    private final AdminDashboardProvider adminDashboardProvider;
    private final ManagerDashboardProvider managerDashboardProvider;
    private final StaffDashboardProvider staffDashboardProvider;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "adminDashboard", key = "'admin:' + #range + ':' + #from + ':' + #to")
    public AdminDashboardDTO buildAdminDashboard(@Nullable DashboardRange range,
                                                 @Nullable LocalDate from,
                                                 @Nullable LocalDate to) {
        DateRange effectiveRange = dateResolver.resolve(range, from, to);
        return adminDashboardProvider.build(effectiveRange);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "managerDashboard", key = "'manager:' + #range + ':' + #from + ':' + #to")
    public ManagerDashboardDTO buildManagerDashboard(@Nullable DashboardRange range,
                                                     @Nullable LocalDate from,
                                                     @Nullable LocalDate to) {
        DateRange effectiveRange = dateResolver.resolve(range, from, to);
        return managerDashboardProvider.build(effectiveRange);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "staffDashboard",
            key = "(#userId != null ? 'staff:' + #userId : 'current') + ':' + #range + ':' + #from + ':' + #to")
    public StaffDashboardDTO buildStaffDashboard(@Nullable Long userId,
                                                 @Nullable DashboardRange range,
                                                 @Nullable LocalDate from,
                                                 @Nullable LocalDate to) {
        DateRange effectiveRange = dateResolver.resolve(range, from, to);
        Long resolvedUserId = currentUserResolver.resolveOrCurrent(userId);
        return staffDashboardProvider.build(resolvedUserId, effectiveRange);
    }

    @Override
    @CacheEvict(cacheNames = {
            "adminDashboard",
            "managerDashboard",
            "staffDashboard"
    }, allEntries = true)
    public void evictAllCaches() {
        // No-op: annotation handles eviction.
    }
}
