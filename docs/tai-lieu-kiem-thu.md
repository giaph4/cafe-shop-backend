# Tài Liệu Kiểm Thử

## Mục tiêu kiểm thử
- Đảm bảo các chức năng cốt lõi hoạt động đúng theo yêu cầu nghiệp vụ.
- Đảm bảo tính ổn định và khả năng chịu tải của hệ thống.
- Xác minh các yêu cầu bảo mật và quyền truy cập.
- Kiểm thử hồi quy mỗi khi triển khai phiên bản mới.

## Chiến lược kiểm thử
| Loại kiểm thử | Phạm vi | Công cụ |
|---------------|---------|---------|
| Unit Test | Service, Utils, Mapper | JUnit 5, Mockito |
| Integration Test | Controller, Repository | Spring Boot Test, H2 |
| API Test | Endpoint REST | Postman/Newman, RestAssured |
| Performance Test | Nghiệp vụ trọng yếu (login, tạo đơn) | JMeter, k6 |
| Security Test | XSS, CSRF, JWT | OWASP ZAP |
| Regression Test | Sau mỗi release | Bộ test case tổng hợp |
| UAT | Quy trình nghiệp vụ chính | Đội vận hành, quản lý |

## Quy trình kiểm thử
1. **Lập kế hoạch**: xác định phạm vi, lịch trình, nguồn lực.
2. **Thiết kế test case**: dựa trên yêu cầu, use case chi tiết.
3. **Chuẩn bị dữ liệu**: seed dữ liệu mẫu (user, product, voucher).
4. **Thực thi**: chạy unit/integration test tự động, sau đó manual test.
5. **Ghi nhận kết quả**: log test case pass/fail, báo cáo bug.
6. **Xác nhận fix**: retest sau khi developer sửa.
7. **Đánh giá & tổng kết**: tạo báo cáo cuối chu kỳ.

## Môi trường kiểm thử
- **Dev**: dùng H2, dữ liệu đơn giản, hỗ trợ debugging.
- **Staging**: sao chép cấu hình gần prod, dữ liệu thực tế ẩn danh.
- **Test Automation**: pipeline CI chạy `./mvnw test` và API test.

## Quản lý lỗi (Bug Tracking)
- Sử dụng Jira/Linear để ghi nhận.
- Phân loại mức độ: Critical, High, Medium, Low.
- Thông tin tối thiểu khi tạo bug: mô tả, bước tái hiện, expected/actual, screenshot/log.
- SLA xử lý: Critical < 4h, High < 24h, Medium < 3 ngày, Low < 1 tuần.

## Báo cáo kiểm thử
- Cấu trúc:
  - Tóm tắt phạm vi, thời gian.
  - Tỷ lệ pass/fail.
  - Danh sách lỗi chưa xử lý.
  - Khuyến nghị go/no-go.

## Kiểm thử bảo mật
- Kiểm tra JWT (hết hạn, giả mạo, missing claim).
- Kiểm thử role-based access: user thường không truy cập được endpoint admin.
- Kiểm tra rate-limit login, brute force.
- Thử nghiệm XSS, SQL Injection ở các endpoint nhận input.

## Đảm bảo chất lượng tự động (CI/CD)
- Pull request phải chạy `./mvnw verify`.
- Chạy static code analysis (SpotBugs, Sonar).
- Đối với release major, chạy performance test tự động (k6 script).

## Kế hoạch đào tạo QA
- Đào tạo về domain quán cà phê: quy trình bán hàng, quản lý kho.
- Hướng dẫn sử dụng tài liệu API (Swagger), hiểu schema dữ liệu.
- Thường xuyên cập nhật tài liệu test case.
