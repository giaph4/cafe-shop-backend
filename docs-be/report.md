# Chức năng: Báo cáo & phân tích

## Vai trò trong hệ thống
- Cung cấp API báo cáo doanh thu, lợi nhuận, top sản phẩm/khách hàng/nhân viên, thống kê thanh toán.
- Xuất Excel dữ liệu đơn hàng, tồn kho, chi phí để phục vụ kiểm toán.
- Hỗ trợ dashboard quản lý (Admin/Manager) theo dõi hoạt động quán.

## Luồng xử lý backend
1. **Doanh thu ngày** (`GET /api/v1/reports/daily-revenue`): tính tổng doanh thu đơn `PAID` theo ngày @src/main/java/com/giapho/coffee_shop_backend/controller/ReportController.java#29-40 @src/main/java/com/giapho/coffee_shop_backend/service/ReportService.java#46-51.
2. **Tồn kho** (`GET /inventory`, `?lowStock=true`): trả danh sách nguyên liệu, hoặc chỉ nguyên liệu dưới mức cảnh báo @src/main/java/com/giapho/coffee_shop_backend/controller/ReportController.java#42-54 @src/main/java/com/giapho/coffee_shop_backend/service/ReportService.java#53-67.
3. **Xuất Excel đơn hàng** (`GET /orders/export`): tạo file `.xlsx` chứa đơn hàng trong khoảng ngày @src/main/java/com/giapho/coffee_shop_backend/controller/ReportController.java#56-76 @src/main/java/com/giapho/coffee_shop_backend/service/ReportService.java#69-147.
4. **Lợi nhuận** (`GET /profit`): tính doanh thu, giá vốn hàng bán, lợi nhuận @src/main/java/com/giapho/coffee_shop_backend/controller/ReportController.java#78-94 @src/main/java/com/giapho/coffee_shop_backend/service/ReportService.java#150-177.
5. **Best Sellers** (`GET /best-sellers`): top sản phẩm theo số lượng hoặc doanh thu @src/main/java/com/giapho/coffee_shop_backend/controller/ReportController.java#96-108 @src/main/java/com/giapho/coffee_shop_backend/service/ReportService.java#181-191.
6. **Tổng quan bán hàng theo sản phẩm/danh mục** (`GET /product-sales-summary`, `/category-sales`): tổng hợp số lượng, doanh thu, % đóng góp @src/main/java/com/giapho/coffee_shop_backend/controller/ReportController.java#111-170 @src/main/java/com/giapho/coffee_shop_backend/service/ReportService.java#194-315.
7. **Doanh thu giờ vàng** (`GET /hourly-sales`): thống kê đơn hàng theo từng giờ trong ngày @src/main/java/com/giapho/coffee_shop_backend/controller/ReportController.java#173-180 @src/main/java/com/giapho/coffee_shop_backend/service/ReportService.java#317-345.
8. **Thống kê phương thức thanh toán** (`GET /payment-method-stats`): tổng hợp số đơn, doanh thu và tỷ lệ theo từng phương thức @src/main/java/com/giapho/coffee_shop_backend/controller/ReportController.java#182-189 @src/main/java/com/giapho/coffee_shop_backend/service/ReportService.java#348-382.
9. **So sánh kỳ bán hàng** (`GET /sales-comparison`): so sánh doanh thu, số đơn giữa hai giai đoạn @src/main/java/com/giapho/coffee_shop_backend/controller/ReportController.java#192-202 @src/main/java/com/giapho/coffee_shop_backend/service/ReportService.java#385-424.
10. **Dashboard** (`GET /dashboard`): thống kê tổng hợp phục vụ màn hình chính @src/main/java/com/giapho/coffee_shop_backend/controller/ReportController.java#204-209 (xem ReportService phần cuối).
11. **Xuất Excel tồn kho/chi phí** (`GET /inventory/export`, `/expenses/export`): tạo file Excel @src/main/java/com/giapho/coffee_shop_backend/controller/ReportController.java#211-247 @src/main/java/com/giapho/coffee_shop_backend/service/ReportService.java#426-520 (phần sau file).
12. **Tổng chi phí, tổng nhập nguyên liệu** (`GET /total-expenses`, `/total-imported-ingredients`): tổng hợp giá trị chi phí/phiếu nhập @src/main/java/com/giapho/coffee_shop_backend/controller/ReportController.java#250-276 @src/main/java/com/giapho/coffee_shop_backend/service/ReportService.java#522-590.

