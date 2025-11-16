# Hướng dẫn chạy Unit Test

Tài liệu này mô tả quy trình thiết lập và chạy bộ unit test cho tầng service của dự án `coffee-shop-backend`, bao gồm cách kiểm tra coverage với mục tiêu tối thiểu **60%** cho các nghiệp vụ tính tiền và quản lý tồn kho.

## 1. Yêu cầu hệ thống
- **JDK 21** (trùng với cấu hình trong `pom.xml`).
- **Maven Wrapper** đi kèm dự án (`mvnw.cmd` trên Windows, `./mvnw` trên Linux/macOS).
- Quyền truy cập internet nếu cần tải dependencies lần đầu.

## 2. Lệnh chạy test
Di chuyển tới thư mục gốc dự án (chứa file `pom.xml`) và chạy:

```powershell
# Windows
.\mvnw.cmd clean test
```

```bash
# Linux / macOS
./mvnw clean test
```

Các test quan trọng bao gồm:
- `OrderServiceTest`: Kiểm tra logic tính tiền, áp voucher, thanh toán, và trừ kho.
- `IngredientServiceTest`: Kiểm tra cập nhật tồn kho và CRUD nguyên liệu.

## 3. Sinh báo cáo coverage
Dự án sử dụng plugin **JaCoCo** (qua Maven) để sinh báo cáo coverage.

```powershell
# Windows
.\mvnw.cmd clean test jacoco:report
```

```bash
# Linux / macOS
./mvnw clean test jacoco:report
```

Sau khi chạy, mở báo cáo HTML tại:
`target/site/jacoco/index.html`

Kiểm tra cột **Instruction Coverage** của các class trong package `com.giapho.coffee_shop_backend.service` (đặc biệt `OrderService` và `IngredientService`) để xác nhận mức phủ tối thiểu 60%.

## 4. Cập nhật coverage mục tiêu
- Nếu coverage chưa đạt yêu cầu, xem báo cáo để xác định nhánh chưa được test.
- Bổ sung test case tương ứng, tập trung vào các nhánh logic về tính tiền, xử lý voucher, và kiểm soát tồn kho.

## 5. Khắc phục sự cố thường gặp
- **`mvnw` không chạy**: Kiểm tra quyền thực thi của file (`chmod +x mvnw` trên Linux/macOS) hoặc dùng PowerShell với `Set-ExecutionPolicy` phù hợp.
- **Thiếu JDK**: Đảm bảo biến môi trường `JAVA_HOME` trỏ về JDK 21.
- **Test thất bại do dữ liệu**: Kiểm tra mock/stub trong các test, bảo đảm repository/service được giả lập đầy đủ.

Tuân thủ các bước trên sẽ giúp đảm bảo test service layer luôn đạt coverage yêu cầu và phát hiện sớm lỗi logic.
