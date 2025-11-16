# ☕ Hệ Thống Quản Lý Quán Cà Phê - Backend API

## 📋 Tổng Quan

Hệ thống quản lý quán cà phê toàn diện với đầy đủ các tính năng cần thiết cho việc vận hành một quán cà phê chuyên nghiệp.

### Quản Lý Cơ Bản
- ✅ Quản lý sản phẩm & danh mục
- ✅ Quản lý đơn hàng & bàn
- ✅ Quản lý nhân viên & phân quyền
- ✅ Quản lý khách hàng & tích điểm
- ✅ Quản lý kho & nguyên liệu
- ✅ Hệ thống khuyến mãi & voucher

### Báo Cáo & Phân Tích
- 📊 Báo cáo doanh thu theo ngày/tuần/tháng
- 📈 Đánh giá hiệu suất nhân viên
- 📉 Thống kê bán hàng theo danh mục
- 🕒 Phân tích giờ cao điểm
- 💳 Thống kê phương thức thanh toán
- 🔄 So sánh doanh thu theo kỳ

### Tính Năng Hệ Thống
- 🔐 Xác thực và phân quyền với JWT
- 📝 Theo dõi lịch sử thay đổi
- 📱 API RESTful chuẩn với OpenAPI 3.0
- 📊 Xuất báo cáo ra file Excel
- 🐳 Hỗ trợ triển khai với Docker

## 🛠 Công Nghệ Sử Dụng

- **Java 21** - Ngôn ngữ lập trình chính
- **Spring Boot 3.5.6** - Framework backend
- **MySQL 8.0+** - Hệ quản trị cơ sở dữ liệu
- **JWT** - Xác thực người dùng
- **MapStruct** - Ánh xạ đối tượng DTO
- **Lombok** - Giảm code boilerplate
- **Apache POI** - Xuất báo cáo Excel
- **SpringDoc OpenAPI** - Tài liệu API tự động

## 🚀 Hướng Dẫn Cài Đặt

### 1. Yêu Cầu Hệ Thống

- JDK 21 trở lên
- MySQL 8.0+ hoặc MariaDB
- Maven 3.8+
- Git

### 2. Cài Đặt Với Docker (Khuyến Nghị)

```bash
# Sao chép mã nguồn
git clone [đường-dẫn-repository]
cd coffee-shop-backend

# Khởi động ứng dụng
docker-compose up -d
```

### 3. Cấu Hình Cơ Sở Dữ Liệu

Tạo database mới:

```sql
CREATE DATABASE coffee_shop_db 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;
```

Cấu hình kết nối trong `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/coffee_shop_db
spring.datasource.username=tên_đăng_nhập
spring.datasource.password=mật_khẩu
```

### 4. Biên Dịch Và Chạy

```bash
# Biên dịch dự án
./mvnw clean install

# Khởi động ứng dụng
./mvnw spring-boot:run
```

Truy cập ứng dụng tại: http://localhost:8088

## 🔐 Bảo Mật

### Cấu Hình JWT

**LƯU Ý QUAN TRỌNG**: Trong môi trường thật, cần thay đổi khóa bí mật JWT:

```properties
# Sử dụng biến môi trường để truyền vào khi chạy ứng dụng
application.jwt.secretKey=${JWT_SECRET_KEY}
gemini.api-key=${GEMINI_API_KEY}
spring.datasource.password=${DB_PASSWORD}
```

> 💡 **Khuyến nghị**: Không commit bất kỳ giá trị mặc định nào lên repository. Thiết lập biến môi trường khi khởi động:
>
> ```bash
> ./mvnw spring-boot:run -DJWT_SECRET_KEY="<jwt_key>" -DGEMINI_API_KEY="<gemini_key>" -DDB_PASSWORD="<db_password>"
> ```

Tạo khóa bảo mật mạnh cho JWT:

```bash
openssl rand -base64 64
```

### Các Vai Trò Mặc Định

Hệ thống có sẵn 3 vai trò:
- `ROLE_ADMIN` - Quản trị viên (toàn quyền)
- `ROLE_MANAGER` - Quản lý (quyền hạn trung bình)
- `ROLE_STAFF` - Nhân viên (quyền cơ bản)

## 📚 Tài Liệu API

Sau khi khởi động ứng dụng, truy cập:

- **Giao Diện Swagger**: http://localhost:8088/swagger-ui.html
- **Tài Liệu API Chi Tiết**: http://localhost:8088/api-docs

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
  "phone": "0901234567",
  "roleIds": [2]
}
```

- Nếu `roleIds` không được cung cấp, hệ thống sẽ tự động gán `ROLE_STAFF`.
- Chỉ người có quyền `ADMIN` hoặc `MANAGER` mới có thể gán vai trò khác.

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

> 📌 **Lưu ý**: Nếu thông tin đăng nhập không chính xác, API sẽ trả về mã lỗi **401 Unauthorized** cùng thông báo "Invalid username or password". Điều này giúp client phân biệt rõ giữa tài khoản không tồn tại và các lỗi tài nguyên (404) khác.

### 3. Sử dụng Token

Thêm header vào mọi request cần authentication:

```http
Authorization: Bearer {your_token}
```

### 4. Quy trình thanh toán (Payment Flow)

- `OrderService` hiện chỉ điều phối và cập nhật trạng thái bàn. Toàn bộ nghiệp vụ thanh toán (kiểm tra phương thức, trừ kho, cộng điểm khách hàng) đã được tách sang `PaymentService` độc lập để dễ bảo trì và mở rộng.
- Khi thanh toán thất bại do vi phạm ràng buộc dữ liệu (ví dụ trùng khóa, vi phạm khóa ngoại), `GlobalExceptionHandler` sẽ trả về thông báo chi tiết hơn dựa trên mã lỗi SQL của database, giúp frontend phản hồi chính xác cho người dùng.
- Các đơn vị test (`PaymentServiceTest`, `OrderServiceTest`) đã được cập nhật để bao phủ cả luồng thành công và thất bại, đảm bảo hành vi mới hoạt động ổn định.

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
GET /api/v1/reports/doanh-thu?tuNgay=2025-01-01&denNgay=2025-12-31
Authorization: Bearer {token}
```

## Cấu Trúc Dự Án

```
src/main/java/com/giapho/coffee_shop_backend/
├── common/              # Hằng số, kiểu liệt kê
├── config/              # Cấu hình Spring
├── controller/          # Điều khiển API
├── domain/
│   ├── entity/         # Thực thể cơ sở dữ liệu
│   └── repository/     # Kho dữ liệu
├── dto/                # Đối tượng truyền dữ liệu
├── exception/          # Xử lý ngoại lệ
├── mapper/             # Ánh xạ đối tượng
├── security/           # Bảo mật
└── service/            # Nghiệp vụ chính
```

## Kiểm Thử

```bash
# Chạy tất cả các bài test
./mvnw test

# Chạy test với báo cáo độ phủ code
./mvnw test jacoco:report

# Chạy kiểm thử tích hợp
./mvnw verify
```

## Cấu Hình Nâng Cao

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
- **Email**: huynhgiapho1234@gmail.com
- **GitHub**: [@giapho](https://github.com/giaph4)

## 🎯 Roadmap


- [ ] Real-time notifications với WebSocket
- [ ] Multi-language support
- [ ] Mobile app integration
- [ ] Advanced analytics dashboard
- [ ] Integration với payment gateways

---

**Happy Coding! ☕️**