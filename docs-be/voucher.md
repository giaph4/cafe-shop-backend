# Chức năng: Quản lý voucher khuyến mãi

## Vai trò trong hệ thống
- Quản trị mã giảm giá (voucher) dùng trong đơn hàng, bao gồm tạo, cập nhật, kích hoạt/vô hiệu, thống kê.
- Cung cấp API kiểm tra voucher cho nhân viên tại POS và áp dụng giảm giá khi thanh toán.
- Theo dõi lượt sử dụng, tình trạng hợp lệ để phục vụ chiến dịch marketing.

## Luồng xử lý backend
1. **Kiểm tra voucher** (`GET /api/v1/vouchers/check`): xác thực mã, điều kiện đơn hàng và trả kết quả `VoucherCheckResponseDTO`. Trả 200 dù voucher không hợp lệ để FE hiển thị thông điệp @src/main/java/com/giapho/coffee_shop_backend/controller/VoucherController.java#29-52 @src/main/java/com/giapho/coffee_shop_backend/service/VoucherService.java#32-75.
2. **CRUD voucher**: tạo/cập nhật/xóa/toggle qua `VoucherController`, `VoucherService` xử lý validate business rules, chuẩn hóa code uppercase, giới hạn usage @src/main/java/com/giapho/coffee_shop_backend/controller/VoucherController.java#54-83 @src/main/java/com/giapho/coffee_shop_backend/service/VoucherService.java#83-154.
3. **Tìm kiếm & phân trang** (`GET /api/v1/vouchers`): filter theo code, type, active, thời gian hiệu lực bằng `Specification` @src/main/java/com/giapho/coffee_shop_backend/controller/VoucherController.java#85-99 @src/main/java/com/giapho/coffee_shop_backend/service/VoucherService.java#156-191.
4. **Chi tiết & thống kê** (`GET /{id}`, `/summary`): cung cấp thông tin chi tiết và tổng quan (số lượng active/inactive, sắp hết hạn) @src/main/java/com/giapho/coffee_shop_backend/controller/VoucherController.java#101-113 @src/main/java/com/giapho/coffee_shop_backend/service/VoucherService.java#193-208.
5. **Ghi nhận lượt sử dụng**: `VoucherService.incrementUsageCount` được gọi từ `PaymentService` khi thanh toán thành công @src/main/java/com/giapho/coffee_shop_backend/service/VoucherService.java#299-311 @src/main/java/com/giapho/coffee_shop_backend/service/PaymentService.java#103-108.

## Thành phần liên quan
- **Controller**: `VoucherController` @src/main/java/com/giapho/coffee_shop_backend/controller/VoucherController.java#1-114
- **Service**: `VoucherService` @src/main/java/com/giapho/coffee_shop_backend/service/VoucherService.java#1-312
- **Repository**: `VoucherRepository`
- **DTO**: `VoucherRequestDTO`, `VoucherResponseDTO`, `VoucherCheckResponseDTO`, `VoucherSummaryDTO` @src/main/java/com/giapho/coffee_shop_backend/dto/VoucherRequestDTO.java#1-41 @src/main/java/com/giapho/coffee_shop_backend/dto/VoucherResponseDTO.java#1-43 @src/main/java/com/giapho/coffee_shop_backend/dto/VoucherCheckResponseDTO.java#1-32 @src/main/java/com/giapho/coffee_shop_backend/dto/VoucherSummaryDTO.java#1-29
- **Entity**: `Voucher`
- **Validation**: `@NotBlank`, `@NotNull`, `@Future`, `@DecimalMin` trong `VoucherRequestDTO` (xem file); các quy tắc bổ sung trong service (thời gian, usage limit, percent <=100, maxDiscount >= discount với loại FIXED, ...).
- **Security**: `@PreAuthorize` trong controller — đọc check yêu cầu STAFF trở lên, CRUD yêu cầu `MANAGER`/`ADMIN`.

## Danh sách API
| Method | URL | Mô tả | Role |
| --- | --- | --- | --- |
| GET | `/api/v1/vouchers/check` | Kiểm tra voucher với số tiền đơn | `STAFF`,`MANAGER`,`ADMIN` |
| POST | `/api/v1/vouchers` | Tạo voucher mới | `MANAGER`,`ADMIN` |
| PUT | `/api/v1/vouchers/{id}` | Cập nhật voucher | `MANAGER`,`ADMIN` |
| PATCH | `/api/v1/vouchers/{id}/toggle` | Bật/tắt trạng thái active | `MANAGER`,`ADMIN` |
| DELETE | `/api/v1/vouchers/{id}` | Xóa voucher (chưa dùng) | `MANAGER`,`ADMIN` |
| GET | `/api/v1/vouchers` | Tìm kiếm & phân trang voucher | `MANAGER`,`ADMIN` |
| GET | `/api/v1/vouchers/{id}` | Lấy chi tiết voucher | `MANAGER`,`ADMIN` |
| GET | `/api/v1/vouchers/summary` | Thống kê tổng quan voucher | `MANAGER`,`ADMIN` |

## Chi tiết API

### GET `/api/v1/vouchers/check`
- **Query**: `code` (String), `amount` (BigDecimal).
- **Logic**: `VoucherService.checkAndCalculateDiscount`
  - Chuẩn hóa code uppercase.
  - Kiểm tra hoạt động, thời gian hợp lệ, usage, min order.
  - Tính discount theo loại (FIXED hoặc PERCENTAGE, giới hạn by maxDiscount).
