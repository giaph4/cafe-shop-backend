package com.giapho.coffee_shop_backend.service.dashboard.helper;

import com.giapho.coffee_shop_backend.domain.enums.DashboardRange;
import com.giapho.coffee_shop_backend.util.DateRange;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Chuẩn hóa logic xác định khoảng thời gian hiệu lực cho dashboard.
 */
@Component
public class DashboardDateResolver {

    public DateRange resolve(@Nullable DashboardRange range,
                             @Nullable LocalDate from,
                             @Nullable LocalDate to) {
        return DateRange.from(range, from, to);
    }
}
