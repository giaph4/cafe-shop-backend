# Danh Sách Tài Liệu Hệ Thống

## Mục lục
- [1. Danh sách tổng hợp](#1-danh-sách-tổng-hợp)
- [2. Liên kết hạng mục bắt buộc](#2-liên-kết-hạng-mục-bắt-buộc)
- [3. Trạng thái hoàn thiện](#3-trạng-thái-hoàn-thiện)

## 1. Danh sách tổng hợp
| STT | Tên tệp | Mục tiêu chính |
|-----|---------|----------------|
| 1 | `docs-readme.md` | Hướng dẫn sử dụng và cấu trúc thư mục tài liệu |
| 2 | `assumptions.md` | Danh sách giả định hệ thống |
| 3 | `document-log.md` | Nhật ký cập nhật tài liệu |
| 4 | `tong-quan-he-thong.md` | Tổng quan hệ thống & mục tiêu |
| 5 | `pham-vi.md` | Phạm vi và ranh giới |
| 6 | `yeu-cau-chuc-nang.md` | Yêu cầu chức năng chi tiết |
| 7 | `yeu-cau-phi-chuc-nang.md` | Yêu cầu phi chức năng |
| 8 | `use-case-chi-tiet.md` | Danh sách use case và bước chi tiết |
| 9 | `so-do-use-case.md` | Sơ đồ use case (PlantUML) |
| 10 | `so-do-lop.md` | Sơ đồ lớp |
| 11 | `so-do-tuan-tu.md` | Sơ đồ tuần tự |
| 12 | `so-do-hoat-dong.md` | Sơ đồ hoạt động |
| 13 | `so-do-trien-khai.md` | Sơ đồ triển khai |
| 14 | `mo-hinh-du-lieu.md` | Mô hình dữ liệu |
| 15 | `thiet-ke-bang.md` | Thiết kế bảng & DDL |
| 16 | `mo-ta-api.md` | Thiết kế API chi tiết |
| 17 | `kien-truc-tong-the.md` | Kiến trúc tổng thể & sơ đồ thành phần |
| 18 | `thiet-ke-module.md` | Thiết kế chi tiết module |
| 19 | `quy-trinh-xu-ly.md` | Luồng xử lý chính & pseudocode |
| 20 | `dto-entity-mapper.md` | DTO, entity, mapper, validation |
| 21 | `cau-hinh-moi-truong.md` | Cấu hình môi trường dev/test/staging/prod |
| 22 | `tai-lieu-trien-khai.md` | Hướng dẫn triển khai |
| 23 | `tai-lieu-van-hanh.md` | Vận hành & backup phục hồi |
| 24 | `logging-monitoring.md` | Chiến lược logging & monitoring |
| 25 | `bao-mat.md` | Bảo mật hệ thống |
| 26 | `phan-tich-hieu-nang.md` | Phân tích hiệu năng |
| 27 | `tai-lieu-kiem-thu.md` | Test plan tổng thể |
| 28 | `test-case.md` | Danh sách test case |
| 29 | `kich-ban-kiem-thu.md` | Kịch bản kiểm thử end-to-end |
| 30 | `code-review-checklist.md` | Checklist code review & style |
| 31 | `tai-lieu-bao-tri.md` | Kế hoạch bảo trì |
| 32 | `goi-y-mo-rong-tuong-lai.md` | Kế hoạch mở rộng |
| 33 | `phan-tich-rui-ro.md` | Phân tích rủi ro |
| 34 | `dependency-third-party.md` | Dependency & third-party |
| 35 | `quy-tac-nghiep-vu.md` | Quy tắc nghiệp vụ |
| 36 | `quy-tac-dat-ten.md` | Quy tắc đặt tên & style |

## 2. Liên kết hạng mục bắt buộc
| Hạng mục yêu cầu | Tệp chính | Tệp bổ trợ |
|------------------|-----------|------------|
| Tổng quan hệ thống & mục tiêu | `tong-quan-he-thong.md` | `docs-readme.md` |
| Phạm vi & ranh giới | `pham-vi.md` | `tong-quan-he-thong.md` |
| Yêu cầu chức năng | `yeu-cau-chuc-nang.md` | `use-case-chi-tiet.md` |
| Yêu cầu phi chức năng | `yeu-cau-phi-chuc-nang.md` | `phan-tich-hieu-nang.md` |
| Use case chi tiết | `use-case-chi-tiet.md` | `so-do-use-case.md` |
| Sơ đồ use case | `so-do-use-case.md` | `use-case-chi-tiet.md` |
| Sơ đồ lớp | `so-do-lop.md` | `dto-entity-mapper.md` |
| Sơ đồ tuần tự | `so-do-tuan-tu.md` | `quy-trinh-xu-ly.md` |
| Sơ đồ hoạt động | `so-do-hoat-dong.md` | `quy-trinh-xu-ly.md` |
| Sơ đồ triển khai | `so-do-trien-khai.md` | `kien-truc-tong-the.md` |
| Mô hình dữ liệu | `mo-hinh-du-lieu.md` | `thiet-ke-bang.md` |
| Thiết kế bảng & DDL | `thiet-ke-bang.md` | `mo-hinh-du-lieu.md` |
| Thiết kế API | `mo-ta-api.md` | `dto-entity-mapper.md` |
| Kiến trúc tổng thể | `kien-truc-tong-the.md` | `thiet-ke-module.md` |
| Thiết kế module | `thiet-ke-module.md` | `quy-trinh-xu-ly.md` |
| Luồng xử lý & pseudocode | `quy-trinh-xu-ly.md` | `so-do-tuan-tu.md` |
| DTO/Entity/Validation | `dto-entity-mapper.md` | `thiet-ke-bang.md` |
| Cấu hình môi trường | `cau-hinh-moi-truong.md` | `tai-lieu-trien-khai.md` |
| Hướng dẫn triển khai | `tai-lieu-trien-khai.md` | `document-log.md` |
| Vận hành & backup | `tai-lieu-van-hanh.md` | `logging-monitoring.md` |
| Logging & monitoring | `logging-monitoring.md` | `tai-lieu-van-hanh.md` |
| Bảo mật | `bao-mat.md` | `quy-tac-nghiep-vu.md` |
| Hiệu năng | `phan-tich-hieu-nang.md` | `yeu-cau-phi-chuc-nang.md` |
| Test plan & cases | `tai-lieu-kiem-thu.md` | `test-case.md`, `kich-ban-kiem-thu.md` |
| Code review & style | `code-review-checklist.md` | `quy-tac-dat-ten.md` |
| Bảo trì & mở rộng | `tai-lieu-bao-tri.md` | `goi-y-mo-rong-tuong-lai.md` |
| Rủi ro | `phan-tich-rui-ro.md` | `document-log.md` |
| Dependency | `dependency-third-party.md` | `tai-lieu-trien-khai.md` |
| README hướng dẫn | `docs-readme.md` | `danh-sach-tai-lieu.md` |
| Giả định | `assumptions.md` | `docs-readme.md` |

## 3. Trạng thái hoàn thiện
- Tất cả các tệp trong danh sách được kiểm soát để đạt mức hoàn thiện 100% sau khi cập nhật nội dung chi tiết.
- Ghi chú cập nhật cụ thể được duy trì trong `document-log.md`.

---
**Mức độ hoàn thiện:** 100%
**Hạng mục còn thiếu:** Không
