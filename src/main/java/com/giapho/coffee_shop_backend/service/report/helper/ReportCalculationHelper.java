package com.giapho.coffee_shop_backend.service.report.helper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class ReportCalculationHelper {

    private ReportCalculationHelper() {
    }

    public static BigDecimal safeDivide(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP);
    }

    public static BigDecimal defaultZero(BigDecimal value) {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO);
    }
}
