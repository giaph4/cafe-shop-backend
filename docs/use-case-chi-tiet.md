# Use Case Chi Tiết

## Mục lục
- [1. Danh sách use case tổng hợp](#1-danh-sách-use-case-tổng-hợp)
- [2. Mẫu mô tả use case](#2-mẫu-mô-tả-use-case)
- [3. Use case chi tiết](#3-use-case-chi-tiết)
- [4. Quan hệ phụ thuộc giữa các use case](#4-quan-hệ-phụ-thuộc-giữa-các-use-case)

## 1. Danh sách use case tổng hợp
| Mã | Tên use case | Diễn viên chính | FR liên quan |
|----|---------------|-----------------|--------------|
| UC-01 | Đăng nhập hệ thống | Nhân viên, Quản lý | FR-01.01, FR-01.02 |
| UC-02 | Đăng ký nhân viên mới | Quản lý, Admin | FR-01.04, FR-02.01 |
| UC-03 | Quản lý danh mục | Quản lý | FR-03.01 |
| UC-04 | Quản lý sản phẩm | Quản lý | FR-03.02, FR-03.03 |
| UC-05 | Tạo đơn hàng tại quầy | Nhân viên thu ngân | FR-05.01 → FR-05.03 |
| UC-06 | Thanh toán đơn hàng | Nhân viên thu ngân | FR-05.04 → FR-05.06 |
| UC-07 | Quản lý khách hàng | CSKH | FR-06.01 → FR-06.04 |
| UC-08 | Quản lý voucher | Marketing/Quản lý | FR-07.01 → FR-07.04 |
| UC-09 | Nhập kho | Nhân viên kho | FR-04.02, FR-04.03 |
| UC-10 | Chấm công ca làm | Quản lý nhân sự | FR-08.01 → FR-08.04 |
| UC-11 | Xuất báo cáo doanh thu | Quản lý | FR-11.01 → FR-11.04 |
| UC-12 | Theo dõi dashboard | Quản lý | FR-11.01 |
| UC-13 | Quản lý chi phí | Kế toán | FR-10.01 → FR-10.04 |
| UC-14 | Upload chứng từ | Kế toán, Quản lý | FR-12.01 → FR-12.03 |

## 2. Mẫu mô tả use case
| Trường | Mô tả |
|--------|-------|
| **Mã** | Định danh use case |
| **Tên** | Tên nghiệp vụ |
| **Diễn viên** | Tác nhân chính, phụ |
| **Mô tả** | Mục đích nghiệp vụ |
| **Tiền điều kiện** | Điều kiện cần trước khi thực hiện |
| **Hậu điều kiện** | Trạng thái hệ thống sau khi hoàn thành |
| **Luồng chính** | Các bước chuẩn |
| **Luồng thay thế / ngoại lệ** | Các trường hợp ngoài luồng |
| **Dữ liệu sử dụng** | Bảng/đối tượng liên quan |
| **Quy tắc nghiệp vụ** | Ràng buộc FR/BR áp dụng |
| **Trigger** | Sự kiện bắt đầu |

## 3. Use case chi tiết

### UC-01: Đăng nhập hệ thống
- **Mô tả**: Người dùng xác thực tài khoản để truy cập các API nội bộ.
- **Diễn viên**: Nhân viên, Quản lý, Admin (primary); Hệ thống bảo mật (secondary).
- **Tiền điều kiện**: Tài khoản ACTIVE, chưa bị khóa vì đăng nhập sai nhiều lần.
- **Hậu điều kiện**: JWT + refresh token được cấp, lịch sử đăng nhập lưu thành công/thất bại.
- **Dữ liệu sử dụng**: `users`, `login_history`.
- **Quy tắc nghiệp vụ**: BR-AUTH-01 (khóa tài khoản sau N lần sai), FR-01.01 → FR-01.03.
- **Luồng chính**:
  1. Diễn viên gửi `POST /api/v1/auth/login` với `username`, `password`.
  2. Hệ thống xác thực thông qua `AuthenticationManager`.
  3. Nếu hợp lệ, sinh JWT + refresh token bằng `JwtService`.
  4. Ghi nhận bản ghi `LoginHistory` thành công.
  5. Trả response chứa token, thông tin cơ bản.
- **Luồng thay thế**:
  - 2a. Tài khoản không tồn tại hoặc mật khẩu sai: ghi login fail, trả HTTP 401.
  - 2b. Tài khoản LOCKED: trả HTTP 403, hiển thị thời gian mở khóa.
  - 3a. Lỗi hệ thống: trả HTTP 500, ghi log bảo mật.

### UC-05: Tạo đơn hàng tại quầy
- **Mô tả**: Nhân viên thu ngân tạo đơn trạng thái PENDING cho khách tại bàn.
- **Diễn viên**: Nhân viên thu ngân (primary), Hệ thống voucher, kho (secondary).
- **Tiền điều kiện**: Bàn tồn tại và không có đơn PENDING; nhân viên đã đăng nhập.
- **Hậu điều kiện**: Order PENDING được lưu, bàn chuyển trạng thái IN_USE, tồn kho dự kiến được kiểm tra.
- **Dữ liệu sử dụng**: `orders`, `order_details`, `cafe_tables`, `products`, `vouchers`.
- **Quy tắc nghiệp vụ**: BR-ORDER-01 (mỗi bàn 1 order pending), BR-VOUCHER-02 (validate điều kiện).
- **Luồng chính**:
  1. Nhân viên gửi `POST /api/v1/orders` với danh sách món, bàn, khách hàng tùy chọn, voucher.
  2. Hệ thống kiểm tra trạng thái bàn, tồn kho sản phẩm.
  3. Nếu có voucher, gọi `VoucherService.validateVoucher()`.
  4. Tính giá trị order (subtotal, discount, total), tạo `Order` + `OrderDetail`.
  5. Lưu order, cập nhật bàn sang `IN_USE`, trả về `OrderResponseDTO`.
- **Luồng thay thế**:
  - 2a. Bàn đã có order PENDING → HTTP 409, hiển thị thông tin order hiện tại.
  - 3a. Voucher không hợp lệ → HTTP 400 với mã lỗi chi tiết.
  - 4a. Sản phẩm ngừng bán → HTTP 400, yêu cầu cập nhật menu.

### UC-06: Thanh toán đơn hàng
- **Mô tả**: Hoàn tất order, ghi nhận thanh toán, giải phóng bàn.
- **Diễn viên**: Nhân viên thu ngân, Quản lý (phê duyệt hủy), Payment service (secondary).
- **Tiền điều kiện**: Order trạng thái PENDING, đã xác nhận món.
- **Hậu điều kiện**: Order chuyển `PAID` (hoặc `CANCELLED`), ghi `paidAt`, cập nhật doanh thu, bàn `AVAILABLE`.
- **Dữ liệu sử dụng**: `orders`, `payments`, `voucher_usage`, `cafe_tables`.
- **Quy tắc nghiệp vụ**: BR-PAY-01 (số tiền phải khớp), BR-PAY-02 (lý do hủy bắt buộc).
- **Luồng chính**:
  1. Gửi `POST /api/v1/orders/{id}/pay` với phương thức và số tiền nhận.
  2. Hệ thống kiểm tra quyền, trạng thái order, số tiền hợp lệ.
  3. Cập nhật order sang `PAID`, ghi `paidAt`, phương thức thanh toán.
  4. Ghi giao dịch vào `PaymentService`, cập nhật voucher usage nếu có.
  5. Bàn chuyển sang `AVAILABLE`, trả response.
- **Luồng thay thế**:
  - 2a. Thanh toán thiếu/thừa -> HTTP 400, yêu cầu điều chỉnh.
  - 2b. Order đã PAID/CANCELLED -> HTTP 409.
  - 4a. Lỗi ghi payment -> rollback transaction, HTTP 500.
- **Luồng ngoại lệ**:
  - 1a. Hủy đơn: `POST /api/v1/orders/{id}/cancel`, yêu cầu lý do, quyền MANAGER.

### UC-08: Quản lý voucher
- **Mô tả**: Marketing tạo/cập nhật voucher với điều kiện áp dụng.
- **Diễn viên**: Marketing, Quản lý.
- **Tiền điều kiện**: User có quyền ROLE_MANAGER+, dữ liệu điều kiện đầy đủ.
- **Hậu điều kiện**: Voucher lưu vào DB, sẵn sàng áp dụng.
- **Dữ liệu sử dụng**: `vouchers`, `voucher_conditions`, `voucher_usage`.
- **Quy tắc nghiệp vụ**: BR-VOUCHER-01 (mã unique), BR-VOUCHER-03 (ngày hiệu lực hợp lệ).
- **Luồng chính**:
  1. Gửi `POST /api/v1/vouchers` với thông tin.
  2. Hệ thống kiểm tra trùng mã, phạm vi ngày, hạn mức sử dụng.
  3. Lưu voucher, điều kiện liên quan.
  4. Trả về chi tiết voucher.
- **Luồng thay thế**:
  - 2a. Mã tồn tại -> HTTP 409.
  - 2b. Ngày không hợp lệ -> HTTP 400.
  - 3a. Lưu DB lỗi -> rollback, HTTP 500.

### UC-09: Nhập kho
- **Mô tả**: Nhân viên kho ghi nhận đơn hàng từ nhà cung cấp.
- **Diễn viên**: Nhân viên kho, Nhà cung cấp (gián tiếp).
- **Tiền điều kiện**: Supplier đã được đăng ký, kho có quyền nhập hàng.
- **Hậu điều kiện**: `PurchaseOrder` trạng thái RECEIVED, tồn kho cập nhật.
- **Dữ liệu sử dụng**: `purchase_orders`, `purchase_order_details`, `ingredients`.
- **Quy tắc nghiệp vụ**: BR-INVENTORY-01 (không nhập số âm), BR-INVENTORY-02 (ghi nhận thời gian nhận).
- **Luồng chính**:
  1. Tạo PO mới `POST /api/v1/purchase-orders` với danh sách nguyên liệu.
  2. Khi hàng về, cập nhật trạng thái `RECEIVED`, nhập số lượng thực tế.
  3. Hệ thống cập nhật tồn kho, ghi lịch sử nhập.
  4. Trả về PO chi tiết.
- **Luồng thay thế**:
  - 2a. Số lượng thực tế < đặt -> tạo cảnh báo.
  - 2b. Hủy PO -> chuyển `CANCELLED`, ghi lý do.

### UC-10: Chấm công ca làm
- **Mô tả**: Ghi nhận check-in/check-out cho ca làm đã phân công.
- **Diễn viên**: Nhân viên, Quản lý nhân sự.
- **Tiền điều kiện**: Ca đã được phân công, trạng thái `SCHEDULED`.
- **Hậu điều kiện**: Bản ghi attendance được lưu, cập nhật sang payroll.
- **Dữ liệu sử dụng**: `shift_assignments`, `attendance_records`, `payroll_summary`.
- **Quy tắc nghiệp vụ**: BR-SHIFT-01 (check-in trong ±15 phút), BR-SHIFT-02 (ghi chú bắt buộc khi điều chỉnh).
- **Luồng chính**:
  1. Nhân viên gửi `POST /api/v1/attendance/check-in`.
  2. Hệ thống xác minh assignment, lưu thời gian.
  3. Kết thúc ca, nhân viên check-out.
  4. Tính tổng giờ, ghi attendance record.
  5. Khi chốt payroll, tổng hợp dữ liệu attendance.
- **Luồng thay thế**:
  - 1a. Check-in ngoài khung giờ -> thông báo cần phê duyệt.
  - 3a. Quên check-out -> quản lý nhập tay, ghi chú bắt buộc.

### UC-11: Xuất báo cáo doanh thu
- **Mô tả**: Quản lý lấy báo cáo doanh thu theo khoảng thời gian.
- **Diễn viên**: Quản lý, Admin.
- **Tiền điều kiện**: Quyền ROLE_MANAGER+, dữ liệu trong khoảng ngày.
- **Hậu điều kiện**: Báo cáo JSON/Excel được trả, ghi nhật ký request.
- **Dữ liệu sử dụng**: `orders`, `order_details`, `payments`, `vouchers`.
- **Quy tắc nghiệp vụ**: BR-REPORT-01 (chỉ tính order PAID), BR-REPORT-02 (ghi audit).
- **Luồng chính**:
  1. Gọi `GET /api/v1/reports/revenue?from=&to=`.
  2. Service xác thực khoảng thời gian, quyền.
  3. Truy vấn dữ liệu, tổng hợp chỉ tiêu.
  4. Tùy chọn xuất Excel qua Apache POI.
  5. Trả kết quả và ghi audit log.
- **Luồng thay thế**:
  - 2a. Khoảng ngày không hợp lệ -> HTTP 400.
  - 3a. Không có dữ liệu -> trả báo cáo trống, hiển thị thông báo.

## 4. Quan hệ phụ thuộc giữa các use case
- UC-01 là tiền đề cho mọi UC yêu cầu xác thực.
- UC-05 phụ thuộc UC-03, UC-04 (dữ liệu sản phẩm) và UC-08 (voucher).
- UC-06 cập nhật dữ liệu cho UC-11, UC-12.
- UC-09 cung cấp dữ liệu tồn kho cho UC-05.
- UC-10 là đầu vào của UC-09 Payroll (tài liệu `thiet-ke-module.md`).

---
**Mức độ hoàn thiện:** 100%
**Hạng mục còn thiếu:** Không
