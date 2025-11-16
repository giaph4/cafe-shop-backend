# Tổng quan Backend Coffee Shop

## Kiến trúc & công nghệ
- **Framework**: Spring Boot 3.5
- **JDK**: 21
- **Build**: Maven (`pom.xml`)
- **Modules chính**:
  - `controller`: REST API theo domain (auth, order, product, report, shift…)
  - `service`: business logic (AuthenticationService, OrderService, PaymentService…)
  - `domain`: entity, repository (JPA)
  - `dto`: request/response object
  - `security`: JWT filter, service, handler
  - `config`: security/application config, CORS, file storage properties
  - `exception`: GlobalExceptionHandler, FileStorageException
  - `service/dashboard`: tổng hợp số liệu dashboards
  - `service/shift`: quản lý ca làm, phân công, chấm công, payroll

## Package structure
```
com.giapho.coffee_shop_backend
├─ config /
├─ controller /
│  ├─ dashboard /
│  └─ ...
├─ domain /
│  ├─ entity /
│  └─ repository /
├─ dto /
│  └─ shift /
├─ exception /
├─ mapper /
├─ security /
├─ service /
│  ├─ dashboard /
│  └─ shift /
└─ util /
```

## Danh sách Controller & tài liệu
| Controller | Đường dẫn | Tài liệu |
| --- | --- | --- |
| `AuthenticationController` | `/api/v1/auth` | [auth.md](auth.md) |
| `LoginHistoryController` | `/api/v1/login-history` | [login-history.md](login-history.md) |
| `UserController` | `/api/v1/users` | [user-management.md](user-management.md) |
| `ProductController` | `/api/v1/products` | [product.md](product.md) |
| `ProductRecipeController` | `/api/v1/products/{id}/recipe` | [product-recipe.md](product-recipe.md) |
| `CategoryController` | `/api/v1/categories` | [category.md](category.md) |
| `OrderController` | `/api/v1/orders` | [order.md](order.md) |
| `VoucherController` | `/api/v1/vouchers` | [voucher.md](voucher.md) |
| `CustomerController` | `/api/v1/customers` | [customer.md](customer.md) |
| `CafeTableController` | `/api/v1/tables` | [table.md](table.md) |
| `IngredientController` | `/api/v1/ingredients` | [ingredient.md](ingredient.md) |
| `SupplierController` | `/api/v1/suppliers` | [supplier.md](supplier.md) |
| `PurchaseOrderController` | `/api/v1/purchase-orders` | [purchase-order.md](purchase-order.md) |
| `ExpenseController` | `/api/v1/expenses` | [expense.md](expense.md) |
| `ReportController` | `/api/v1/reports` | [report.md](report.md) |
| `FileController` | `/api/v1/files` | [file.md](file.md) |
| `Payment (OrderController)` | `/api/v1/orders/{id}/payment` | [payment.md](payment.md) |
| `Dashboard` (Admin/Manager/Staff) | `/api/admin/manager/staff/dashboard` | [dashboard.md](dashboard.md) |
| `AttendanceController` | `/api/v1/attendance` | [attendance.md](attendance.md) |
| `ShiftTemplateController` | `/api/v1/shifts/templates` | [shift-template.md](shift-template.md) |
| `ShiftInstanceController` | `/api/v1/shifts/instances` | [shift-instance.md](shift-instance.md) |
| `ShiftAssignmentController` | `/api/v1/shifts/assignments` | [shift-assignment.md](shift-assignment.md) |
| `ShiftPerformanceAdjustmentController` | `/api/v1/shifts/adjustments` | [shift-performance-adjustment.md](shift-performance-adjustment.md) |
| `PayrollController` | `/api/v1/shifts/payroll` | [payroll.md](payroll.md) |
| `AdminAnalyticsController` | `/api/admin/analytics` | [dashboard.md](dashboard.md) (phần admin) |
| `FileController` | `/api/v1/files` | [file.md](file.md) |

