# Quy Tắc Nghiệp Vụ

## Bán hàng & thanh toán
1. Đơn hàng ở trạng thái **PENDING** trước khi thanh toán.
2. Chỉ đơn hàng PENDING mới được thanh toán hoặc hủy.
3. Thanh toán thành công chuyển trạng thái sang **PAID**, ghi `paidAt` và phương thức thanh toán.
4. Hủy đơn chuyển trạng thái **CANCELLED** và ghi lý do.
5. Mọi đơn PAID đều cập nhật tồn kho (trừ món không dùng nguyên liệu).
6. Bàn chỉ có tối đa 1 đơn PENDING.
7. Áp dụng voucher trước khi thanh toán; voucher chỉ dùng một lần cho mỗi đơn.

## Voucher & khuyến mãi
1. Voucher có ngày hiệu lực (`validFrom`, `validTo`).
2. Voucher chỉ dùng khi đơn đạt giá trị tối thiểu (`minOrderTotal`).
3. Số lần sử dụng bị giới hạn bởi `usageLimit`.
4. Voucher dạng phần trăm không vượt `maxDiscount`.
5. Voucher có thể giới hạn khách hàng (theo nhóm/tên).
6. Mã voucher trùng hoặc hết hạn phải bị từ chối ngay trong service.

## Quản lý kho & nguyên liệu
1. Mọi nguyên liệu có `reorderPoint`, nếu tồn thấp hơn phải tạo PO.
2. Nhập kho cập nhật số lượng, ghi nhận nhà cung cấp.
3. Xuất kho dựa trên công thức sản phẩm (bom/recipe).
4. Kiểm kê định kỳ ghi `InventoryAdjustment` và tạo audit.
5. Không cho phép tồn kho âm trừ khi bật chế độ cho phép (config).

## Khách hàng & loyalty
1. Điểm tích lũy tăng theo giá trị đơn (ví dụ 1 điểm/10.000đ).
2. Điểm trừ khi đổi voucher loyalty.
3. Khách mới mặc định tier Bronze.
4. Khóa khách hàng khi vi phạm (spam voucher) – vẫn giữ lịch sử.

## Ca làm & nhân sự
1. Check-in muộn hoặc check-out sớm có thể tạo `ShiftPerformanceAdjustment`.
2. Ca làm bắt buộc gắn với nhân viên và ngày.
3. Bảng lương tổng hợp theo `PayrollCycle` (tháng/quý).
4. Tăng ca cần duyệt bởi quản lý trước khi ghi nhận.

## Chi phí
1. Mọi chi phí phải kèm danh mục và chứng từ (tập tin).
2. Chi phí ngoài chính sách cần phê duyệt (status PENDING → APPROVED).

## Audit & bảo mật
1. Ghi audit khi thao tác nhạy cảm: cập nhật giá, xoá đơn, đổi quyền.
2. Tự động khóa tài khoản sau N lần đăng nhập sai liên tiếp.
3. JWT chỉ hợp lệ trong 24 giờ, sau đó phải refresh.

## Báo cáo & dashboard
1. Dashboard mặc định lấy dữ liệu 7 ngày gần nhất.
2. Báo cáo doanh thu chỉ tính đơn PAID.
3. Top sản phẩm dựa trên `OrderDetail.quantity`.
4. Hiệu suất nhân viên dựa trên doanh thu cá nhân (Order.user).
