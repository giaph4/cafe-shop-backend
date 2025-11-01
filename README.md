# Coffee Shop Management System - Backend API

## 📋 Tổng Quan

Hệ thống quản lý quán cà phê toàn diện với các tính năng:
- ✅ Quản lý sản phẩm, danh mục
- ✅ Quản lý đơn hàng, bàn
- ✅ Quản lý nhân viên, khách hàng
- ✅ Quản lý kho, nguyên liệu
- ✅ Hệ thống voucher giảm giá
- ✅ Báo cáo doanh thu, lợi nhuận
- ✅ Authentication & Authorization với JWT
- ✅ Audit Trail (tracking thay đổi)

## 🛠 Công Nghệ Sử Dụng

- **Java 21**
- **Spring Boot 3.5.6**
- **MySQL 8.0+**
- **JWT (JSON Web Token)**
- **MapStruct** (DTO Mapping)
- **Lombok** (Boilerplate Reduction)
- **Apache POI** (Excel Export)
- **SpringDoc OpenAPI** (API Documentation)

## 📦 Cài Đặt

### 1. Yêu Cầu Hệ Thống

```bash
- JDK 21+
- Maven 3.8+
- MySQL 8.0+
- Git
```

### 2. Clone Repository

```bash
git clone https://github.com/your-repo/coffee-shop-backend.git
cd coffee-shop-backend
```

### 3. Cấu Hình Database

Tạo database MySQL:

```sql
CREATE DATABASE coffee_shop_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Cập nhật `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/coffee_shop_db
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 4. Build & Run

```bash
# Build project
./mvnw clean install

# Run application
./mvnw spring-boot:run
```

Hoặc trên Windows:

```bash
mvnw.cmd clean install
mvnw.cmd spring-boot:run
```

Application sẽ chạy tại: `http://localhost:8088`

## 🔐 Security

### JWT Configuration

**QUAN TRỌNG**: Trong môi trường production, đổi JWT secret key:

```properties
# Sử dụng biến môi trường
application.jwt.secretKey=${JWT_SECRET_KEY}
```

Tạo secret key mạnh:

```bash
openssl rand -base64 64
```

### Default Roles

Hệ thống tự động tạo 3 roles:
- `ROLE_ADMIN` - Toàn quyền
- `ROLE_MANAGER` - Quản lý
- `ROLE_STAFF` - Nhân viên

## 📚 API Documentation

Sau khi chạy ứng dụng, truy cập:

- **Swagger UI**: http://localhost:8088/swagger-ui.html
- **API Docs**: http://localhost:8088/api-docs

## 🔑 Authentication

### 1. Register (Đăng ký tài khoản mới)

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "staff01",
  "password": "password123",
  "fullName": "Nguyen Van A",
  "email": "staff01@example.com",
  "phone": "0901234567"
}
```

### 2. Login (Đăng nhập)

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "staff01",
  "password": "password123"
}
```

**Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "staff01"
}
```

### 3. Sử dụng Token

Thêm header vào mọi request cần authentication:

```http
Authorization: Bearer {your_token}
```

## 📊 Ví Dụ API Calls

### Tạo Order Mới

```http
POST /api/v1/orders
Authorization: Bearer {token}
Content-Type: application/json

{
  "tableId": 1,
  "type": "AT_TABLE",
  "customerId": null,
  "items": [
    {
      "productId": 1,
      "quantity": 2,
      "notes": "Ít đá"
    }
  ]
}
```

### Apply Voucher

```http
POST /api/v1/orders/1/voucher
Authorization: Bearer {token}
Content-Type: application/json

{
  "voucherCode": "GIAM10K"
}
```

### Thanh Toán Order

```http
POST /api/v1/orders/1/payment
Authorization: Bearer {token}
Content-Type: application/json

{
  "paymentMethod": "CASH"
}
```

### Báo Cáo Doanh Thu

```http
GET /api/v1/reports/daily-revenue?date=2025-11-01
Authorization: Bearer {token}
```

## 🗂 Cấu Trúc Dự Án

```
src/main/java/com/giapho/coffee_shop_backend/
├── common/              # Constants, Enums
├── config/              # Spring Configuration
├── controller/          # REST Controllers
├── domain/
│   ├── entity/         # JPA Entities
│   └── repository/     # JPA Repositories
├── dto/                 # Data Transfer Objects
├── exception/           # Exception Handling
├── mapper/              # MapStruct Mappers
├── security/            # Security Components
└── service/             # Business Logic
```

## 🧪 Testing

```bash
# Chạy tất cả tests
./mvnw test

# Chạy với coverage report
./mvnw test jacoco:report
```

## 🔧 Cấu Hình Nâng Cao

### CORS Configuration

Trong `application.properties`:

```properties
app.cors.allowed-origins=http://localhost:5173,http://localhost:3000
```

### Database Connection Pool

```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
```

### Logging

```properties
logging.level.com.giapho.coffee_shop_backend=DEBUG
logging.file.name=logs/application.log
```

## 📝 Changelog

### Version 1.0.0 (Latest)

**New Features:**
- ✅ Audit Trail System
- ✅ Voucher Management
- ✅ Improved Exception Handling
- ✅ API Response Wrapper
- ✅ Async Processing
- ✅ Enhanced Security

**Improvements:**
- 🔧 Constants Management
- 🔧 Better Logging
- 🔧 Code Refactoring
- 🔧 Performance Optimization

## 🐛 Troubleshooting

### Lỗi kết nối Database

```
Error: Access denied for user...
```

**Giải pháp**: Kiểm tra username/password trong `application.properties`

### Lỗi JWT Invalid

```
Error: JWT signature does not match...
```

**Giải pháp**: Token hết hạn hoặc secret key không khớp. Login lại để lấy token mới.

### Port đã được sử dụng

```
Error: Port 8088 is already in use
```

**Giải pháp**: Đổi port trong `application.properties`:
```properties
server.port=8089
```

## 🤝 Contributing

1. Fork repository
2. Tạo feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Tạo Pull Request

## 📄 License

This project is licensed under the MIT License.

## 👥 Contact

- **Developer**: Gia Pho
- **Email**: giapho@example.com
- **GitHub**: [@giapho](https://github.com/giapho)

## 🎯 Roadmap

- [ ] File upload cho product images
- [ ] Real-time notifications với WebSocket
- [ ] Multi-language support
- [ ] Mobile app integration
- [ ] Advanced analytics dashboard
- [ ] Integration với payment gateways

---

**Happy Coding! ☕️**