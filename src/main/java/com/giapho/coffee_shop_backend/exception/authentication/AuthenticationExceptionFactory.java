package com.giapho.coffee_shop_backend.exception.authentication;

import com.giapho.coffee_shop_backend.exception.user.UserEmailAlreadyExistsException;
import com.giapho.coffee_shop_backend.exception.user.UserUsernameAlreadyExistsException;

public final class AuthenticationExceptionFactory {

    private AuthenticationExceptionFactory() {
    }

    public static AuthenticationValidationException missingCredentials() {
        return new AuthenticationValidationException("Username or password must not be blank");
    }

    public static AuthenticationValidationException invalidPasswordFormat() {
        return new AuthenticationValidationException("Password must be 8-64 characters and include upper, lower, digit, and special characters");
    }

    public static AuthenticationValidationException invalidEmailFormat() {
        return new AuthenticationValidationException("Email is invalid");
    }

    public static AuthenticationValidationException invalidPhoneFormat() {
        return new AuthenticationValidationException("Phone number is invalid");
    }

    public static AuthenticationValidationException missingField(String fieldName) {
        return new AuthenticationValidationException(String.format("%s must not be blank", fieldName));
    }

    public static UserUsernameAlreadyExistsException usernameConflict(String username) {
        return new UserUsernameAlreadyExistsException(username);
    }

    public static UserEmailAlreadyExistsException emailConflict(String email) {
        return new UserEmailAlreadyExistsException(email);
    }

    public static AuthenticationFailedException authenticationFailed(String message) {
        return new AuthenticationFailedException(message);
    }

    public static AuthenticationAccountDisabledException accountDisabled(String message) {
        return new AuthenticationAccountDisabledException(message);
    }
}
