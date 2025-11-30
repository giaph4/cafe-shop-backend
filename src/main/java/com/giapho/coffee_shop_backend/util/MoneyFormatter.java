package com.giapho.coffee_shop_backend.util;

import com.giapho.coffee_shop_backend.domain.valueobject.Money;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/**
 * Tiện ích định dạng tiền tệ theo locale
 */
@Component
public class MoneyFormatter {

    /**
     * Định dạng số tiền theo locale hiện tại
     */
    public String format(BigDecimal amount, String currencyCode) {
        return format(amount, Currency.getInstance(currencyCode), LocaleContextHolder.getLocale());
    }

    /**
     * Định dạng số tiền theo locale chỉ định
     */
    public String format(BigDecimal amount, Currency currency, Locale locale) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
        formatter.setCurrency(currency);
        return formatter.format(amount);
    }

    /**
     * Định dạng đối tượng Money theo locale hiện tại
     */
    public String format(Money money) {
        return format(money.getAmount(), money.getCurrency(), LocaleContextHolder.getLocale());
    }

    /**
     * Định dạng số tiền VND (mặc định không có số thập phân)
     */
    public String formatVND(long amount) {
        return formatVND(BigDecimal.valueOf(amount));
    }

    /**
     * Định dạng số tiền VND (mặc định không có số thập phân)
     */
    public String formatVND(BigDecimal amount) {
        Locale vnLocale = new Locale("vi", "VN");
        NumberFormat formatter = NumberFormat.getCurrencyInstance(vnLocale);
        formatter.setMaximumFractionDigits(0);
        formatter.setCurrency(Currency.getInstance("VND"));
        return formatter.format(amount);
    }

    /**
     * Chuyển đổi chuỗi đã định dạng về số tiền
     */
    public BigDecimal parse(String formattedValue, Locale locale) throws NumberFormatException {
        NumberFormat formatter = NumberFormat.getNumberInstance(locale);
        try {
            return new BigDecimal(formatter.parse(formattedValue).toString());
        } catch (Exception e) {
            throw new NumberFormatException("Định dạng số không hợp lệ: " + formattedValue);
        }
    }
}
