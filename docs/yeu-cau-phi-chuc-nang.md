# Yêu Cầu Phi Chức Năng

## Mục lục
- [1. Hiệu năng](#1-hiệu-năng)
- [2. Khả năng mở rộng](#2-khả-năng-mở-rộng)
- [3. Tính sẵn sàng & dự phòng](#3-tính-sẵn-sàng--dự-phòng)
- [4. Bảo mật](#4-bảo-mật)
- [5. Khả năng bảo trì & phát triển](#5-khả-năng-bảo-trì--phát-triển)
- [6. Khả năng quan sát](#6-khả-năng-quan-sát)
- [7. Khả năng sử dụng (Usability)](#7-khả-năng-sử-dụng-usability)
- [8. Tuân thủ & pháp lý](#8-tuân-thủ--pháp-lý)
- [9. Quốc tế hóa & bản địa hóa](#9-quốc-tế-hóa--bản-địa-hóa)
- [10. Ma trận mapping chất lượng](#10-ma-trận-mapping-chất-lượng)

## 1. Hiệu năng
- Thời gian phản hồi trung bình của API nghiệp vụ < 300 ms, p95 < 500 ms.
- Khả năng xử lý đồng thời tối thiểu 300 request/giây trong giờ cao điểm, độ trễ không vượt ngưỡng.
- Batch báo cáo doanh thu 30 ngày hoàn thành < 5 phút.
- Giao dịch thanh toán phải hoàn tất < 3 giây (bao gồm tính voucher và cập nhật kho).
- Sử dụng connection pool (Hikari) với tối thiểu 20 kết nối, tối đa 100.

## 2. Khả năng mở rộng
- Scale theo chiều ngang thông qua Kubernetes auto-scaling (HPA dựa trên CPU/RPS).
- Kiến trúc module hóa, có thể tách thành microservice khi QPS > 500.
- Redis cache cho dữ liệu ít thay đổi (danh mục, voucher active) với cache invalidation rõ ràng.
- Cho phép bật feature toggle khi triển khai chức năng mới.

## 3. Tính sẵn sàng & dự phòng
- Uptime ≥ 99% trong khung giờ 06:00–23:00.
- Có cơ chế backup DB hằng ngày, snapshot hàng tuần, retention 30 ngày.
- Database replication (Primary-Replica, multi-AZ) và failover tự động.
- Kiểm thử DR (Disaster Recovery) ít nhất 2 lần/năm.
- Hỗ trợ deploy rolling update, không downtime > 1 phút.

## 4. Bảo mật
- Toàn bộ kết nối HTTPS/TLS 1.2 trở lên.
- JWT ngắn hạn (15 phút), refresh token 7 ngày, lưu bằng HttpOnly cookie.
- BCrypt (cost ≥ 12) cho mật khẩu, ẩn thông tin nhạy cảm trong log.
- RBAC chi tiết, kiểm tra quyền ở cả controller (annotation) và service (business rule).
- Phát hiện brute force: khóa tài khoản sau N lần thất bại, ghi log bảo mật.
- Kiểm tra input tránh SQL Injection, XSS, CSRF (xem `bao-mat.md`).

## 5. Khả năng bảo trì & phát triển
- Code chuẩn Clean Architecture, layer rõ ràng.
- Unit test ≥ 70% cho service trọng yếu, integration test cho flow chính.
- Code style theo `code-review-checklist.md` và `quy-tac-dat-ten.md`.
- CI/CD bắt buộc chạy `./mvnw verify` và static analysis.
- Tài liệu cập nhật đồng bộ (design, API, change log).

## 6. Khả năng quan sát
- Log chuẩn JSON, có trường `traceId`, `spanId`, `userId`.
- Expose `/actuator` cho health, metrics, prometheus scrape.
- Tích hợp Prometheus/Grafana, cảnh báo khi CPU > 80% hoặc lỗi 5xx > 1%.
- Sử dụng ELK/Splunk để lưu trữ log ≥ 90 ngày.

## 7. Khả năng sử dụng (Usability)
- API trả lỗi chuẩn hóa `code`, `message`, `errors[]`.
- Chính sách idempotent cho PUT/PATCH/POST checkout (sử dụng idempotency key).
- Tài liệu API (Swagger) đầy đủ ví dụ request/response.
- Hỗ trợ phân trang, sort, filter cho danh sách lớn.

## 8. Tuân thủ & pháp lý
- Tuân thủ quy định bảo vệ dữ liệu cá nhân (ẩn thông tin nhạy cảm, yêu cầu consent khi cần).
- Audit log lưu tối thiểu 12 tháng, đảm bảo không chỉnh sửa được.
- Hỗ trợ export dữ liệu khách hàng theo yêu cầu (Data Subject Request).

## 9. Quốc tế hóa & bản địa hóa
- Lưu dữ liệu Unicode, hỗ trợ ký tự tiếng Việt, tiếng Anh.
- Các trường địa chỉ/điện thoại linh hoạt theo quốc gia (tối thiểu VN, có thể mở rộng).
- Thời gian lưu theo UTC, hiển thị theo profile người dùng.

## 10. Ma trận mapping chất lượng
| Chất lượng | Định nghĩa | Thước đo | Công cụ giám sát |
|------------|------------|----------|------------------|
| Performance | Đáp ứng nhanh | p95 < 500 ms | Prometheus, k6 |
| Availability | Dịch vụ luôn sẵn sàng | Uptime ≥ 99% | Grafana, UptimeRobot |
| Security | Bảo vệ dữ liệu | Không có CVE mức High chưa xử lý >7 ngày | Snyk, Dependabot |
| Maintainability | Dễ bảo trì | Code smell < 3% LOC | SonarQube |
| Observability | Dễ giám sát | Đủ log/metric/tracing | ELK, Jaeger |
| Compliance | Tuân thủ pháp lý | Audit log đầy đủ | ELK, Policy review |

---
**Mức độ hoàn thiện:** 100%
**Hạng mục còn thiếu:** Không
