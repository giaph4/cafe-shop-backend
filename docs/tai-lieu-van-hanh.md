# Tài Liệu Vận Hành

## 1. Giám sát hệ thống
### 1.1 Health Check
- Endpoint: `GET /actuator/health`
- Trả về trạng thái `UP` nếu hệ thống hoạt động bình thường.
- Tích hợp với load balancer để tự động loại bỏ node lỗi.

### 1.2 Logging
- Log dạng JSON theo chuẩn `timestamp`, `level`, `logger`, `message`, `traceId`.
- Lưu trữ log trong ELK stack hoặc dịch vụ cloud (CloudWatch, Stackdriver).
- Giữ log ít nhất 30 ngày để phục vụ audit.

### 1.3 Metrics
- Expose `/actuator/metrics` (bật profile phù hợp).
- Tích hợp Prometheus/Grafana để giám sát: request count, response time, DB connection pool.

## 2. Quy trình vận hành hàng ngày
| Thời gian | Công việc | Người phụ trách |
|-----------|-----------|-----------------|
| Mở ca | Kiểm tra trạng thái dịch vụ, DB, cache | DevOps |
| Trong ca | Giám sát dashboard, queue warning | DevOps + Quản lý |
| Cuối ca | Xuất báo cáo doanh thu, kho | Quản lý |
| Hàng ngày | Kiểm tra backup thành công | DBA |

## 3. Xử lý sự cố thường gặp
### 3.1 Dịch vụ không phản hồi
1. Kiểm tra `/actuator/health`.
2. Xem log `docker logs -f app`.
3. Nếu lỗi OOM: tăng RAM hoặc tối ưu JVM (`-Xms`, `-Xmx`).
4. Khởi động lại container, thông báo cho đội phát triển.

### 3.2 DB kết nối lỗi
1. Kiểm tra trạng thái MySQL (`systemctl status mysql`).
2. Xác minh thông số kết nối trong biến môi trường.
3. Kiểm tra connection pool (HikariCP) trong log.
4. Nếu quá tải, nâng cấp instance hoặc tối ưu query.

### 3.3 Tốc độ phản hồi chậm
1. Kiểm tra metrics: CPU, RAM, network.
2. Kiểm tra slow query log của MySQL.
3. Bật cache cho dữ liệu tĩnh (category, menu).
4. Cân nhắc scale-out thêm instance.

## 4. Bảo trì định kỳ
| Chu kỳ | Công việc |
|--------|-----------|
| Hàng ngày | Kiểm tra backup, log lỗi |
| Hàng tuần | Dọn log cũ, kiểm tra dung lượng ổ đĩa |
| Hàng tháng | Kiểm tra index DB, tối ưu query, cập nhật tài liệu |
| Hàng quý | Kiểm thử DR (Disaster Recovery), đánh giá bảo mật |

## 5. Quyền truy cập vận hành
- DevOps có quyền SSH vào server, quản lý Docker.
- DBA quản lý MySQL, thực hiện backup/restore.
- Quản lý cửa hàng chỉ truy cập dashboard, không có quyền server.
- Mọi phiên SSH phải dùng khóa công khai và được audit.

## 6. Lịch backup
| Loại | Tần suất | Công cụ |
|------|----------|---------|
| Backup DB full | Hàng ngày | mysqldump hoặc snapshot RDS |
| Backup incremental | Hàng giờ | Binary log |
| Backup file uploads | Hàng ngày | rsync/Cloud storage sync |
| Lưu trữ | 30 ngày | S3, Google Cloud Storage |

## 7. Kiểm soát cấu hình
- Sử dụng GitOps hoặc lưu cấu hình trong repo riêng.
- Thay đổi cấu hình phải thông qua pull request và review.
- Ghi nhận thay đổi trong audit log.

## 8. SLA & hỗ trợ
- SLA nội bộ: phản hồi sự cố trong 30 phút, khắc phục trong 4 giờ.
- Đội vận hành trực ca 24/7: sử dụng hệ thống ticket (Jira, Zendesk).
- Xuất báo cáo uptime hàng tháng cho ban điều hành.
