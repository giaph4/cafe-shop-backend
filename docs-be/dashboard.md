# Chức năng: Dashboard theo vai trò

## Vai trò trong hệ thống
- Cung cấp API tổng hợp số liệu cho từng nhóm người dùng:
  - **Admin**: nhìn tổng quan doanh thu, đơn hàng, tồn kho, cảnh báo toàn hệ thống.
  - **Manager**: theo dõi vận hành ca, hiệu suất đội ngũ, tồn kho, phê duyệt.
  - **Staff**: xem lịch làm, hiệu suất cá nhân, tình trạng chấm công, lương sơ bộ.
- Tận dụng dữ liệu báo cáo, phân công, chấm công, lương, đơn hàng để dựng widget hiển thị.
- Tối ưu bằng cache để giảm tải truy vấn.

## Luồng xử lý backend
1. **Admin dashboard** (`GET /api/admin/dashboard`): nhận tham số `range`, `from`, `to`, gọi `RoleDashboardService.buildAdminDashboard(range, from, to)`.
   - Tính doanh thu (ngày/tháng/năm), lợi nhuận, giá trị đơn trung bình.
   - Thống kê đơn hủy, cảnh báo tồn kho thấp, đơn hủy cao.
   - Tận dụng `DashboardAnalyticsService.collectMetrics` để lấy top sản phẩm/khách hàng/nhân viên.
   @src/main/java/com/giapho/coffee_shop_backend/controller/dashboard/AdminDashboardController.java#17-32
   @src/main/java/com/giapho/coffee_shop_backend/service/dashboard/RoleDashboardService.java#82-142
2. **Manager dashboard** (`GET /api/manager/dashboard`): `RoleDashboardService.buildManagerDashboard(range, from, to)` (nếu controller không truyền tham số thì dùng mặc định).
   - Thống kê ca hôm nay (lịch, locked, completed, cancelled).
   - Danh sách ca sắp tới, cảnh báo tồn kho, hiệu suất đội, phê duyệt chờ.
   - Tận dụng dữ liệu `ShiftInstance`, `ShiftAssignment`, `IngredientRepository`, `PurchaseOrderRepository`, `AttendanceRecordRepository`, `PayrollSummaryRepository`.
   @src/main/java/com/giapho/coffee_shop_backend/controller/dashboard/ManagerDashboardController.java#12-24
   @src/main/java/com/giapho/coffee_shop_backend/service/dashboard/RoleDashboardService.java#144-170
3. **Staff dashboard (current)** (`GET /api/staff/dashboard`): `RoleDashboardService.buildStaffDashboard(userId?)`.
   - Nếu không truyền `userId`, xác định từ JWT (`SecurityUtil`).
   - Tổng hợp lịch ca sắp tới, chấm công, hiệu suất, lương sơ bộ, nhắc nhở.
   - `DashboardRange` tùy chọn (controller hiện chỉ truyền userId, service hỗ trợ range mặc định).
   @src/main/java/com/giapho/coffee_shop_backend/controller/dashboard/StaffDashboardController.java#24-29
   @src/main/java/com/giapho/coffee_shop_backend/service/dashboard/RoleDashboardService.java#173-202
4. **Staff dashboard for manager** (`GET /api/staff/dashboard/{userId}`): cho manager xem KPI của nhân viên bất kỳ.
   @src/main/java/com/giapho/coffee_shop_backend/controller/dashboard/StaffDashboardController.java#31-34
5. **Analytics bổ trợ**: `DashboardAnalyticsService`, `ReportService` cung cấp số liệu gốc (top sản phẩm, top khách, doanh thu, chi phí).
   @src/main/java/com/giapho/coffee_shop_backend/service/DashboardAnalyticsService.java (xem file để biết chi tiết metrics)
6. **Cache**: `RoleDashboardService` cấu hình `@CacheConfig` và `@Cacheable` cho admin/manager/staff dashboard; `@CacheEvict` (nếu có) sẽ làm mới cache khi dữ liệu thay đổi.

## Thành phần liên quan
- **Controllers**: `AdminDashboardController`, `ManagerDashboardController`, `StaffDashboardController`
- **Service**: `RoleDashboardService`, `DashboardAnalyticsService`, `ReportService`
- **DTO**: `AdminDashboardDTO`, `ManagerDashboardDTO`, `StaffDashboardDTO`, `DashboardRange` (enum), `DashboardMetricsDTO`
- **Repository**: nhiều repo được inject (orders, ingredients, suppliers, purchase orders, shift assignments/instances, attendance, payroll summary, user)
- **Security**:
  - `/api/admin/dashboard`: `hasRole('ADMIN')`
  - `/api/manager/dashboard`: `hasAnyRole('MANAGER','ADMIN')`
  - `/api/staff/dashboard`: `hasAnyRole('STAFF','MANAGER','ADMIN')`; endpoint `/api/staff/dashboard/{userId}` thêm điều kiện manager/admin.

## Danh sách API
| Method | URL | Mô tả | Role |
| --- | --- | --- | --- |
| GET | `/api/admin/dashboard` | Dashboard tổng quan cho admin (supports range/from/to) | `ADMIN` |
| GET | `/api/manager/dashboard` | Dashboard vận hành cho manager | `MANAGER`,`ADMIN` |
| GET | `/api/staff/dashboard` | Dashboard cá nhân cho nhân viên (hoặc userId query) | `STAFF`,`MANAGER`,`ADMIN` |
| GET | `/api/staff/dashboard/{userId}` | Manager/Admin xem dashboard nhân viên | `MANAGER`,`ADMIN` |

