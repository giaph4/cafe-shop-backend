# Tài Liệu Triển Khai

## 1. Chuẩn bị môi trường
### 1.1 Phần cứng tối thiểu
| Môi trường | CPU | RAM | Ổ đĩa |
|------------|-----|-----|-------|
| Dev | 2 vCPU | 4 GB | 20 GB |
| Staging | 4 vCPU | 8 GB | 50 GB |
| Prod | 4-8 vCPU (auto scale) | 16 GB | 100 GB SSD |

### 1.2 Phần mềm
- Hệ điều hành: Ubuntu 22.04 LTS hoặc tương đương.
- Docker 26+, Docker Compose 2+.
- JDK 21 (Amazon Corretto, Temurin).
- Maven 3.9+ (nếu build ngoài Docker).
- MySQL 8.0+ (Managed RDS hoặc self-host).

## 2. Cấu hình hệ thống
### 2.1 Biến môi trường
| Biến | Mô tả | Ví dụ |
|------|-------|-------|
| `SPRING_PROFILES_ACTIVE` | Profile chạy | `prod` |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | Kết nối DB | `db.internal`, `3306`, `coffee_shop`, ... |
| `JWT_SECRET_KEY` | Khóa bí mật JWT (>=32 ký tự) | `openssl rand -base64 64` |
| `JWT_EXPIRATION` | Hạn token (ms) | `86400000` |
| `APP_CORS_ALLOWED_ORIGINS` | Danh sách origin | `https://admin.example.com` |
| `LOG_LEVEL` | Mức log | `INFO` |

### 2.2 cấu hình MySQL
```sql
CREATE DATABASE coffee_shop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'coffee_user'@'%' IDENTIFIED BY 'StrongPass!2025';
GRANT ALL PRIVILEGES ON coffee_shop.* TO 'coffee_user'@'%';
FLUSH PRIVILEGES;
```

## 3. Quy trình build & deploy
### 3.1 Build Docker image
```bash
git clone https://github.com/your-org/coffee-shop-backend.git
cd coffee-shop-backend
./mvnw clean package -DskipTests
# hoặc
docker build -t registry.example.com/coffee-shop-backend:1.0.0 .
```

### 3.2 Sử dụng Docker Compose
`docker-compose.yml` mẫu:
```yaml
version: '3.9'
services:
  app:
    image: registry.example.com/coffee-shop-backend:1.0.0
    env_file:
      - .env.prod
    ports:
      - "8080:8080"
    depends_on:
      - db
  db:
    image: mysql:8.0
    environment:
      MYSQL_DATABASE: coffee_shop
      MYSQL_USER: coffee_user
      MYSQL_PASSWORD: StrongPass!2025
      MYSQL_ROOT_PASSWORD: RootPass!2025
    volumes:
      - db_data:/var/lib/mysql
volumes:
  db_data:
```

Chạy:
```bash
docker compose up -d
```

### 3.3 CI/CD gợi ý
1. **Build stage**: chạy kiểm thử (`./mvnw test`), build image.
2. **Security scan**: Trivy cho image, OWASP Dependency Check cho Maven.
3. **Deploy stage**: sử dụng ArgoCD/GitOps hoặc GitHub Actions + SSH/Ansible.
4. **Post-deploy**: chạy migration, seed dữ liệu (nếu cần), health check.

## 4. Migration dữ liệu
- Sử dụng Flyway/Liquibase (khuyến nghị) – tạo script `V2025_11_01__initial_schema.sql`.
- Quy trình:
  1. Sao lưu DB cũ.
  2. Chạy migration trên môi trường staging.
  3. Kiểm tra dữ liệu.
  4. Áp dụng production (off-peak).

## 5. Kiểm tra sau triển khai
- Kiểm tra endpoint `/actuator/health`.
- Thực hiện smoke test: đăng nhập, tạo đơn, thanh toán thử.
- Giám sát log `docker logs -f app`.
- Xác minh Dashboard, báo cáo.

## 6. Rollback
- Sử dụng Docker tag trước đó, giữ lại bản backup DB.
- Quy trình: dừng container mới → khởi động container cũ → khôi phục backup dữ liệu nếu cần.

## 7. Bảo mật triển khai
- Chạy reverse proxy (Nginx) để terminate SSL.
- Bật logging truy cập, hạn chế IP truy cập admin.
- Thường xuyên rotate `JWT_SECRET_KEY` và mật khẩu DB.
- Bật backup tự động hàng ngày, retention 30 ngày.

## 8. Tài liệu bổ trợ
- Lưu `docker-compose.override.yml` cho môi trường dev (H2, swagger).
- Tạo playbook Ansible/Kubernetes manifest nếu triển khai trên cluster.
