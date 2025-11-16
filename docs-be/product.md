# Chức năng: Quản lý sản phẩm

## Vai trò trong hệ thống
- Duy trì danh mục đồ uống/sản phẩm bán ra và trạng thái khả dụng.
- Hỗ trợ CRUD, lọc, phân trang, quản lý ảnh đại diện sản phẩm.
- Kết nối với công thức pha chế (recipe), đơn hàng và quản lý tồn kho nguyên liệu.

## Luồng xử lý backend
1. **Tạo/Cập nhật sản phẩm**: `ProductController` nhận `ProductRequest`, `ProductService` chuẩn hóa mã sản phẩm, kiểm tra trùng và map tới entity `Product`; liên kết tới `Category` trước khi lưu @src/main/java/com/giapho/coffee_shop_backend/controller/ProductController.java#27-60 @src/main/java/com/giapho/coffee_shop_backend/service/ProductService.java#52-120.
2. **Tra cứu sản phẩm**: Hỗ trợ phân trang, lọc theo tên và danh mục bằng `Specification` trong service @src/main/java/com/giapho/coffee_shop_backend/controller/ProductController.java#34-49 @src/main/java/com/giapho/coffee_shop_backend/service/ProductService.java#75-94.
3. **Quản lý trạng thái khả dụng**: Endpoint toggle gọi `ProductService.toggleProductAvailability`, lật cờ `available` @src/main/java/com/giapho/coffee_shop_backend/controller/ProductController.java#69-74 @src/main/java/com/giapho/coffee_shop_backend/service/ProductService.java#142-149.
4. **Quản lý ảnh**: Các endpoint multipart và upload/delete ảnh sử dụng `FileStorageService` để lưu, xóa file cũ @src/main/java/com/giapho/coffee_shop_backend/controller/ProductController.java#76-129 @src/main/java/com/giapho/coffee_shop_backend/service/ProductService.java#151-287.
5. **Xóa sản phẩm**: Kiểm tra ràng buộc tồn tại trong `OrderDetail`, xóa ảnh và công thức liên quan trước khi xóa product @src/main/java/com/giapho/coffee_shop_backend/controller/ProductController.java#62-67 @src/main/java/com/giapho/coffee_shop_backend/service/ProductService.java#122-139.

## Thành phần liên quan
- **Controller**: `ProductController` @src/main/java/com/giapho/coffee_shop_backend/controller/ProductController.java#1-129
- **Service**: `ProductService` @src/main/java/com/giapho/coffee_shop_backend/service/ProductService.java#1-310
- **Repository**: `ProductRepository`, `CategoryRepository`, `ProductIngredientRepository`, `OrderDetailRepository`
- **DTO**: `ProductRequest`, `ProductResponse` @src/main/java/com/giapho/coffee_shop_backend/dto/ProductRequest.java#1-38 @src/main/java/com/giapho/coffee_shop_backend/dto/ProductResponse.java#1-40
- **Entity**: `Product`, `Category`, `ProductIngredient`
- **Mapper**: `ProductMapper`, `ProductIngredientMapper`
- **Validation**: Annotation trong `ProductRequest` (`@NotBlank`, `@Positive`, `@NotNull`) và kiểm tra bổ sung trong service.
- **Liên kết khác**: `FileStorageService` (quản lý file), `ProductRecipeController` & `ProductRecipeService` (công thức), `OrderService` (đọc dữ liệu product khi tính đơn hàng).

## Danh sách API
| Method | URL | Mô tả | Role |
| --- | --- | --- | --- |
| POST | `/api/v1/products` | Tạo sản phẩm mới (JSON) | `MANAGER`, `ADMIN` |
| GET | `/api/v1/products` | Lấy danh sách sản phẩm (lọc & phân trang) | `STAFF`, `MANAGER`, `ADMIN` |
| GET | `/api/v1/products/{id}` | Lấy chi tiết sản phẩm | `STAFF`, `MANAGER`, `ADMIN` |
| PUT | `/api/v1/products/{id}` | Cập nhật sản phẩm (JSON) | `MANAGER`, `ADMIN` |
| DELETE | `/api/v1/products/{id}` | Xóa sản phẩm | `MANAGER`, `ADMIN` |
| PATCH | `/api/v1/products/{id}/toggle-availability` | Bật/tắt khả dụng | `MANAGER`, `ADMIN` |
| POST | `/api/v1/products` (multipart) | Tạo sản phẩm kèm ảnh | `MANAGER`, `ADMIN` |
| PUT | `/api/v1/products/{id}` (multipart) | Cập nhật sản phẩm & ảnh | `MANAGER`, `ADMIN` |
| DELETE | `/api/v1/products/{id}/image` | Xóa ảnh hiện tại | `MANAGER`, `ADMIN` |
| POST | `/api/v1/products/{id}/image` | Upload ảnh mới | `MANAGER`, `ADMIN` |

## Chi tiết API

### POST `/api/v1/products`
- **Request body (`ProductRequest`)**:
  ```json
  {
    "name": "Latte",
    "code": "LATTE",
    "price": 45000,
    "cost": 22000,
    "description": "Cà phê sữa đá",
    "imageUrl": null,
    "categoryId": 3
  }
  ```
- **Logic**: Chuẩn hóa mã (uppercase), kiểm tra trùng code, kiểm tra tồn tại `Category`, đặt `available=true` mặc định.
- **Response 200** (`ProductResponse`): trả về thông tin sản phẩm với `available`, `categoryName`, `createdAt`…
- **Lỗi 400**: thiếu trường bắt buộc, giá âm, code rỗng (`MethodArgumentNotValidException` hoặc `IllegalArgumentException`).
- **Lỗi 404**: không tìm thấy `Category` (`EntityNotFoundException`).
- **Lỗi 409**: trùng code (IllegalArgumentException).
- **Lỗi 500**: lỗi hệ thống.

