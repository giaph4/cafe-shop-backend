# Mô Tả API

## Mục lục
- [1. Giới thiệu & quy ước](#1-giới-thiệu--quy-ước)
- [2. Danh mục endpoint tổng hợp](#2-danh-mục-endpoint-tổng-hợp)
- [3. Đặc tả chi tiết theo module](#3-đặc-tả-chi-tiết-theo-module)
  - [3.1 Authentication](#31-authentication)
  - [3.2 User Management](#32-user-management)
  - [3.3 Product & Category](#33-product--category)
  - [3.4 Order & Payment](#34-order--payment)
  - [3.5 Voucher](#35-voucher)
  - [3.6 Customer](#36-customer)
  - [3.7 Inventory & Purchase Order](#37-inventory--purchase-order)
  - [3.8 Shift & Payroll](#38-shift--payroll)
  - [3.9 Reporting & Dashboard](#39-reporting--dashboard)
  - [3.10 File Service](#310-file-service)
- [4. Chuẩn hóa lỗi & mã trạng thái](#4-chuẩn-hóa-lỗi--mã-trạng-thái)
- [5. Bảo mật & headers bắt buộc](#5-bảo-mật--headers-bắt-buộc)

## 1. Giới thiệu & quy ước
- **Base URL**: `https://{host}/api/v1`
- **Định dạng**: JSON UTF-8. Request gửi `Content-Type: application/json`. Response dùng `application/json` hoặc `application/octet-stream` (download).
- **Xác thực**: JWT Bearer. Header: `Authorization: Bearer <access_token>`.
- **Phân trang**: query `page` (0-based), `size` (<=100), `sort=field,asc|desc`.
- **Datetime**: ISO 8601 (UTC). Ví dụ: `2025-11-14T07:30:00Z`.
- **Swagger/OpenAPI**: `/swagger-ui.html` (profile dev/staging).

## 2. Danh mục endpoint tổng hợp
| Module | Method | Path | Quyền |
|--------|--------|------|-------|
| Auth | POST | `/auth/login` | Public |
| Auth | POST | `/auth/register` | ROLE_ADMIN/ROLE_MANAGER |
| User | GET | `/users` | ROLE_MANAGER |
| Order | POST | `/orders` | ROLE_STAFF+ |
| Order | POST | `/orders/{id}/pay` | ROLE_STAFF+ |
| Voucher | POST | `/vouchers/validate` | ROLE_STAFF+ |
| Inventory | POST | `/purchase-orders` | ROLE_MANAGER |
| Attendance | POST | `/attendance/check-in` | ROLE_STAFF |
| Reporting | GET | `/reports/revenue` | ROLE_MANAGER |
| File | POST | `/files/upload` | ROLE_MANAGER/ROLE_ADMIN |

## 3. Đặc tả chi tiết theo module

### 3.1 Authentication
#### POST `/auth/login`
| Thuộc tính | Giá trị |
|------------|---------|
| Quyền | Public |
| Mô tả | Đăng nhập, nhận access token & refresh token |
| Headers | `Content-Type: application/json` |

**Request body**
```json
{
  "username": "staff01",
  "password": "ChangeMe123!"
}
```

**Response 200**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "df8f1e1d-...",
  "username": "staff01",
  "expiresIn": 900
}
```

**Mã lỗi**
| HTTP | Code | Mô tả |
|------|------|-------|
| 401 | `AUTH_INVALID_CREDENTIALS` | Sai username/password |
| 423 | `AUTH_ACCOUNT_LOCKED` | Tài khoản bị khóa |

#### POST `/auth/register`
- Quyền: ROLE_ADMIN/ROLE_MANAGER.
- Tạo nhân viên mới, gán vai trò.
- Body (rút gọn):
```json
{
  "username": "staff02",
  "password": "StrongPass#2025",
  "fullName": "Nguyễn Văn B",
  "email": "staff02@coffee.test",
  "roleIds": [3]
}
```

### 3.2 User Management
#### GET `/users`
- Quyền: ROLE_MANAGER.
- Query: `page`, `size`, `status`.
- Response (200):
```json
{
  "content": [
    {
      "id": 15,
      "username": "staff01",
      "fullName": "Lê Thu Ngân",
      "email": "staff01@coffee.test",
      "roles": ["ROLE_STAFF"],
      "status": "ACTIVE"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 45,
  "totalPages": 3
}
```
- Lỗi: 403 (`FORBIDDEN`), 401 (`UNAUTHORIZED`).

#### PATCH `/users/{id}/status`
- Body: `{ "status": "LOCKED" }`.
- Trả: 204 No Content.

### 3.3 Product & Category
#### GET `/products`
- Query: `categoryId`, `keyword`, `availableOnly`.
- Response trích: `ProductResponseDTO` gồm `id`, `code`, `name`, `price`, `isAvailable`, `category`.

#### POST `/products`
- Quyền: ROLE_MANAGER.
- Body mẫu:
```json
{
  "code": "LATTE-M",
  "name": "Cà phê Latte",
  "price": 45000,
  "cost": 22000,
  "categoryId": 5,
  "ingredients": [
    { "ingredientId": 2, "quantity": 150 },
    { "ingredientId": 7, "quantity": 30 }
  ]
}
```
- Response 201: `Location` header `/api/v1/products/{id}`.
- Lỗi: 400 (`PRODUCT_CODE_EXISTS`), 404 (`CATEGORY_NOT_FOUND`).

### 3.4 Order & Payment
#### POST `/orders`
- Quyền: ROLE_STAFF+.
- Body mẫu:
```json
{
  "tableId": 12,
  "customerId": 102,
  "type": "DINE_IN",
  "voucherCode": "WEEKEND20",
  "items": [
    { "productId": 5, "quantity": 2 },
    { "productId": 9, "quantity": 1 }
  ]
}
```
- Response 201: `OrderResponseDTO` (status `PENDING`).
- Lỗi: 409 (`TABLE_BUSY`), 400 (`VOUCHER_INVALID`).

#### POST `/orders/{id}/pay`
- Body:
```json
{
  "amountPaid": 120000,
  "method": "CASH"
}
```
- Response 200:
```json
{
  "orderId": 512,
  "status": "PAID",
  "paidAt": "2025-11-14T07:45:10Z",
  "change": 10000
}
```
- Lỗi: 400 (`PAYMENT_AMOUNT_INVALID`), 409 (`ORDER_ALREADY_PAID`).

### 3.5 Voucher
#### POST `/vouchers`
- Quyền: ROLE_MANAGER.
- Body: mã, loại, giá trị, điều kiện.
- Response 201: voucher mới.
- Error codes: `VOUCHER_CODE_EXISTS`, `VOUCHER_DATE_INVALID`.

#### POST `/vouchers/validate`
- Input: `orderTotal`, `customerId`, `code`.
- Response 200:
```json
{
  "valid": true,
  "discountAmount": 20000,
  "message": "Áp dụng thành công"
}
```

### 3.6 Customer
#### GET `/customers/{id}/orders`
- Trả danh sách order (phân trang) kèm tổng chi tiêu, điểm loyalty.
- Error: 404 (`CUSTOMER_NOT_FOUND`).

### 3.7 Inventory & Purchase Order
#### POST `/purchase-orders`
- Body:
```json
{
  "supplierId": 3,
  "items": [
    { "ingredientId": 2, "quantity": 500, "cost": 18000 },
    { "ingredientId": 6, "quantity": 200, "cost": 9000 }
  ]
}
```
- Response 201: PO status `CREATED`.
- Lỗi: 404 (`SUPPLIER_NOT_FOUND`).

#### POST `/inventory/adjust`
- Dùng cho kiểm kê/điều chỉnh bất thường.
- Body: `{ "ingredientId": 5, "delta": -5, "reason": "Hao hụt" }`.

### 3.8 Shift & Payroll
#### POST `/attendance/check-in`
- Body: `{ "assignmentId": 702 }`.
- Response 200: `AttendanceDTO`.
- Lỗi: 409 (`ALREADY_CHECKED_IN`), 400 (`SHIFT_NOT_ASSIGNED`).

#### GET `/payroll/summary`
- Query: `cycleId`, `userId`.
- Response: danh sách bảng lương chi tiết, tổng kết.

### 3.9 Reporting & Dashboard
#### GET `/reports/revenue`
- Query: `from`, `to`, `format=json|excel`.
- Response JSON:
```json
{
  "from": "2025-11-01",
  "to": "2025-11-07",
  "totalRevenue": 185000000,
  "totalOrders": 1240,
  "discountAmount": 12500000,
  "topProducts": [ { "productId": 5, "quantity": 320 } ]
}
```
- Nếu `format=excel`, trả file `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.

#### GET `/dashboard/metrics`
- Query tùy chọn: `from`, `to`, `includeTopProducts`, `includeVoucherStats`.
- Response: `DashboardMetricsDTO`.

### 3.10 File Service
#### POST `/files/upload`
- Header: `Content-Type: multipart/form-data`.
- Field: `file`, `module` (PRODUCT/AUDIT).
- Response 201: `{ "id": "uuid", "url": "https://.../files/{id}" }`.
- Lỗi: 400 (`FILE_TOO_LARGE`), 415 (`FILE_TYPE_UNSUPPORTED`).

#### GET `/files/{id}`
- Public GET (dùng để hiển thị hình sản phẩm).
- Hỗ trợ HTTP caching (`ETag`, `Cache-Control`).

## 4. Chuẩn hóa lỗi & mã trạng thái
| HTTP | Code | Ý nghĩa |
|------|------|---------|
| 400 | `VALIDATION_ERROR` | Sai định dạng/thiếu trường bắt buộc |
| 401 | `UNAUTHORIZED` | Thiếu/Token không hợp lệ |
| 403 | `FORBIDDEN` | Không đủ quyền |
| 404 | `{RESOURCE}_NOT_FOUND` | Không tìm thấy |
| 409 | `CONFLICT`/`BUSINESS_RULE_VIOLATION` | Vi phạm quy tắc nghiệp vụ |
| 422 | `UNPROCESSABLE_ORDER` | Order không thể thanh toán |
| 500 | `INTERNAL_SERVER_ERROR` | Lỗi ngoài ý muốn |

**Cấu trúc lỗi chuẩn**
```json
{
  "timestamp": "2025-11-02T11:40:25Z",
  "code": "ORDER_NOT_FOUND",
  "message": "Đơn hàng không tồn tại",
  "errors": [
    { "field": "orderId", "message": "Giá trị phải lớn hơn 0" }
  ],
  "path": "/api/v1/orders/999",
  "traceId": "0af76519dcd24f0"
}
```

## 5. Bảo mật & headers bắt buộc
- **Headers**: `Authorization`, `Content-Type`, `Accept`, `X-Request-Id` (tùy chọn để truy vết).
- **CORS**: cho phép origin cấu hình qua `app.cors.allowed-origins`.
- **Rate limiting**: đề xuất cấu hình tại API Gateway (Nginx/Traefik) tùy vai trò (ví dụ login endpoint 5 req/minute).
- **Input sanitization**: mọi string được trim và escape; filter SQL injection ở repository.
- **Auditing**: thao tác nhạy cảm (cập nhật giá, thay đổi quyền, hủy đơn) ghi vào `audit_logs`.

---
**Mức độ hoàn thiện:** 100%
**Hạng mục còn thiếu:** Không
