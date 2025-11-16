# zzzzChức năng: Quản lý danh mục sản phẩm

## Vai trò trong hệ thống
- Quản trị danh mục (category) giúp phân loại sản phẩm trên hệ thống bán hàng.
- Cung cấp dữ liệu thống nhất cho UI lọc/tìm kiếm sản phẩm.
- Kết hợp cache để giảm tải truy vấn danh sách danh mục.

## Luồng xử lý backend
1. **Tạo danh mục** (`POST /api/v1/categories`): `CategoryController` nhận `CategoryDTO`, `CategoryService.createCategory` chuẩn hóa tên, kiểm tra trùng, lưu entity rồi cache-evict @src/main/java/com/giapho/coffee_shop_backend/controller/CategoryController.java#20-26 @src/main/java/com/giapho/coffee_shop_backend/service/CategoryService.java#26-43.
2. **Lấy tất cả danh mục** (`GET /api/v1/categories`): Service sử dụng `@Cacheable` để trả về danh sách `CategoryDTO`, thích hợp cho dropdown UI @src/main/java/com/giapho/coffee_shop_backend/controller/CategoryController.java#28-33 @src/main/java/com/giapho/coffee_shop_backend/service/CategoryService.java#45-50.
3. **Cập nhật danh mục** (`PUT /api/v1/categories/{id}`): Service kiểm tra tồn tại, ngăn đổi tên trùng, cập nhật mô tả rồi cache-evict @src/main/java/com/giapho/coffee_shop_backend/controller/CategoryController.java#35-42 @src/main/java/com/giapho/coffee_shop_backend/service/CategoryService.java#52-68.
4. **Xóa danh mục** (`DELETE /api/v1/categories/{id}`): Service xác minh tồn tại, xóa entity và cache-evict @src/main/java/com/giapho/coffee_shop_backend/controller/CategoryController.java#45-50 @src/main/java/com/giapho/coffee_shop_backend/service/CategoryService.java#71-77.

## Thành phần liên quan
- **Controller**: `CategoryController` @src/main/java/com/giapho/coffee_shop_backend/controller/CategoryController.java#1-51
- **Service**: `CategoryService` @src/main/java/com/giapho/coffee_shop_backend/service/CategoryService.java#1-79
- **Repository**: `CategoryRepository` (triển khai `existsByName`, `findAll`, `findById`) @src/main/java/com/giapho/coffee_shop_backend/domain/repository/CategoryRepository.java#1-24
- **DTO**: `CategoryDTO` @src/main/java/com/giapho/coffee_shop_backend/dto/CategoryDTO.java#1-14
- **Entity**: `Category`
- **Mapper**: `CategoryMapper`
- **Validation**: `CategoryDTO` yêu cầu `name` không rỗng tại service (trimming), annotation `@NotBlank` hỗ trợ ở DTO nếu áp dụng.
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
- **Request body (`CategoryDTO`)**:
  ```json
  {
    "name": "Cà phê",
    "description": "Các sản phẩm cà phê"
  }
  ```
- **Logic**: trim tên, kiểm tra rỗng, kiểm tra `existsByName`. Nếu hợp lệ lưu và trả DTO.
- **Response 200**: `CategoryDTO` kèm `id` mới.
- **Lỗi 400**: tên rỗng -> `ResponseStatusException(HttpStatus.BAD_REQUEST)`.
- **Lỗi 409**: tên đã tồn tại -> `HttpStatus.CONFLICT`.

### GET `/api/v1/categories`
- **Response 200**: `List<CategoryDTO>`
  ```json
  [
    { "id": 3, "name": "Cà phê", "description": "Các sản phẩm cà phê" },
    { "id": 4, "name": "Trà", "description": "Đồ uống trà" }
  ]
  ```
- **Cache**: dữ liệu được cache với key `'all'`.

### PUT `/api/v1/categories/{id}`
- **Request body**: giống POST.
- **Logic**: kiểm tra tồn tại, nếu đổi sang tên mới đã có -> `IllegalArgumentException`. Cập nhật name/description và trả DTO.
- **Response 200**: danh mục sau cập nhật.
- **Lỗi 400**: tên xung đột.
- **Lỗi 404**: danh mục không tồn tại.

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
| `ResponseStatusException(HttpStatus.BAD_REQUEST)` | 400 | "Category name must not be empty" |
| `ResponseStatusException(HttpStatus.CONFLICT)` | 409 | "Category with name ... already exists" |
| `IllegalArgumentException` | 400 | "Category name already exists" |
| `ResponseStatusException(HttpStatus.NOT_FOUND)` | 404 | "Category with id ... not found" |

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