- **Response 200** (khi hợp lệ):
  ```json
  {
    "valid": true,
    "message": "Áp dụng voucher thành công!",
    "code": "SUMMER25",
    "discountAmount": 25000,
    "type": "PERCENTAGE"
  }
  ```
- **Response 200** (khi không hợp lệ):
  ```json
  {
    "valid": false,
    "message": "Voucher đã hết hạn.",
    "code": "SUMMER25",
    "discountAmount": 0,
    "type": "PERCENTAGE"
  }
  ```
- **Lỗi 500**: Bất kỳ lỗi không xử lý -> trả body với `message` lỗi hệ thống.

### POST `/api/v1/vouchers`
- **Request (`VoucherRequestDTO`)**:
  ```json
  {
    "code": "SUMMER25",
    "description": "Giảm 25% tối đa 50.000",
    "type": "PERCENTAGE",
    "discountValue": 25,
    "maximumDiscountAmount": 50000,
    "minimumOrderAmount": 150000,
    "validFrom": "2025-06-01T00:00:00",
    "validTo": "2025-06-30T23:59:59",
    "usageLimit": 200,
    "active": true
  }
  ```
- **Logic**:
  - Chuẩn hóa code uppercase.
  - Kiểm tra trùng code (`existsByCodeIgnoreCase`).
  - Validate business (validFrom < validTo, phần trăm <= 100, fixed <= maxDiscount, usageLimit >= 0).
  - Khởi tạo `timesUsed=0`, set thời gian hiện tại.
- **Response 200**: `VoucherResponseDTO` chứa đầy đủ thông tin và `timesUsed=0`.
- **Lỗi 400**: Vi phạm rule (ví dụ usageLimit < timesUsed khi update, hoặc fixed > maxDiscount).
- **Lỗi 409**: code đã tồn tại (`DataIntegrityViolationException`).

### PUT `/api/v1/vouchers/{id}`
- **Logic**: Giống POST, nhưng kiểm tra code khác ID hiện tại, validate usageLimit >= timesUsed.
- **Response**: `VoucherResponseDTO` cập nhật.
- **Lỗi 404**: không tìm thấy ID.

### PATCH `/api/v1/vouchers/{id}/toggle`
- **Logic**: đảo trạng thái `active`, cập nhật `updatedAt`.
- **Response**: `VoucherResponseDTO` mới.
- **Lỗi 404**: id không tồn tại.

### DELETE `/api/v1/vouchers/{id}`
- **Logic**: chỉ cho xóa nếu `timesUsed == 0`, kẻo vi phạm dữ liệu.
- **Response 204**.
- **Lỗi 400**: "Không thể xóa voucher đã được sử dụng".
- **Lỗi 404**: id không tồn tại.

### GET `/api/v1/vouchers`
- **Query**: `code`, `type`, `active`, `validFrom`, `validTo`, `page`, `size` (`PageableDefault size=10`).
- **Response 200** (`Page<VoucherResponseDTO>`) chứa danh sách và meta.

### GET `/api/v1/vouchers/{id}`
- **Response**: `VoucherResponseDTO`.
- **Lỗi 404**: id không tồn tại.

### GET `/api/v1/vouchers/summary`
- **Response**: `VoucherSummaryDTO`
  ```json
  {
    "activeCount": 15,
    "inactiveCount": 5,
    "expiringSoonCount": 2,
    "redeemedCount": 320
  }
  ```

## Điều kiện nghiệp vụ & validation
- Code không rỗng, được chuẩn hóa uppercase.
- Khoảng thời gian phải hợp lệ (`validFrom < validTo`).
- Voucher `PERCENTAGE`: `discountValue` ≤ 100.
- Voucher `FIXED_AMOUNT`: `maximumDiscountAmount` (nếu có) ≥ `discountValue`.
- `usageLimit` không nhỏ hơn `timesUsed` khi cập nhật.
- Khi thanh toán, nếu voucher không còn hợp lệ -> xóa khỏi order (logic `recalculateOrderTotals`).

## Luồng lỗi & thông điệp
| Exception | HTTP | Message |
| --- | --- | --- |
| `IllegalArgumentException` | 400 | Ví dụ: "Voucher code cannot be empty", "Order does not have any voucher" |
| `EntityNotFoundException` | 404 | "Voucher không tồn tại: ..." |
| `DataIntegrityViolationException` | 409 | "Voucher code đã tồn tại" |
| `IllegalStateException` | 400 | "Không thể xóa voucher đã được sử dụng" |
| `Exception` | 500 | "Lỗi hệ thống khi kiểm tra voucher." (controller tạo response) |

## Role/Permission
- Kiểm tra voucher: `hasAnyRole('STAFF','MANAGER','ADMIN')`.
- CRUD, toggle, search, summary: `hasAnyRole('MANAGER','ADMIN')`.

## Quan hệ với chức năng khác
- **Order/Payment**: áp dụng voucher trong quá trình tạo/Thanh toán đơn (`order.md`).
- **Report**: thống kê voucher sử dụng có thể xuất hiện trong dashboard/báo cáo.
- **Authentication**: cần JWT hợp lệ để gọi tất cả endpoint.

## Các tệp liên quan trong BE
- Controller: `VoucherController.java`
- Service: `VoucherService.java`
- DTO: `VoucherRequestDTO.java`, `VoucherResponseDTO.java`, `VoucherCheckResponseDTO.java`, `VoucherSummaryDTO.java`
- Repository: `VoucherRepository.java`
- Entity: `Voucher.java`
