# Kịch Bản Kiểm Thử

## 1. Kịch bản đăng nhập – bán hàng – thanh toán
**Mục tiêu**: Đảm bảo quy trình bán hàng hoàn chỉnh hoạt động ổn định.

| Bước | Mô tả | Kết quả mong đợi |
|------|-------|------------------|
| 1 | Đăng nhập bằng tài khoản staff | JWT hợp lệ, chuyển tới màn hình POS |
| 2 | Chọn bàn A1, tạo đơn với 3 món | Đơn PENDING, tính tổng tiền chính xác |
| 3 | Áp dụng voucher hợp lệ | Giảm giá đúng theo điều kiện |
| 4 | Xác nhận thanh toán tiền mặt | Đơn chuyển sang PAID, ghi `paidAt`, bàn AVAILABLE |
| 5 | Tra cứu lịch sử đơn | Đơn xuất hiện trong danh sách, số tiền khớp |

**Kiểm tra bổ sung**: log hệ thống, tồn kho giảm đúng.

## 2. Kịch bản quản lý kho
**Mục tiêu**: Đảm bảo nhập – xuất – kiểm kê hoạt động chính xác.

1. Đăng nhập với quyền `ROLE_MANAGER`.
2. Tạo nguyên liệu mới (Arabica Beans).
3. Tạo công thức sản phẩm sử dụng nguyên liệu.
4. Ghi nhận nhập kho 10kg.
5. Bán 5 ly cà phê -> tồn giảm tương ứng.
6. Kiểm kê thực tế còn 4.8kg -> ghi điều chỉnh.
7. Hệ thống cập nhật tồn kho = 4.8kg, tạo bản ghi `InventoryAdjustment`.

## 3. Kịch bản quản lý nhân sự & bảng lương
1. HR tạo `ShiftTemplate` (Ca sáng 7h-15h).
2. Phân công nhân viên Staff01 cho ngày 10/11.
3. Nhân viên check-in lúc 06:55 (ghi nhận đúng giờ).
4. Check-out lúc 15:05 (tính 8h10p, áp dụng quy tắc làm tròn).
5. Quản lý ghi nhận thưởng ca +50.000đ.
6. Tạo `PayrollCycle` tháng 11, tổng hợp bảng lương.
7. Kiểm tra `PayrollSummary` hiển thị thời gian và tiền thưởng chính xác.

## 4. Kịch bản báo cáo & dashboard
1. Quản lý đăng nhập vào dashboard.
2. Chọn khoảng thời gian 01-07/11.
3. Hệ thống hiển thị tổng doanh thu, số đơn, top sản phẩm.
4. So sánh dữ liệu với câu truy vấn thủ công trong DB.
5. Xuất báo cáo Excel và mở file kiểm tra định dạng.

## 5. Kịch bản bảo mật
- Thử đăng nhập bằng mật khẩu sai 5 lần → tài khoản bị khóa tạm thời.
- Truy cập endpoint `/api/v1/users` bằng token staff → nhận 403.
- Sử dụng JWT hết hạn → nhận 401.
- Thử SQL injection vào trường tìm kiếm khách hàng → hệ thống trả lỗi hợp lệ, không crash.

## 6. Kịch bản hiệu năng
- Dùng k6 mô phỏng 200 user đồng thời tạo đơn trong 10 phút.
- Theo dõi p95 response < 500ms, CPU < 70%, lỗi < 1%.
- Sau test, kiểm tra DB để đảm bảo dữ liệu không bị trùng.

## 7. Kịch bản khôi phục sau sự cố
1. Sao lưu dữ liệu trước (mysqldump, snapshot uploads).
2. Xóa nhầm bảng `orders` (trên staging) → restore từ backup.
3. Chạy lại migration để đảm bảo schema đồng bộ.
4. Thực hiện smoke test xác nhận hệ thống hoạt động bình thường.

## 8. Kịch bản cập nhật phiên bản mới
1. Triển khai build mới trên staging.
2. Chạy toàn bộ bộ test tự động.
3. QA thực thi test regression quan trọng.
4. DevOps deploy production bằng Docker image mới.
5. Theo dõi log và metric 30 phút.
6. Nếu phát hiện lỗi nghiêm trọng → rollback theo tài liệu triển khai.
