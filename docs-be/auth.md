# Chức năng: Xác thực & Nhật ký đăng nhập

## Vai trò trong hệ thống
- Cung cấp API đăng nhập, đăng ký nội bộ và phát hành JWT.
- Quản lý danh sách vai trò, thông tin người dùng để phân quyền.
- Ghi nhận đầy đủ lịch sử đăng nhập phục vụ audit bảo mật.

## Thành phần liên quan
- **Controllers**:
  - `AuthenticationController` (`/api/v1/auth`) @src/main/java/com/giapho/coffee_shop_backend/controller/AuthenticationController.java#1-43
  - `LoginHistoryController` (`/api/v1/login-history`) @src/main/java/com/giapho/coffee_shop_backend/controller/LoginHistoryController.java#1-55
  - `UserController` (các API đổi mật khẩu, danh sách role, quản lý user) @src/main/java/com/giapho/coffee_shop_backend/controller/UserController.java#1-72
- **Services**:
  - `AuthenticationService` (đăng nhập, đăng ký, ghi log) @src/main/java/com/giapho/coffee_shop_backend/service/AuthenticationService.java#1-319
  - `LoginHistoryService` (truy vấn/ghi lịch sử đăng nhập) @src/main/java/com/giapho/coffee_shop_backend/service/LoginHistoryService.java#1-153
  - `UserService` (quản lý tài khoản, đổi mật khẩu) @src/main/java/com/giapho/coffee_shop_backend/service/UserService.java#1-143
  - `JwtService` (tạo/đọc token) @src/main/java/com/giapho/coffee_shop_backend/security/JwtService.java#1-83
- **Security**: `SecurityConfig`, `JwtAuthenticationFilter`, `CustomAccessDeniedHandler` (xem `security.md`).
- **DTO**: `LoginRequest`, `RegisterRequest`, `AuthenticationResponse`, `LoginHistoryResponseDTO`, `ChangePasswordRequestDTO`, `UserUpdateRequestDTO`, `UserResponseDTO`, `RoleDTO`.
- **Entity/Repository**: `User`, `Role`, `LoginHistory` + `UserRepository`, `RoleRepository`, `LoginHistoryRepository`.
- **Validation**: annotations ở DTO (`@NotBlank`, `@Email`, `@Pattern`, `@Size`, ...). Regex kiểm tra password/email/phone trong `AuthenticationService`.

## API chi tiết

### Đăng ký (`POST /api/v1/auth/register`)
- **Role yêu cầu**: `ADMIN` hoặc `MANAGER` (theo `SecurityConfig`).
- **Request (`RegisterRequest`)**:
  ```json
  {
    "username": "staff001",
    "password": "Str0ng@Pass",
    "fullName": "Nguyen Van A",
    "email": "staff001@coffee.vn",
    "phone": "0912345678",
    "roleIds": [2,3]
  }
  ```
- **Logic** (`AuthenticationService.register`):
  1. Chuẩn hóa input (trim, lowercase email, ...).
  2. Validate password bằng regex `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^\w\s]).{8,64}$`.
  3. Kiểm tra trùng username/email/phone.
  4. Nếu `roleIds` trống → tự gán ROLE_STAFF.
  5. Mã hóa mật khẩu (BCrypt), lưu user, sinh JWT (`JwtService.generateToken`).
- **Response 200** (`AuthenticationResponse`):
  ```json
  {
    "token": "<jwt>",
    "username": "staff001"
  }
  ```
- **Lỗi 400**: username/email/phone trùng, password/email định dạng sai.
- **Lỗi 401/403**: không đủ quyền đăng ký.

### Đăng nhập (`POST /api/v1/auth/login`)
- **Public** (permitAll trong `SecurityConfig`).
- **Request (`LoginRequest`)**:
  ```json
  {
    "username": "staff001",
    "password": "Str0ng@Pass"
  }
  ```
- **Logic** (`AuthenticationService.login`):
  1. Kiểm tra username/password không rỗng (nếu rỗng → ghi log thất bại + `BadCredentialsException`).
  2. Authenticate bằng `AuthenticationManager`.
  3. Nếu thành công → lấy user với roles, kiểm tra `ACTIVE`, sinh JWT.
  4. Ghi lại thành công/thất bại vào `LoginHistoryService` (bao gồm IP, User-Agent).