Không có API thừa hoặc thiếu; controllers chỉ expose các endpoint trên.

## Chi tiết API

### GET `/api/admin/dashboard`
- **Query**: `range` (enum `DashboardRange`: DAILY, WEEKLY, MONTHLY, CUSTOM), `from`, `to` (ISO DATE, optional).
- **Logic**: xác định `DateRange` hiệu lực, lấy snapshot doanh thu (ngày/tháng/năm), lợi nhuận, đơn hủy, cảnh báo tồn kho, top khách/sản phẩm/nhân viên.
- **Response 200**: `AdminDashboardDTO` gồm sub-objects `RevenueSnapshot`, `OrderSnapshot`, `InventorySnapshot`, `topStaff`, `topProducts`, `topCustomers`, `alerts`.
- **Cache**: key = `range-from-to`; trả về cache nếu có.

### GET `/api/manager/dashboard`
- **Logic**: `RoleDashboardService.buildManagerDashboard` (hỗ trợ range/from/to nhưng controller mặc định null).
- **Dữ liệu**: shift overview (số ca hôm nay, locked, completed, cancelled), upcoming shifts (limit 6), team performance (doanh thu, đơn, top staff), tồn kho cần chú ý, pending approvals (phiếu nhập, payroll, ...), attendance alerts (đi muộn, chưa check-in), service issues (đơn hủy, feedback xấu).
- **Response 200**: `ManagerDashboardDTO` chứa các widget tương ứng.
- **Cache**: `managerDashboard`.

### GET `/api/staff/dashboard`
- **Query**: `userId` tùy chọn (cho manager xem hộ nhân viên). Nếu không truyền, dùng `SecurityUtil` lấy user hiện tại.
- **Data**: tóm tắt ca (số ca đã làm, giờ thực tế), ca sắp tới (7 ngày tới), hiệu suất (doanh thu/đơn trong window 30 ngày), tình trạng chấm công gần đây, snapshot lương (dựa vào `PayrollSummaryRepository`).
- **Response 200**: `StaffDashboardDTO`.
- **Cache**: key khác nhau cho user hiện tại và user cụ thể.

### GET `/api/staff/dashboard/{userId}`
- **Logic**: dùng `userId` trong path; `@PreAuthorize` yêu cầu manager/admin.
- **Response**: giống endpoint trên nhưng cho user cụ thể.

## Điều kiện nghiệp vụ & validation
- `DashboardRange` giúp xác định `DateRange` (từ `RoleDashboardService.DateRange.from`). Nếu range là CUSTOM nhưng thiếu from/to -> service sử dụng mặc định.
- Các truy vấn trong service đảm bảo an toàn null: `defaultZero`, `Optional`, check list rỗng.
- Khi user có ít dữ liệu, dashboards trả list rỗng (không throw).
- Cache cần được evict khi có thay đổi lớn (không thấy `@CacheEvict` trong file, nên có thể rely vào TTL cấu hình bên ngoài).

## Luồng lỗi & thông điệp
- Controllers không có xử lý lỗi riêng; lỗi phát sinh (ví dụ: user không tồn tại khi build staff dashboard) -> `EntityNotFoundException` với thông điệp "Không tìm thấy nhân viên".
- Các lỗi khác (ví dụ sai range) được `DateRange.from` xử lý (có thể ném `IllegalArgumentException`).

## Role/Permission
- Admin dashboard: `hasRole('ADMIN')`.
- Manager dashboard: `hasAnyRole('MANAGER','ADMIN')`.
- Staff dashboard: `hasAnyRole('STAFF','MANAGER','ADMIN')`; route `/staff/dashboard/{userId}` yêu cầu manager/admin.

## Quan hệ với chức năng khác
- **ReportService**: cung cấp dữ liệu doanh thu, lợi nhuận, inventory cho admin/manager dashboards.
- **DashboardAnalyticsService**: build metrics top sản phẩm/khách/nhân viên.
- **ShiftInstance/Assignment/Attendance**: dùng để tính shift overview, attendance alerts, performance.
- **Purchase Order/Supplier**: thông tin pending approvals, inventory alerts.
- **Payroll**: snapshot lương trong staff dashboard.
- **Security**: user hiện tại xác định bằng `SecurityUtil`.

## Các tệp liên quan trong BE
- Controller: `AdminDashboardController.java`, `ManagerDashboardController.java`, `StaffDashboardController.java`
- Service: `RoleDashboardService.java`, `DashboardAnalyticsService.java`
- DTO: `AdminDashboardDTO.java`, `ManagerDashboardDTO.java`, `StaffDashboardDTO.java`, `DashboardRange.java`, `DashboardMetricsDTO.java`
- Repository: `OrderRepository.java`, `IngredientRepository.java`, `SupplierRepository.java`, `PurchaseOrderRepository.java`, `ShiftAssignmentRepository.java`, `ShiftInstanceRepository.java`, `AttendanceRecordRepository.java`, `PayrollSummaryRepository.java`, `UserRepository.java`
