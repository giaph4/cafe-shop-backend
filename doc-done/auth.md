# Chức năng: Xác thực & Nhật ký đăng nhập

## Vai trò trong hệ thống
- Cung cấp cơ chế đăng ký, đăng nhập và phát hành JWT cho người dùng nội bộ.
- Ghi nhận lịch sử đăng nhập giúp đội vận hành/audit truy vết sự kiện bảo mật.
- Cấu hình bảo mật dựa trên JWT và phân quyền (Role-based Access Control) ở cấp phương thức.

## Luồng xử lý Backend
1. **Đăng ký (`POST /api/v1/auth/register`)**: kiểm tra hợp lệ input, gán vai trò mặc định (ROLE_STAFF) nếu không truyền, lưu người dùng mới và trả về JWT.
2. **Đăng nhập (`POST /api/v1/auth/login`)**: xác thực thông tin thông qua `AuthenticationManager`, phát hành JWT nếu thành công, ghi nhận lịch sử đăng nhập thành công/thất bại.
3. **Lấy lịch sử đăng nhập (`GET /api/v1/login-history`)**: lọc theo nhiều tiêu chí (username, success, khoảng thời gian) và phân trang để phục vụ công tác audit.
4. **Bảo mật JWT**: mọi request (ngoại trừ login/register & GET file) đều phải mang header `Authorization: Bearer <token>`, được xử lý bởi `JwtAuthenticationFilter`.

## Thành phần liên quan
- **Controller**
  - `AuthenticationController` @src/main/java/com/giapho/coffee_shop_backend/controller/AuthenticationController.java#1-43
  - `LoginHistoryController` @src/main/java/com/giapho/coffee_shop_backend/controller/LoginHistoryController.java#1-55
- **Service**
  - `AuthenticationService` xử lý đăng ký/đăng nhập @src/main/java/com/giapho/coffee_shop_backend/service/AuthenticationService.java#1-319
  - `LoginHistoryService` ghi và truy vấn lịch sử đăng nhập @src/main/java/com/giapho/coffee_shop_backend/service/LoginHistoryService.java#1-153
  - `JwtService` tạo/đọc token @src/main/java/com/giapho/coffee_shop_backend/security/JwtService.java#1-83
- **Config & Security**
  - `SecurityConfig` thiết lập filter chain & phân quyền @src/main/java/com/giapho/coffee_shop_backend/config/SecurityConfig.java#1-86
  - `JwtAuthenticationFilter` @src/main/java/com/giapho/coffee_shop_backend/security/JwtAuthenticationFilter.java#1-72
  - `CustomAccessDeniedHandler` (dùng cho 403, cùng package security)
- **Repository & Entity**
  - `UserRepository`, `RoleRepository`, `LoginHistoryRepository`
  - Entities: `User`, `Role`, `LoginHistory` @src/main/java/com/giapho/coffee_shop_backend/domain/entity/User.java#1-125
- **DTO**
  - `RegisterRequest`, `LoginRequest`, `AuthenticationResponse` @src/main/java/com/giapho/coffee_shop_backend/dto/RegisterRequest.java#1-41
  - `LoginHistoryResponseDTO` @src/main/java/com/giapho/coffee_shop_backend/dto/LoginHistoryResponseDTO.java#1-27
- **Validation & Exception**
  - Validation annotations trên DTO (`@NotBlank`, `@Email`, `@Pattern`).
  - Regex kiểm tra password/email/phone trong `AuthenticationService`.
  - `GlobalExceptionHandler` chuẩn hóa lỗi 400/401/403/500 @src/main/java/com/giapho/coffee_shop_backend/exception/GlobalExceptionHandler.java#1-296.

