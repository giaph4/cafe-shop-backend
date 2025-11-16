# Chức năng: Quản lý nguyên liệu & tồn kho

## Vai trò trong hệ thống
- Lưu trữ danh sách nguyên liệu, thông tin định lượng tồn kho phục vụ pha chế.
- Cho phép tìm kiếm theo tên, thêm/sửa/xóa nguyên liệu và điều chỉnh số lượng tồn kho.
- Hỗ trợ audit khi điều chỉnh tồn kho (ghi nhật ký thao tác).
- Cung cấp dữ liệu cho công thức sản phẩm và trừ kho khi thanh toán đơn hàng.

## Luồng xử lý backend
1. **Tra cứu & phân trang** (`GET /api/v1/ingredients`): nếu có `name` thì tìm kiếm, ngược lại lấy toàn bộ; kết quả trả về `Page<IngredientResponseDTO>` @src/main/java/com/giapho/coffee_shop_backend/controller/IngredientController.java#24-37 @src/main/java/com/giapho/coffee_shop_backend/service/IngredientService.java#27-36.
2. **Xem chi tiết** (`GET /api/v1/ingredients/{id}`): lấy theo ID, ném `EntityNotFoundException` nếu không tồn tại @src/main/java/com/giapho/coffee_shop_backend/controller/IngredientController.java#39-44 @src/main/java/com/giapho/coffee_shop_backend/service/IngredientService.java#38-43.
3. **Tạo nguyên liệu** (`POST`): kiểm tra trùng tên, map DTO -> entity và lưu @src/main/java/com/giapho/coffee_shop_backend/controller/IngredientController.java#46-51 @src/main/java/com/giapho/coffee_shop_backend/service/IngredientService.java#45-54.
4. **Cập nhật thông tin** (`PUT /{id}`): kiểm tra tồn tại, kiểm tra trùng tên khi đổi, update entity qua mapper @src/main/java/com/giapho/coffee_shop_backend/controller/IngredientController.java#53-60 @src/main/java/com/giapho/coffee_shop_backend/service/IngredientService.java#56-69.
5. **Xóa nguyên liệu** (`DELETE /{id}`): xác minh tồn tại rồi xóa @src/main/java/com/giapho/coffee_shop_backend/controller/IngredientController.java#63-67 @src/main/java/com/giapho/coffee_shop_backend/service/IngredientService.java#71-77.
6. **Điều chỉnh tồn kho** (`PATCH /api/v1/ingredients/adjust-inventory`): nhận `InventoryAdjustmentRequestDTO`, cập nhật số lượng, ghi audit log thành công/thất bại @src/main/java/com/giapho/coffee_shop_backend/controller/IngredientController.java#70-77 @src/main/java/com/giapho/coffee_shop_backend/service/IngredientService.java#79-128.
7. **Thanh toán đơn hàng**: `PaymentService.subtractInventoryForOrder` trừ tồn kho dựa trên công thức sản phẩm, gọi `IngredientRepository.findByIdForUpdate` đảm bảo concurrency @src/main/java/com/giapho/coffee_shop_backend/service/PaymentService.java#132-176.

## Thành phần liên quan
- **Controller**: `IngredientController` @src/main/java/com/giapho/coffee_shop_backend/controller/IngredientController.java#1-78
- **Service**: `IngredientService` @src/main/java/com/giapho/coffee_shop_backend/service/IngredientService.java#1-130
- **Repository**: `IngredientRepository`, `ProductIngredientRepository` (khi trừ kho), `AuditLogRepository` (qua `AuditLogService`)
- **DTO**: `IngredientRequestDTO`, `IngredientResponseDTO`, `InventoryAdjustmentRequestDTO` @src/main/java/com/giapho/coffee_shop_backend/dto/IngredientRequestDTO.java#1-31 @src/main/java/com/giapho/coffee_shop_backend/dto/IngredientResponseDTO.java#1-25 @src/main/java/com/giapho/coffee_shop_backend/dto/InventoryAdjustmentRequestDTO.java#1-26
- **Entity**: `Ingredient`
- **Mapper**: `IngredientMapper`
- **Audit**: `AuditLogService.recordAction` được gọi khi điều chỉnh tồn kho thất bại/thành công.
- **Security**: tất cả endpoint yêu cầu `MANAGER` hoặc `ADMIN`; riêng xóa yêu cầu `ADMIN`.

