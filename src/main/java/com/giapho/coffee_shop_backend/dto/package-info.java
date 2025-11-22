/**
 * Data Transfer Objects (DTOs) for API request/response handling.
 * 
 * <h2>DTO Organization by Domain:</h2>
 * 
 * <h3>🔐 Authentication & User:</h3>
 * <ul>
 *   <li><b>LoginRequest</b> - Login credentials</li>
 *   <li><b>RegisterRequest</b> - User registration</li>
 *   <li><b>AuthenticationResponse</b> - JWT token response</li>
 *   <li><b>UserResponseDTO</b> - User profile data</li>
 *   <li><b>UserUpdateRequestDTO</b> - User update data</li>
 *   <li><b>ChangePasswordRequestDTO</b> - Password change</li>
 *   <li><b>RoleDTO</b> - Role information</li>
 * </ul>
 * 
 * <h3>🛍️ Product Management:</h3>
 * <ul>
 *   <li><b>ProductRequest</b> - Product creation/update</li>
 *   <li><b>ProductResponse</b> - Product details</li>
 *   <li><b>ProductRecipeDTO</b> - Product recipe</li>
 *   <li><b>ProductIngredientDTO</b> - Product ingredients</li>
 *   <li><b>ProductSalesSummaryDTO</b> - Sales statistics</li>
 * </ul>
 * <p>See {@code category/} package for category DTOs.</p>
 * 
 * <h3>📋 Order Management:</h3>
 * <ul>
 *   <li><b>OrderCreateRequestDTO</b> - New order creation</li>
 *   <li><b>OrderResponseDTO</b> - Complete order details</li>
 *   <li><b>OrderSummaryDTO</b> - Order summary view</li>
 *   <li><b>OrderDetailRequestDTO</b> - Order item addition</li>
 *   <li><b>OrderDetailResponseDTO</b> - Order item details</li>
 *   <li><b>OrderDetailUpdateRequestDTO</b> - Order item update</li>
 *   <li><b>PaymentRequestDTO</b> - Payment processing</li>
 * </ul>
 * 
 * <h3>🪑 Table Management:</h3>
 * <ul>
 *   <li><b>CafeTableRequest</b> - Table creation/update</li>
 *   <li><b>CafeTableResponse</b> - Table details</li>
 *   <li><b>CafeTableStatusUpdateRequest</b> - Table status change</li>
 * </ul>
 * 
 * <h3>👥 Customer Management:</h3>
 * <ul>
 *   <li><b>CustomerDTO</b> - Customer profile</li>
 *   <li><b>CustomerAnalyticsDTO</b> - Customer analytics</li>
 *   <li><b>CustomerPurchaseHistoryItemDTO</b> - Purchase history item</li>
 *   <li><b>CustomerPurchaseHistoryResponseDTO</b> - Full purchase history</li>
 * </ul>
 * 
 * <h3>🎫 Voucher & Promotions:</h3>
 * <ul>
 *   <li><b>VoucherRequestDTO</b> - Voucher creation/update</li>
 *   <li><b>VoucherResponseDTO</b> - Voucher details</li>
 *   <li><b>VoucherSummaryDTO</b> - Voucher summary</li>
 *   <li><b>VoucherApplyRequestDTO</b> - Apply voucher to order</li>
 *   <li><b>VoucherCheckResponseDTO</b> - Voucher validation result</li>
 * </ul>
 * 
 * <h3>📦 Inventory Management:</h3>
 * <ul>
 *   <li><b>IngredientRequestDTO</b> - Ingredient creation/update</li>
 *   <li><b>IngredientResponseDTO</b> - Ingredient details</li>
 *   <li><b>InventoryAdjustmentRequestDTO</b> - Inventory adjustment</li>
 *   <li><b>SupplierDTO</b> - Supplier information</li>
 *   <li><b>PurchaseOrderRequestDTO</b> - Purchase order creation</li>
 *   <li><b>PurchaseOrderResponseDTO</b> - Purchase order details</li>
 *   <li><b>PurchaseOrderDetailRequestDTO</b> - PO item</li>
 *   <li><b>PurchaseOrderDetailResponseDTO</b> - PO item details</li>
 * </ul>
 * 
 * <h3>💰 Expense Management:</h3>
 * <ul>
 *   <li><b>ExpenseDTO</b> - Expense record</li>
 * </ul>
 * 
 * <h3>⏰ Shift Management:</h3>
 * <p>See {@code shift/} package (24 DTOs) for comprehensive shift management DTOs.</p>
 * 
 * <h3>📊 Analytics & Reporting:</h3>
 * <p>See {@code analytics/} and {@code dashboard/} packages for:</p>
 * <ul>
 *   <li>Dashboard metrics</li>
 *   <li>Sales analytics</li>
 *   <li>Performance statistics</li>
 *   <li>Best sellers</li>
 *   <li>Hourly sales</li>
 * </ul>
 * <p>Standalone analytics DTOs:</p>
 * <ul>
 *   <li><b>BestSellerDTO</b> - Best selling products</li>
 *   <li><b>HourlySalesDTO</b> - Sales by hour</li>
 *   <li><b>PaymentMethodStatsDTO</b> - Payment statistics</li>
 *   <li><b>SalesComparisonDTO</b> - Sales comparison</li>
 *   <li><b>StaffPerformanceDTO</b> - Staff performance metrics</li>
 *   <li><b>CategorySalesDTO</b> - Category sales data</li>
 * </ul>
 * 
 * <h3>📝 Audit & Logging:</h3>
 * <p>See {@code audit/} package for audit trail DTOs.</p>
 * <ul>
 *   <li><b>LoginHistoryResponseDTO</b> - Login history records</li>
 * </ul>
 * 
 * <h3>📎 File Management:</h3>
 * <ul>
 *   <li><b>FileUploadResponse</b> - File upload result</li>
 * </ul>
 * 
 * <h2>DTO Naming Conventions:</h2>
 * <ul>
 *   <li><b>*Request</b> - API request payload</li>
 *   <li><b>*Response</b> - API response payload</li>
 *   <li><b>*DTO</b> - General data transfer object</li>
 *   <li><b>*RequestDTO</b> - Specific request object</li>
 *   <li><b>*ResponseDTO</b> - Specific response object</li>
 * </ul>
 * 
 * <h2>Validation:</h2>
 * <p>All request DTOs use Jakarta Validation annotations:</p>
 * <ul>
 *   <li>@NotNull, @NotBlank for required fields</li>
 *   <li>@Size for string length constraints</li>
 *   <li>@Email for email validation</li>
 *   <li>@Min, @Max for numeric ranges</li>
 *   <li>@Pattern for regex validation</li>
 * </ul>
 * 
 * <h2>Usage Example:</h2>
 * <pre>
 * // In Controller
 * public ResponseEntity&lt;ProductResponse&gt; create(@Valid @RequestBody ProductRequest request) {
 *     // Validation happens automatically
 *     return service.create(request);
 * }
 * </pre>
 * 
 * @since 1.0.0
 * @see com.giapho.coffee_shop_backend.mapper Mappers for Entity-DTO conversion
 * @see com.giapho.coffee_shop_backend.controller Controllers using these DTOs
 */
package com.giapho.coffee_shop_backend.dto;