## Danh sách API
| Method | URL | Mô tả | Role yêu cầu |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/register` | Tạo người dùng nội bộ mới | `ADMIN`, `MANAGER` |
| POST | `/api/v1/auth/login` | Đăng nhập, trả JWT | Public |
| GET | `/api/v1/login-history` | Tra cứu lịch sử đăng nhập (phân trang) | `ADMIN` |

## Chi tiết API

### POST `/api/v1/auth/register`
- **Controller**: `AuthenticationController.register`
- **Request body** (`RegisterRequest`):
  ```json
  {
    "username": "staff001",
    "password": "Str0ng@Pass",
    "fullName": "Nguyen Van A",
    "email": "staff001@coffee.vn",
    "phone": "0912345678",
    "roleIds": [2, 3]
  }
  ```
  - Validation: `@NotBlank`, `@Size(min=6)` cho password, `@Pattern` điện thoại VN.
- **Service logic** (`AuthenticationService.register`):
  1. Chuẩn hóa input (trim, normalize).
  2. Kiểm tra trùng username/email; nếu không có `roleIds` -> tự gán ROLE_STAFF.
  3. Mã hóa mật khẩu bằng `PasswordEncoder`.
  4. Lưu `User` với trạng thái `ACTIVE`, sinh JWT qua `JwtService.generateToken`.
- **Response 200 (OK)** (`AuthenticationResponse`):
  ```json
  {
    "token": "<jwt_token>",
    "username": "staff001"
  }
  ```
- **Response 400**: dữ liệu không hợp lệ (`IllegalArgumentException`, `MethodArgumentNotValidException`).
- **Response 401**: Không áp dụng (endpoint yêu cầu auth cho caller, sai role -> 403).
- **Response 403**: Caller thiếu role `ADMIN`/`MANAGER` (`AccessDeniedException`).
- **Response 500**: lỗi hệ thống/DB.

### POST `/api/v1/auth/login`
- **Controller**: `AuthenticationController.login`
- **Request body** (`LoginRequest`):
  ```json
  {
    "username": "staff001",
    "password": "Str0ng@Pass"
  }
  ```
- **Service logic**:
  1. Dùng `AuthenticationManager` xác thực thông tin.
  2. Nếu thiếu username/password -> ghi failed login + `BadCredentialsException`.
  3. Nếu tài khoản không `ACTIVE` -> `DisabledException`.
  4. Sinh JWT, ghi nhật ký thành công (`recordSuccessfulLogin`).
- **Response 200**:
  ```json
  {
    "token": "<jwt_token>",
    "username": "staff001"
  }
  ```
- **Response 400**: username/password rỗng.
- **Response 401**: sai thông tin đăng nhập (`BadCredentialsException`).
- **Response 403**: tài khoản bị khóa/disabled (`DisabledException`).
- **Response 500**: lỗi khác khi authenticate.

### GET `/api/v1/login-history`
- **Controller**: `LoginHistoryController.getLoginHistory`
- **Query params**:
  - `username` (tùy chọn, tìm kiếm like)
  - `success` (Boolean)
  - `startDate`, `endDate` (ISO-8601 DateTime)
  - Phân trang: `page`, `size`, `sort`
- **Service logic**:
  1. Ghép `Specification` filter tùy theo tham số.
  2. Query `Page<LoginHistory>` và map sang DTO (kèm thông tin user nếu có).
- **Response 200** mẫu:
  ```json
  {
    "content": [
      {
        "id": 123,
        "userId": 45,
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
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20
    },
    "totalElements": 1,
    "totalPages": 1
  }
  ```
- **Response 400**: sai định dạng ngày hoặc sort (TypeMismatchException).
- **Response 401**: thiếu/invalid JWT.
- **Response 403**: caller không phải ADMIN.
- **Response 500**: lỗi truy vấn DB.

## Điều kiện nghiệp vụ & Validation chính
- Username/email/phone duy nhất trong hệ thống.
- Password phải thỏa regex `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\w\s]).{8,64}$`.
- Điện thoại theo định dạng Việt Nam `^(\+?84|0)\d{9}$`.
- Tài khoản không `ACTIVE` không được đăng nhập.
- Mọi lần đăng nhập (thành công/thất bại) đều được ghi vào `login_history` với IP, User-Agent.

## Luồng lỗi (Exception + Message)
| Exception | Nguồn | HTTP | Message |
| --- | --- | --- | --- |
| `IllegalArgumentException` | Đăng ký/Đăng nhập | 400 | Tùy thông điệp (trùng username/email, voucher rỗng, ...) |
| `MethodArgumentNotValidException` | DTO invalid | 400 | "Dữ liệu đầu vào không hợp lệ" |
| `BadCredentialsException` | Đăng nhập | 401 | "Invalid username or password" |
| `DisabledException` | Đăng nhập | 403 | "Account is disabled or locked" |
| `AccessDeniedException` | Thiếu quyền | 403 | "You do not have permission to access this resource" |
| `Exception` | Khác | 500 | "An unexpected error occurred. Please try again later or contact support." |

## Role/Permission yêu cầu
- `POST /auth/register`: `hasAnyRole('ADMIN', 'MANAGER')`.
- `POST /auth/login`: public (không cần token).
- `GET /login-history`: `hasRole('ADMIN')`.
- Các request khác trong hệ thống cần JWT hợp lệ do endpoint này cấp.

## Quan hệ với chức năng khác
- JWT chứa `authorities`, `userId`, `fullName` -> hỗ trợ Audit, Order/Payment gán nhân viên.
- Login history có thể được sử dụng trong báo cáo bảo mật/ROLE dashboard.
- Thay đổi mật khẩu & quản lý role nằm ở chức năng "Quản lý người dùng".

## Các tệp liên quan khác trong Backend
- DTO: `ChangePasswordRequestDTO` (đổi mật khẩu ở user-management).
- Config: `ApplicationConfig` (khởi tạo bean PasswordEncoder).
- Exception: `ErrorResponse`, `GlobalExceptionHandler`.
