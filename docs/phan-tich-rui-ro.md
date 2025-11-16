# Phân Tích Rủi Ro

## Mục đích
Đánh giá các rủi ro chính có thể ảnh hưởng đến vận hành hệ thống quản lý quán cà phê và đề xuất biện pháp giảm thiểu.

## Danh sách rủi ro
| Mã | Rủi ro | Mô tả | Xác suất | Tác động | Mức độ | Biện pháp giảm thiểu |
|----|--------|-------|----------|----------|--------|-----------------------|
| R-01 | Mất dữ liệu | DB lỗi, backup thất bại | Trung bình | Nghiêm trọng | Cao | Thiết lập backup tự động, kiểm tra định kỳ, DR test |
| R-02 | Tấn công bảo mật | Brute force, lộ JWT | Cao | Nghiêm trọng | Cao | Rate limiting, 2FA, rotate key, log audit |
| R-03 | Downtime | Server quá tải, lỗi phần cứng | Trung bình | Cao | Cao | Triển khai HA, autoscale, giám sát chặt chẽ |
| R-04 | Sai lệch tồn kho | Nhân viên nhập sai, bug logic | Trung bình | Trung bình | Trung bình | Kiểm kê định kỳ, audit trail, cảnh báo khi lệch lớn |
| R-05 | Sai giá bán | Cập nhật giá sai | Thấp | Cao | Trung bình | Phê duyệt hai lớp, audit log, rollback nhanh |
| R-06 | Thất bại tích hợp | Lỗi kết nối POS/mobile | Thấp | Cao | Trung bình | Giám sát API, retry, tài liệu fallback |
| R-07 | Hiệu năng kém | Cổng POS chậm giờ cao điểm | Trung bình | Trung bình | Trung bình | Benchmark định kỳ, cache, tối ưu query |
| R-08 | Sai lệch bảng lương | Dữ liệu ca làm thiếu | Thấp | Trung bình | Thấp | Quy trình xác nhận ca, check-in/out bắt buộc |
| R-09 | Không tuân thủ pháp lý | Không lưu audit | Thấp | Cao | Trung bình | Chính sách audit bắt buộc, lưu trữ tối thiểu 12 tháng |
| R-10 | Mất nhân sự chủ chốt | DevOps nghỉ việc | Trung bình | Cao | Cao | Document hóa, kế hoạch kế thừa, cross-training |

## Kế hoạch giảm thiểu chi tiết
1. **R-01 Mất dữ liệu**
   - Thiết lập backup full hàng ngày, incremental hàng giờ.
   - Kiểm tra restore hàng quý.
   - Sử dụng multi-AZ database.

2. **R-02 Tấn công bảo mật**
   - Tích hợp WAF, rate limit login.
   - Bật thông báo đăng nhập bất thường.
   - Thực hiện penetration test định kỳ.

3. **R-03 Downtime**
   - Thiết kế kiến trúc HA (2 instance app, load balancer).
   - Giám sát CPU/RAM, scale out tự động.
   - Có quy trình failover DB.

4. **R-07 Hiệu năng kém**
   - Xây dựng performance baseline.
   - Áp dụng caching và load test trước sự kiện lớn.
   - Theo dõi query chậm, tối ưu index.

5. **R-10 Mất nhân sự chủ chốt**
   - Chính sách bàn giao tài liệu.
   - Pair programming, rotation.
   - Giữ bản mô tả hạ tầng chi tiết.

## Theo dõi rủi ro
- Tạo risk register trong hệ thống quản lý dự án.
- Rà soát hàng tháng, cập nhật trạng thái (Open, Mitigated, Closed).
- Khi rủi ro xảy ra, lưu lại RCA và điều chỉnh kế hoạch.
