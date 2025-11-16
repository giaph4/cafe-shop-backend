# 📘 Payment API & PaymentService Guide

Tài liệu này mô tả chi tiết luồng thanh toán sau khi tách PaymentService, nhằm giúp frontend (FE) tích hợp chính xác.

## 1. Tổng quan kiến trúc

```
Client (FE) --> OrderController.payOrder --> OrderService.payOrder --> PaymentService.processPayment
                                             |--> PaymentService.subtractInventoryForOrder
                                             |--> PaymentService.updateCustomerLoyaltyPoints
```

## 2. Endpoint thanh toán

- **URL**: `POST /api/v1/orders/{orderId}/payment`
- **Auth**: Bearer Token (vai trò `STAFF`, `MANAGER`, `ADMIN`)
- **Headers**:
  - `Authorization: Bearer <token>`
  - `Content-Type: application/json`

### 2.1 Request body

```json
{
  "paymentMethod": "CASH | TRANSFER | CARD",
  "customerId": 123,
  "voucherCode": "SAVE20"
}
```

| Trường | Kiểu dữ liệu | Bắt buộc | Mô tả |
|--------|---------------|----------|-------|
| `paymentMethod` | string | ✔ | Chỉ nhận `CASH`, `TRANSFER`, `CARD` (không phân biệt hoa thường). |
| `customerId` | number | ✖ | Nếu order chưa có khách hàng, FE gửi ID để gắn khách hàng vào order trước khi thanh toán. |
| `voucherCode` | string | ✖ | Mã voucher hợp lệ. BE sẽ tính lại tổng tiền và cập nhật lượt sử dụng nếu thanh toán thành công. |

> ⚠️ Nếu FE cố gắng thanh toán đơn không ở trạng thái `PENDING`, backend trả về lỗi 400 với thông báo: `Cannot pay order with status <status>`. FE cần hiển thị thông báo phù hợp.

### 2.2 Response body (OrderResponseDTO)

```json
{
  "id": 10,
  "tableName": "T1",
  "staffUsername": "staff01",
  "type": "ON_SITE",
  "status": "PAID",
  "subTotal": 150000,
  "discountAmount": 15000,
  "totalAmount": 135000,
  "customerId": 501,
  "customerName": "Nguyen Van A",
  "customerPhone": "0909...",
  "voucherCode": "SAVE10",
  "createdAt": "2025-01-10T08:01:22",
  "paidAt": "2025-01-10T08:10:05",
  "paymentMethod": "CASH",
  "customerLoyaltyPoints": 120,
  "orderDetails": [
    {
      "productId": 200,
      "productName": "Mocha",
      "quantity": 3,
      "price": 50000
    }
  ]
}
```

Frontend có thể dùng `status`, `paidAt`, `paymentMethod` để cập nhật UI ngay sau khi thanh toán thành công.

## 3. Các nhánh logic chính trong PaymentService

### 3.1 Xác thực đơn hàng & trạng thái
- Tìm order bằng `orderRepository.findByIdWithCustomer(orderId)` – nếu không thấy ném `EntityNotFoundException`.
- Chỉ chấp nhận trạng thái `PENDING`. Nếu khác → `IllegalStateException` với thông báo rõ ràng.

### 3.2 Kiểm tra phương thức thanh toán
- Chuẩn hóa và validate `paymentMethod`. Nếu không nằm trong tập `[CASH, TRANSFER, CARD]` → `IllegalArgumentException`.
- FE cần hiển thị thông báo “Phương thức thanh toán không hợp lệ”.

### 3.3 Gắn khách hàng (tuỳ chọn)
- Nếu request có `customerId` và order chưa có khách: gọi `customerRepository.findById`. Nếu không tồn tại, trả về 404.
- Khi gắn thành công, BE log lại và tiếp tục các bước còn lại.

### 3.4 Trừ tồn kho
- Dựa vào `OrderDetail` và công thức (`ProductIngredient`), truy vấn nguyên liệu bằng khóa pessimistic (`ingredientRepository.findByIdForUpdate`).
- Nếu số lượng tồn kho không đủ → `IllegalArgumentException` với thông điệp “Not enough stock…“.
- **Lưu ý cho FE**: Khi nhận lỗi này, hiển thị thông báo “Kho không đủ nguyên liệu, vui lòng thử lại sau”.

### 3.5 Hoàn tất đơn & cộng điểm loyalty
- Cập nhật trạng thái `PAID`, set `paidAt` là thời gian hiện tại, lưu `paymentMethod` đã chuẩn hóa.
- Gọi `customerService.updateLoyaltyPoints(customerId, totalAmount)` trong `try/catch`. Nếu cộng điểm lỗi, BE log nhưng không rollback thanh toán (đảm bảo order vẫn PAID).
- Nếu thanh toán kèm voucher, BE tự động gọi `voucherService.incrementUsageCount` để ghi nhận lượt sử dụng thành công.

## 4. Kịch bản lỗi & cách xử lý ở FE

| Tình huống | HTTP Status | Message | Gợi ý hiển thị |
|------------|-------------|---------|----------------|
| Order không tồn tại | 404 | `Order not found with id: ...` | “Đơn hàng không tồn tại hoặc đã bị xoá.” |
| Order không ở trạng thái PENDING | 400 | `Cannot pay order with status ...` | “Đơn đã được thanh toán hoặc không khả dụng.” |
| Phương thức thanh toán sai | 400 | `Invalid payment method...` | “Phương thức thanh toán không hợp lệ.” |
| Vi phạm khóa ngoại/unique | 409 | `Foreign key constraint violation / Duplicate data violates unique constraint` | Thông báo chi tiết, hướng dẫn kiểm tra dữ liệu. |
| Không đủ tồn kho | 400 | `Not enough stock for ingredient ...` | “Kho không đủ nguyên liệu, liên hệ quản lý.” |

## 5. Hướng phát triển tiếp theo

1. **Voucher thanh toán**: mở rộng `PaymentRequestDTO` để FE truyền mã voucher, PaymentService kiểm tra hợp lệ trước khi cộng điểm.
2. **Điểm thưởng nâng cao**: thêm logic quy đổi điểm hoặc rank khách hàng, tách sang `CustomerLoyaltyService` chuyên biệt.
3. **Log giao dịch**: lưu nhật ký thanh toán (phương thức, thời gian, nhân viên) để phục vụ báo cáo tài chính.

FE nên chuẩn bị UI để:
- Cho phép chọn khách hàng (autocomplete) khi order chưa có khách.
- Chỉ cho phép thanh toán khi order đang PENDING.
- Hiển thị thông báo cụ thể dựa trên `message` backend trả về (đặc biệt 409 Constraint).

---
📞 Mọi câu hỏi thêm, liên hệ backend team để đồng bộ logic mới trước khi phát hành sản phẩm.
