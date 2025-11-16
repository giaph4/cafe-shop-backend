# Quy Tắc Đặt Tên

## Mục tiêu
Đảm bảo sự nhất quán trong toàn bộ codebase, dễ đọc, dễ bảo trì và hỗ trợ đội ngũ frontend tích hợp.

## Quy tắc chung
- Sử dụng tiếng Anh cho tên code (class, method, biến), tiếng Việt cho tài liệu.
- Sử dụng `camelCase` cho biến, method; `PascalCase` cho class, enum; `UPPER_SNAKE_CASE` cho hằng số.
- Tên bảng và cột trong database dùng `snake_case`.
- API endpoint sử dụng danh từ số nhiều và `kebab-case` (`/api/v1/order-details`).
- DTO phân biệt rõ Request (`CreateOrderRequestDTO`), Response (`OrderResponseDTO`).

## Backend (Java)
| Thành phần | Quy tắc | Ví dụ |
|------------|---------|-------|
| Package | chữ thường, phân theo module | `com.giapho.coffee_shop_backend.service` |
| Class | PascalCase | `OrderService`, `CustomerController` |
| Interface | PascalCase, có hậu tố `Repository`, `Service` nếu cần | `OrderRepository` |
| Method | camelCase, động từ dẫn đầu | `createOrder`, `calculateDiscount` |
| Biến | camelCase, ngắn gọn nhưng rõ nghĩa | `totalAmount`, `voucherCode` |
| Enum | PascalCase, hằng số UPPER_SNAKE_CASE | `OrderStatus.PENDING` |
| Mapper | `XxxMapper` | `OrderMapper` |
| Test | `ClassNameTest` | `OrderServiceTest` |

## CSDL
- Tên bảng: số nhiều, `snake_case` (`orders`, `order_details`).
- Cột: `snake_case` (`created_at`, `total_amount`).
- Khóa ngoại: `{entity}_id` (`user_id`, `product_id`).
- Index: `idx_{table}_{column}` (`idx_orders_created_at`).

## API & DTO
- Endpoint: `/api/v1/{resource}`.
- Query param: `camelCase` (`?page=0&size=20`).
- JSON field: `camelCase` (`"totalAmount": 125000`).
- Response wrapper (nếu có): `ApiResponse<T>`.

## File & thư mục
- Tài liệu: `lowercase-with-hyphen.md`.
- Resource tĩnh: `kebab-case` hoặc `snake_case` cho hình ảnh (`coffee-banner.jpg`).

## Log & audit
- Logger dùng dạng `log.info("Action: {}", value)`.
- Audit action đặt theo chuẩn `MODULE_ACTION` (`ORDER_CREATE`, `USER_UPDATE_ROLE`).

## Môi trường & cấu hình
- Biến môi trường: `UPPER_SNAKE_CASE` (`JWT_SECRET_KEY`).
- Profile Spring: `dev`, `staging`, `prod`.
