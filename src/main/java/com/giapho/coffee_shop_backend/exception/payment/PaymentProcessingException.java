package com.giapho.coffee_shop_backend.exception.payment;

import com.giapho.coffee_shop_backend.exception.BaseException;
import org.springframework.http.HttpStatus;

/**
 * Ngoại lệ xảy ra khi có lỗi trong quá trình xử lý thanh toán
 */
public class PaymentProcessingException extends BaseException {

    public PaymentProcessingException(String message) {
        super(HttpStatus.BAD_REQUEST, "PAYMENT_ERROR", message);
    }

    public PaymentProcessingException(String message, Throwable cause) {
        super(HttpStatus.BAD_REQUEST, "PAYMENT_ERROR", message, cause);
    }

    public PaymentProcessingException(String errorCode, String message) {
        super(HttpStatus.BAD_REQUEST, errorCode, message);
    }

    public PaymentProcessingException(HttpStatus status, String errorCode, String message) {
        super(status, errorCode, message);
    }
}
