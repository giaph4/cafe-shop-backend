# Chức năng: Nhật ký đăng nhập (Login History)

## Vai trò trong hệ thống
- Ghi nhận toàn bộ phiên đăng nhập thành công/thất bại của người dùng nội bộ.
- Cho phép Admin truy vấn lịch sử theo username, trạng thái, khoảng thời gian.
- Hỗ trợ audit bảo mật và phát hiện bất thường.

## Luồng xử lý backend
1. **Ghi nhận login**:
   - `AuthenticationService` sau khi xác thực thành công/ thất bại sẽ gọi `LoginHistoryService.recordSuccessfulLogin` hoặc `recordFailedLogin`, lưu thông tin (user, ip, userAgent, message) @src/main/java/com/giapho/coffee_shop_backend/service/AuthenticationService.java#96-175.
2. **API tra cứu** (`GET /api/v1/login-history`):
   - Chỉ ADMIN được truy cập (`@PreAuthorize("hasRole('ADMIN')")`).
   - Controller nhận `username`, `success`, `startDate`, `endDate`, phân trang, sort mặc định `loginAt DESC` @src/main/java/com/giapho/coffee_shop_backend/controller/LoginHistoryController.java#20-53.
   - Service xây `Specification` tùy theo filter, trả `Page<LoginHistoryResponseDTO>` @src/main/java/com/giapho/coffee_shop_backend/service/LoginHistoryService.java#100-134.

## Thành phần liên quan
- **Controller**: `LoginHistoryController`
- **Service**: `LoginHistoryService`
- **Repository**: `LoginHistoryRepository`, `UserRepository`
- **DTO**: `LoginHistoryResponseDTO`
- **Entity**: `LoginHistory`
- **Security**: endpoint yêu cầu `hasRole('ADMIN')`.

## API chi tiết
| Method | URL | Mô tả | Role |
| --- | --- | --- | --- |
| GET | `/api/v1/login-history` | Phân trang lịch sử login | `ADMIN` |

### GET `/api/v1/login-history`
- **Query params**:
  - `username` (String, optional) – tìm kiếm like (case-insensitive).
  - `success` (Boolean, optional).
  - `startDate`, `endDate` (ISO DATE-TIME, optional).
  - `page`, `size`, `sort` (Spring pageable, mặc định size=20, sort=`loginAt DESC`).
- **Response 200** (mẫu):
  ```json
  {
    "content": [
      {
        "id": 12,
        "userId": 5,
        "username": "staff001",
        "fullName": "Nguyen Van A",
        "email": "staff001@coffee.vn",
        "status": "ACTIVE",
        "success": true,
        "loginAt": "2025-11-15T08:30:00",
        "ipAddress": "203.113.1.10",
        "userAgent": "Mozilla/5.0",
        "message": "Login successful"
      }
    ],
    "pageable": { "pageNumber": 0, "pageSize": 20 },
    "totalElements": 45,
    "totalPages": 3
  }
  ```
- **Lỗi 400**: sai định dạng ngày/ sort → `GlobalExceptionHandler` trả 400.

## Điều kiện nghiệp vụ & validation
- Giao diện ADMIN nên truyền `startDate` ≤ `endDate`; service chấp nhận null.
- `recordLoginAttempt` lấy user từ DB nếu tồn tại; cho phép null user (khi username không xác định).
- Login thành công/thất bại đều được lưu với thông điệp cụ thể.

## Luồng lỗi & thông điệp
| Exception | HTTP | Message |
| --- | --- | --- |
| `MethodArgumentNotValidException` | 400 | (khi pageable invalid) |
| `IllegalArgumentException` | 400 | (từ filter không hợp lệ) |
- Lỗi truy cập: nếu caller không phải ADMIN → 403 (CustomAccessDeniedHandler).

## Quan hệ với chức năng khác
- **AuthenticationService**: nguồn ghi log login.
- **LoginHistoryService**: cung cấp API, hỗ trợ audit.
- **Security**: JWT filter xác thực token trước khi vào controller.

## Các tệp liên quan
- Controller: `LoginHistoryController.java`
- Service: `LoginHistoryService.java`
- DTO: `LoginHistoryResponseDTO.java`
- Repository: `LoginHistoryRepository.java`, `UserRepository.java`
- Entity: `LoginHistory.java`
- Config/Security: `SecurityConfig.java` (phân quyền ADMIN)
