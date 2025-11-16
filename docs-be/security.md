# Chức năng: Bảo mật & JWT

## Vai trò trong hệ thống
- Bảo vệ tất cả API backend bằng JWT, triển khai xác thực/ủy quyền dựa trên Spring Security.
- Cấu hình phân quyền URL + `@PreAuthorize` cho từng chức năng nghiệp vụ.
- Cung cấp cơ chế mã hóa mật khẩu, load user/role, xử lý lỗi truy cập chuẩn.

## Thành phần chính
- **SecurityConfig**: định nghĩa `SecurityFilterChain`, CORS, session stateless, phân quyền URL, gắn `JwtAuthenticationFilter`, `CustomAccessDeniedHandler` @src/main/java/com/giapho/coffee_shop_backend/config/SecurityConfig.java#1-86.
- **ApplicationConfig**: khai báo `UserDetailsService`, `PasswordEncoder`, `AuthenticationProvider`, `AuthenticationManager` @src/main/java/com/giapho/coffee_shop_backend/config/ApplicationConfig.java#1-48.
- **JwtService**: tạo/giải mã token, thêm extra claims (userId, fullName), kiểm tra hạn token @src/main/java/com/giapho/coffee_shop_backend/security/JwtService.java#1-83.
- **JwtAuthenticationFilter**: intercept request, đọc header Authorization, validate token, set authentication @src/main/java/com/giapho/coffee_shop_backend/security/JwtAuthenticationFilter.java#1-72.
- **CustomAccessDeniedHandler**: trả JSON lỗi 403 khi thiếu quyền @src/main/java/com/giapho/coffee_shop_backend/security/CustomAccessDeniedHandler.java#1-37.
- **GlobalExceptionHandler**: chuẩn hóa lỗi validation/401/403/404/500 @src/main/java/com/giapho/coffee_shop_backend/exception/GlobalExceptionHandler.java#1-296.

## Luồng bảo mật
1. **Đăng nhập** (`POST /api/v1/auth/login`): `AuthenticationService` authenticate, `JwtService.generateToken` trả JWT cho FE.
2. **Yêu cầu API**: client gửi `Authorization: Bearer <token>`.
3. **JwtAuthenticationFilter**:
   - Đọc header → token.
   - Gọi `JwtService.extractUsername`.
   - Load `UserDetails` → `JwtService.isTokenValid`.
   - Set `UsernamePasswordAuthenticationToken` vào `SecurityContextHolder`.
4. **Authorization**: `SecurityConfig` + `@PreAuthorize` quyết định quyền truy cập.
5. **Access denied**: `CustomAccessDeniedHandler` trả JSON 403.
6. **Token invalid/expired**: filter trả 401 (`Invalid JWT Token`).

## Phân quyền URL (SecurityConfig)
- `POST /api/v1/auth/login` : permitAll.
- `POST /api/v1/auth/register` : `hasAnyRole('ADMIN','MANAGER')`.
- `GET /api/v1/files/**` : permitAll.
- `POST/DELETE /api/v1/files/**` : `hasAnyRole('MANAGER','ADMIN')`.
- Các endpoint khác: authenticated → chịu `@PreAuthorize` tại controller (ví dụ `/api/admin/dashboard` chỉ ADMIN, `/api/v1/orders/**` yêu cầu STAFF/MANAGER/ADMIN, ...).

## CORS & Session
- CORS: allow origins dựa trên `app.cors.allowed-origins` (config), allow methods GET/POST/PUT/PATCH/DELETE/OPTIONS, headers `Authorization`, `Content-Type`, `X-Requested-With`.
- Session: `SessionCreationPolicy.STATELESS` (server không lưu session).

## JWT cấu hình
- Secret: `application.jwt.secretKey` (Base64) trong cấu hình.
- Expiration: `application.jwt.expirationMs`.
- Extra claims: thêm `authorities`, `userId`, `fullName` trong token để FE dễ đọc.
- Thuật toán: HS256 (`io.jsonwebtoken`).

## Bean & Password
- `UserDetailsService`: load user + roles qua `UserRepository.findWithRolesByUsername`.
- `PasswordEncoder`: BCrypt.
- `AuthenticationProvider`: `DaoAuthenticationProvider` sử dụng userDetailsService + passwordEncoder.
- `AuthenticationManager`: lấy từ `AuthenticationConfiguration`.

## Xử lý lỗi
- 401: token thiếu/invalid → filter `sendError`.
- 403: `CustomAccessDeniedHandler` trả JSON `ErrorResponse`.
- Validation lỗi (`MethodArgumentNotValidException`): `GlobalExceptionHandler` trả 400.
- 404 (`EntityNotFoundException`): `GlobalExceptionHandler` trả JSON.
- 500: bắt trong `GlobalExceptionHandler`, log và trả thông điệp chuẩn.

## Quan hệ với chức năng khác
- **AuthenticationController/Service**: tạo JWT, xử lý đăng ký.
- **LoginHistoryService**: ghi log thành công/thất bại login (tích hợp trong `AuthenticationService`).
- **File upload**: rely on SecurityConfig cho phép GET public.
- **Tất cả controller**: sử dụng `@PreAuthorize` để xác định role.
- **Audit**: `GlobalExceptionHandler` và `CustomAccessDeniedHandler` log sự kiện bảo mật.

## Checklist bảo mật hiện tại
- [x] JWT filter trước mỗi request.
- [x] Stateless session.
- [x] CORS theo cấu hình.
- [x] Password encoder (BCrypt).
- [x] AccessDeniedHandler tùy chỉnh.
- [x] Global error handling.
- [x] Role-based method security (`@EnableMethodSecurity`).

## Các tệp liên quan
- Config: `SecurityConfig.java`, `ApplicationConfig.java`
- Security: `JwtService.java`, `JwtAuthenticationFilter.java`, `CustomAccessDeniedHandler.java`
- Exception: `GlobalExceptionHandler.java`, `ErrorResponse.java`
- Repository: `UserRepository.java`
- Service: `AuthenticationService.java`, `LoginHistoryService.java`
