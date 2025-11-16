# Bảo Mật Hệ Thống

## Mục lục
- [1. Mục tiêu bảo mật](#1-mục-tiêu-bảo-mật)
- [2. Kiến trúc bảo mật tổng thể](#2-kiến-trúc-bảo-mật-tổng-thể)
- [3. Xác thực & phân quyền](#3-xác-thực--phân-quyền)
- [4. Quản lý phiên & JWT](#4-quản-lý-phiên--jwt)
- [5. Bảo vệ API](#5-bảo-vệ-api)
- [6. Bảo vệ dữ liệu](#6-bảo-vệ-dữ-liệu)
- [7. Giám sát & phản ứng sự cố](#7-giám-sát--phản-ứng-sự-cố)
- [8. Checklist bảo mật triển khai](#8-checklist-bảo-mật-triển-khai)

## 1. Mục tiêu bảo mật
- Bảo vệ dữ liệu khách hàng, nhân viên và giao dịch tài chính khỏi truy cập trái phép.
- Đảm bảo chỉ người có thẩm quyền mới thực hiện được các thao tác nhạy cảm (quản lý giá, hủy đơn, phân quyền).
- Theo dõi, phát hiện và phản ứng nhanh với sự cố bảo mật.
- Tuân thủ quy định pháp lý về dữ liệu cá nhân và bảo mật thông tin.

## 2. Kiến trúc bảo mật tổng thể
```plantuml
@startuml
!theme plain
rectangle "Client" {
  actor POS
  actor AdminPortal
  actor MobileApp
}
rectangle "Gateway" {
  component "API Gateway" as APIGW
}
rectangle "Security Layer" {
  component "JwtAuthenticationFilter"
  component "CustomAccessDeniedHandler"
  component "SecurityConfig"
}
rectangle "Application" {
  component "Auth Service"
  component "Order Service"
  component "User Service"
  component "Audit Service"
}
rectangle "Data" {
  database "MySQL"
  storage "File Storage"
  database "Audit Logs"
}
POS --> APIGW
AdminPortal --> APIGW
MobileApp --> APIGW
APIGW --> JwtAuthenticationFilter
JwtAuthenticationFilter --> Auth Service
Auth Service --> MySQL
Order Service --> Audit Logs
User Service --> Audit Logs
@enduml
```

## 3. Xác thực & phân quyền
- Spring Security + JWT (HS512). Tất cả endpoint (trừ `/auth/login`) yêu cầu token.
- Phân quyền theo vai trò:
  - `ROLE_ADMIN`: quản trị toàn hệ thống.
  - `ROLE_MANAGER`: quản lý cửa hàng, nhân viên, voucher, báo cáo.
  - `ROLE_STAFF`: bán hàng, cập nhật tồn kho.
- Sử dụng `@PreAuthorize`, `@Secured` cho controller/service nhạy cảm.
- Đăng ký tài khoản mới chỉ dành cho `ROLE_ADMIN`/`ROLE_MANAGER`.

## 4. Quản lý phiên & JWT
- Access token sống 15 phút (`expiresIn`), refresh token 7 ngày (lưu database hoặc Redis nếu triển khai).
- Refresh token trả về ở `HttpOnly` cookie (tùy chọn) hoặc response body.
- Mỗi lần đăng nhập ghi vào `login_history` (username, ip, user-agent, trạng thái).
- Khóa tài khoản khi vượt quá số lần đăng nhập thất bại (`MAX_FAILED_ATTEMPTS`, mặc định 5).
- Thu hồi token bằng cách lưu danh sách token vô hiệu trong cache (khi cần).

## 5. Bảo vệ API
- Sử dụng HTTPS, TLS 1.2 trở lên.
- Rate limiting tại gateway: ví dụ `/auth/login` 5 request/phút.
- CORS: chỉ cho phép origin cấu hình trong `app.cors.allowed-origins`.
- Input validation & sanitization:
  - DTO validation (Jakarta Validation).
  - Escaping & Parameterized query (do Spring Data JPA).
- Chống XSS: lọc input, encode output (frontend), set header `Content-Security-Policy`.
- Chống CSRF: API stateless nên disable CSRF, nhưng với form login nội bộ phải có token CSRF.
- Chống SQL Injection: dùng ORM, không xây SQL bằng concat string.

## 6. Bảo vệ dữ liệu
- Mật khẩu mã hóa bằng BCrypt strength >= 12.
- Không lưu thông tin thẻ thanh toán (giao tiếp qua POS ngoại).
- Dữ liệu nhạy cảm (email, phone) không xuất trong log.
- Sao lưu DB hằng ngày, snapshot hàng tuần, mã hóa khi lưu trữ (S3 SSE).
- File upload quét virus (tùy chọn) và phân quyền truy cập.
- Script backup: sử dụng `mysqldump` + chuyển lên storage an toàn.

## 7. Giám sát & phản ứng sự cố
- Logging chuẩn JSON, gồm `traceId`, `userId`, `action`.
- Audit log cho các thao tác:
  - Cập nhật giá, voucher, phân quyền, hủy đơn, điều chỉnh kho.
- Alert khi phát hiện:
  - >10 lần đăng nhập sai từ cùng IP trong 5 phút.
  - Số lần hủy đơn vượt ngưỡng.
  - Thay đổi cấu hình hệ thống.
- Lưu trữ log >= 90 ngày, audit log >= 12 tháng.
- Quy trình phản ứng sự cố: xem `quy-trinh-xu-ly.md` (mục 8).

## 8. Checklist bảo mật triển khai
- [x] HTTPS/TLS cấu hình chuẩn.
- [x] JWT secret >= 64 ký tự và lưu trong Secret Manager.
- [x] Bật CORS theo danh sách origin tin cậy.
- [x] Rate limit login & sensitive API.
- [x] Audit log và login history hoạt động.
- [x] Sao lưu DB tự động và kiểm thử restore.
- [x] Phân quyền IAM cho file storage chỉ đọc/xóa theo role.
- [x] Scan dependency & container (Snyk/Trivy) trước khi deploy.

---
**Mức độ hoàn thiện:** 100%
**Hạng mục còn thiếu:** Không
