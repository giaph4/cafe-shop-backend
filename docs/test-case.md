# Test Case

## Bảng test case chức năng chính
| ID | Use case | Mô tả | Bước kiểm thử | Kết quả mong đợi |
|----|----------|-------|---------------|------------------|
| TC-01 | UC-01 Đăng nhập | Đăng nhập thành công | 1. Gửi POST `/auth/login` với thông tin hợp lệ.<br>2. Kiểm tra response. | HTTP 200, trả JWT, ghi `LoginHistory` success. |
| TC-02 | UC-01 Đăng nhập | Đăng nhập sai mật khẩu | 1. Gửi POST `/auth/login` với mật khẩu sai 3 lần.<br>2. Quan sát response và log. | HTTP 401, thông báo lỗi, ghi `LoginHistory` fail, áp dụng policy khóa nếu vượt ngưỡng. |
| TC-03 | UC-05 Tạo đơn | Tạo đơn hàng tại bàn trống | 1. Gửi POST `/orders` với bàn trống, danh sách món hợp lệ.<br>2. Kiểm tra tồn kho sau khi tạo. | HTTP 201/200, trả order status PENDING, tồn kho giảm theo định mức. |
| TC-04 | UC-05 Tạo đơn | Bàn đã có đơn PENDING | 1. Tạo đơn PENDING cho bàn.<br>2. Gửi POST `/orders` lần nữa cùng bàn. | HTTP 409, thông báo đang có đơn chờ. |
| TC-05 | UC-06 Thanh toán | Thanh toán đơn hợp lệ | 1. Tạo đơn PENDING.<br>2. Gửi POST `/orders/{id}/pay` với số tiền chính xác. | HTTP 200, order chuyển PAID, ghi `paidAt`, bàn AVAILABLE. |
| TC-06 | UC-06 Thanh toán | Thanh toán sai số tiền | 1. Tạo đơn PENDING.<br>2. Gửi pay với số tiền < tổng. | HTTP 400, thông báo số tiền không hợp lệ. |
| TC-07 | UC-08 Voucher | Áp dụng voucher hợp lệ | 1. Gửi POST `/vouchers/validate` với đơn đạt điều kiện.<br>2. Quan sát giảm giá. | HTTP 200, trả discountValue, `usedCount` tăng sau thanh toán. |
| TC-08 | UC-08 Voucher | Voucher hết hạn | 1. Gửi validate với voucher hết hạn. | HTTP 400, thông báo voucher expired. |
| TC-09 | UC-09 Nhập kho | Tạo PO mới | 1. POST `/purchase-orders` với supplier, danh sách nguyên liệu.<br>2. Duyệt nhận hàng. | HTTP 201, trạng thái CREATED -> RECEIVED, tồn kho tăng. |
| TC-10 | UC-10 Chấm công | Check-in/check-out | 1. POST `/attendance/check-in` đúng giờ.<br>2. POST `/attendance/check-out`.<br>3. Kiểm tra PayrollSummary. | Thời gian làm việc được ghi nhận, Payroll cập nhật tổng giờ. |
| TC-11 | UC-11 Báo cáo | Xuất báo cáo doanh thu | 1. Gọi GET `/reports/revenue?from=&to=`.<br>2. Kiểm tra dữ liệu trả về hoặc file Excel. | HTTP 200, số liệu khớp với DB. |
| TC-12 | Bảo mật | Truy cập endpoint không quyền | 1. Login với ROLE_STAFF.<br>2. Gửi DELETE `/products/{id}`. | HTTP 403, không xóa sản phẩm. |
| TC-13 | Bảo mật | JWT hết hạn | 1. Sử dụng token hết hạn.<br>2. Gọi API bất kỳ. | HTTP 401, yêu cầu đăng nhập lại. |
| TC-14 | Hiệu năng | Tải cao tạo đơn | 1. Dùng JMeter gửi 200 req/s tạo đơn song song.<br>2. Theo dõi thời gian phản hồi. | Tỷ lệ lỗi <1%, p95 < 500ms. |

## Test dữ liệu cần chuẩn bị
- Người dùng: admin (ROLE_ADMIN), manager (ROLE_MANAGER), staff (ROLE_STAFF).
- Danh mục & sản phẩm mẫu.
- Voucher (hợp lệ, hết hạn, hết lượt).
- Bàn (AVAILABLE, IN_USE).
- Khách hàng có điểm loyalty.
- Dữ liệu ca làm, bảng lương mẫu cho test payroll.
