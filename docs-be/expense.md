# Chức năng: Quản lý chi phí (Expense)

## Vai trò trong hệ thống
- Ghi nhận các khoản chi hoạt động (nhập hàng, marketing, chi phí vận hành, ...).
- Cho phép lọc theo khoảng ngày, cập nhật, xóa khoản chi.
- Gắn với người tạo (user) để phục vụ audit và báo cáo.

## Luồng xử lý backend
1. **Danh sách chi phí** (`GET /api/v1/expenses`): hỗ trợ lọc theo `startDate`, `endDate` (ISO DATE) và phân trang, sort mặc định `expenseDate DESC` @src/main/java/com/giapho/coffee_shop_backend/controller/ExpenseController.java#33-43 @src/main/java/com/giapho/coffee_shop_backend/service/ExpenseService.java#31-40.
2. **Chi tiết chi phí** (`GET /api/v1/expenses/{id}`): lấy theo ID, trả `ExpenseDTO`, nếu không có -> 404 @src/main/java/com/giapho/coffee_shop_backend/controller/ExpenseController.java#46-50 @src/main/java/com/giapho/coffee_shop_backend/service/ExpenseService.java#46-51.
3. **Tạo khoản chi** (`POST`): service lấy user hiện tại từ JWT, map DTO -> entity, gán user, lưu @src/main/java/com/giapho/coffee_shop_backend/controller/ExpenseController.java#26-31 @src/main/java/com/giapho/coffee_shop_backend/service/ExpenseService.java#56-68.
4. **Cập nhật** (`PUT /{id}`): kiểm tra tồn tại rồi cập nhật thông tin, mapper bỏ qua trường user/timestamp @src/main/java/com/giapho/coffee_shop_backend/controller/ExpenseController.java#53-60 @src/main/java/com/giapho/coffee_shop_backend/service/ExpenseService.java#73-88.
5. **Xóa** (`DELETE /{id}`): kiểm tra tồn tại và xóa @src/main/java/com/giapho/coffee_shop_backend/controller/ExpenseController.java#63-67 @src/main/java/com/giapho/coffee_shop_backend/service/ExpenseService.java#92-100.

## Thành phần liên quan
- **Controller**: `ExpenseController` @src/main/java/com/giapho/coffee_shop_backend/controller/ExpenseController.java#1-69
- **Service**: `ExpenseService` @src/main/java/com/giapho/coffee_shop_backend/service/ExpenseService.java#1-102
- **Repository**: `ExpenseRepository`, `UserRepository`
- **DTO**: `ExpenseDTO` @src/main/java/com/giapho/coffee_shop_backend/dto/ExpenseDTO.java#1-37
- **Entity**: `Expense`, `User`
- **Mapper**: `ExpenseMapper`
- **Security**: `@PreAuthorize` — đọc/ghi yêu cầu `MANAGER` hoặc `ADMIN`, xóa yêu cầu `ADMIN`.

## Danh sách API
| Method | URL | Mô tả | Role |
| --- | --- | --- | --- |
| POST | `/api/v1/expenses` | Tạo khoản chi mới | `MANAGER`,`ADMIN` |
| GET | `/api/v1/expenses` | Danh sách chi phí (lọc ngày, phân trang) | `MANAGER`,`ADMIN` |
| GET | `/api/v1/expenses/{id}` | Chi tiết khoản chi | `MANAGER`,`ADMIN` |
| PUT | `/api/v1/expenses/{id}` | Cập nhật khoản chi | `MANAGER`,`ADMIN` |
| DELETE | `/api/v1/expenses/{id}` | Xóa khoản chi | `ADMIN` |

## Chi tiết API

### POST `/api/v1/expenses`
- **Request (`ExpenseDTO`)**:
  ```json
  {
    "expenseDate": "2025-11-15",
    "category": "PURCHASE",
    "amount": 2500000,
    "description": "Nhập nguyên liệu tuần 46"
  }
  ```
- **Logic**: lấy user hiện tại => `expense.setUser(currentUser)`; lưu và trả DTO.
- **Response 201**: `ExpenseDTO` chứa `id`, `createdAt`, `userId` (tùy mapper).
- **Lỗi 400**: DTO invalid (`MethodArgumentNotValidException`).
- **Lỗi 404**: user hiện tại không tìm thấy (hiếm).

### GET `/api/v1/expenses`
- **Query**: `startDate`, `endDate` (ISO DATE), `page`, `size`, `sort`.
- **Response 200**:
  ```json
  {
    "content": [
      {
        "id": 55,
        "expenseDate": "2025-11-15",
        "category": "PURCHASE",
        "amount": 2500000,
        "description": "Nhập nguyên liệu tuần 46",
        "userId": 10,
        "createdAt": "2025-11-15T09:00:00"
      }
    ],
    "pageable": { "pageNumber": 0, "pageSize": 10 },
    "totalElements": 32,
    "totalPages": 4
  }
  ```

### PUT `/api/v1/expenses/{id}`
- **Logic**: mapper cập nhật (không đổi user); có thể bổ sung check quyền (commented code).
- **Response 200**: DTO mới.
- **Lỗi 404**: không tìm thấy khoản chi.

### DELETE `/api/v1/expenses/{id}`
- **Response 204**: không nội dung.
- **Lỗi 404**: khoản chi không tồn tại.

## Điều kiện nghiệp vụ & validation
- `ExpenseDTO` yêu cầu `expenseDate`, `category`, `amount` > 0 (xem annotation trong DTO).
- Người tạo gắn từ JWT, không gửi trực tiếp.
- Bộ lọc ngày: nếu thiếu start/end -> lấy toàn bộ.

## Luồng lỗi & thông điệp
| Exception | HTTP | Message |
| --- | --- | --- |
| `MethodArgumentNotValidException` | 400 | "Dữ liệu đầu vào không hợp lệ" |
| `EntityNotFoundException` | 404 | "Expense not found with id ...", "Current user not found" |

## Role/Permission
- POST/GET/PUT: `hasAnyRole('MANAGER','ADMIN')`
- DELETE: `hasRole('ADMIN')`

## Quan hệ với chức năng khác
- **Report**: dữ liệu chi phí dùng trong báo cáo lợi nhuận (`report.md`).
- **Purchase Order**: chi phí nhập hàng có thể ghi nhận từ PO (chưa tự động).
- **User**: gán thông tin người lập phiếu.

## Các tệp liên quan trong BE
- Controller: `ExpenseController.java`
- Service: `ExpenseService.java`
- DTO: `ExpenseDTO.java`
- Entity & Repository: `Expense.java`, `ExpenseRepository.java`
- Mapper: `ExpenseMapper.java`
- User integration: `UserRepository.java`
