# Yêu Cầu Chức Năng

## Mục lục
- [1. Phân loại yêu cầu](#1-phân-loại-yêu-cầu)
- [2. Yêu cầu chi tiết theo domain](#2-yêu-cầu-chi-tiết-theo-domain)
- [3. Dòng dữ liệu và kiểm soát](#3-dòng-dữ-liệu-và-kiểm-soát)
- [4. Ma trận truy vết use case](#4-ma-trận-truy-vết-use-case)
- [5. Tiêu chí chấp nhận chung](#5-tiêu-chí-chấp-nhận-chung)

## 1. Phân loại yêu cầu
| Mã | Nhóm chức năng | Mô tả ngắn |
|----|----------------|-----------|
| FR-01 | Authentication & Authorization | Đăng nhập, đăng ký, cấp JWT, quản lý phiên |
| FR-02 | User & Role Management | CRUD người dùng, vai trò, reset mật khẩu, khóa/mở |
| FR-03 | Category & Product | Quản lý danh mục, sản phẩm, công thức pha chế |
| FR-04 | Inventory & Supplier | Tồn kho, nhập/xuất, nhà cung cấp, đơn mua |
| FR-05 | Order & Table Service | Tạo đơn, cập nhật, quản lý bàn, thanh toán |
| FR-06 | Customer & Loyalty | Hồ sơ khách, lịch sử mua, tích điểm |
| FR-07 | Voucher & Promotion | Tạo, phân phối, áp dụng voucher |
| FR-08 | Shift & Attendance | Phân ca, chấm công, điều chỉnh hiệu suất |
| FR-09 | Payroll | Tính bảng lương, xuất phiếu lương |
| FR-10 | Expense Management | Ghi nhận chi phí, phê duyệt, báo cáo |
| FR-11 | Reporting & Dashboard | Dashboard realtime, báo cáo đa chiều |
| FR-12 | File Management | Upload/download/xóa file, phân quyền |
| FR-13 | Audit & Logging | Audit log thao tác, login history |
| FR-14 | Integration Utilities | OpenAPI, health-check, xuất dữ liệu |

## 2. Yêu cầu chi tiết theo domain
### FR-01 Authentication & Authorization
- FR-01.01: Hệ thống cho phép user đăng nhập bằng `username/password`, trả JWT + refresh token.
- FR-01.02: Ghi nhận lịch sử đăng nhập (thành công/thất bại, ip, user-agent).
- FR-01.03: Giới hạn số lần đăng nhập thất bại (`MAX_FAILED_ATTEMPTS`) và khóa tài khoản tạm thời.
- FR-01.04: Endpoint đăng ký chỉ quyền `ROLE_ADMIN` hoặc `ROLE_MANAGER` sử dụng.

### FR-02 User & Role Management
- FR-02.01: CRUD user, bao gồm cập nhật thông tin cá nhân, reset mật khẩu.
- FR-02.02: Quản lý vai trò (ROLE_ADMIN, ROLE_MANAGER, ROLE_STAFF), gán nhiều role cho một user.
- FR-02.03: Khóa/mở tài khoản bằng API PATCH trạng thái.
- FR-02.04: Đồng bộ vai trò vào JWT claim.

### FR-03 Category & Product
- FR-03.01: CRUD danh mục, cấu hình hiển thị trong POS.
- FR-03.02: CRUD sản phẩm, bao gồm tên, mô tả, giá, trạng thái, ảnh.
- FR-03.03: Quản lý công thức (`ProductIngredient`) với định mức nguyên liệu.
- FR-03.04: API tra cứu sản phẩm hỗ trợ filter theo danh mục, trạng thái, từ khóa.

### FR-04 Inventory & Supplier
- FR-04.01: Theo dõi tồn kho nguyên liệu, cập nhật theo giao dịch order.
- FR-04.02: Ghi nhận nhập kho (Purchase Order) từ nhà cung cấp, trạng thái CREATED/RECEIVED/CANCELLED.
- FR-04.03: Điều chỉnh tồn kho (kiểm kê, hủy hỏng) với lý do bắt buộc.
- FR-04.04: Cảnh báo khi tồn kho < reorder point.

### FR-05 Order & Table Service
- FR-05.01: Tạo đơn PENDING với danh sách món, gán bàn, khách hàng (tùy chọn).
- FR-05.02: Thêm/sửa/xóa món trong đơn trước khi thanh toán.
- FR-05.03: Áp dụng voucher, tính toán giảm giá, tổng tiền, thuế.
- FR-05.04: Thanh toán (cash/card/e-wallet), cập nhật trạng thái (PAID/CANCELLED).
- FR-05.05: Quản lý trạng thái bàn (AVAILABLE, IN_USE, CLEANING, RESERVED).
- FR-05.06: Ghi nhận lịch sử thay đổi trạng thái đơn và bàn.

### FR-06 Customer & Loyalty
- FR-06.01: CRUD khách hàng, đảm bảo uniqueness email/phone.
- FR-06.02: Ghi nhận lịch sử mua hàng, tổng chi tiêu.
- FR-06.03: Tích điểm tự động theo giá trị thanh toán sau giảm giá.
- FR-06.04: Phân nhóm khách hàng dựa trên điểm, tần suất, giá trị đơn.

### FR-07 Voucher & Promotion
- FR-07.01: Tạo/cập nhật voucher, thiết lập loại (PERCENT, FIXED, FREESHIP).
- FR-07.02: Định nghĩa điều kiện áp dụng (ngày hiệu lực, hóa đơn tối thiểu, giới hạn khách hàng, số lần sử dụng).
- FR-07.03: API `validate` kiểm tra điều kiện, trả thông tin giảm giá.
- FR-07.04: Ghi nhận lịch sử voucher đã sử dụng (order id, user id, thời gian).

### FR-08 Shift & Attendance
- FR-08.01: Thiết lập ca chuẩn (Shift Template) với khung giờ, vị trí.
- FR-08.02: Phân công nhân viên cho từng ngày (`ShiftAssignment`).
- FR-08.03: Ghi nhận check-in/check-out, tính giờ làm thực tế.
- FR-08.04: Điều chỉnh hiệu suất (bonus/penalty) với lý do và người phê duyệt.

### FR-09 Payroll
- FR-09.01: Tạo chu kỳ lương (`PayrollCycle`) theo tháng/quý.
- FR-09.02: Tính bảng lương dựa trên giờ làm, lương cơ bản, thưởng/phạt.
- FR-09.03: Xuất phiếu lương (PDF/Excel) cho từng nhân viên.
- FR-09.04: Lưu lịch sử bảng lương, trạng thái phê duyệt.

### FR-10 Expense Management
- FR-10.01: Ghi nhận chi phí vận hành (điện, nước, marketing, bảo trì).
- FR-10.02: Đính kèm chứng từ (file).
- FR-10.03: Quy trình phê duyệt hai bước (MANAGER → ADMIN).
- FR-10.04: Báo cáo chi phí theo danh mục, thời gian.

### FR-11 Reporting & Dashboard
- FR-11.01: Dashboard realtime hiển thị doanh thu ngày/tuần/tháng.
- FR-11.02: Báo cáo sản phẩm bán chạy, doanh thu theo danh mục.
- FR-11.03: Báo cáo hiệu suất nhân viên, ca làm.
- FR-11.04: Xuất báo cáo dạng Excel, cung cấp API cho BI.

### FR-12 File Management
- FR-12.01: Upload file (ảnh sản phẩm, chứng từ) với giới hạn dung lượng.
- FR-12.02: Lưu metadata (tên file, loại, chủ sở hữu, checksum).
- FR-12.03: Kiểm soát quyền truy cập theo vai trò và nguồn phát sinh.

### FR-13 Audit & Logging
- FR-13.01: Audit log cho thao tác quan trọng (cập nhật giá, đổi quyền, hủy đơn, điều chỉnh kho).
- FR-13.02: Truy vấn audit theo người dùng, thời gian, loại hành động.
- FR-13.03: Login history tra cứu theo user, trạng thái.

### FR-14 Integration Utilities
- FR-14.01: Publish OpenAPI/Swagger UI theo profile `dev/staging`.
- FR-14.02: Endpoint health-check `/actuator/health`, `/actuator/info`.
- FR-14.03: Xuất dữ liệu định kỳ (CSV/JSON) cho hệ thống ngoài.

## 3. Dòng dữ liệu và kiểm soát
- Mọi giao dịch tài chính (đơn, chi phí, bảng lương) phải log audit với transaction id.
- Khi đơn hàng hoàn tất, cập nhật tồn kho và ghi lịch sử loyalty atomically.
- Voucher usage chỉ tăng sau khi đơn ở trạng thái PAID.
- Check-in/out phải gắn với thiết bị hoặc IP whitelisted (tùy chọn cấu hình).

## 4. Ma trận truy vết use case
| Use Case | FR liên quan |
|----------|--------------|
| UC-01 Đăng nhập | FR-01.01, FR-01.02 |
| UC-02 Đăng ký nhân viên | FR-01.04, FR-02.01 |
| UC-05 Tạo đơn | FR-05.01, FR-05.02, FR-05.03 |
| UC-06 Thanh toán | FR-05.04, FR-05.06, FR-12.01 |
| UC-08 Quản lý voucher | FR-07.01 → FR-07.04 |
| UC-09 Nhập kho | FR-04.02, FR-04.04 |
| UC-10 Chấm công | FR-08.02 → FR-08.04 |
| UC-11 Báo cáo doanh thu | FR-11.01 → FR-11.04 |

## 5. Tiêu chí chấp nhận chung
- API trả về theo chuẩn JSON, trạng thái HTTP phù hợp.
- Ghi log audit, login history đầy đủ, truy vết được theo correlation id.
- Story hoàn thành khi có unit/integration test bao phủ các luồng chính.
- UI frontend (ngoài phạm vi tài liệu) sử dụng API đã mô tả trong `mo-ta-api.md`.

---
**Mức độ hoàn thiện:** 100%
**Hạng mục còn thiếu:** Không
