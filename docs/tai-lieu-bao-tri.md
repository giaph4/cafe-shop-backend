# Tài Liệu Bảo Trì

## 1. Mục tiêu bảo trì
- Đảm bảo hệ thống hoạt động ổn định lâu dài.
- Cập nhật chức năng, sửa lỗi nhanh chóng và an toàn.
- Duy trì chất lượng mã nguồn, tài liệu và quy trình.

## 2. Quy trình quản lý thay đổi (Change Management)
1. Ghi nhận yêu cầu thay đổi qua hệ thống ticket (Jira, Trello).
2. Phân loại: bug, enhancement, feature, security patch.
3. Phân tích ảnh hưởng (impact analysis) và ước lượng effort.
4. Lập kế hoạch, phân công developer.
5. Triển khai trên branch riêng, tuân thủ quy tắc git-flow (`feature/*`, `bugfix/*`).
6. Code review bắt buộc (≥1 reviewer), kiểm tra CI.
7. QA kiểm thử trên môi trường staging.
8. Deploy production theo lịch đã duyệt.
9. Cập nhật tài liệu liên quan, thông báo stakeholder.

## 3. Chuẩn kiểm soát chất lượng mã nguồn
- Sử dụng `./mvnw clean verify` cho unit & integration test.
- Áp dụng SonarQube/CodeQL (khuyến nghị) để phân tích tĩnh.
- Kiểm soát độ bao phủ kiểm thử (coverage) ≥ 70% cho module cốt lõi.
- Review code theo checklist: chức năng, security, hiệu năng, readability.

## 4. Bảo trì cơ sở dữ liệu
- Tối ưu index định kỳ (ANALYZE TABLE).
- Cleanup dữ liệu cũ (login_history, audit_log) theo retention.
- Kiểm tra khóa ngoại, constraint khi thực hiện migration.
- Script migration (Flyway) phải reversible hoặc có kế hoạch rollback.

## 5. Nâng cấp phụ thuộc
- Theo dõi CVE cho Spring Boot, Spring Security, JJWT, MySQL driver.
- Lập lịch nâng cấp hàng quý, ưu tiên security patch.
- Thử nghiệm nâng cấp trên staging trước khi đưa lên production.

## 6. Quy trình khắc phục sự cố (Incident Response)
1. Nhận cảnh báo (monitoring, ticket).
2. Phân loại mức độ nghiêm trọng (critical/high/medium/low).
3. Tập hợp đội ứng phó, thông báo stakeholder.
4. Thu thập log, trace, metric.
5. Xác định nguyên nhân gốc (Root Cause Analysis).
6. Đưa ra bản vá tạm thời (workaround) nếu cần.
7. Triển khai fix, kiểm chứng.
8. Ghi nhận Post-Incident Report.

## 7. Quản lý cấu hình & bí mật
- Sử dụng Vault/Secret Manager lưu trữ JWT key, DB password.
- Không commit bí mật vào repo.
- Xoay vòng (rotate) key định kỳ 6 tháng hoặc khi nghi ngờ lộ lọt.

## 8. Tài liệu & đào tạo
- Cập nhật tài liệu thiết kế, API, quy trình sau mỗi thay đổi lớn.
- Tổ chức đào tạo dev/QA định kỳ (quý) về module mới, best practices.

## 9. Lịch bảo trì dự kiến
| Chu kỳ | Hạng mục |
|--------|----------|
| Hàng tuần | Kiểm tra log lỗi, cập nhật backlog |
| Hàng tháng | Kiểm tra index DB, cập nhật tài liệu |
| Hàng quý | Review bảo mật, nâng cấp dependency |
| Hàng năm | Đánh giá kiến trúc tổng thể, lập kế hoạch refactor |

## 10. Công cụ hỗ trợ
- **Version Control**: Git + GitHub/GitLab.
- **CI/CD**: GitHub Actions, Jenkins, GitLab CI.
- **Issue Tracking**: Jira, Linear.
- **Documentation**: Docs repo (Markdown), Confluence.
- **Monitoring**: Prometheus, Grafana, ELK, Sentry.
