# Chức năng: Phiếu nhập hàng (Purchase Order)

## Vai trò trong hệ thống
- Quản lý quá trình nhập nguyên liệu từ nhà cung cấp.
- Cho phép lọc, xem chi tiết, tạo phiếu nhập, hoàn tất (cập nhật tồn kho) và hủy phiếu.
- Gắn với supplier, người tạo (user), danh sách nguyên liệu và tổng giá trị nhập.

## Luồng xử lý backend
1. **Danh sách phiếu nhập** (`GET /api/v1/purchase-orders`): hỗ trợ lọc theo trạng thái, nhà cung cấp, khoảng ngày; sử dụng `Specification` để xây predicate @src/main/java/com/giapho/coffee_shop_backend/controller/PurchaseOrderController.java#36-51 @src/main/java/com/giapho/coffee_shop_backend/service/PurchaseOrderService.java#37-85.
2. **Chi tiết phiếu** (`GET /api/v1/purchase-orders/{id}`): lấy PO và map sang `PurchaseOrderResponseDTO`, ném 404 nếu không tồn tại @src/main/java/com/giapho/coffee_shop_backend/controller/PurchaseOrderController.java#53-57 @src/main/java/com/giapho/coffee_shop_backend/service/PurchaseOrderService.java#87-95.
3. **Tạo phiếu nhập** (`POST`): service lấy user hiện tại, tìm supplier, xử lý từng item -> `PurchaseOrderDetail`, tính tổng tiền, lưu PO @src/main/java/com/giapho/coffee_shop_backend/controller/PurchaseOrderController.java#27-33 @src/main/java/com/giapho/coffee_shop_backend/service/PurchaseOrderService.java#97-149.
4. **Hoàn tất phiếu (COMPLETED)** (`POST /{id}/complete`): kiểm tra trạng thái `PENDING`, set `COMPLETED`, cộng tồn kho cho từng ingredient @src/main/java/com/giapho/coffee_shop_backend/controller/PurchaseOrderController.java#60-64 @src/main/java/com/giapho/coffee_shop_backend/service/PurchaseOrderService.java#151-179.
5. **Hủy phiếu (CANCELLED)** (`POST /{id}/cancel`): chỉ cho phép nếu đang `PENDING`, cập nhật trạng thái @src/main/java/com/giapho/coffee_shop_backend/controller/PurchaseOrderController.java#67-71 @src/main/java/com/giapho/coffee_shop_backend/service/PurchaseOrderService.java#181-197.

## Thành phần liên quan
- **Controller**: `PurchaseOrderController` @src/main/java/com/giapho/coffee_shop_backend/controller/PurchaseOrderController.java#1-73
- **Service**: `PurchaseOrderService` @src/main/java/com/giapho/coffee_shop_backend/service/PurchaseOrderService.java#1-199
- **Repository**: `PurchaseOrderRepository`, `PurchaseOrderDetailRepository`, `SupplierRepository`, `UserRepository`, `IngredientRepository`
- **DTO**: `PurchaseOrderRequestDTO`, `PurchaseOrderDetailRequestDTO`, `PurchaseOrderResponseDTO`, `PurchaseOrderDetailResponseDTO` @src/main/java/com/giapho/coffee_shop_backend/dto/PurchaseOrderRequestDTO.java#1-42 @src/main/java/com/giapho/coffee_shop_backend/dto/PurchaseOrderDetailRequestDTO.java#1-26 @src/main/java/com/giapho/coffee_shop_backend/dto/PurchaseOrderResponseDTO.java#1-58
- **Entity**: `PurchaseOrder`, `PurchaseOrderDetail`, `Supplier`, `Ingredient`, `User`
- **Mapper**: `PurchaseOrderMapper`
- **Security**: tất cả endpoint yêu cầu `MANAGER` hoặc `ADMIN`.

## Danh sách API
| Method | URL | Mô tả | Role |
| --- | --- | --- | --- |
| POST | `/api/v1/purchase-orders` | Tạo phiếu nhập mới | `MANAGER`,`ADMIN` |
| GET | `/api/v1/purchase-orders` | Danh sách phiếu nhập (lọc & phân trang) | `MANAGER`,`ADMIN` |
| GET | `/api/v1/purchase-orders/{id}` | Chi tiết phiếu nhập | `MANAGER`,`ADMIN` |
| POST | `/api/v1/purchase-orders/{id}/complete` | Hoàn tất phiếu (cộng tồn kho) | `MANAGER`,`ADMIN` |
| POST | `/api/v1/purchase-orders/{id}/cancel` | Hủy phiếu nhập | `MANAGER`,`ADMIN` |