## Thành phần liên quan
- **Controller**: `ReportController` @src/main/java/com/giapho/coffee_shop_backend/controller/ReportController.java#1-277
- **Service**: `ReportService` @src/main/java/com/giapho/coffee_shop_backend/service/ReportService.java#1-590
- **Repository**: `OrderRepository`, `OrderDetailRepository`, `IngredientRepository`, `ExpenseRepository`, `PurchaseOrderRepository`, `CustomerRepository`, `UserRepository`, `ProductRepository`, `CategoryRepository`
- **DTO**: `BestSellerDTO`, `ProductSalesSummaryResponseDTO`, `IngredientResponseDTO`, `CustomerAnalyticsDTO`, `StaffPerformanceDTO`, `CategorySalesDTO`, `HourlySalesDTO`, `PaymentMethodStatsDTO`, `SalesComparisonDTO`, `VoucherCheckResponseDTO`, ... (xem package `dto`)
- **Mapper**: `IngredientMapper`
- **Excel Utils**: Apache POI (`Workbook`, `Sheet`, ...)
- **Security**: tất cả endpoint yêu cầu `hasAnyRole('MANAGER','ADMIN')`.

## Danh sách API (không thừa/thiếu)
| Method | URL | Mô tả | Role |
| --- | --- | --- | --- |
| GET | `/api/v1/reports/daily-revenue` | Doanh thu ngày | `MANAGER`,`ADMIN` |
| GET | `/api/v1/reports/inventory` | Danh sách tồn kho (`?lowStock=true` lọc thấp) | `MANAGER`,`ADMIN` |
| GET | `/api/v1/reports/orders/export` | Xuất Excel đơn hàng | `MANAGER`,`ADMIN` |
| GET | `/api/v1/reports/profit` | Báo cáo lợi nhuận | `MANAGER`,`ADMIN` |
| GET | `/api/v1/reports/best-sellers` | Top sản phẩm bán chạy | `MANAGER`,`ADMIN` |
| GET | `/api/v1/reports/product-sales-summary` | Tổng quan doanh thu theo sản phẩm | `MANAGER`,`ADMIN` |
| GET | `/api/v1/reports/revenue-by-date` | Doanh thu theo ngày | `MANAGER`,`ADMIN` |
| GET | `/api/v1/reports/expenses-by-date` | Chi phí theo ngày + danh mục | `MANAGER`,`ADMIN` |
| GET | `/api/v1/reports/top-customers` | Top khách hàng | `MANAGER`,`ADMIN` |
| GET | `/api/v1/reports/staff-performance` | Hiệu suất nhân viên | `MANAGER`,`ADMIN` |
| GET | `/api/v1/reports/category-sales` | Doanh thu theo danh mục | `MANAGER`,`ADMIN` |
| GET | `/api/v1/reports/hourly-sales` | Doanh thu theo giờ | `MANAGER`,`ADMIN` |
| GET | `/api/v1/reports/payment-method-stats` | Thống kê phương thức thanh toán | `MANAGER`,`ADMIN` |
| GET | `/api/v1/reports/sales-comparison` | So sánh hai giai đoạn | `MANAGER`,`ADMIN` |
| GET | `/api/v1/reports/dashboard` | Thống kê tổng quan dashboard | `MANAGER`,`ADMIN` |
| GET | `/api/v1/reports/inventory/export` | Xuất Excel tồn kho | `MANAGER`,`ADMIN` |
| GET | `/api/v1/reports/expenses/export` | Xuất Excel chi phí | `MANAGER`,`ADMIN` |
| GET | `/api/v1/reports/total-expenses` | Tổng chi phí theo khoảng ngày | `MANAGER`,`ADMIN` |
| GET | `/api/v1/reports/total-imported-ingredients` | Tổng tiền nhập nguyên liệu | `MANAGER`,`ADMIN` |

## Chi tiết API chính

### GET `/api/v1/reports/daily-revenue`
- **Query**: `date` (ISO DATE).
- **Response 200**:
  ```json
  {
    "date": "2025-11-15",
    "totalRevenue": 15250000
  }
  ```
