/**
 * Custom exception hierarchy and global exception handling.
 * 
 * <h2>Exception Architecture:</h2>
 * <pre>
 * RuntimeException
 *     └── BusinessException (base for all business exceptions)
 *         ├── Product exceptions (product/)
 *         ├── Order exceptions (order/)
 *         ├── User exceptions (user/)
 *         ├── Authentication exceptions (authentication/)
 *         └── ... (86 custom exceptions total)
 * </pre>
 * 
 * <h2>Key Components:</h2>
 * <ul>
 *   <li>{@link com.giapho.coffee_shop_backend.exception.BusinessException} - Base exception with HTTP status</li>
 *   <li>{@link com.giapho.coffee_shop_backend.exception.GlobalExceptionHandler} - Centralized exception handler</li>
 *   <li>{@link com.giapho.coffee_shop_backend.exception.ErrorResponse} - Standardized error response</li>
 * </ul>
 * 
 * <h2>Exception Organization:</h2>
 * <p>Exceptions are organized in subpackages by domain:</p>
 * <ul>
 *   <li>{@code authentication/} - Authentication & authorization exceptions</li>
 *   <li>{@code user/} - User management exceptions</li>
 *   <li>{@code product/} - Product-related exceptions</li>
 *   <li>{@code order/} - Order processing exceptions</li>
 *   <li>{@code shift/} - Shift management exceptions</li>
 *   <li>And 15+ more domain-specific packages</li>
 * </ul>
 * 
 * <h2>Usage:</h2>
 * <pre>
 * throw new ProductNotFoundException(productId);
 * // Automatically handled by GlobalExceptionHandler
 * // Returns appropriate HTTP status and error response
 * </pre>
 * 
 * @since 1.0.0
 */
package com.giapho.coffee_shop_backend.exception;

