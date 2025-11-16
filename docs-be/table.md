# Chức năng: Quản lý bàn (Cafe Table)

## Vai trò trong hệ thống
- Duy trì danh sách bàn, trạng thái (EMPTY/BUSY/RESERVED/CLEANING) phục vụ order.
- Cung cấp API cho nhân viên cập nhật trạng thái bàn theo thực tế.
- Ngăn xóa bàn khi đang/đã được sử dụng trong lịch sử đơn.

## Luồng xử lý backend
1. **Danh sách & chi tiết bàn** (`GET /api/v1/tables`, `/{id}`): `CafeTableService` đọc `CafeTableRepository`, map entity sang `CafeTableResponse` @src/main/java/com/giapho/coffee_shop_backend/controller/CafeTableController.java#27-44 @src/main/java/com/giapho/coffee_shop_backend/service/CafeTableService.java#27-39.
2. **Tạo và cập nhật thông tin bàn** (`POST`, `PUT`): service kiểm tra trùng tên, dùng mapper cập nhật entity @src/main/java/com/giapho/coffee_shop_backend/controller/CafeTableController.java#46-61 @src/main/java/com/giapho/coffee_shop_backend/service/CafeTableService.java#41-68.
3. **Cập nhật trạng thái bàn** (`PATCH /{id}/status`): parse string sang `TableStatus` enum, lưu @src/main/java/com/giapho/coffee_shop_backend/controller/CafeTableController.java#63-71 @src/main/java/com/giapho/coffee_shop_backend/service/CafeTableService.java#71-82.
4. **Xóa bàn** (`DELETE /{id}`): kiểm tra tồn tại, đếm số đơn hàng liên quan, chặn thao tác nếu đã có order @src/main/java/com/giapho/coffee_shop_backend/controller/CafeTableController.java#74-78 @src/main/java/com/giapho/coffee_shop_backend/service/CafeTableService.java#84-98.
5. **Tích hợp Order**: `OrderService` thay đổi trạng thái bàn khi tạo/hoàn tất/hủy đơn để giữ đồng bộ @src/main/java/com/giapho/coffee_shop_backend/service/OrderService.java#107-140,#224-302.

## Thành phần liên quan
- **Controller**: `CafeTableController` @src/main/java/com/giapho/coffee_shop_backend/controller/CafeTableController.java#1-80
- **Service**: `CafeTableService` @src/main/java/com/giapho/coffee_shop_backend/service/CafeTableService.java#1-114
- **Repository**: `CafeTableRepository`, `OrderRepository` (đếm số order theo bàn)
- **DTO**: `CafeTableRequest`, `CafeTableResponse`, `CafeTableStatusUpdateRequest` @src/main/java/com/giapho/coffee_shop_backend/dto/CafeTableRequest.java#1-26 @src/main/java/com/giapho/coffee_shop_backend/dto/CafeTableResponse.java#1-33 @src/main/java/com/giapho/coffee_shop_backend/dto/CafeTableStatusUpdateRequest.java#1-14
- **Entity**: `CafeTable`, Enum `TableStatus`
- **Mapper**: `CafeTableMapper`
- **Validation**: `CafeTableRequest` sử dụng `@NotBlank`, `@Positive` (xem file DTO); service kiểm tra trùng tên.
- **Security**: `@PreAuthorize` — đọc require `STAFF` trở lên, create/update/delete require `MANAGER`/`ADMIN`.

## Danh sách API
| Method | URL | Mô tả | Role |
| --- | --- | --- | --- |
| GET | `/api/v1/tables` | Lấy danh sách bàn | `STAFF`,`MANAGER`,`ADMIN` |
| GET | `/api/v1/tables/{id}` | Xem chi tiết bàn | `STAFF`,`MANAGER`,`ADMIN` |
| POST | `/api/v1/tables` | Tạo bàn mới | `MANAGER`,`ADMIN` |
| PUT | `/api/v1/tables/{id}` | Cập nhật thông tin bàn | `MANAGER`,`ADMIN` |
| PATCH | `/api/v1/tables/{id}/status` | Cập nhật trạng thái bàn | `STAFF`,`MANAGER`,`ADMIN` |
| DELETE | `/api/v1/tables/{id}` | Xóa bàn | `MANAGER`,`ADMIN` |

## Chi tiết API

### POST `/api/v1/tables`
- **Request (`CafeTableRequest`)**:
  ```json
  {
    "name": "B05",
    "capacity": 4,
    "location": "Tầng 1" }
  ```
- **Logic**: kiểm tra trùng tên (`existsByName`), map DTO -> entity, set status mặc định (thường `EMPTY`), lưu.
- **Response 201**: `CafeTableResponse` bao gồm `status`, `createdAt`.
- **Lỗi 400**: tên trùng (IllegalArgumentException), DTO invalid (MethodArgumentNotValidException).

### PATCH `/api/v1/tables/{id}/status`
- **Request body**: `{ "status": "BUSY" }` qua `CafeTableStatusUpdateRequest`.
- **Logic**: parse string sang enum, ném lỗi nếu không hợp lệ.
- **Response 200**: `CafeTableResponse` sau cập nhật.
- **Lỗi 400**: status rỗng/không hợp lệ.
- **Lỗi 404**: không thấy bàn.

### DELETE `/api/v1/tables/{id}`
- **Logic**:
  1. Lấy bàn theo ID.
  2. Đếm số order liên quan (`orderRepository.countByCafeTableId`).
  3. Nếu >0 -> ném `IllegalArgumentException` hướng dẫn xử lý trước.
  4. Nếu 0 -> `deleteById`.
- **Response 204**.

## Điều kiện nghiệp vụ & validation
- Tên bàn duy nhất, phân biệt chữ hoa thường theo logic repository (kiểm tra equals).
- Không cho xóa bàn đã có đơn hàng.
- Trạng thái bàn chỉ nhận giá trị trong enum `TableStatus`.
- Khi order tạo/hủy, `OrderService` tự cập nhật trạng thái bàn (không phải gọi API update status).

## Luồng lỗi & thông điệp
| Exception | HTTP | Message |
| --- | --- | --- |
| `EntityNotFoundException` | 404 | "Table not found with id ..." |
| `IllegalArgumentException` | 400 | "Table with name ... already exists", "Cannot delete table ... because it has associated orders", "Invalid status..." |
| `MethodArgumentNotValidException` | 400 | "Dữ liệu đầu vào không hợp lệ" |

## Role/Permission
- GET/Status: `hasAnyRole('STAFF','MANAGER','ADMIN')`
- POST/PUT/DELETE: `hasAnyRole('MANAGER','ADMIN')`

## Quan hệ với chức năng khác
- **Order**: Tạo/hủy/hoàn thành đơn sẽ gọi `OrderService.updateTableStatusOnOrderCreate/Completion` để chuyển trạng thái.
- **Report**: Thông tin trạng thái bàn có thể tích hợp vào dashboard quản lý vận hành.

## Các tệp liên quan trong BE
- Controller: `CafeTableController.java`
- Service: `CafeTableService.java`
- DTO: `CafeTableRequest.java`, `CafeTableResponse.java`, `CafeTableStatusUpdateRequest.java`
- Entity & Repository: `CafeTable.java`, `CafeTableRepository.java`
- Enum: `TableStatus.java`
- Mapper: `CafeTableMapper.java`
- Order integration: `OrderService.java`, `OrderRepository.java`
