# Cấu Hình Môi Trường

## Mục lục
- [1. Tổng quan](#1-tổng-quan)
- [2. Thông số chung](#2-thông-số-chung)
- [3. Môi trường Development](#3-môi-trường-development)
- [4. Môi trường Test/Staging](#4-môi-trường-teststaging)
- [5. Môi trường Production](#5-môi-trường-production)
- [6. Quản lý biến môi trường](#6-quản-lý-biến-môi-trường)
- [7. Cấu hình bảo mật](#7-cấu-hình-bảo-mật)
- [8. Checklist cấu hình](#8-checklist-cấu-hình)

## 1. Tổng quan
Tài liệu liệt kê các thông số cấu hình chính cho từng môi trường triển khai của hệ thống backend quán cà phê. Các giá trị mẫu giúp xây dựng file `.env` hoặc cấu hình CI/CD.

## 2. Thông số chung
| Key | Mục đích | Ghi chú |
|-----|----------|--------|
| `SPRING_PROFILES_ACTIVE` | Chọn profile Spring | `dev`, `test`, `staging`, `prod` |
| `SERVER_PORT` | Cổng ứng dụng | Mặc định 8080, có thể thay đổi |
| `JWT_SECRET_KEY` | Khóa ký JWT | Chuỗi base64 >=64 ký tự |
| `JWT_EXPIRATION` | TTL access token (ms) | 900000 (15 phút) |
| `JWT_REFRESH_EXPIRATION` | TTL refresh token | 604800000 (7 ngày) |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | Kết nối MySQL | Host nội bộ hoặc RDS |
| `DB_USERNAME`, `DB_PASSWORD` | Tài khoản DB | Lưu trong Secret Manager |
| `APP_CORS_ALLOWED_ORIGINS` | Danh sách origin | CSV: `https://admin.example.com` |
| `FILE_STORAGE_PATH` | Thư mục lưu file | `/app/uploads` (dev) |
| `LOG_LEVEL_ROOT` | Mức log | `INFO`, `WARN`, `DEBUG` |

## 3. Môi trường Development
- Chạy bằng Docker Compose hoặc trực tiếp trên máy dev.
- Sử dụng profile `dev`.
- Database: MySQL hoặc H2 in-memory.
- Cấu hình mẫu `.env.dev`:
```env
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=8088
DB_HOST=localhost
DB_PORT=3306
DB_NAME=coffee_shop_dev
DB_USERNAME=dev_user
DB_PASSWORD=dev_pass
JWT_SECRET_KEY=DEV_SECRET_BASE64==
APP_CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173
LOG_LEVEL_ROOT=DEBUG
```
- Bật Swagger UI, H2 console, logging DEBUG.

## 4. Môi trường Test/Staging
- Dùng để kiểm thử tự động và UAT.
- Profile `staging`.
- Database: MySQL riêng biệt, dữ liệu gần giống production (ẩn danh hoá).
- Cấu hình mẫu:
```env
SPRING_PROFILES_ACTIVE=staging
SERVER_PORT=8080
DB_HOST=staging-db.internal
DB_NAME=coffee_shop_staging
DB_USERNAME=staging_user
DB_PASSWORD=${STAGING_DB_PASSWORD}
JWT_SECRET_KEY=${STAGING_JWT_SECRET}
APP_CORS_ALLOWED_ORIGINS=https://staging-admin.example.com
LOG_LEVEL_ROOT=INFO
```
- Disable Swagger UI public (chỉ accessible sau auth).
- Bật Prometheus metrics.

## 5. Môi trường Production
- Profile `prod`.
- Database: MySQL HA (Multi-AZ). Backup hằng ngày.
- File storage: S3 hoặc NFS bảo mật.
- Cấu hình `.env.prod` (ví dụ sử dụng Secret Manager):
```env
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
DB_HOST=prod-db.cluster-123.ap-southeast-1.rds.amazonaws.com
DB_PORT=3306
DB_NAME=coffee_shop_prod
DB_USERNAME=${DB_USER}
DB_PASSWORD=${DB_PASSWORD}
JWT_SECRET_KEY=${JWT_SECRET_KEY}
JWT_EXPIRATION=900000
JWT_REFRESH_EXPIRATION=604800000
APP_CORS_ALLOWED_ORIGINS=https://admin.coffee-chain.vn,https://pos.coffee-chain.vn
FILE_STORAGE_PATH=/mnt/storage
LOG_LEVEL_ROOT=INFO
```
- Bật rate limiting tại gateway, cấu hình HTTPS.
- Ghi log mức INFO, gửi đến ELK.

## 6. Quản lý biến môi trường
- Sử dụng Secret Manager (AWS Secrets Manager, Azure Key Vault) cho thông tin nhạy cảm.
- Không commit `.env` chứa thông tin thật.
- CI/CD inject biến thông qua pipeline (GitHub Actions secrets).
- Tất cả biến phải được liệt kê trong tài liệu để dễ audit.

## 7. Cấu hình bảo mật
- TLS certificate quản lý bằng ACM (AWS) hoặc tương đương.
- Bật security header tại gateway: `Strict-Transport-Security`, `X-Frame-Options`, `X-Content-Type-Options`.
- Giới hạn IP truy cập endpoint quản trị (VPN hoặc WAF rule).
- Kiểm tra lỗ hổng dependency bằng Snyk/OWASP Dependency Check mỗi lần build.

## 8. Checklist cấu hình
- [x] Định nghĩa profile & biến bắt buộc.
- [x] Document giá trị mặc định/an toàn cho từng môi trường.
- [x] Cấu hình logging & monitoring theo môi trường.
- [x] Tài khoản DB/Service Account nằm trong Secret Manager.
- [x] Kiểm tra lại CORS và rate limiting trước khi go-live.

---
**Mức độ hoàn thiện:** 100%
**Hạng mục còn thiếu:** Không
