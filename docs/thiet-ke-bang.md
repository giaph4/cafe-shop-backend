# Thiết Kế Bảng

## Mục lục
- [1. Nguyên tắc thiết kế](#1-nguyên-tắc-thiết-kế)
- [2. Định nghĩa bảng chi tiết](#2-định-nghĩa-bảng-chi-tiết)
- [3. Chỉ mục & tối ưu hóa](#3-chỉ-mục--tối-ưu-hóa)
- [4. Ràng buộc và chính sách dữ liệu](#4-ràng-buộc-và-chính-sách-dữ-liệu)
- [5. DDL tham chiếu](#5-ddl-tham-chiếu)

## 1. Nguyên tắc thiết kế
- Tên bảng dùng số nhiều (`orders`, `products`), định dạng `snake_case`.
- Khóa chính `BIGINT` tự tăng (`AUTO_INCREMENT`).
- Trường ngày giờ lưu `TIMESTAMP`/`DATETIME` ở UTC, cột `created_at`, `updated_at` chuẩn hóa.
- Trạng thái lưu bằng `VARCHAR` + constraint, mapping sang enum trong code.
- Dùng `DECIMAL(12,2)` cho giá trị tài chính, `INT` cho số lượng/điểm.
- Ràng buộc ngoại khóa với `ON DELETE SET NULL` hoặc `ON DELETE CASCADE` tùy nghiệp vụ.

## 2. Định nghĩa bảng chi tiết
### 2.1 Bảng người dùng (`users`)
| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | BIGINT | PK | Khóa chính |
| username | VARCHAR(50) | UNIQUE, NOT NULL | Tên đăng nhập |
| password | VARCHAR(255) | NOT NULL | Mật khẩu hash Bcrypt |
| full_name | VARCHAR(100) | NOT NULL | Họ tên |
| email | VARCHAR(100) | UNIQUE | Email |
| phone | VARCHAR(20) | UNIQUE | Điện thoại |
| status | VARCHAR(20) | NOT NULL | ACTIVE/INACTIVE/LOCKED |
| created_at | DATETIME | NOT NULL | Tự động `CURRENT_TIMESTAMP` |
| updated_at | DATETIME | | `ON UPDATE CURRENT_TIMESTAMP` |

### 2.2 Vai trò & liên kết (`roles`, `user_roles`)
| Bảng | Cột | Ghi chú |
|------|------|--------|
| roles | id (PK), name | `ROLE_ADMIN/ROLE_MANAGER/ROLE_STAFF` |
| user_roles | user_id (FK), role_id (FK) | Composite PK `(user_id, role_id)` |

### 2.3 Khách hàng (`customers`)
| Cột | Kiểu | Ràng buộc |
|------|------|-----------|
| id | BIGINT | PK |
| full_name | VARCHAR(100) | |
| email | VARCHAR(100) | UNIQUE |
| phone | VARCHAR(20) | UNIQUE |
| loyalty_points | INT | DEFAULT 0 |
| tier | VARCHAR(20) | Bronze/Silver/Gold |
| created_at | DATETIME | DEFAULT CURRENT_TIMESTAMP |

### 2.4 Sản phẩm & danh mục (`products`, `categories`)
| Bảng | Cột chính | Mô tả |
|------|-----------|-------|
| categories | id, name, description, status | Danh mục, `status` default `ACTIVE` |
| products | id, code, name, price, cost, description, image_url, is_available, category_id, created_at, updated_at | FK `category_id` -> `categories.id` |

### 2.5 Voucher (`vouchers`)
| Cột | Kiểu | Ghi chú |
|------|------|--------|
| id | BIGINT | PK |
| code | VARCHAR(50) | UNIQUE, NOT NULL |
| type | VARCHAR(20) | `PERCENT`, `FIXED`, `FREESHIP` |
| discount_value | DECIMAL(12,2) | Giá trị giảm |
| max_discount | DECIMAL(12,2) | Giới hạn trên (nếu PERCENT) |
| min_order_total | DECIMAL(12,2) | Đơn tối thiểu |
| usage_limit | INT | Tổng lượt |
| used_count | INT | Lượt đã dùng |
| valid_from/to | DATE | Khoảng hiệu lực |
| status | VARCHAR(20) | ACTIVE/INACTIVE/EXPIRED |

### 2.6 Đơn hàng (`orders`, `order_details`)
- `orders`: id, user_id, customer_id, table_id, type, status, sub_total, discount_amount, total_amount, voucher_id, voucher_code, payment_method, created_at, paid_at.
- `order_details`: id, order_id, product_id, quantity, unit_price, line_total, notes.
- Ràng buộc: `order_id` cascade delete details; `voucher_id` optional (SET NULL khi xóa voucher).

### 2.7 Bàn (`cafe_tables`)
| Cột | Kiểu | Ghi chú |
|------|------|--------|
| id | BIGINT | PK |
| code | VARCHAR(20) | UNIQUE |
| status | VARCHAR(20) | AVAILABLE/IN_USE/CLEANING/RESERVED |
| capacity | INT | Sức chứa |

### 2.8 Kho & nguyên liệu
- `ingredients`: id, name, unit, current_stock, reorder_point, cost.
- `product_ingredients`: (product_id, ingredient_id, quantity_required) PK kép.
- `purchase_orders`: id, supplier_id, status (CREATED/RECEIVED/CANCELLED), total_amount, ordered_at, received_at.
- `purchase_order_details`: id, purchase_order_id, ingredient_id, quantity, cost.

### 2.9 Nhân sự & bảng lương
- `shift_templates` (không liệt kê chi tiết ở trên): khung giờ chuẩn.
- `shift_assignments`: id, user_id, shift_template_id, assign_date, status.
- `attendance_records`: id, user_id, shift_assignment_id, check_in_at, check_out_at, notes.
- `payroll_cycle`: id, name, start_date, end_date, status (OPEN/CLOSED).
- `payroll_summary`: id, payroll_cycle_id, user_id, total_hours, base_salary, bonus, deductions, final_amount.

### 2.10 Chi phí & audit
- `expenses`: id, category, amount, description, incurred_at, created_by.
- `audit_logs`: id, actor, action, entity_name, entity_id, payload JSON, created_at.
- `login_history`: id, username, ip_address, user_agent, success, attempt_at.

## 3. Chỉ mục & tối ưu hóa
- **Unique**: `users(username)`, `users(email)`, `products(code)`, `vouchers(code)`, `customers(phone)`.
- **B-tree index**: `orders(created_at)`, `orders(status)`, `order_details(product_id)`, `attendance_records(user_id, check_in_at)`.
- **Partition gợi ý**: `login_history` partition theo tháng, `audit_logs` partition theo ngày.
- **View/Materialized view**: tạo view `v_order_summary` phục vụ báo cáo doanh thu.

## 4. Ràng buộc và chính sách dữ liệu
- FK `orders.customer_id` dùng `ON DELETE SET NULL` để không mất lịch sử đơn khi xóa khách.
- FK `order_details.order_id` dùng `ON DELETE CASCADE`.
- `purchase_orders` → `purchase_order_details` dùng cascade.
- Trigger gợi ý: cập nhật `vouchers.used_count` sau mỗi lần thanh toán thành công.
- Sử dụng `CHECK` constraint (MySQL 8+) cho `discount_value >= 0`, `usage_limit >= used_count`.

## 5. DDL tham chiếu
Ví dụ DDL rút gọn:
```sql
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  full_name VARCHAR(100) NOT NULL,
  email VARCHAR(100) UNIQUE,
  phone VARCHAR(20) UNIQUE,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE orders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  customer_id BIGINT,
  table_id BIGINT,
  type VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  sub_total DECIMAL(12,2) NOT NULL,
  discount_amount DECIMAL(12,2) DEFAULT 0,
  total_amount DECIMAL(12,2) NOT NULL,
  voucher_id BIGINT,
  voucher_code VARCHAR(50),
  payment_method VARCHAR(20),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  paid_at DATETIME,
  CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL,
  CONSTRAINT fk_orders_voucher FOREIGN KEY (voucher_id) REFERENCES vouchers(id) ON DELETE SET NULL
);
```
Các script đầy đủ được lưu trong thư mục `db/migration` (đề xuất sử dụng Flyway).

---
**Mức độ hoàn thiện:** 100%
**Hạng mục còn thiếu:** Không
