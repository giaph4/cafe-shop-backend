# zzzzChức năng: Quản lý danh mục sản phẩm

## Vai trò trong hệ thống
- Quản trị danh mục (category) giúp phân loại sản phẩm trên hệ thống bán hàng.
- Cung cấp dữ liệu thống nhất cho UI lọc/tìm kiếm sản phẩm.
- Kết hợp cache để giảm tải truy vấn danh sách danh mục.

## Luồng xử lý backend
1. **Tạo danh mục** (`POST /api/v1/categories`): `CategoryController` nhận `CategoryCreateRequest`, `CategoryService.createCategory` chuẩn hóa tên, kiểm tra trùng (ignore-case), lưu entity rồi cache-evict @src/main/java/com/giapho/coffee_shop_backend/controller/CategoryController.java#23-27 @src/main/java/com/giapho/coffee_shop_backend/service/impl/CategoryServiceImpl.java#35-46.
2. **Lấy tất cả danh mục** (`GET /api/v1/categories`): Service sử dụng `@Cacheable` để trả về danh sách `CategoryResponse`, thích hợp cho dropdown UI @src/main/java/com/giapho/coffee_shop_backend/controller/CategoryController.java#30-35 @src/main/java/com/giapho/coffee_shop_backend/service/impl/CategoryServiceImpl.java#48-53.
3. **Cập nhật danh mục** (`PUT /api/v1/categories/{id}`): Service kiểm tra tồn tại, chuẩn hóa dữ liệu và ngăn đổi tên trùng (so sánh theo id) trước khi cache-evict @src/main/java/com/giapho/coffee_shop_backend/controller/CategoryController.java#37-44 @src/main/java/com/giapho/coffee_shop_backend/service/impl/CategoryServiceImpl.java#55-68.
4. **Xóa danh mục** (`DELETE /api/v1/categories/{id}`): Service xác minh tồn tại, xóa entity và cache-evict @src/main/java/com/giapho/coffee_shop_backend/controller/CategoryController.java#45-50 @src/main/java/com/giapho/coffee_shop_backend/service/CategoryService.java#71-77.

## Thành phần liên quan
- **Controller**: `CategoryController` @src/main/java/com/giapho/coffee_shop_backend/controller/CategoryController.java#1-52
- **Service**: `CategoryService` & `CategoryServiceImpl` @src/main/java/com/giapho/coffee_shop_backend/service/CategoryService.java#9-18 @src/main/java/com/giapho/coffee_shop_backend/service/impl/CategoryServiceImpl.java#28-119
- **Repository**: `CategoryRepository` (exists/find ignore-case) @src/main/java/com/giapho/coffee_shop_backend/domain/repository/CategoryRepository.java#9-17
- **DTO**: `CategoryCreateRequest`, `CategoryUpdateRequest`, `CategoryResponse` @src/main/java/com/giapho/coffee_shop_backend/dto/category
- **Entity**: `Category`
- **Mapper**: `CategoryMapper`
- **Validation**: `CategoryCreateRequest`/`CategoryUpdateRequest` áp dụng `@NotBlank`, service tiếp tục trim & kiểm tra dữ liệu.
- **Caching**: sử dụng cache `categories` cho danh sách.

## Danh sách API
| Method | URL | Mô tả | Role |
| --- | --- | --- | --- |
| POST | `/api/v1/categories` | Tạo danh mục mới | `MANAGER`, `ADMIN` |
| GET | `/api/v1/categories` | Lấy danh sách danh mục | `STAFF`, `MANAGER`, `ADMIN` |
| PUT | `/api/v1/categories/{id}` | Cập nhật tên/mô tả danh mục | `MANAGER`, `ADMIN` |
| DELETE | `/api/v1/categories/{id}` | Xóa danh mục | `MANAGER`, `ADMIN` |

## Chi tiết API

### POST `/api/v1/categories`
- **Request body (`CategoryCreateRequest`)**:
  ```json
  {
    "name": "Cà phê",
    "description": "Các sản phẩm cà phê"
  }
  ```
- **Logic**: chuẩn hóa (trim) tên/mô tả, kiểm tra rỗng, kiểm tra `existsByNameIgnoreCase`. Nếu hợp lệ lưu và trả `CategoryResponse`.
- **Response 200**: `CategoryResponse` kèm `id` mới.
- **Lỗi 400**: tên rỗng -> `CategoryValidationException`.
- **Lỗi 409**: tên đã tồn tại -> `CategoryAlreadyExistsException`.

### GET `/api/v1/categories`
- **Response 200**: `List<CategoryResponse>`
  ```json
  [
    { "id": 3, "name": "Cà phê", "description": "Các sản phẩm cà phê" },
    { "id": 4, "name": "Trà", "description": "Đồ uống trà" }
  ]
  ```
- **Cache**: dữ liệu được cache với key `'all'`.

### PUT `/api/v1/categories/{id}`
- **Request body (`CategoryUpdateRequest`)**: giống POST.
- **Logic**: kiểm tra tồn tại, chuẩn hóa dữ liệu, gọi `existsByNameIgnoreCaseAndIdNot` để ngăn trùng tên. Trả `CategoryResponse` sau cập nhật.
- **Response 200**: danh mục sau cập nhật.
- **Lỗi 400**: tên rỗng -> `CategoryValidationException`.
- **Lỗi 409**: tên trùng -> `CategoryAlreadyExistsException`.
- **Lỗi 404**: danh mục không tồn tại -> `CategoryNotFoundException`.

### DELETE `/api/v1/categories/{id}`
- **Logic**: xác minh tồn tại, xóa entity, cache-evict.
- **Response 204**: không nội dung.
- **Lỗi 404**: danh mục không tồn tại.

## Điều kiện nghiệp vụ
- Tên danh mục không được rỗng và không trùng lặp (so sánh theo giá trị trim).
- Khi cập nhật/xóa phải đảm bảo danh mục tồn tại.
- Các thay đổi danh mục yêu cầu phân quyền quản lý.

## Luồng lỗi & thông điệp
| Exception | HTTP | Message |
| --- | --- | --- |
| `CategoryValidationException` | 400 | "Category name must not be blank" |
| `CategoryAlreadyExistsException` | 409 | "Category with name '...' already exists" |
| `CategoryNotFoundException` | 404 | "Category not found with id: ..." |

## Role/Permission
- `POST/PUT/DELETE`: yêu cầu `hasAnyRole('MANAGER','ADMIN')`.
- `GET`: cho phép `STAFF`, `MANAGER`, `ADMIN`.

## Quan hệ với chức năng khác
- Sản phẩm (`product.md`) phụ thuộc `categoryId`. Khi xóa danh mục cần đảm bảo không có sản phẩm tham chiếu (logic xử lý ở nghiệp vụ khác nếu cần).
- Báo cáo doanh thu theo danh mục sử dụng dữ liệu từ đây.

## Tệp liên quan
- `CategoryRepository.java`
- `CategoryService.java`
- `CategoryMapper.java`
- `CategoryController.java`