- **Response 200**: token JWT & username.
- **Lỗi 401**: sai thông tin.
- **Lỗi 403**: tài khoản bị disable/locked (`DisabledException`).

### Lịch sử đăng nhập (`GET /api/v1/login-history`)
- **Role yêu cầu**: `ADMIN`.
- **Query**: `username`, `success`, `startDate`, `endDate`, phân trang (`size=20`, `sort=loginAt DESC`).
- **Logic**: `LoginHistoryService.searchLoginHistory` dựng `Specification` theo filter, map sang DTO (bao gồm thông tin user nếu gắn).
- **Response**: `Page<LoginHistoryResponseDTO>` chứa ID, username, trạng thái, IP, userAgent, message, thời gian.

### Quản lý người dùng
- **Danh sách role** (`GET /api/v1/users/roles`) – `ADMIN`.
- **Danh sách user** (`GET /api/v1/users`) – `ADMIN`, phân trang sort theo username.
- **Chi tiết user** (`GET /api/v1/users/{id}`) – chính chủ hoặc `ADMIN`.
- **Cập nhật user** (`PUT /api/v1/users/{id}`) – chính chủ hoặc `ADMIN`; kiểm tra trùng phone/email, đảm bảo user có ít nhất 1 role.
- **Đổi mật khẩu** (`POST /api/v1/users/change-password`) – tất cả user đăng nhập; kiểm tra mật khẩu hiện tại, mật khẩu mới không trùng cũ, so khớp confirm.

## Ràng buộc bảo mật & JWT
- Chi tiết trong `security.md`:
  - `JwtAuthenticationFilter` bắt buộc token cho tất cả request (trừ login/register/GET file).
  - `@PreAuthorize` áp dụng role cho từng API.
  - `CustomAccessDeniedHandler` trả JSON 403 khi thiếu quyền.

## Quy tắc validation chính
- Username: 3-50 ký tự, không trùng.
- Password: 8-64 ký tự, bao gồm chữ hoa, chữ thường, số, ký tự đặc biệt.
- Email: định dạng hợp lệ, không trùng.
- Phone: regex Việt Nam `^(\+?84|0)\d{9}$`.
- User update: phải có ít nhất một role.
- Đổi mật khẩu: mật khẩu mới ≠ mật khẩu cũ.

## Luồng lỗi & thông điệp nổi bật
| Exception | HTTP | Message |
| --- | --- | --- |
| `IllegalArgumentException` | 400 | "Username is already taken", "Email is already in use", "Password must ..." |
| `BadCredentialsException` | 401 | "Invalid username or password" |
| `DisabledException` | 403 | "Account is disabled or locked" |
| `AccessDeniedException` | 403 | "You do not have permission ..." |
| `MethodArgumentNotValidException` | 400 | "Dữ liệu đầu vào không hợp lệ" (GlobalExceptionHandler) |
| `EntityNotFoundException` | 404 | "User not found ..." |

## Quan hệ với chức năng khác
- **Security/JWT**: login/register cấp token; mọi request khác được bảo vệ.
- **LoginHistory**: ghi nhận audit login.
- **User Management**: update hồ sơ, đổi mật khẩu.
- **Dashboard**: token chứa `authorities`, `userId`, `fullName` hỗ trợ hiển thị.

## Các tệp liên quan trong BE
- Controller: `AuthenticationController.java`, `LoginHistoryController.java`, `UserController.java`
- Service: `AuthenticationService.java`, `LoginHistoryService.java`, `UserService.java`, `JwtService.java`
- Repository: `UserRepository.java`, `RoleRepository.java`, `LoginHistoryRepository.java`
- DTO: `LoginRequest.java`, `RegisterRequest.java`, `AuthenticationResponse.java`, `LoginHistoryResponseDTO.java`, `UserUpdateRequestDTO.java`, `UserResponseDTO.java`, `ChangePasswordRequestDTO.java`, `RoleDTO.java`
- Entity: `User.java`, `Role.java`, `LoginHistory.java`
- Security: `SecurityConfig.java`, `JwtAuthenticationFilter.java`, `CustomAccessDeniedHandler.java`
