package com.giapho.coffee_shop_backend.exception;

import com.giapho.coffee_shop_backend.exception.FileStorageException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(
            EntityNotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Entity not found: {} - Path: {}", ex.getMessage(), request.getRequestURI());

        return buildResponseEntity(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = ex.getStatus();
        log.warn("Business rule violated: {} - Path: {}", ex.getMessage(), request.getRequestURI());

        return buildResponseEntity(status, status.getReasonPhrase(), ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        log.warn("Invalid argument: {} - Path: {}", ex.getMessage(), request.getRequestURI());

        return buildResponseEntity(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        log.warn("Validation failed - Path: {}", request.getRequestURI());

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
            log.debug("Field validation error - {}: {}", fieldName, errorMessage);
        });

        return buildResponseEntity(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                "Dữ liệu đầu vào không hợp lệ",
                request,
                fieldErrors
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        log.warn("Access denied - Path: {} - User: {}",
                request.getRequestURI(),
                request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "Anonymous");

        return buildResponseEntity(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                "You do not have permission to access this resource",
                request
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {
        log.warn("Authentication failed - Path: {}", request.getRequestURI());

        return buildResponseEntity(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "Invalid username or password",
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        log.error("Data integrity violation - Path: {}", request.getRequestURI(), ex);

        String message = buildConstraintViolationMessage(ex);

        return buildResponseEntity(HttpStatus.CONFLICT, "Conflict", message, request);
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            TypeMismatchException ex,
            HttpServletRequest request
    ) {
        String parameterName = ex.getPropertyName() != null ? ex.getPropertyName() : "unknown";

        log.warn("Type mismatch - Parameter: {} - Path: {}", parameterName, request.getRequestURI());

        String message = String.format("Invalid value for parameter '%s'. Expected type: %s",
                parameterName,
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        return buildResponseEntity(HttpStatus.BAD_REQUEST, "Bad Request", message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        log.warn("Malformed JSON payload - Path: {}", request.getRequestURI(), ex);

        return buildResponseEntity(
                HttpStatus.BAD_REQUEST,
                "Invalid Payload",
                "Không thể đọc dữ liệu gửi lên. Vui lòng kiểm tra lại định dạng JSON.",
                request
        );
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledException(
            DisabledException ex,
            HttpServletRequest request
    ) {
        log.warn("Authentication blocked due to account status - Path: {}", request.getRequestURI());

        return buildResponseEntity(
                HttpStatus.FORBIDDEN,
                "Account Disabled",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        HttpStatus effectiveStatus = status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;

        if (effectiveStatus.is5xxServerError()) {
            log.error("ResponseStatusException triggered 5xx - Path: {}", request.getRequestURI(), ex);
        } else if (effectiveStatus.is4xxClientError()) {
            log.warn("ResponseStatusException triggered 4xx - Path: {}", request.getRequestURI(), ex);
        }

        return buildResponseEntity(
                effectiveStatus,
                effectiveStatus.getReasonPhrase(),
                ex.getReason(),
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception - Path: {}", request.getRequestURI(), ex);

        return buildResponseEntity(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later or contact support.",
                request
        );
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorageException(
            FileStorageException ex,
            HttpServletRequest request
    ) {
        log.error("File storage error - Path: {}", request.getRequestURI(), ex);

        return buildResponseEntity(
                HttpStatus.BAD_REQUEST,
                "File Storage Error",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request
    ) {
        log.warn("File size exceeded - Path: {}", request.getRequestURI());

        return buildResponseEntity(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "File Too Large",
                "File size exceeds the maximum allowed limit",
                request
        );
    }

    private ResponseEntity<ErrorResponse> buildResponseEntity(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request
    ) {
        return buildResponseEntity(status, error, message, request, null);
    }

    private ResponseEntity<ErrorResponse> buildResponseEntity(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors
    ) {
        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                error,
                message,
                request.getRequestURI(),
                fieldErrors
        );
        return new ResponseEntity<>(errorResponse, status);
    }

    private String buildConstraintViolationMessage(DataIntegrityViolationException ex) {
        Throwable rootCause = ex.getRootCause();

        if (rootCause instanceof java.sql.SQLIntegrityConstraintViolationException sqlEx) {
            return mapSqlIntegrityViolation(sqlEx);
        }

        if (rootCause instanceof ConstraintViolationException constraintEx) {
            String constraintName = constraintEx.getConstraintName();
            if (constraintName != null) {
                String lower = constraintName.toLowerCase();
                if (lower.contains("fk") || lower.contains("foreign")) {
                    return "Foreign key constraint violation. The record is referenced by other data.";
                }
                if (lower.contains("unique") || lower.contains("uk")) {
                    return "Duplicate data violates a unique constraint.";
                }
            }
            return "Database constraint violation: " + constraintEx.getMessage();
        }

        return "Database constraint violation. Operation violates database constraints.";
    }

    private String mapSqlIntegrityViolation(java.sql.SQLIntegrityConstraintViolationException sqlEx) {
        String sqlState = sqlEx.getSQLState();
        int errorCode = sqlEx.getErrorCode();

        if (errorCode == 1062 || "23505".equals(sqlState)) {
            return "Duplicate data violates a unique constraint.";
        }
        if (errorCode == 1451 || errorCode == 1452 || "23503".equals(sqlState)) {
            return "Foreign key constraint violation. The record is referenced by other data.";
        }

        return "Database constraint violation. Operation violates database constraints.";
    }
}