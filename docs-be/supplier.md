# Chức năng: Quản lý nhà cung cấp

## Vai trò trong hệ thống
- Quản lý thông tin nhà cung cấp nguyên liệu để phục vụ các phiếu nhập hàng.
- Cho phép tạo/cập nhật/xóa nhà cung cấp, đảm bảo tên và số điện thoại không trùng lặp.
- Cung cấp danh sách phục vụ các module nhập hàng (purchase order) và báo cáo chi phí.

## Luồng xử lý backend
1. **Lấy danh sách** (`GET /api/v1/suppliers`): `SupplierController.getAllSuppliers` trả danh sách `SupplierDTO` từ service (không phân trang) @src/main/java/com/giapho/coffee_shop_backend/controller/SupplierController.java#25-30 @src/main/java/com/giapho/coffee_shop_backend/service/SupplierService.java#25-29.
2. **Xem chi tiết** (`GET /api/v1/suppliers/{id}`): tìm theo ID, ném `RuntimeException` nếu không thấy (cần lưu ý thông điệp) @src/main/java/com/giapho/coffee_shop_backend/controller/SupplierController.java#36-40 @src/main/java/com/giapho/coffee_shop_backend/service/SupplierService.java#35-38.
3. **Tạo nhà cung cấp** (`POST`): kiểm tra trùng tên/điện thoại, map DTO -> entity và lưu @src/main/java/com/giapho/coffee_shop_backend/controller/SupplierController.java#47-52 @src/main/java/com/giapho/coffee_shop_backend/service/SupplierService.java#47-58.
4. **Cập nhật** (`PUT /{id}`): kiểm tra tồn tại; nếu đổi tên/SĐT thì kiểm tra trùng trước khi cập nhật @src/main/java/com/giapho/coffee_shop_backend/controller/SupplierController.java#58-65 @src/main/java/com/giapho/coffee_shop_backend/service/SupplierService.java#63-82.
5. **Xóa** (`DELETE /{id}`): xác minh tồn tại, sau đó xóa @src/main/java/com/giapho/coffee_shop_backend/controller/SupplierController.java#72-76 @src/main/java/com/giapho/coffee_shop_backend/service/SupplierService.java#85-92.
6. **Liên kết Purchase Order**: `PurchaseOrderService` sử dụng Supplier ID khi tạo phiếu nhập; validation khi tạo PO đảm bảo supplier tồn tại (xem tài liệu `purchase-order.md`).

## Thành phần liên quan
- **Controller**: `SupplierController` @src/main/java/com/giapho/coffee_shop_backend/controller/SupplierController.java#1-78
- **Service**: `SupplierService` @src/main/java/com/giapho/coffee_shop_backend/service/SupplierService.java#1-93
- **Repository**: `SupplierRepository`
- **DTO**: `SupplierDTO` @src/main/java/com/giapho/coffee_shop_backend/dto/SupplierDTO.java#1-27
- **Entity**: `Supplier`
- **Mapper**: `SupplierMapper`
- **Security**: GET/POST/PUT yêu cầu `MANAGER` hoặc `ADMIN`, DELETE yêu cầu `ADMIN`.

## Danh sách API
| Method | URL | Mô tả | Role |
| --- | --- | --- | --- |
| GET | `/api/v1/suppliers` | Danh sách nhà cung cấp | `MANAGER`,`ADMIN` |
| GET | `/api/v1/suppliers/{id}` | Chi tiết nhà cung cấp | `MANAGER`,`ADMIN` |
| POST | `/api/v1/suppliers` | Tạo nhà cung cấp | `MANAGER`,`ADMIN` |
| PUT | `/api/v1/suppliers/{id}` | Cập nhật nhà cung cấp | `MANAGER`,`ADMIN` |
| DELETE | `/api/v1/suppliers/{id}` | Xóa nhà cung cấp | `ADMIN` |

## Chi tiết API

### POST `/api/v1/suppliers`
- **Request (`SupplierDTO`)**:
  ```json
  {
    "name": "Cong ty ABC",
    "contactPerson": "Nguyen Van C",
    "phone": "0987654321",
    "email": "contact@abc.com",
    "address": "123 Nguyen Trai",
    "notes": "Cung cap syrup"
  }
  ```
- **Logic**: kiểm tra `existsByName` và `existsByPhone`; nếu trùng -> `IllegalArgumentException`.
- **Response 201**: DTO chứa `id` mới.
- **Lỗi 400**: tên/SĐT trùng.

### PUT `/api/v1/suppliers/{id}`
- **Logic**:
  1. Tìm supplier; nếu không -> `EntityNotFoundException`.
  2. Nếu đổi tên/phone -> kiểm tra trùng.
  3. Mapper cập nhật entity; lưu và trả DTO.
- **Response 200**: supplier cập nhật.
- **Lỗi 404**: không tìm thấy ID.
- **Lỗi 400**: trùng tên/phone.

### DELETE `/api/v1/suppliers/{id}`
- **Logic**: kiểm tra `existsById`; nếu không -> 404; nếu có -> delete.
- **Response 204**.

## Điều kiện nghiệp vụ & validation
- Tên và SĐT phải duy nhất.
- Các thuộc tính khác (email, địa chỉ, notes) tùy chọn.
- Khi supplier bị xóa cần đảm bảo không còn phiếu nhập liên quan; hiện tại code không kiểm tra ràng buộc, nhưng DB có thể giới hạn (cần chú ý khi triển khai thực tế).

## Luồng lỗi & thông điệp
| Exception | HTTP | Message |
| --- | --- | --- |
| `RuntimeException` | 404 | "Supplier not found" (nên chuẩn hóa trong tương lai) |
| `EntityNotFoundException` | 404 | "Supplier not found with id ..." |
| `IllegalArgumentException` | 400 | "Supplier name already exists" / "Supplier phone number already exists" |

## Role/Permission
- GET/POST/PUT: `hasAnyRole('MANAGER','ADMIN')`
- DELETE: `hasRole('ADMIN')`

## Quan hệ với chức năng khác
- **Purchase Order**: sử dụng `supplierId` để tạo phiếu nhập, hiển thị thông tin supplier trong kết quả.
- **Expense/Report**: chi phí nhập hàng có thể nhóm theo supplier.

## Các tệp liên quan trong BE
- Controller: `SupplierController.java`
- Service: `SupplierService.java`
- DTO: `SupplierDTO.java`
- Entity & Repository: `Supplier.java`, `SupplierRepository.java`
- Mapper: `SupplierMapper.java`
