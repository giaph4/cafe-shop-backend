package com.giapho.coffee_shop_backend.domain.valueobject;

import com.giapho.coffee_shop_backend.util.MoneyUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

/**
 * Đại diện cho một giá trị tiền tệ bất biến (immutable).
 * Hỗ trợ các phép toán số học cơ bản và làm tròn chính xác.
 */
@Getter
@EqualsAndHashCode
public final class Money {
    private static final Currency DEFAULT_CURRENCY = Currency.getInstance("VND");
    private static final int DEFAULT_SCALE = 0; // 0 decimal places for VND
    private static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;

    private final BigDecimal amount;
    private final Currency currency;

    private Money(BigDecimal amount, Currency currency) {
        Assert.notNull(amount, "Số tiền không được để trống");
        Assert.notNull(currency, "Đơn vị tiền tệ không được để trống");
        
        this.amount = amount.setScale(currency.getDefaultFractionDigits(), DEFAULT_ROUNDING);
        this.currency = currency;
    }

    /**
     * Tạo đối tượng Money từ số tiền (mặc định VND)
     */
    public static Money ofVND(BigDecimal amount) {
        return new Money(amount, DEFAULT_CURRENCY);
    }

    /**
     * Tạo đối tượng Money từ số tiền và đơn vị tiền tệ
     */
    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    /**
     * Tạo đối tượng Money từ số tiền (long) - tiện cho VND
     */
    public static Money ofVND(long amount) {
        return new Money(BigDecimal.valueOf(amount), DEFAULT_CURRENCY);
    }

    /**
     * Cộng hai số tiền (cùng đơn vị tiền tệ)
     */
    public Money add(Money other) {
        checkSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    /**
     * Trừ hai số tiền (cùng đơn vị tiền tệ)
     */
    public Money subtract(Money other) {
        checkSameCurrency(other);
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    /**
     * Nhân với một hệ số
     */
    public Money multiply(BigDecimal multiplier) {
        return new Money(amount.multiply(multiplier), currency);
    }

    /**
     * Chia cho một hệ số
     */
    public Money divide(BigDecimal divisor) {
        return new Money(amount.divide(divisor, DEFAULT_SCALE, DEFAULT_ROUNDING), currency);
    }

    /**
     * Kiểm tra số tiền có dương không
     */
    public boolean isPositive() {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Kiểm tra số tiền có âm không
     */
    public boolean isNegative() {
        return amount.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * Kiểm tra số tiền bằng 0
     */
    public boolean isZero() {
        return amount.compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * Làm tròn số tiền
     */
    public Money round(int scale) {
        return new Money(amount.setScale(scale, DEFAULT_ROUNDING), currency);
    }

    /**
     * Chuyển đổi sang đơn vị tiền tệ khác
     */
    public Money convert(Currency toCurrency, BigDecimal rate) {
        if (this.currency.equals(toCurrency)) {
            return this;
        }
        BigDecimal convertedAmount = this.amount.multiply(rate);
        return new Money(convertedAmount, toCurrency);
    }

    /**
     * Định dạng tiền tệ theo locale
     */
    public String format() {
        return String.format("%,d %s", amount.longValue(), currency.getCurrencyCode());
    }

    private void checkSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("Không thể thực hiện phép toán trên các đơn vị tiền tệ khác nhau: %s và %s",
                    this.currency, other.currency));
        }
    }

    @Override
    public String toString() {
        return format();
    }
}
