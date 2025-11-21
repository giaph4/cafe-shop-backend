package com.giapho.coffee_shop_backend.exception.expense;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

public class InvalidExpenseDateRangeException extends BusinessException {

    public InvalidExpenseDateRangeException(LocalDate startDate, LocalDate endDate) {
        super(HttpStatus.BAD_REQUEST, buildMessage(startDate, endDate));
    }

    private static String buildMessage(LocalDate startDate, LocalDate endDate) {
        return String.format("Start date %s must be before or equal to end date %s", startDate, endDate);
    }
}