## Chi tiết API

### POST `/api/v1/purchase-orders`
- **Request (`PurchaseOrderRequestDTO`)**:
  ```json
  {
    "supplierId": 5,
    "expectedDate": "2025-11-20",
    "items": [
      { "ingredientId": 12, "quantity": 10, "unitPrice": 12000 },
      { "ingredientId": 15, "quantity": 5, "unitPrice": 8500 }
    ]
  }
  ```
- **Logic**:
  1. Lấy user hiện tại từ `SecurityContext`.
  2. Tìm supplier và từng ingredient.
  3. Tạo `PurchaseOrderDetail` cho mỗi item, cộng tổng tiền (`quantity * unitPrice`).
  4. Lưu `PurchaseOrder` với trạng thái `PENDING`.
- **Response 201**: `PurchaseOrderResponseDTO` chứa danh sách chi tiết, tổng tiền.
- **Lỗi 404**: không tìm thấy supplier/ingredient/user.
- **Lỗi 400**: DTO thiếu items (validation trong DTO `@NotEmpty`).

### POST `/api/v1/purchase-orders/{id}/complete`
- **Logic**:
  1. Lấy phiếu theo ID.
  2. Kiểm tra trạng thái `PENDING`. Nếu khác -> `IllegalArgumentException`.
  3. Đổi sang `COMPLETED` và cộng tồn kho: `ingredient.quantityOnHand += detail.quantity`.
  4. Lưu PO và trả về `PurchaseOrderResponseDTO` cập nhật.
- **Lỗi 400**: trạng thái không hợp lệ.
- **Lỗi 404**: không tìm thấy phiếu.

### POST `/api/v1/purchase-orders/{id}/cancel`
- **Logic**: tương tự `complete`, nhưng set `CANCELLED`, không thay đổi tồn kho.

### GET `/api/v1/purchase-orders`
- **Query**: `status`, `supplierId`, `startDate`, `endDate`, phân trang (`size=10`, sort=`orderDate` DESC).
- **Response 200** (`Page<PurchaseOrderResponseDTO>`): chứa meta và chi tiết phiếu.

### GET `/api/v1/purchase-orders/{id}`
- **Response 200**: chi tiết phiếu, bao gồm items (`PurchaseOrderDetailResponseDTO`).

## Điều kiện nghiệp vụ & validation
- Trạng thái ban đầu `PENDING`; chỉ `PENDING` mới được hoàn tất/hủy.
- Khi hoàn tất, tồn kho nguyên liệu tăng tương ứng.
- Tên supplier, ingredient phải tồn tại; nếu không -> 404.
- Tổng tiền tính từ chi tiết, không nhận trực tiếp từ request.
- Người tạo phiếu là user đăng nhập hiện tại.

## Luồng lỗi & thông điệp
| Exception | HTTP | Message |
| --- | --- | --- |
| `EntityNotFoundException` | 404 | "Purchase Order not found", "Supplier not found", "Ingredient not found" |
| `IllegalArgumentException` | 400 | "Purchase Order is not in PENDING status", "Cannot cancel ..." |
| `MethodArgumentNotValidException` | 400 | "Dữ liệu đầu vào không hợp lệ" (items rỗng, ... ) |

## Role/Permission
- Mọi endpoint: `hasAnyRole('MANAGER','ADMIN')`.

## Quan hệ với chức năng khác
- **Supplier**: dùng `supplierId` và hiển thị thông tin nhà cung cấp.
- **Ingredient**: phiếu hoàn tất sẽ tăng tồn kho, tích hợp với `ingredient.md`.
- **Expense/Report**: dữ liệu nhập hàng dùng cho báo cáo chi phí.
- **Audit**: chưa ghi log cụ thể; có thể bổ sung qua `AuditLogService` nếu cần.

## Các tệp liên quan trong BE
- Controller: `PurchaseOrderController.java`
- Service: `PurchaseOrderService.java`
- DTO: `PurchaseOrderRequestDTO.java`, `PurchaseOrderDetailRequestDTO.java`, `PurchaseOrderResponseDTO.java`
- Entity & Repository: `PurchaseOrder.java`, `PurchaseOrderDetail.java`, `PurchaseOrderRepository.java`, `PurchaseOrderDetailRepository.java`
- Mapper: `PurchaseOrderMapper.java`
- Liên kết: `SupplierRepository.java`, `IngredientRepository.java`, `UserRepository.java`
