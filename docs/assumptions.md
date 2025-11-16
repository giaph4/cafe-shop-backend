# Giả Định Hệ Thống

## Mục lục
- [1. Mục đích](#1-mục-đích)
- [2. Giả định tổng quát](#2-giả-định-tổng-quát)
- [3. Giả định kiến trúc & hạ tầng](#3-giả-định-kiến-trúc--hạ-tầng)
- [4. Giả định nghiệp vụ](#4-giả-định-nghiệp-vụ)
- [5. Giả định dữ liệu](#5-giả-định-dữ-liệu)
- [6. Giả định bảo mật & tuân thủ](#6-giả-định-bảo-mật--tuân-thủ)
- [7. Giả định triển khai & vận hành](#7-giả-định-triển-khai--vận-hành)

## 1. Mục đích
Tập hợp tất cả giả định được sử dụng trong quá trình thiết kế và soạn thảo tài liệu nhằm đảm bảo minh bạch, dễ theo dõi khi các giả định thay đổi.

## 2. Giả định tổng quát
- Hệ thống phục vụ chuỗi quán cà phê có tối đa 50 chi nhánh ban đầu, mở rộng dễ dàng tới 200 chi nhánh.
- Lưu lượng trung bình 200 đơn hàng/giờ/chi nhánh vào giờ cao điểm.
- Người dùng cuối là nhân viên quán (thu ngân, quản lý, kho, marketing) và bộ phận hỗ trợ doanh nghiệp.

## 3. Giả định kiến trúc & hạ tầng
- Ứng dụng triển khai trên hạ tầng cloud (AWS/Azure/GCP) với khả năng auto scaling.
- Sử dụng MySQL 8.0 làm cơ sở dữ liệu chính, có hỗ trợ read-replica.
- Redis được dùng cho cache và hàng đợi nhẹ (Pub/Sub) ở giai đoạn mở rộng.
- File tĩnh (hóa đơn, hình ảnh) lưu trữ trên S3 hoặc dịch vụ tương đương.
- Hệ thống CI/CD sẵn có (GitHub Actions) để build & deploy.

## 4. Giả định nghiệp vụ
- Quán cà phê bán đồ uống, bánh, đồ ăn nhẹ; công thức pha chế có định mức nguyên liệu cụ thể.
- Voucher có thể áp dụng cho toàn bộ hoặc một phần danh mục sản phẩm.
- Chương trình loyalty tính điểm dựa trên tổng giá trị thanh toán sau giảm giá.
- Mỗi nhân viên có thể đảm nhiệm nhiều vai trò (phục vụ, thu ngân) tùy ca.

## 5. Giả định dữ liệu
- Tên khách hàng, sản phẩm, danh mục cho phép Unicode.
- Định dạng tiền tệ theo VND, làm tròn tới đồng.
- Múi giờ chuẩn UTC+07 (Asia/Ho_Chi_Minh), lưu trữ trong DB theo UTC.
- Tất cả request/response sử dụng JSON UTF-8.

## 6. Giả định bảo mật & tuân thủ
- Áp dụng chuẩn bảo vệ dữ liệu cá nhân tương đương Nghị định 13/2023/ND-CP.
- Không lưu thông tin thẻ tín dụng; thanh toán không tiền mặt do bên thứ ba xử lý.
- JWT ký bằng thuật toán HS512 với khóa bí mật tối thiểu 64 ký tự.
- Tất cả endpoint quản trị phải chạy qua VPN hoặc Zero Trust Network Access.

## 7. Giả định triển khai & vận hành
- Quy mô đội vận hành gồm 2 DevOps, 1 DBA, 1 Security Engineer.
- Giờ hoạt động của quán 06:00–23:00, downtime kế hoạch chỉ thực hiện sau 23:00.
- Sao lưu dữ liệu tối thiểu 1 lần/ngày, lưu trữ 30 ngày.
- Log và metric tập trung vào ELK + Prometheus/Grafana.

---
**Mức độ hoàn thiện:** 100%
**Hạng mục còn thiếu:** Không
