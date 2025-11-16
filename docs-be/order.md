# Chức năng: Quản lý đơn hàng & thanh toán

## Vai trò trong hệ thống
- Xử lý toàn bộ vòng đời đơn hàng tại quán (tạo, cập nhật món, thanh toán, hủy).
- Tích hợp voucher giảm giá, cập nhật tồn kho nguyên liệu, cộng điểm khách hàng thân thiết.
- Cung cấp API cho nhân viên/ quản lý kiểm soát trạng thái đơn và thống kê.

## Luồng xử lý backend
1. **Tạo đơn hàng** (`POST /api/v1/orders`): `OrderController.createOrder` nhận `OrderCreateRequestDTO`, `OrderService.createOrder` kiểm tra bàn/khách hàng, khởi tạo `Order` ở trạng thái `PENDING`, tính tổng tạm @src/main/java/com/giapho/coffee_shop_backend/controller/OrderController.java#45-50 @src/main/java/com/giapho/coffee_shop_backend/service/OrderService.java#91-137.
2. **Lấy danh sách & chi tiết**: APIs trả `OrderResponseDTO`, map qua `OrderMapper` @src/main/java/com/giapho/coffee_shop_backend/controller/OrderController.java#52-76 @src/main/java/com/giapho/coffee_shop_backend/service/OrderService.java#46-88.
3. **Quản lý món trong đơn**: thêm/sửa/xóa `OrderDetail` bằng các endpoint `/items`, service đảm bảo đơn ở trạng thái `PENDING` và tự tính lại tổng tiền @src/main/java/com/giapho/coffee_shop_backend/controller/OrderController.java#85-114 @src/main/java/com/giapho/coffee_shop_backend/service/OrderService.java#139-218.
4. **Voucher**: áp dụng / gỡ `voucherCode`, `OrderService.applyVoucher/removeVoucher` gọi `VoucherService.checkAndCalculateDiscount` @src/main/java/com/giapho/coffee_shop_backend/controller/OrderController.java#126-141 @src/main/java/com/giapho/coffee_shop_backend/service/OrderService.java#232-279.
5. **Thanh toán**: `POST /{orderId}/payment` gọi `PaymentService.processPayment` để cập nhật trạng thái `PAID`, trừ tồn kho, cộng điểm khách hàng, tăng số lần dùng voucher, giải phóng bàn @src/main/java/com/giapho/coffee_shop_backend/controller/OrderController.java#116-124 @src/main/java/com/giapho/coffee_shop_backend/service/OrderService.java#220-230 @src/main/java/com/giapho/coffee_shop_backend/service/PaymentService.java#39-189.
6. **Hủy đơn/điều chỉnh trạng thái**: `cancel` đổi trạng thái sang `CANCELLED`, cập nhật lại trạng thái bàn @src/main/java/com/giapho/coffee_shop_backend/controller/OrderController.java#164-168 @src/main/java/com/giapho/coffee_shop_backend/service/OrderService.java#291-304.
7. **Liên kết bàn & khách**: `OrderService` cập nhật tình trạng bàn qua `CafeTableRepository` khi tạo/hoàn tất @src/main/java/com/giapho/coffee_shop_backend/service/OrderService.java#107-140,#224-304.

## Thành phần liên quan
- **Controller**: `OrderController` @src/main/java/com/giapho/coffee_shop_backend/controller/OrderController.java#1-170
- **Service**: `OrderService`, `PaymentService`, `VoucherService`, `CustomerService` @src/main/java/com/giapho/coffee_shop_backend/service/OrderService.java#1-400 @src/main/java/com/giapho/coffee_shop_backend/service/PaymentService.java#1-191 @src/main/java/com/giapho/coffee_shop_backend/service/VoucherService.java#1-312 @src/main/java/com/giapho/coffee_shop_backend/service/CustomerService.java#1-236
- **Repository**: `OrderRepository`, `OrderDetailRepository`, `ProductRepository`, `CafeTableRepository`, `CustomerRepository`, `VoucherRepository`, `IngredientRepository`, `ProductIngredientRepository`
- **DTO**: `OrderCreateRequestDTO`, `OrderDetailRequestDTO`, `OrderDetailUpdateRequestDTO`, `OrderResponseDTO`, `PaymentRequestDTO`, `VoucherApplyRequestDTO`, `VoucherCheckResponseDTO` @src/main/java/com/giapho/coffee_shop_backend/dto/OrderCreateRequestDTO.java#1-25 @src/main/java/com/giapho/coffee_shop_backend/dto/OrderDetailRequestDTO.java#1-23 @src/main/java/com/giapho/coffee_shop_backend/dto/OrderDetailUpdateRequestDTO.java#1-19 @src/main/java/com/giapho/coffee_shop_backend/dto/OrderResponseDTO.java#1-31 @src/main/java/com/giapho/coffee_shop_backend/dto/PaymentRequestDTO.java#1-31 @src/main/java/com/giapho/coffee_shop_backend/dto/VoucherApplyRequestDTO.java#1-17 @src/main/java/com/giapho/coffee_shop_backend/dto/VoucherCheckResponseDTO.java#1-32
- **Entity**: `Order`, `OrderDetail`, `Product`, `CafeTable`, `Customer`, `Voucher`, `Ingredient`, `ProductIngredient`
- **Validation**: DTO sử dụng `@NotBlank`, `@NotEmpty`, `@Valid` (Order items), `@Positive`, `@NotNull`.
- **Security**: Role theo `@PreAuthorize` trong `OrderController` (STAFF/MANAGER/ADMIN).

