# Chức năng: Công thức sản phẩm (Product Recipe)

## Vai trò trong hệ thống
- Quản lý định lượng nguyên liệu cho từng sản phẩm bán ra.
- Cho phép manager/admin xem và cập nhật công thức (danh sách nguyên liệu + lượng cần dùng).
- Hỗ trợ trừ kho chính xác khi thanh toán đơn hàng.

## Luồng xử lý backend
1. **Xem công thức** (`GET /api/v1/products/{productId}/recipe`):
   - `ProductRecipeController.getRecipe` gọi `ProductRecipeService.getRecipeByProductId` để lấy danh sách `ProductIngredientDTO` @src/main/java/com/giapho/coffee_shop_backend/controller/ProductRecipeController.java#22-27 @src/main/java/com/giapho/coffee_shop_backend/service/ProductRecipeService.java#36-44.
   - Service kiểm tra sản phẩm tồn tại (`ProductRepository.existsById`), map entity → DTO qua `ProductIngredientMapper`.
2. **Cập nhật công thức** (`PUT /api/v1/products/{productId}/recipe`):
   - Controller nhận `ProductRecipeDTO` (gồm danh sách nguyên liệu + quantity), service xóa toàn bộ công thức cũ, xây danh sách mới @src/main/java/com/giapho/coffee_shop_backend/controller/ProductRecipeController.java#29-36 @src/main/java/com/giapho/coffee_shop_backend/service/ProductRecipeService.java#49-77.
   - Với mỗi item: kiểm tra ingredient tồn tại, map DTO → entity, gán product & ingredient đầy đủ, lưu lại.
   - Trả lại danh sách DTO mới.
3. **Repository & Mapper**:
   - `ProductIngredientRepository.findByProductId(productId)` lấy danh sách hiện tại.
   - `ProductIngredientMapper` chuyển đổi giữa entity ↔ DTO (được sử dụng trong service).

## Thành phần liên quan
- **Controller**: `ProductRecipeController` @src/main/java/com/giapho/coffee_shop_backend/controller/ProductRecipeController.java#1-38
- **Service**: `ProductRecipeService` @src/main/java/com/giapho/coffee_shop_backend/service/ProductRecipeService.java#1-79
- **Repository**: `ProductRepository`, `IngredientRepository`, `ProductIngredientRepository`
- **DTO**: `ProductRecipeDTO`, `ProductIngredientDTO`
- **Entity**: `ProductIngredient`, `Product`, `Ingredient`
- **Mapper**: `ProductIngredientMapper`
- **Security**: `@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")` cho cả GET & PUT.

## Danh sách API
| Method | URL | Mô tả | Role |
| --- | --- | --- | --- |
| GET | `/api/v1/products/{productId}/recipe` | Lấy công thức (danh sách nguyên liệu + định lượng) | `MANAGER`,`ADMIN` |
| PUT | `/api/v1/products/{productId}/recipe` | Ghi đè công thức sản phẩm | `MANAGER`,`ADMIN` |

## Chi tiết API

### GET `/api/v1/products/{productId}/recipe`
- **Response 200** (ví dụ):
  ```json
  [
    { "ingredientId": 5, "ingredientName": "Espresso", "unit": "ml", "quantityNeeded": 30 },
    { "ingredientId": 8, "ingredientName": "Sữa tươi", "unit": "ml", "quantityNeeded": 120 }
  ]
  ```
- **Lỗi 404**: nếu sản phẩm không tồn tại.

### PUT `/api/v1/products/{productId}/recipe`
- **Request (`ProductRecipeDTO`)**:
  ```json
  {
    "ingredients": [
      { "ingredientId": 5, "quantityNeeded": 30 },
      { "ingredientId": 8, "quantityNeeded": 120 }
    ]
  }
  ```
- **Logic**:
  1. Kiểm tra sản phẩm tồn tại.
  2. Xóa công thức cũ (`productIngredientRepository.deleteByProductId`).
  3. Với mỗi item: kiểm tra ingredient có tồn tại, map DTO→entity, gán product & ingredient, thêm vào set.
  4. Lưu tất cả (`saveAll`) và trả DTO mới.
- **Lỗi 404**: sản phẩm hoặc nguyên liệu không tồn tại.
- **Lỗi 400**: nếu mapper/service bắt quantity <= 0 (validation trong DTO/service).

## Điều kiện nghiệp vụ
- Công thức ghi đè hoàn toàn: PUT xóa sạch dữ liệu cũ trước khi ghi mới.
- `quantityNeeded` phải > 0 (service giả định; cần đảm bảo DTO validation).
- Hỗ trợ import nhiều nguyên liệu cùng lúc.

## Quan hệ với chức năng khác
- **Order Service**: dùng công thức để tính tổng lượng nguyên liệu cần trừ khi thanh toán (`PaymentService.subtractInventoryForOrder`).
- **Ingredient tồn kho**: khi cập nhật công thức mới, FE nên đồng bộ để đảm bảo số lượng hợp lý.
- **ProductService**: hiển thị công thức khi quản lý sản phẩm.

## Các tệp liên quan trong BE
- Controller: `ProductRecipeController.java`
- Service: `ProductRecipeService.java`
- Repository: `ProductIngredientRepository.java`, `ProductRepository.java`, `IngredientRepository.java`
- DTO: `ProductRecipeDTO.java`, `ProductIngredientDTO.java`
- Mapper: `ProductIngredientMapper.java`