### GET `/api/v1/products`
- **Query**: `name` (keyword), `categoryId`, `page`, `size` (mặc định `size=10`), `sort`.
- **Response 200** (Page):
  ```json
  {
    "content": [
      {
        "id": 15,
        "name": "Latte",
        "code": "LATTE",
        "price": 45000,
        "available": true,
        "categoryId": 3,
        "categoryName": "Cà phê"
      }
    ],
    "pageable": { "pageNumber": 0, "pageSize": 10 },
    "totalElements": 58,
    "totalPages": 6
  }
  ```
- **Lỗi 400**: `categoryId` không tồn tại -> `EntityNotFoundException`.

### GET `/api/v1/products/{id}`
- **Response 200**: `ProductResponse` với trường chi tiết.
- **Lỗi 404**: không tìm thấy sản phẩm.

### PUT `/api/v1/products/{id}`
- **Giống request POST**; hỗ trợ cập nhật `code` nhưng kiểm tra trùng.
- **Logic**: giữ nguyên trạng thái `available`, cập nhật mọi trường khác.
- **Response**: trả sản phẩm mới nhất.
- **Lỗi 400**: validation hoặc trùng code.
- **Lỗi 404**: sản phẩm/loại không tồn tại.

### DELETE `/api/v1/products/{id}`
- **Logic**: Kiểm tra sản phẩm tồn tại trong `OrderDetail` (không cho xóa nếu đã bán), xóa ảnh và công thức trước khi xóa entity.
- **Response 204**: không nội dung.
- **Lỗi 400**: đã tồn tại trong lịch sử đơn hàng.
- **Lỗi 404**: không tìm thấy ID.

### PATCH `/api/v1/products/{id}/toggle-availability`
- **Logic**: Đảo `available`, lưu lại.
- **Response 200**: `ProductResponse` mới.
- **Lỗi 404**: không tìm thấy sản phẩm.

### POST `/api/v1/products` (Multipart)
- **Phần thân**: `product` (JSON string) + `image` (file tùy chọn, hỗ trợ null). Ví dụ form-data:
  - `product`: `{ "name": "Latte", ... }`
  - `image`: file `.jpg`
- **Logic**: parse JSON bằng `ObjectMapper`, nếu có ảnh -> lưu file, lấy URL.
- **Response 200**: như POST JSON.
- **Lỗi 400**: parse JSON thất bại -> RuntimeException "Failed to parse product data".

### PUT `/api/v1/products/{id}` (Multipart)
- **Tham số** giống POST multipart, hỗ trợ xóa ảnh bằng gửi `product.imageUrl = null` và bỏ file.
- **Logic**: nếu upload ảnh mới => lưu mới, xóa file cũ; nếu imageUrl null => xóa ảnh.
- **Response 200**: `ProductResponse` mới.

### DELETE `/api/v1/products/{id}/image`
- **Logic**: lấy đường dẫn ảnh hiện tại, đổi `imageUrl=null`, xóa file qua `FileStorageService`.
- **Response 200**: trả sản phẩm không còn ảnh.
- **Lỗi 400**: sản phẩm không có ảnh.

### POST `/api/v1/products/{id}/image`
- **Request**: `image` (Multipart, bắt buộc).
- **Logic**: lưu file mới, xóa file cũ (nếu có), cập nhật `imageUrl`.
- **Response 200**: sản phẩm với ảnh mới.
- **Lỗi 400**: thiếu file (`IllegalArgumentException`).

## Điều kiện nghiệp vụ & validation
- Code sản phẩm được chuẩn hóa uppercase, không trùng lặp.
- Giá phải dương, Category phải tồn tại.
- Không cho xóa sản phẩm đã được sử dụng trong `OrderDetail` – hướng dẫn đánh dấu `available=false` thay thế.
- File ảnh xử lý qua `FileStorageService`, xóa file cũ an toàn.

## Luồng lỗi & thông điệp chính
| Exception | Nguồn | HTTP | Thông điệp |
| --- | --- | --- | --- |
| `MethodArgumentNotValidException` | Payload không hợp lệ | 400 | "Dữ liệu đầu vào không hợp lệ" |
| `IllegalArgumentException` | Code trùng, sản phẩm đã bán, thiếu image | 400 |
| `EntityNotFoundException` | Không tìm thấy Product/Category | 404 |
| `ResponseStatusException` | CategoryService (name rỗng/trùng) | 400/409/404 |
| `RuntimeException` | Parse JSON multipart thất bại | 500 |

## Role/Permission
- Tạo/Cập nhật/Xóa/Upload ảnh: `MANAGER` hoặc `ADMIN`.
- Tra cứu danh sách/chi tiết: `STAFF`, `MANAGER`, `ADMIN`.

## Quan hệ với chức năng khác
- **Category**: bắt buộc `categoryId` hợp lệ.
- **Product Recipe**: xác định định lượng nguyên liệu cho sản phẩm.
- **Order & Payment**: đọc giá/availability, kiểm tra tồn kho khi thanh toán.
- **File**: dùng chung dịch vụ lưu trữ ảnh.

## Các tệp liên quan
- Mapper: `ProductMapper`, `ProductIngredientMapper` trong package `mapper`.
- Service phụ trợ: `FileStorageService`, `ProductRecipeService`.
- Entity & Repository: `Product`, `Category`, `ProductIngredient`, `ProductRepository`, `CategoryRepository`, `ProductIngredientRepository`, `OrderDetailRepository`.
