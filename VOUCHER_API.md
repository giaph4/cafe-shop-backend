# 📘 Tài liệu API Voucher

_Tài liệu này dành cho đội frontend & vận hành, mô tả đầy đủ cách làm việc với hệ thống quản lý voucher sau khi mở rộng dịch vụ VoucherService._

## 1. Tổng quan

- Mục tiêu của module: quản lý danh sách voucher khuyến mãi, kiểm tra điều kiện áp dụng và theo dõi hiệu suất sử dụng.
- Các nhóm người dùng:
  - **Nhân viên quầy** (`STAFF`): chỉ được phép kiểm tra và áp dụng voucher khi thanh toán.
  - **Quản lý / Admin** (`MANAGER`, `ADMIN`): được phép CRUD, bật/tắt, tìm kiếm và xem thống kê voucher.

Các Endpoint đều nằm dưới prefix: `/api/v1/vouchers`.

---

## 2. DTO chính

### 2.1 VoucherRequestDTO
| Trường | Kiểu | Bắt buộc | Ghi chú |
|--------|------|----------|---------|
| `code` | string | ✔ | Tối đa 50 ký tự, được tự động chuẩn hóa HOA và cắt khoảng trắng. |
| `description` | string | ✔ | Mô tả hiển thị trên UI, tối đa 255 ký tự. |
| `type` | enum | ✔ | `FIXED_AMOUNT` hoặc `PERCENTAGE`. |
| `discountValue` | number | ✔ | > 0. Nếu type = `PERCENTAGE`, giá trị ≤ 100. |
| `minimumOrderAmount` | number | ✖ | > 0. Nếu bỏ trống, không giới hạn giá trị đơn tối thiểu. |
| `maximumDiscountAmount` | number | ✖ | > 0. Chỉ dùng khi cần chặn mức giảm tối đa. |
| `validFrom` | datetime | ✔ | ISO-8601 `yyyy-MM-dd'T'HH:mm:ss`. |
| `validTo` | datetime | ✔ | Phải lớn hơn `validFrom`. |
| `usageLimit` | number | ✔ | Số lượt áp dụng cho phép, > 0. Không thể cập nhật nhỏ hơn `timesUsed` hiện có. |
| `active` | boolean | ✖ | Mặc định `true` khi tạo mới. |

### 2.2 VoucherResponseDTO
Chứa đầy đủ thông tin voucher: `id`, các trường trong request + `timesUsed`, `createdAt`, `updatedAt`.

### 2.3 VoucherSummaryDTO
Gồm 4 số liệu:
- `activeCount`: tổng voucher đang bật.
- `inactiveCount`: tổng voucher đang tắt.
- `expiringSoonCount`: số voucher sắp hết hạn trong 7 ngày tới.
- `redeemedCount`: tổng lượt sử dụng đã ghi nhận.

---

## 3. Endpoint chi tiết

### 3.1 Kiểm tra voucher (STAFF)
- **Method/URL**: `GET /check`
- **Quyền**: `STAFF`, `MANAGER`, `ADMIN`
- **Query**:
  - `code`: mã voucher FE nhập
  - `amount`: tổng tiền đơn hàng trước chiết khấu
- **Response** (`VoucherCheckResponseDTO`):
  ```json
  {
    "valid": true,
    "message": "Áp dụng voucher thành công!",
    "code": "SAVE20",
    "discountAmount": 20000,
    "type": "FIXED_AMOUNT"
  }
  ```
- **Lưu ý**: nếu mã không tồn tại hoặc hết hạn, BE trả `isValid=false` và thông điệp tiếng Việt cụ thể.

### 3.2 Tạo voucher (MANAGER/ADMIN)
- **Method/URL**: `POST /`
- **Body**: `VoucherRequestDTO`
- **Response**: `VoucherResponseDTO`
- **Validation đặc biệt**:
  - Mã không trùng (case-insensitive).
  - `validFrom < validTo`.
  - Nếu type = `PERCENTAGE`, `discountValue ≤ 100`.
  - Nếu type = `FIXED_AMOUNT` và có `maximumDiscountAmount`, giá trị này ≥ `discountValue`.