## Danh sách API
| Method | URL | Mô tả | Role |
| --- | --- | --- | --- |
| POST | `/api/v1/orders` | Tạo đơn mới | `ADMIN` hoặc `STAFF` |
| GET | `/api/v1/orders` | Danh sách đơn (phân trang) | `STAFF`,`MANAGER`,`ADMIN` |
| GET | `/api/v1/orders/{id}` | Xem chi tiết đơn | `STAFF`,`MANAGER`,`ADMIN` |
| GET | `/api/v1/orders/table/{tableId}/pending` | Lấy đơn PENDING của bàn | `STAFF`,`MANAGER`,`ADMIN` |
| POST | `/api/v1/orders/{orderId}/items` | Thêm món vào đơn | `STAFF`,`MANAGER`,`ADMIN` |
| PUT | `/api/v1/orders/{orderId}/items/{orderDetailId}` | Cập nhật món | `STAFF`,`MANAGER`,`ADMIN` |
| DELETE | `/api/v1/orders/{orderId}/items/{orderDetailId}` | Xóa món | `STAFF`,`MANAGER`,`ADMIN` |
| POST | `/api/v1/orders/{orderId}/payment` | Thanh toán đơn | `STAFF`,`MANAGER`,`ADMIN` |
| POST | `/api/v1/orders/{orderId}/voucher` | Áp voucher | `STAFF`,`MANAGER`,`ADMIN` |
| DELETE | `/api/v1/orders/{orderId}/voucher` | Gỡ voucher | `STAFF`,`MANAGER`,`ADMIN` |
| GET | `/api/v1/orders/status/{status}` | Lọc đơn theo trạng thái | `MANAGER`,`ADMIN` |
| GET | `/api/v1/orders/date-range` | Lọc đơn theo ngày | `MANAGER`,`ADMIN` |
| PUT | `/api/v1/orders/{orderId}/cancel` | Hủy đơn | `MANAGER`,`ADMIN` |

Không có API ẩn khác trong controller.

## Chi tiết API & dữ liệu chính

### POST `/api/v1/orders`
- **Request (`OrderCreateRequestDTO`)**:
  ```json
  {
    "tableId": 5,
    "type": "DINE_IN",
    "customerId": 12,
    "voucherCode": "SUMMER25",
    "items": [
      { "productId": 20, "quantity": 2, "notes": "Ít đá" },
      { "productId": 33, "quantity": 1 }
    ]
  }
  ```
- **Validation**: `type` & `items` bắt buộc, `items` phải >=1 phần tử, mỗi item require `productId`, `quantity>0`.
- **Logic**:
  1. Lấy nhân viên hiện tại từ `SecurityContext`.
  2. Kiểm tra tồn tại customer (nếu có) & bàn (nếu có) và trạng thái bàn `EMPTY`.
  3. Tạo `Order` với tổng tiền ban đầu và set `OrderDetail` thông qua `processOrderItems`.
  4. Tính subtotal, discount, total (nếu có voucher).
- **Response 201** (`OrderResponseDTO`) minh họa:
  ```json
  {
    "id": 101,
    "tableName": "B05",
    "staffUsername": "staff001",
    "type": "DINE_IN",
    "status": "PENDING",
    "subTotal": 135000,
    "discountAmount": 15000,
    "totalAmount": 120000,
    "customerId": 12,
    "voucherCode": "SUMMER25",
    "createdAt": "2025-11-15T09:05:00",
    "orderDetails": [
      {
        "orderDetailId": 501,
        "productId": 20,
        "productName": "Latte",
        "quantity": 2,
        "priceAtOrder": 45000,
        "notes": "Ít đá"
      }
    ]
  }
  ```
- **Lỗi 400**: bàn đang bận, sản phẩm không khả dụng, voucher không hợp lệ, items rỗng.
- **Lỗi 404**: không tìm thấy bàn, khách, sản phẩm.

### POST `/api/v1/orders/{orderId}/items`
- **Request (`OrderDetailRequestDTO`)**
  ```json
  { "productId": 20, "quantity": 1, "notes": "Không đường" }
  ```
- **Logic**: Nếu món đã có -> cộng dồn số lượng, update ghi chú; tính lại subtotal & voucher.
- **Lỗi 400**: đơn không ở trạng thái `PENDING`, product không available.

### PUT `/api/v1/orders/{orderId}/items/{orderDetailId}`
- **Request (`OrderDetailUpdateRequestDTO`)**
  ```json
  { "quantity": 2, "notes": "Vừa ngọt" }
  ```
- **Logic**: Tìm detail theo ID, cập nhật số lượng & notes, tính lại tổng.
- **Lỗi 404**: không tìm thấy detail.

