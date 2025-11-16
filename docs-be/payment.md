# Chức năng: Thanh toán đơn hàng (Payment)

## Vai trò trong hệ thống
- Xử lý thanh toán cho đơn hàng PENDING: áp dụng voucher, cập nhật trạng thái, cộng điểm khách hàng.
- Trừ tồn kho nguyên liệu dựa trên công thức sản phẩm.
- Ghi nhận phương thức thanh toán, thời gian thanh toán, cập nhật bàn.

## Luồng xử lý backend
1. **Endpoint gọi service**: `OrderController.payOrder` (`POST /api/v1/orders/{orderId}/payment`) gọi `PaymentService.processPayment` @src/main/java/com/giapho/coffee_shop_backend/controller/OrderController.java#116-123 @src/main/java/com/giapho/coffee_shop_backend/service/PaymentService.java#39-189.
2. **Xử lý chính (`processPayment`)**:
   - Lấy order (bao gồm customer) bằng `orderRepository.findByIdWithCustomer`.
   - Kiểm tra trạng thái `PENDING`; nếu khác → `IllegalStateException`.
   - Chuẩn hóa phương thức thanh toán (`CASH`, `TRANSFER`, `CARD`), nếu không hợp lệ → `IllegalArgumentException`.
   - Nếu request chứa `customerId` và order chưa có customer → gán vào order.
   - Xác định subtotal; nếu request có `voucherCode` → gọi `VoucherService.checkAndCalculateDiscount` để kiểm tra và áp dụng.
   - Tính total = subtotal - discount (không âm).
   - Gọi `subtractInventoryForOrder` để trừ tồn kho nguyên liệu theo công thức sản phẩm.
   - Cập nhật order: `status = PAID`, `paidAt = now`, `paymentMethod = method`.
   - Gọi `CustomerService.updateLoyaltyPoints` để cộng điểm (nếu order có customer).
   - Lưu order (`orderRepository.save`).
   - Nếu order có voucher → `voucherService.incrementUsageCount`.
   - Trả `Order` đã cập nhật (được mapper thành `OrderResponseDTO` ở controller).
3. **subtractInventoryForOrder**:
   - Với từng `OrderDetail`: lấy `Product`, tìm công thức (`ProductIngredientRepository.findByProductId`).
   - Với mỗi `ProductIngredient`: lấy ingredient bằng `IngredientRepository.findByIdForUpdate` (lock) @src/main/java/com/giapho/coffee_shop_backend/service/PaymentService.java#164-176.
   - Tính lượng cần trừ = quantityNeededPerProduct × orderQuantity.
   - Nếu tồn kho < lượng cần → `IllegalArgumentException` (dừng thanh toán).
   - Trừ tồn kho và lưu ingredient.
4. **updateCustomerLoyaltyPoints**: dùng `CustomerService.updateLoyaltyPoints(customerId, totalAmount)` (theo tiers 30k/50k/100k) @src/main/java/com/giapho/coffee_shop_backend/service/CustomerService.java#68-95.
5. **Voucher**: áp dụng/loại bỏ thông qua `OrderService.applyVoucher`/`removeVoucher`, nhưng `PaymentService` đảm bảo voucher hợp lệ tại thời điểm thanh toán.

## Thành phần liên quan
- **Controller**: `OrderController` (`POST /orders/{orderId}/payment`)
- **Service**: `PaymentService`
- **Repository**: `OrderRepository`, `CustomerRepository`, `IngredientRepository`, `ProductIngredientRepository`
- **DTO**: `PaymentRequestDTO`, `OrderResponseDTO`
- **Entity**: `Order`, `OrderDetail`, `Ingredient`, `ProductIngredient`, `Customer`
- **Service phụ trợ**: `CustomerService`, `VoucherService`
- **Security**: endpoint yêu cầu `hasAnyRole('STAFF','MANAGER','ADMIN')`.

## API liên quan
| Method | URL | Mô tả | Role |
| --- | --- | --- | --- |
| POST | `/api/v1/orders/{orderId}/payment` | Thanh toán đơn hàng | `STAFF`,`MANAGER`,`ADMIN` |

### Request (`PaymentRequestDTO`)
```json
{
  "paymentMethod": "CASH",
  "amountPaid": 120000,
  "customerId": 15,
  "voucherCode": "SUMMER25"
}
```
- `paymentMethod` bắt buộc (CASH/TRANSFER/CARD).
- `customerId` optional: gán customer nếu order chưa có.
- `voucherCode` optional: re-validate voucher trước khi thanh toán.

### Response 200 (`OrderResponseDTO`)
- Đơn hàng với trạng thái `PAID`, `paidAt`, `paymentMethod`, `totalAmount` cập nhật.
- Các trường khác (table, items, voucher, customer…) giữ nguyên.

### Lỗi thường gặp
| Lỗi | HTTP | Thông điệp |
| --- | --- | --- |
| `IllegalStateException` | 400 | "Cannot pay order with status: ..." |
| `IllegalArgumentException` | 400 | "Invalid payment method", "Not enough stock for ingredient ...", "Voucher ..." |
| `EntityNotFoundException` | 404 | "Order not found ...", "Ingredient not found ...", "Customer not found ..." |

## Điều kiện nghiệp vụ
- Chỉ thanh toán đơn `PENDING`.
- Phải đảm bảo tồn kho đủ trước khi trừ; nếu thiếu, thanh toán bị chặn.
- Voucher phải còn hiệu lực, đủ điều kiện (kiểm tra lại qua `VoucherService`).
- Tổng tiền không âm; discount tối đa = subtotal.
- Điểm loyalty chỉ cộng khi order có customer và total > 0.

## Quan hệ với chức năng khác
- **OrderService**: cung cấp các API khác (add/remove items, apply voucher) trước khi thanh toán.
- **VoucherService**: xác thực voucher, đếm lượt sử dụng.
- **IngredientService/Inventory**: tồn kho được cập nhật theo công thức sản phẩm.
- **CustomerService**: cộng điểm loyalty.
- **ReportService**: doanh thu/chi phí lấy từ order đã thanh toán.

## Các tệp liên quan
- Controller: `OrderController.java`
- Service: `PaymentService.java`, `CustomerService.java`, `VoucherService.java`
- Repository: `OrderRepository.java`, `IngredientRepository.java`, `ProductIngredientRepository.java`, `CustomerRepository.java`
- DTO: `PaymentRequestDTO.java`, `OrderResponseDTO.java`
- Entity: `Order.java`, `OrderDetail.java`, `Ingredient.java`, `ProductIngredient.java`