## Danh sách Service tiêu biểu
| Service | Vai trò |
| --- | --- |
| `AuthenticationService` | Đăng nhập/đăng ký, ghi login history |
| `UserService` | Quản lý user, đổi mật khẩu |
| `OrderService` | CRUD order, áp dụng voucher, quản lý món |
| `PaymentService` | Thanh toán order, trừ kho, cộng điểm |
| `ProductService` | Quản lý sản phẩm & ảnh |
| `ProductRecipeService` | Công thức sản phẩm |
| `VoucherService` | Kiểm tra, CRUD voucher |
| `CustomerService` | Khách hàng & lịch sử mua |
| `CafeTableService` | Bàn, trạng thái |
| `IngredientService` | Nguyên liệu, điều chỉnh tồn |
| `SupplierService` | Nhà cung cấp |
| `PurchaseOrderService` | Phiếu nhập hàng |
| `ExpenseService` | Ghi nhận chi phí |
| `ReportService` | Báo cáo doanh thu/lợi nhuận/Excel |
| `FileStorageService` | Lưu trữ file |
| `DashboardAnalyticsService` | Tính top sản phẩm/khách/nhân viên |
| `RoleDashboardService` | Xây dựng dashboard cho từng role |
| `AttendanceService` | Check-in/out ca |
| `ShiftTemplateService` | Mẫu ca |
| `ShiftInstanceService` | Ca thực tế |
| `ShiftAssignmentService` | Phân công nhân viên |
| `ShiftPerformanceAdjustmentService` | Thưởng/phạt |
| `PayrollService` | Chu kỳ lương, bảng lương |
| `AuditLogService` | Lưu audit log |
| `LoginHistoryService` | Tra cứu nhật ký đăng nhập |

## Danh sách Entity chính
- **User, Role, LoginHistory** – quản lý tài khoản & bảo mật
- **Product, Category, Voucher, Customer, Order, OrderDetail** – nghiệp vụ bán hàng
- **Ingredient, ProductIngredient, PurchaseOrder, PurchaseOrderDetail, Supplier, Expense** – quản lý kho & chi phí
- **ShiftTemplate, ShiftInstance, ShiftAssignment, ShiftPerformanceAdjustment, AttendanceRecord, PayrollCycle, PayrollSummary** – ca làm, chấm công, lương
- **AuditLog** – nhật ký thao tác

## Danh sách DTO tiêu biểu
- Auth: `LoginRequest`, `RegisterRequest`, `AuthenticationResponse`
- User: `UserResponseDTO`, `UserUpdateRequestDTO`, `ChangePasswordRequestDTO`, `RoleDTO`
- Product: `ProductRequest`, `ProductResponse`, `ProductRecipeDTO`, `ProductIngredientDTO`
- Order: `OrderCreateRequestDTO`, `OrderResponseDTO`, `PaymentRequestDTO`
- Voucher: `VoucherRequestDTO`, `VoucherResponseDTO`, `VoucherCheckResponseDTO`
- Customer: `CustomerDTO`, `CustomerPurchaseHistoryResponseDTO`
- Report: `BestSellerDTO`, `PaymentMethodStatsDTO`, `CategorySalesDTO`, `ProductSalesSummaryResponseDTO`, `SalesComparisonDTO`, `HourlySalesDTO`
- Shift: `ShiftTemplateRequestDTO`, `ShiftInstanceCreateRequestDTO`, `ShiftAssignmentRequestDTO`, `ShiftPerformanceAdjustmentRequestDTO`, `AttendanceCheckRequestDTO`, `PayrollCycleRequestDTO`, `PayrollSummaryDTO`
- Dashboard: `AdminDashboardDTO`, `ManagerDashboardDTO`, `StaffDashboardDTO`, `DashboardRange`
- File: `FileUploadResponse`

## Danh sách Exception tùy chỉnh
- `GlobalExceptionHandler` – chuẩn hóa lỗi REST (400/401/403/404/409/500)
- `FileStorageException`

## Bảo mật (Security)
- JWT-based (xem `security.md`)
- Roles: `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_STAFF`
- Các controller dùng `@PreAuthorize` xác định role chính xác.
- CORS: cấu hình tại `SecurityConfig` (`app.cors.allowed-origins`).

## Luồng FE → BE
1. FE đăng nhập → nhận JWT.
2. FE gửi request kèm header `Authorization: Bearer <token>`.
3. Backend (`JwtAuthenticationFilter`) xác thực; `SecurityConfig` + `@PreAuthorize` kiểm tra quyền.
4. Controller xử lý, trả JSON.
5. Các thao tác quan trọng ghi `AuditLog`/`LoginHistory`.

## Checklist tài liệu module
- [x] auth.md
- [x] user-management.md
- [x] product.md
- [x] product-recipe.md
- [x] category.md
- [x] order.md
- [x] payment.md
- [x] voucher.md
- [x] customer.md
- [x] table.md
- [x] ingredient.md
- [x] supplier.md
- [x] purchase-order.md
- [x] expense.md
- [x] report.md
- [x] file.md
- [x] dashboard.md
- [x] attendance.md
- [x] shift-template.md
- [x] shift-instance.md
- [x] shift-assignment.md
- [x] shift-performance-adjustment.md
- [x] payroll.md
- [x] login-history.md
- [x] security.md
- [x] audit-log.md

Tất cả tài liệu chi tiết đặt trong `docs-be/`. Khi cập nhật code, cần cập nhật tài liệu tương ứng để giữ tính đồng bộ.
