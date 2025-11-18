package com.giapho.coffee_shop_backend.util;

import com.giapho.coffee_shop_backend.domain.enums.DashboardRange;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class DateRange {

    private final LocalDate start;
    private final LocalDate end;

    public static DateRange from(DashboardRange range, LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        if (Objects.nonNull(from) || Objects.nonNull(to)) {
            LocalDate effectiveTo = Objects.requireNonNullElse(to, today);
            LocalDate effectiveFrom = Objects.requireNonNullElse(from, effectiveTo.minusDays(30));
            if (effectiveFrom.isAfter(effectiveTo)) {
                throw new IllegalArgumentException("Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc");
            }
            return new DateRange(effectiveFrom, effectiveTo);
        }

        DashboardRange effectiveRange = Objects.requireNonNullElse(range, DashboardRange.LAST_30_DAYS);
        return switch (effectiveRange) {
            case TODAY -> new DateRange(today, today);
            case WEEK -> new DateRange(today.with(DayOfWeek.MONDAY), today);
            case MONTH -> new DateRange(today.withDayOfMonth(1), today);
            case LAST_30_DAYS -> new DateRange(today.minusDays(29), today);
            case CUSTOM -> throw new IllegalArgumentException("Phải truyền from/to khi chọn CUSTOM range");
        };
    }

    public LocalDate monthStart() {
        return end.withDayOfMonth(1);
    }

    public LocalDate yearStart() {
        return end.withDayOfYear(1);
    }

    @Override
    public String toString() {
        return start + "_" + end;
    }
}
