# Phạm Vi

## Mục lục
- [1. Phạm vi nghiệp vụ](#1-phạm-vi-nghiệp-vụ)
- [2. Phạm vi dữ liệu](#2-phạm-vi-dữ-liệu)
- [3. Ranh giới hệ thống](#3-ranh-giới-hệ-thống)
- [4. Tương tác & tích hợp ngoài](#4-tương-tác--tích-hợp-ngoài)
- [5. Các hạng mục ngoài phạm vi](#5-các-hạng-mục-ngoài-phạm-vi)
- [6. Giả định & ràng buộc](#6-giả-định--ràng-buộc)

## 1. Phạm vi nghiệp vụ
Hệ thống bao phủ toàn bộ chu trình vận hành quán cà phê:
- Quản lý danh mục sản phẩm, công thức pha chế, nguyên liệu.
- Quản lý đơn hàng tại chỗ, mang đi, giao hàng, đặt trước; cập nhật trạng thái bàn.
- Áp dụng voucher, chương trình khuyến mãi, loyalty.
- Quản lý khách hàng, lịch sử mua sắm, phân nhóm.
- Quản lý kho: nhập xuất, kiểm kê, nhà cung cấp, đơn mua hàng.
- Quản lý nhân sự: lịch ca, chấm công, bảng lương, điều chỉnh hiệu suất.
- Báo cáo: doanh thu, chi phí, tồn kho, hiệu suất nhân viên, phân tích khách hàng.
- Ghi nhận audit log, login history, cảnh báo bảo mật.

## 2. Phạm vi dữ liệu
| Nhóm dữ liệu | Mô tả | Nguồn |
|--------------|------|-------|
| Bán hàng | Đơn hàng, chi tiết món, thanh toán, voucher áp dụng | POS, mobile |
| Sản phẩm & danh mục | Thông tin sản phẩm, công thức, giá, trạng thái | Quản lý cửa hàng |
| Kho & nguyên liệu | Tồn kho, nhập xuất, kiểm kê, nhà cung cấp | Bộ phận kho |
| Khách hàng | Thông tin cá nhân, loyalty, lịch sử tương tác | POS, CSKH |
| Nhân sự | Ca làm, chấm công, điều chỉnh, bảng lương | Nhân sự |
| Chi phí | Expense, hóa đơn, chứng từ | Kế toán |
| Hệ thống | Audit log, login history, cấu hình | Backend platform |

## 3. Ranh giới hệ thống
| Mô-đun nội bộ | Trách nhiệm | Ranh giới |
|---------------|-------------|-----------|
| Backend API | Xử lý nghiệp vụ, cung cấp REST API | Không bao gồm UI/Frontend |
| Database | Lưu trữ dữ liệu vận hành | Không cung cấp trực tiếp truy cập cho khách ngoài |
| File Service | Quản lý file (hình ảnh, hóa đơn) | Không xử lý CDN, nén ảnh nâng cao |
| Reporting | Tổng hợp báo cáo JSON/Excel | Không bao gồm BI visualization (Power BI) |
| Notification (tùy chọn) | Gửi email nội bộ | Không gồm SMS/Push (mở rộng sau) |

## 4. Tương tác & tích hợp ngoài
- **POS/Web/Mobile**: tiêu thụ API RESTful do backend cung cấp.
- **Hệ thống thanh toán**: tích hợp nội bộ (tiền mặt, thẻ qua POS). Cổng thanh toán online là hạng mục mở rộng.
- **Hệ thống kế toán/ERP**: xuất dữ liệu định kỳ dạng CSV/Excel, API mở rộng ở giai đoạn sau.
- **BI/Analytics**: cung cấp endpoint dữ liệu và file để nhập vào công cụ BI.
- **Third-party storage/CDN**: tích hợp qua API `/api/v1/files`.

## 5. Các hạng mục ngoài phạm vi
- Xây dựng giao diện POS hoặc mobile app (được phát triển riêng).
- Xử lý thanh toán trực tuyến (VNPay, Momo) – ghi nhận là mở rộng tương lai.
- Quản lý tài chính kế toán tổng thể (ERP) vượt ngoài nghiệp vụ quán.
- Hệ thống CRM chuyên sâu hoặc marketing automation đa kênh.

## 6. Giả định & ràng buộc
- Mỗi chi nhánh sử dụng cùng phiên bản backend, hỗ trợ multi-tenant.
- Tải đồng thời dự kiến 100–300 request/giây, có thể scale-out khi vượt ngưỡng.
- Dữ liệu khách hàng phải tuân thủ quy định bảo vệ dữ liệu cá nhân.
- Endpoint nội bộ yêu cầu xác thực JWT, kết hợp kiểm soát IP ở môi trường production.

---
**Mức độ hoàn thiện:** 100%
**Hạng mục còn thiếu:** Không