### DELETE `/api/v1/orders/{orderId}/items/{orderDetailId}`
- **Logic**: Xóa detail; nếu hết món -> reset tổng & voucher; ngược lại tính lại subtotal.

### POST `/api/v1/orders/{orderId}/payment`
- **Request (`PaymentRequestDTO`)**
  ```json
  {
    "paymentMethod": "CASH",
    "amountPaid": 120000,
    "customerId": 12,
    "voucherCode": "SUMMER25"
  }
  ```
- **Logic chính (`PaymentService`):**
  1. Đảm bảo order đang `PENDING`.
  2. (Tùy chọn) gán customer nếu chưa có.
  3. Validate voucher lại theo subtotal.
  4. Gọi `subtractInventoryForOrder` trừ tồn kho dựa trên recipe của từng `Product` (nếu không đủ -> lỗi).
  5. Đặt trạng thái `PAID`, lưu thời gian `paidAt`, phương thức thanh toán.
  6. Cộng điểm loyalty khách hàng theo số tiền.
  7. Tăng bộ đếm `Voucher.timesUsed`.
- **Lỗi 400**: payment method không hợp lệ (chỉ `CASH`, `TRANSFER`, `CARD`), tồn kho không đủ, voucher sai.
- **Lỗi 404**: order/ingredient không tồn tại.

### POST `/api/v1/orders/{orderId}/voucher`
- **Request (`VoucherApplyRequestDTO`)**
  ```json
  { "voucherCode": "SUMMER25" }
  ```
- **Response 200**: `OrderResponseDTO` cập nhật.
- **Lỗi 400**: voucher rỗng hoặc không đủ điều kiện.
- **Lỗi 404**: không tìm thấy order/voucher.

### DELETE `/api/v1/orders/{orderId}/voucher`
- **Logic**: gỡ voucher, đặt discount=0, total=subtotal.
- **Lỗi 400**: đơn không có voucher.

### GET `/api/v1/orders/status/{status}` & `/date-range`
- **Query**: status (ví dụ `PAID`, `PENDING`), `startDate`, `endDate` (ISO DATE).
- **Response 200**: `Page<OrderResponseDTO>`.
- **Validation**: status được chuẩn hóa uppercase, `LocalDate` convert -> `LocalDateTime` trong service.

### PUT `/api/v1/orders/{orderId}/cancel`
- **Logic**: kiểm tra `PENDING`, set trạng thái `CANCELLED`, giải phóng bàn.

## Điều kiện nghiệp vụ & validation chính
- Mỗi bàn chỉ có 1 đơn `PENDING`; khi thanh toán/hủy phải cập nhật trạng thái bàn.
- Voucher phải hoạt động, chưa hết lượt sử dụng, trong thời gian hiệu lực và đủ giá trị tối thiểu.
- Trừ tồn kho dựa trên recipe, nếu thiếu báo lỗi.
- Không thể thanh toán/hủy đơn không ở trạng thái `PENDING`.
- `quantity` món phải dương.

## Luồng lỗi & thông điệp nổi bật
| Exception | Nguồn | HTTP | Message |
| --- | --- | --- | --- |
| `EntityNotFoundException` | Order/Product/Table/Customer/Voucher | 404 | "... not found" |
| `IllegalArgumentException` | Bàn đã có đơn, voucher invalid, sản phẩm không available, thiếu voucher | 400 |
| `IllegalStateException` | Thanh toán đơn không ở trạng thái PENDING | 400 |
| `BadCredentialsException` (đăng nhập) | Không áp dụng trực tiếp |
| `Exception` | Khác | 500 |

## Role/Permission
- Phần lớn endpoint yêu cầu `hasAnyRole('STAFF','MANAGER','ADMIN')`; các endpoint quản lý chung (`status`, `date-range`, `cancel`) yêu cầu `MANAGER` hoặc `ADMIN`.

## Quan hệ với chức năng khác
- **Voucher** (`voucher.md`): áp dụng/đếm số lượt dùng.
- **Customer** (`customer.md`): liên kết đơn & cộng điểm loyalty.
- **Inventory/Ingredient** (`ingredient.md`, `product-recipe.md`): trừ tồn kho khi thanh toán.
- **Cafe Table** (`cafe-table.md`): cập nhật trạng thái bàn.
- **Reports** (`report.md`): lấy dữ liệu đơn hàng cho báo cáo doanh thu.

## Các tệp liên quan trong BE
- Controller: `OrderController.java`
- Services: `OrderService.java`, `PaymentService.java`, `VoucherService.java`, `CustomerService.java`
- DTO: `OrderCreateRequestDTO.java`, `OrderDetailRequestDTO.java`, `OrderDetailUpdateRequestDTO.java`, `OrderResponseDTO.java`, `PaymentRequestDTO.java`, `VoucherApplyRequestDTO.java`, `VoucherCheckResponseDTO.java`
- Entity/Repository: `Order.java`, `OrderDetail.java`, `OrderRepository.java`, `OrderDetailRepository.java`, `CafeTableRepository.java`, `ProductRepository.java`, `CustomerRepository.java`, `VoucherRepository.java`, `IngredientRepository.java`, `ProductIngredientRepository.java`