### 3.3 Cập nhật voucher
- **Method/URL**: `PUT /{id}`
- **Body**: `VoucherRequestDTO`
- **Response**: `VoucherResponseDTO`
- BE giữ nguyên `timesUsed`, chỉ cập nhật các trường còn lại.
- Không thể giảm `usageLimit` xuống thấp hơn `timesUsed` hiện tại.

### 3.4 Bật/tắt nhanh
- **Method/URL**: `PATCH /{id}/toggle`
- **Response**: `VoucherResponseDTO`
- Đảo trạng thái `active`, cập nhật `updatedAt`.

### 3.5 Xóa voucher
- **Method/URL**: `DELETE /{id}`
- **Response**: `204 No Content`
- Ràng buộc: không được xóa khi `timesUsed > 0`. UI nên hiển thị thông báo “Voucher đã được sử dụng, chỉ có thể tắt thay vì xóa”.

### 3.6 Lấy danh sách và lọc
- **Method/URL**: `GET /`
- **Quyền**: `MANAGER`, `ADMIN`
- **Query** (tùy chọn):
  - `code`: tìm kiếm chứa (case-insensitive)
  - `type`: `FIXED_AMOUNT` | `PERCENTAGE`
  - `active`: true/false
  - `validFrom`: ISO datetime – lọc voucher có `validTo ≥ validFrom`
  - `validTo`: ISO datetime – lọc voucher có `validFrom ≤ validTo`
  - `page`, `size`, `sort`
- **Response**: `Page<VoucherResponseDTO>`
  ```json
  {
    "content": [
      {
        "id": 1,
        "code": "SAVE20",
        "discountValue": 20000,
        "usageLimit": 50,
        "timesUsed": 10,
        "active": true,
        "validFrom": "2025-01-01T00:00:00",
        "validTo": "2025-02-01T23:59:59"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "number": 0,
    "size": 10
  }
  ```

### 3.7 Lấy chi tiết
- **Method/URL**: `GET /{id}`
- **Response**: `VoucherResponseDTO`
- Nếu không tìm thấy → 404 `Voucher không tồn tại: {id}`.

### 3.8 Thống kê nhanh
- **Method/URL**: `GET /summary`
- **Response**:
  ```json
  {
    "activeCount": 12,
    "inactiveCount": 4,
    "expiringSoonCount": 3,
    "redeemedCount": 240
  }
  ```
- FE có thể dùng để hiển thị widget dashboard.

---

## 4. Luồng áp dụng voucher khi thanh toán

1. FE gọi `GET /api/v1/vouchers/check` để xác minh mã.
2. Nếu hợp lệ, FE truyền `voucherCode` vào `POST /api/v1/orders/{orderId}/payment`.
3. Sau khi thanh toán thành công, BE tự động tăng `timesUsed` và tính lại tổng tiền.
4. Nếu người dùng hủy voucher trước khi thanh toán, FE gọi `DELETE /api/v1/orders/{orderId}/voucher` (đã có sẵn trong OrderController).

---

## 5. Best practice cho FE
- Luôn chuẩn hóa mã nhập vào (trim, upper-case) trước khi gọi API.
- Khi BE trả `isValid=false`, hiển thị thông điệp từ field `message` để người dùng hiểu lý do.
- Với trang quản trị, nên hiển thị các cột `usageLimit`, `timesUsed`, `active`, `validFrom`, `validTo` để quản lý ra quyết định.
- Phân biệt rõ hành động **tạm tắt** và **xóa** để tránh mất dữ liệu lịch sử.

---

## 6. Kiểm thử & checklist
- Đã bổ sung `VoucherServiceTest` bao phủ các nhánh chính (tạo, cập nhật, bật/tắt, xóa, thống kê, tăng lượt).
- Khi triển khai trên môi trường mới, cần chạy `mvn test` để đảm bảo các case mock này hoạt động bình thường.
- Nếu hệ thống không cài Maven CLI, hãy yêu cầu DevOps cài đặt (`mvn -version` phải hoạt động) trước khi build.

---

📞 Thắc mắc thêm, vui lòng liên hệ backend team để được hỗ trợ tích hợp.