## Danh sách API
| Method | URL | Mô tả | Role |
| --- | --- | --- | --- |
| GET | `/api/v1/ingredients` | Danh sách/ tìm kiếm nguyên liệu (phân trang) | `MANAGER`,`ADMIN` |
| GET | `/api/v1/ingredients/{id}` | Xem chi tiết | `MANAGER`,`ADMIN` |
| POST | `/api/v1/ingredients` | Tạo nguyên liệu mới | `MANAGER`,`ADMIN` |
| PUT | `/api/v1/ingredients/{id}` | Cập nhật thông tin | `MANAGER`,`ADMIN` |
| DELETE | `/api/v1/ingredients/{id}` | Xóa nguyên liệu | `ADMIN` |
| PATCH | `/api/v1/ingredients/adjust-inventory` | Điều chỉnh tồn kho | `MANAGER`,`ADMIN` |

## Chi tiết API chính

### POST `/api/v1/ingredients`
- **Request (`IngredientRequestDTO`)**:
  ```json
  {
    "name": "Syrup Caramel",
    "unit": "ml",
    "quantityOnHand": 5000,
    "reorderLevel": 1000,
    "supplierName": "Supplier A"
  }
  ```
- **Logic**: kiểm tra `existsByName`, lưu entity mới.
- **Response 200**: `IngredientResponseDTO` với thông tin đầy đủ.
- **Lỗi 400**: tên trùng (`IllegalArgumentException`), validation fail (`MethodArgumentNotValidException`).

### PATCH `/api/v1/ingredients/adjust-inventory`
- **Request (`InventoryAdjustmentRequestDTO`)**:
  ```json
  {
    "ingredientId": 5,
    "newQuantityOnHand": 3500,
    "reason": "Kiểm kê cuối ngày"
  }
  ```
- **Logic**:
  1. Tìm nguyên liệu theo ID; nếu không có -> ghi audit fail (`INGREDIENT_INVENTORY_ADJUSTMENT_FAILED`) và ném 404.
  2. Lưu số lượng mới.
  3. Ghi audit success (`INGREDIENT_INVENTORY_ADJUSTED`) với chi tiết JSON.
- **Response 200**: `IngredientResponseDTO` cập nhật.

### DELETE `/api/v1/ingredients/{id}`
- **Logic**: kiểm tra tồn tại; nếu có liên kết (không kiểm tra trực tiếp, assumption: cho phép xóa) -> xóa ngay.
- **Response 204**.

## Điều kiện nghiệp vụ & validation
- Tên nguyên liệu duy nhất.
- Số lượng tồn kho (`quantityOnHand`) dùng kiểu `BigDecimal` để hỗ trợ đơn vị nhỏ.
- Điều chỉnh tồn kho yêu cầu ghi lý do (optional) — audit log lưu `reason` nếu có.
- Khi thanh toán đơn hàng, nếu không đủ tồn kho -> ném `IllegalArgumentException` và dừng thanh toán.

## Luồng lỗi & thông điệp
| Exception | HTTP | Message |
| --- | --- | --- |
| `EntityNotFoundException` | 404 | "Ingredient not found with id ..." |
| `IllegalArgumentException` | 400 | "Ingredient name already exists", "Not enough stock for ingredient ..." |
| `MethodArgumentNotValidException` | 400 | "Dữ liệu đầu vào không hợp lệ" |

## Role/Permission
- GET/POST/PUT/PATCH: `hasAnyRole('MANAGER','ADMIN')`
- DELETE: `hasRole('ADMIN')`

## Quan hệ với chức năng khác
- **Product Recipe**: xác định lượng nguyên liệu cần cho từng sản phẩm.
- **Order & Payment**: khi thanh toán, số lượng nguyên liệu giảm dựa trên công thức (nếu thiếu -> lỗi).
- **Audit log**: điều chỉnh tồn kho ghi lại hành động cho mục đích kiểm soát.
- **Report**: báo cáo tồn kho/thống kê nguyên liệu sử dụng dữ liệu ở đây.

## Các tệp liên quan trong BE
- Controller: `IngredientController.java`
- Service: `IngredientService.java`
- DTO: `IngredientRequestDTO.java`, `IngredientResponseDTO.java`, `InventoryAdjustmentRequestDTO.java`
- Entity/Repository: `Ingredient.java`, `IngredientRepository.java`
- Mapper: `IngredientMapper.java`
- Audit: `AuditLogService.java`
- Liên kết Payment: `PaymentService.java`
