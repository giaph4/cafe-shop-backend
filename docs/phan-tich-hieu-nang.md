# Phân Tích Hiệu Năng

## Mục tiêu
Đảm bảo hệ thống đáp ứng nhu cầu giao dịch cao trong giờ cao điểm, tối ưu chi phí hạ tầng và mang lại trải nghiệm mượt mà cho người dùng.

## KPI hiệu năng
| Chỉ số | Mục tiêu |
|--------|----------|
| Thời gian phản hồi trung bình API | < 300ms |
| Thời gian phản hồi p95 | < 500ms |
| Throughput | ≥ 300 request/giây |
| Tỷ lệ lỗi | < 0.5% |
| Sử dụng CPU | < 70% trung bình |
| Sử dụng bộ nhớ | < 75% dung lượng |

## Các nghiệp vụ trọng yếu cần tối ưu
1. **Đăng nhập**: tần suất cao đầu ca, cần cache public key, tối ưu hash.
2. **Tạo đơn hàng**: nhiều truy vấn database, tính toán voucher.
3. **Tra cứu menu**: nên cache ở CDN/Redis, preload dữ liệu.
4. **Dashboard**: tổng hợp dữ liệu lớn, khuyến nghị caching hoặc precompute.

## Phân tích bottleneck tiềm năng
- **Database**: truy vấn join nhiều bảng (orders, order_details, products). Giải pháp: index phù hợp, tách read replica.
- **Voucher validation**: logic phức tạp → nên cache voucher active, kiểm soát concurrency.
- **Payment process**: Ghi nhiều log, cập nhật nhiều bảng → cần transaction ngắn gọn.
- **Reporting**: Query tổng hợp → dùng view/materialized view cho báo cáo lớn.

## Chiến lược tối ưu
- **Caching**: Redis cho danh mục, voucher, cấu hình hệ thống.
- **Connection Pool**: HikariCP với cấu hình tối ưu (`maximumPoolSize` dựa trên CPU).
- **Asynchronous processing**: Gửi email, ghi audit log nặng chuyển sang queue.
- **Pagination & filter**: Limit 50 bản ghi mỗi trang, tránh load toàn bộ.
- **Batching**: Sử dụng batch insert/update cho import số lượng lớn.

## Kế hoạch kiểm thử hiệu năng
1. **Smoke test**: 50 RPS trong 5 phút.
2. **Load test**: 200 RPS trong 30 phút (khối lượng trung bình).
3. **Stress test**: tăng dần đến 500 RPS để tìm ngưỡng.
4. **Endurance test**: 100 RPS trong 8 giờ mô phỏng hoạt động dài.

## Công cụ đề xuất
- **k6**: scripting bằng JavaScript, tích hợp CI.
- **JMeter**: test phức tạp với file CSV.
- **Grafana**: theo dõi metric thời gian thực.
- **New Relic/AppDynamics**: APM để xác định bottleneck ở code.

## Theo dõi sau triển khai
- Thiết lập alert cho thời gian phản hồi > 500ms hoặc tỷ lệ lỗi >1%.
- Log tracer ID cho phép truy vết request chậm.
- So sánh KPI hàng tuần, điều chỉnh cấu hình nếu lệch ngưỡng.

## Kế hoạch cải thiện dài hạn
- Tách dịch vụ (orders, reporting) thành microservice khi lưu lượng cao.
- Sử dụng ElasticSearch hoặc clickhouse cho truy vấn báo cáo realtime.
- Cân nhắc thiết kế event-driven để giảm tải trực tiếp vào DB giao dịch.
