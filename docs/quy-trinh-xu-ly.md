# Quy Trình Xử Lý

## Mục lục
- [1. Bán hàng tại quầy](#1-bán-hàng-tại-quầy)
- [2. Quản lý voucher](#2-quản-lý-voucher)
- [3. Kiểm kê kho](#3-kiểm-kê-kho)
- [4. Phân ca & chấm công](#4-phân-ca--chấm-công)
- [5. Xử lý chi phí](#5-xử-lý-chi-phí)
- [6. Báo cáo cuối ngày](#6-báo-cáo-cuối-ngày)
- [7. Quản lý cấu hình hệ thống](#7-quản-lý-cấu-hình-hệ-thống)
- [8. Quy trình xử lý sự cố](#8-quy-trình-xử-lý-sự-cố)

## 1. Bán hàng tại quầy
| Bước | Tác nhân | Hành động | Hệ thống |
|------|----------|-----------|----------|
| 1 | Nhân viên | Đăng nhập POS | JWT xác thực, ghi `LoginHistory` |
| 2 | Nhân viên | Chọn bàn, tạo đơn mới | `OrderService` kiểm tra bàn, tạo Order PENDING |
| 3 | Nhân viên | Thêm món | Kiểm tra tồn kho, cập nhật `OrderDetail` |
| 4 | Nhân viên | Áp dụng voucher | `VoucherService` validate, cập nhật giảm giá |
| 5 | Nhân viên | Xem tổng tiền | `OrderService` tính toán `subTotal`, `discount`, `total` |
| 6 | Nhân viên | Thanh toán | `PaymentService` cập nhật trạng thái PAID, ghi `paidAt` |
| 7 | Nhân viên | In hóa đơn/giao khách | Hệ thống giải phóng bàn, ghi doanh thu |

**Pseudocode tóm tắt**
```java
Order createOrder(CreateOrderRequest req) {
    CafeTable table = tableService.validateAvailable(req.tableId);
    Order order = orderMapper.fromRequest(req);
    order.setStatus(Status.PENDING);
    order.addDetails(loadProducts(req.items));
    if (req.voucherCode != null) {
        VoucherResult result = voucherService.validate(order, req.voucherCode);
        order.applyDiscount(result);
    }
    order.recalculateAmounts();
    return orderRepository.save(order);
}
```

## 2. Quản lý voucher
1. Quản lý đăng nhập hệ thống.
2. Gửi `POST /api/v1/vouchers` với mã, loại, giá trị, điều kiện.
3. `VoucherService` kiểm tra trùng mã, ngày hiệu lực, usage limit.
4. Lưu vào DB, đồng bộ cache (nếu bật Redis).
5. Khi checkout, thu ngân gọi `/vouchers/validate` để xác nhận.
6. Sau thanh toán thành công, cập nhật `usedCount`, lưu lịch sử sử dụng.

## 3. Kiểm kê kho
1. Nhân viên kho xuất danh sách tồn (`GET /ingredients`).
2. Thực hiện kiểm kê thực tế, ghi số liệu chênh lệch.
3. Gửi `POST /inventory/adjust` với `delta`, `reason`.
4. Hệ thống tạo bản ghi `InventoryAdjustment`, cập nhật `current_stock`.
5. Nếu `current_stock < reorder_point`, phát cảnh báo tạo `purchase-order` mới.

## 4. Phân ca & chấm công
1. HR tạo `ShiftTemplate`, lên lịch tuần bằng `POST /shifts/assign`.
2. Nhân viên check-in (`POST /attendance/check-in`), hệ thống kiểm tra assignment.
3. Kết thúc ca check-out, `AttendanceRecord` cập nhật `checkOutAt`.
4. Nếu cần điều chỉnh, quản lý tạo `ShiftPerformanceAdjustment`.
5. Kết thúc chu kỳ, `PayrollService` tổng hợp dữ liệu attendance → `PayrollSummary`.

## 5. Xử lý chi phí
1. Kế toán ghi chi phí mới (`POST /expenses`) kèm chứng từ upload.
2. Chi phí mặc định trạng thái `PENDING`.
3. Quản lý phê duyệt (`PATCH /expenses/{id}/approve`) → trạng thái `APPROVED`.
4. Khi từ chối, cập nhật `REJECTED` và lưu lý do.
5. Dữ liệu được đưa vào báo cáo tài chính (`/reports/cost`).

## 6. Báo cáo cuối ngày
1. Cuối ca, quản lý gọi `GET /reports/revenue?from=&to=`.
2. `ReportService` tổng hợp doanh thu, số đơn, voucher, chi phí.
3. Tùy chọn `format=excel` để xuất file, ghi lại `AuditLog`.
4. Quản lý lưu báo cáo, ký xác nhận (ngoài hệ thống).

## 7. Quản lý cấu hình hệ thống
1. Admin cập nhật biến môi trường/Secret Manager (ví dụ CORS, JWT secret).
2. Triển khai lại dịch vụ qua pipeline CI/CD (rolling deploy).
3. Ghi chú thay đổi vào `audit_logs` hoặc `document-log.md`.
4. Thực hiện smoke test sau khi áp dụng cấu hình.

## 8. Quy trình xử lý sự cố
1. Nhận cảnh báo từ monitoring (Prometheus/Grafana) hoặc phản hồi người dùng.
2. Giải phóng truy vết: kiểm tra log trong ELK theo `traceId`.
3. Xác định nguyên nhân gốc (RCA), tạo ticket.
4. Thử nghiệm bản vá trên staging, chạy regression.
5. Deploy production, theo dõi sau deploy ≥ 30 phút.
6. Cập nhật tài liệu vận hành, close incident.

---
**Mức độ hoàn thiện:** 100%
**Hạng mục còn thiếu:** Không