- **Logic**: sum orders `PAID` giữa `date` và `date + 1`.

### GET `/api/v1/reports/orders/export`
- **Query**: `startDate`, `endDate` (ISO DATE).
- **Response**: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` (stream file).
- **Logic**: truy vấn đơn hàng, ghi sheet với cột: ID, bàn, nhân viên, type, status, thời gian, subtotal, discount, total, items.

### GET `/api/v1/reports/profit`
- **Query**: `startDate`, `endDate`.
- **Response 200**:
  ```json
  {
    "startDate": "2025-11-01",
    "endDate": "2025-11-15",
    "totalRevenue": 32500000,
    "totalCostOfGoodsSold": 17500000,
    "totalProfit": 15000000
  }
  ```
- **Logic**: tổng doanh thu từ order, giá vốn từ `product.cost * quantity`.

### GET `/api/v1/reports/best-sellers`
- **Query**: `startDate`, `endDate`, `top` (default 10), `sortBy=quantity|revenue`.
- **Response 200**: danh sách `BestSellerDTO`.

### GET `/api/v1/reports/hourly-sales`
- **Query**: `date`.
- **Response 200**: mảng `HourlySalesDTO` (0-23 giờ).

### GET `/api/v1/reports/payment-method-stats`
- **Query**: `startDate`, `endDate`.
- **Response**: `List<PaymentMethodStatsDTO>` với tổng số đơn, tổng tiền, tỷ lệ %.

### GET `/api/v1/reports/sales-comparison`
- **Query**: `currentStart`, `currentEnd`, `previousStart`, `previousEnd`.
- **Response**: `SalesComparisonDTO` gồm revenue/order của mỗi kỳ và tăng trưởng.

### GET `/api/v1/reports/expenses/export`
- **Query**: `startDate`, `endDate`.
- **Response**: file Excel chi phí với cột ngày, hạng mục, số tiền, mô tả.

### GET `/api/v1/reports/total-expenses`
- **Query**: `startDate`, `endDate` (tùy chọn).
- **Response 200**: tổng chi phí, kèm lại tham số.

## Điều kiện nghiệp vụ & validation
- Tất cả endpoint yêu cầu có `startDate` <= `endDate`; service không validate sâu, FE cần đảm bảo.
- Một số API (best-sellers, staff-performance) giới hạn `top` qua `PageRequest`.
- Khi export Excel, nếu lỗi IO -> trả 500 (controller trả body null).
- Khi tính lợi nhuận, sản phẩm thiếu `cost` => log cảnh báo, bỏ qua chi phí.

## Luồng lỗi & thông điệp chính
| Exception | HTTP | Message |
| --- | --- | --- |
| `IllegalArgumentException` | 400 | sortBy không hợp lệ (controller đổi về quantity) |
| `IOException` | 500 | Khi export thất bại (controller trả 500, body null) |
| `EntityNotFoundException` | 404 | Với các phương thức phụ thuộc repo (ít khi dùng) |

## Role/Permission
- Tất cả endpoint `ReportController` dùng `@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")`.

## Quan hệ với chức năng khác
- **Order**, **Expense**, **Purchase Order**, **Voucher**, **Customer**, **User**, **Product**, **Category**, **Ingredient** cung cấp dữ liệu đầu vào.
- **Dashboard Controllers** (admin/manager/staff) dùng dữ liệu tổng hợp từ đây.
- **Export Excel** liên kết với File API nếu cần lưu trữ.

## Các tệp liên quan trong BE
- Controller: `ReportController.java`
- Service: `ReportService.java`
- DTO: `BestSellerDTO`, `ProductSalesSummaryDTO`, `CustomerAnalyticsDTO`, `StaffPerformanceDTO`, `CategorySalesDTO`, `HourlySalesDTO`, `PaymentMethodStatsDTO`, `SalesComparisonDTO`, `VoucherCheckResponseDTO`, `IngredientResponseDTO`, `DashboardStatsDTO`
- Repository: `OrderRepository.java`, `OrderDetailRepository.java`, `IngredientRepository.java`, `ExpenseRepository.java`, `PurchaseOrderRepository.java`, `CustomerRepository.java`, `ProductRepository.java`, `UserRepository.java`, `CategoryRepository.java`
