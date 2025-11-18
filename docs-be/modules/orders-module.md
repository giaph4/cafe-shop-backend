# Tài liệu module Đơn hàng

## Tổng quan
- Quản lý vòng đời đơn hàng: tạo, cập nhật món, áp dụng voucher, thanh toán, hủy.
- Đảm bảo tất cả nghiệp vụ giữ nguyên logic hiện tại, áp dụng chuẩn clean code.

## API chính
1. `POST /api/v1/orders` – tạo đơn mới.
2. `GET /api/v1/orders` – phân trang đơn hàng.
3. `GET /api/v1/orders/{id}` – xem chi tiết.
4. `GET /api/v1/orders/table/{tableId}/pending` – đơn pending theo bàn.
5. `POST /api/v1/orders/{orderId}/items` – thêm món.
6. `PUT /api/v1/orders/{orderId}/items/{orderDetailId}` – cập nhật món.
7. `DELETE /api/v1/orders/{orderId}/items/{orderDetailId}` – xóa món.
8. `POST /api/v1/orders/{orderId}/payment` – thanh toán.
9. `POST /api/v1/orders/{orderId}/voucher` – gán voucher.
10. `DELETE /api/v1/orders/{orderId}/voucher` – bỏ voucher.
11. `GET /api/v1/orders/status/{status}` – lọc theo trạng thái.
12. `GET /api/v1/orders/date-range` – lọc theo khoảng ngày.
13. `PUT /api/v1/orders/{orderId}/cancel` – hủy đơn pending.

## Kiến trúc
- `OrderService` (interface) định nghĩa contract.
- `OrderServiceImpl` xử lý nghiệp vụ chính, ủy thác pricing cho `OrderPricingService` và truy vấn cho `OrderQueryService`.
- `OrderPricingService` tái sử dụng cho recalculation, voucher.
- `OrderValidator` kiểm tra trạng thái bàn, sản phẩm, order.
- `PaymentService` giữ vai trò thanh toán, cập nhật kho và khách hàng.

## Ngoại lệ & Response
- Sử dụng `BusinessException` với các subclass: `OrderNotFoundException`, `OrderInvalidStateException`, `OrderDetailNotFoundException`, `TableNotFoundException`, `ProductUnavailableException`, `VoucherInvalidException`, `PaymentMethodInvalidException`, `InsufficientInventoryException`.
- `GlobalExceptionHandler` trả `ErrorResponse` chuẩn.

## Quy trình
- Tạo đơn: validate bàn, khách, items, tính subtotal -> save.
- Thêm/cập nhật/xóa món: xác thực order pending, sản phẩm khả dụng, recalculation.
- Voucher: chuẩn hóa mã, gọi `VoucherService.checkAndCalculateDiscount`, cập nhật discount/total.
- Thanh toán: `PaymentService` xử lý payment method, loyalty, inventory (kho). Bàn chuyển trạng thái nếu không còn đơn pending.
- Hủy đơn: chỉ cho phép PENDING.

## Kiểm thử
- Cập nhật `OrderServiceTest` tùy thay đổi API nội bộ.
- Tự viết thêm test cho pricing, voucher, validator.

## Ghi chú
- Giữ nguyên contract controller.
- Mọi comment trong code ngắn gọn, nêu lý do các bước đặc biệt.
- Format code theo chuẩn Google Java Style.
