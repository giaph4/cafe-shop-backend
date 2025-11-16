# Hướng Dẫn Sử Dụng Bộ Tài Liệu

## Mục lục
- [1. Giới thiệu](#1-giới-thiệu)
- [2. Cấu trúc thư mục](#2-cấu-trúc-thư-mục)
- [3. Cách tra cứu nhanh](#3-cách-tra-cứu-nhanh)
- [4. Quy ước tài liệu](#4-quy-ước-tài-liệu)
- [5. Quy trình cập nhật tài liệu](#5-quy-trình-cập-nhật-tài-liệu)
- [6. Liên hệ & hỗ trợ](#6-liên-hệ--hỗ-trợ)

## 1. Giới thiệu
Bộ tài liệu mô tả toàn bộ kiến trúc, thiết kế, triển khai, vận hành và kiểm thử của hệ thống backend quản lý quán cà phê. Mỗi hạng mục được trình bày trong một tệp Markdown độc lập nhằm thuận tiện tra cứu và bảo trì.

## 2. Cấu trúc thư mục
| Tệp/Thư mục | Mục đích |
|-------------|----------|
| `danh-sach-tai-lieu.md` | Danh sách tổng hợp và ánh xạ hạng mục |
| `docs-readme.md` | Hướng dẫn sử dụng bộ tài liệu |
| `assumptions.md` | Danh sách giả định hệ thống |
| `document-log.md` | Nhật ký cập nhật tài liệu |
| Các tệp `.md` còn lại | Nội dung chi tiết theo từng chủ đề |

## 3. Cách tra cứu nhanh
1. Đọc `danh-sach-tai-lieu.md` để nắm danh mục và trạng thái hoàn thiện.
2. Mở tệp tương ứng với hạng mục quan tâm (mục lục ở đầu mỗi tệp hỗ trợ điều hướng).
3. Sử dụng tính năng tìm kiếm toàn cục (Ctrl+Shift+F) cho từ khóa cụ thể.
4. Tham chiếu `document-log.md` để biết lịch sử cập nhật gần nhất.

## 4. Quy ước tài liệu
- Ngôn ngữ: Tiếng Việt, thuật ngữ kỹ thuật giữ nguyên tiếng Anh khi cần.
- Định dạng: Markdown, tiêu đề đa cấp, bảng, danh sách, block code.
- Sơ đồ: Mô tả bằng PlantUML hoặc JSON/YAML.
- Ví dụ minh họa: sử dụng code block với ngôn ngữ (`java`, `sql`, `json`, `yaml`).
- Cuối mỗi tệp ghi rõ **Mức độ hoàn thiện** và liệt kê hạng mục còn thiếu (nếu có).

## 5. Quy trình cập nhật tài liệu
1. Ghi lại giả định mới trong `assumptions.md` trước khi soạn nội dung.
2. Cập nhật tệp liên quan, đảm bảo bổ sung mục lục và các mục bắt buộc.
3. Ghi lại thay đổi vào `document-log.md`:
   - Ngày/giờ cập nhật
   - Tệp liên quan
   - Mô tả ngắn gọn nội dung bổ sung/sửa đổi
4. Kiểm tra lại checklist yêu cầu. Nếu đầy đủ, đánh dấu **Mức độ hoàn thiện: 100%**.

## 6. Liên hệ & hỗ trợ
- **Team phụ trách**: Backend Platform
- **Kênh trao đổi**: Slack `#coffee-backend-docs`
- **Email**: backend-docs@company.example
- **Lịch cập nhật định kỳ**: Tuần đầu tiên mỗi tháng hoặc khi có thay đổi lớn.

---
**Mức độ hoàn thiện:** 100%
**Hạng mục còn thiếu:** Không
