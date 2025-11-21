package com.giapho.coffee_shop_backend.exception.expense;

import com.giapho.coffee_shop_backend.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class ExpenseNotFoundException extends BusinessException {

    public ExpenseNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, String.format("Expense not found with id: %d", id));
    }

    public ExpenseNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
