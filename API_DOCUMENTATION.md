# Hướng dẫn sử dụng Swagger/OpenAPI

Tài liệu này mô tả cách truy cập và tùy chỉnh trang tài liệu API tự động được sinh bởi **springdoc-openapi** trong dự án `coffee-shop-backend`.

## 1. Tổng quan
- Thư viện sử dụng: `springdoc-openapi-starter-webmvc-ui` (khai báo trong `pom.xml`).
- Cấu hình mô tả API: `src/main/java/com/giapho/coffee_shop_backend/config/OpenApiConfig.java` với annotation `@OpenAPIDefinition`.
- Swagger UI mặc định lắng nghe tại endpoint: `http://localhost:8080/swagger-ui.html`.

## 2. Khởi động ứng dụng
```powershell
# Windows
.\mvnw.cmd spring-boot:run
```
```bash
# Linux / macOS
./mvnw spring-boot:run
```
Sau khi ứng dụng chạy, mở trình duyệt và truy cập `http://localhost:8080/swagger-ui.html` để xem tài liệu API.

## 3. API JSON Schema
Ngoài giao diện Swagger UI, có thể lấy tài liệu JSON tại endpoint:
```
GET http://localhost:8080/v3/api-docs
```
Endpoint này hữu ích khi tích hợp với các công cụ như Postman, Insomnia hoặc khi tạo client tự động.

## 4. Tùy chỉnh thông tin tài liệu
Chỉnh sửa các trường trong `OpenApiConfig`:
- `title`, `version`, `description`: mô tả tổng quan hệ thống.
- `contact`: thông tin liên hệ hỗ trợ.
- `license`: tên và URL giấy phép sử dụng API.

Ví dụ cập nhật:
```java
@OpenAPIDefinition(
    info = @Info(
        title = "Coffee Shop API",
        version = "1.1.0",
        description = "REST API quản lý quán cà phê",
        contact = @Contact(name = "Team Backend", email = "backend@example.com"),
        license = @License(name = "Internal", url = "https://intranet.example.com/license")
    )
)
```

## 5. Bảo mật và môi trường
- Với môi trường production, có thể bật xác thực (ví dụ OAuth2, JWT) cho các endpoint tài liệu bằng cách cấu hình Spring Security.
- Nếu cần đổi đường dẫn Swagger UI, thêm vào `application.yml`:
```yaml
springdoc:
  swagger-ui:
    path: /docs
```
Sau đó truy cập `http://localhost:8080/docs`.

## 6. Các bước kiểm tra nhanh
- Kiểm tra dependency trong `pom.xml` đã có `springdoc-openapi-starter-webmvc-ui`.
- Đảm bảo class `OpenApiConfig` nằm trong package được Spring Boot quét (`com.giapho.coffee_shop_backend`).
- Khởi động ứng dụng và xác minh truy cập `swagger-ui.html` thành công.

## 7. Khắc phục sự cố
- **404 Not Found**: Kiểm tra ứng dụng đã chạy chưa và đúng port (mặc định 8080).
- **Không hiển thị API**: Đảm bảo controller (`@RestController`) nằm trong phạm vi quét của Spring Boot và không bị chặn bởi security.
- **Tùy biến khác**: Tham khảo tài liệu chính thức: [https://springdoc.org](https://springdoc.org)

Thực hiện đúng hướng dẫn giúp đội ngũ phát triển và QA truy cập tài liệu API sống động, cập nhật theo code base hiện tại.
