# Chức năng: Quản lý khách hàng

## Vai trò trong hệ thống
- Lưu trữ thông tin khách hàng, phục vụ bán hàng và chương trình khách hàng thân thiết.
- Cho phép tra cứu nhanh theo số điện thoại, cập nhật hồ sơ, xóa khách hàng.
- Cung cấp lịch sử mua hàng có phân trang để hỗ trợ chăm sóc khách hàng & phân tích.

## Luồng xử lý backend
1. **Tạo khách hàng** (`POST /api/v1/customers`): `CustomerController.createCustomer` nhận `CustomerDTO`, `CustomerService.createCustomer` kiểm tra trùng SĐT/Email, lưu entity @src/main/java/com/giapho/coffee_shop_backend/controller/CustomerController.java#27-32 @src/main/java/com/giapho/coffee_shop_backend/service/CustomerService.java#110-125.
2. **Tìm kiếm danh sách** (`GET /api/v1/customers`): controller nhận `keyword`, phân trang; service chọn findAll hoặc findByFullNameContainingIgnoreCaseOrPhoneContaining @src/main/java/com/giapho/coffee_shop_backend/controller/CustomerController.java#34-42 @src/main/java/com/giapho/coffee_shop_backend/service/CustomerService.java#45-55.
3. **Lấy chi tiết** (`GET /api/v1/customers/{id}` / `phone/{phone}`): service tìm theo ID/SĐT, ném `EntityNotFoundException` nếu không có @src/main/java/com/giapho/coffee_shop_backend/controller/CustomerController.java#44-56 @src/main/java/com/giapho/coffee_shop_backend/service/CustomerService.java#60-105.
4. **Lịch sử mua hàng** (`GET /{id}/purchase-history`): service gom đơn hàng, trả tổng quan và danh sách đã phân trang @src/main/java/com/giapho/coffee_shop_backend/controller/CustomerController.java#58-68 @src/main/java/com/giapho/coffee_shop_backend/service/CustomerService.java#167-224.
5. **Cập nhật & Xóa** (`PUT`/`DELETE`): kiểm tra trùng dữ liệu trước khi cập nhật, xác minh tồn tại trước khi xóa @src/main/java/com/giapho/coffee_shop_backend/controller/CustomerController.java#71-85 @src/main/java/com/giapho/coffee_shop_backend/service/CustomerService.java#128-164.
6. **Điểm thưởng**: sau khi thanh toán, `CustomerService.updateLoyaltyPoints` cộng điểm tùy tổng tiền, được gọi từ `PaymentService` @src/main/java/com/giapho/coffee_shop_backend/service/CustomerService.java#68-95 @src/main/java/com/giapho/coffee_shop_backend/service/PaymentService.java#111-129.

## Thành phần liên quan
- **Controller**: `CustomerController` @src/main/java/com/giapho/coffee_shop_backend/controller/CustomerController.java#1-87
- **Service**: `CustomerService` @src/main/java/com/giapho/coffee_shop_backend/service/CustomerService.java#1-236
- **Repository**: `CustomerRepository`, `OrderRepository` @src/main/java/com/giapho/coffee_shop_backend/domain/repository/CustomerRepository.java#1-44
- **DTO**: `CustomerDTO`, `CustomerPurchaseHistoryResponseDTO`, `CustomerPurchaseHistoryItemDTO`, `CustomerAnalyticsDTO` @src/main/java/com/giapho/coffee_shop_backend/dto/CustomerDTO.java#1-29
- **Entity**: `Customer`, `Order`
- **Mapper**: `CustomerMapper`, `CustomerPurchaseHistoryMapper`
- **Validation**: `@NotBlank`, `@Pattern` (SĐT Việt Nam), `@Email` trên `CustomerDTO` @src/main/java/com/giapho/coffee_shop_backend/dto/CustomerDTO.java#15-24.
- **Security**: `@PreAuthorize` theo vai trò trong controller (STAFF/MANAGER/ADMIN hoặc ADMIN).

## Danh sách API
| Method | URL | Mô tả | Role |
| --- | --- | --- | --- |
| POST | `/api/v1/customers` | Tạo khách hàng mới | `STAFF`,`MANAGER`,`ADMIN` |
| GET | `/api/v1/customers` | Tìm kiếm & phân trang khách hàng | `STAFF`,`MANAGER`,`ADMIN` |
| GET | `/api/v1/customers/{id}` | Lấy chi tiết theo ID | `STAFF`,`MANAGER`,`ADMIN` |
| GET | `/api/v1/customers/phone/{phone}` | Lấy chi tiết theo số điện thoại | `STAFF`,`MANAGER`,`ADMIN` |
| GET | `/api/v1/customers/{id}/purchase-history` | Lịch sử mua hàng của khách | `STAFF`,`MANAGER`,`ADMIN` |
| PUT | `/api/v1/customers/{id}` | Cập nhật khách hàng | `MANAGER`,`ADMIN` |
| DELETE | `/api/v1/customers/{id}` | Xóa khách hàng | `ADMIN` |

## Chi tiết API tiêu biểu

### POST `/api/v1/customers`
- **Request body (`CustomerDTO`)**:
  ```json
  {
    "phone": "0912345678",
    "fullName": "Nguyen Van B",
    "email": "customer@example.com"
  }
  ```
- **Logic**: Trim input, kiểm tra `existsByPhone`/`existsByEmail`, lưu và trả DTO.
- **Response 201**: `CustomerDTO` kèm `id`, `loyaltyPoints` mặc định 0.
- **Lỗi 400**: Thiếu trường bắt buộc / SĐT sai định dạng (`MethodArgumentNotValidException`).
- **Lỗi 409**: SĐT/Email đã tồn tại (`IllegalArgumentException`).

### GET `/api/v1/customers`
- **Query**: `keyword` (tùy chọn), `page`, `size`, `sort` (`PageableDefault size=15, sort=fullName ASC`).
- **Response 200** (`Page<CustomerDTO>`):
  ```json
  {
    "content": [
      {
        "id": 12,
        "phone": "0912345678",
        "fullName": "Nguyen Van B",
        "email": "customer@example.com",
        "loyaltyPoints": 80,
        "createdAt": "2025-03-01T09:00:00",
        "updatedAt": "2025-11-10T10:15:00"
      }
    ],
    "pageable": { "pageNumber": 0, "pageSize": 15 },
    "totalElements": 120,
    "totalPages": 8
  }
  ```

### GET `/api/v1/customers/{id}/purchase-history`
- **Query**: `startDate`, `endDate` (ISO DATE), `status`, phân trang (`size=10`, sort `createdAt DESC`).
- **Response 200** (`CustomerPurchaseHistoryResponseDTO`) chứa tổng quan, danh sách đơn, tổng chi tiêu.
- **Lỗi 404**: không tìm thấy khách (`EntityNotFoundException`).

### PUT `/api/v1/customers/{id}`
- **Request**: giống POST, bắt buộc `loyaltyPoints` không gửi (service bỏ qua).
- **Logic**: Kiểm tra trùng phone/email nếu thay đổi, cập nhật bằng mapper.
- **Lỗi 400**: phone/email đã tồn tại ở khách khác.

### DELETE `/api/v1/customers/{id}`
- **Logic**: kiểm tra tồn tại, thực hiện `customerRepository.deleteById`.
- **Response 204**.
- **Lỗi 404**: khách không tồn tại.

## Điều kiện nghiệp vụ & validation
- SĐT phải đúng regex Việt Nam `^(\+?84|0)\d{9}$`.
- Email (nếu có) phải hợp lệ.
- Không cho trùng `phone` hoặc `email` giữa các khách hàng.
- Điểm loyalty chỉ cập nhật qua `CustomerService.updateLoyaltyPoints` khi thanh toán (không cập nhật trực tiếp từ API).

## Luồng lỗi & thông điệp
| Exception | HTTP | Thông điệp |
| --- | --- | --- |
| `MethodArgumentNotValidException` | 400 | "Dữ liệu đầu vào không hợp lệ" (từ `GlobalExceptionHandler`). |
| `IllegalArgumentException` | 400/409 | "Phone number already exists", "Email already exists". |
| `EntityNotFoundException` | 404 | "Customer not found with id ...". |

## Role/Permission
- Tạo/đọc: `STAFF`, `MANAGER`, `ADMIN`.
- Cập nhật: `MANAGER`, `ADMIN`.
- Xóa: `ADMIN`.

## Quan hệ với chức năng khác
- **Order/Payment**: đơn hàng liên kết `customerId`; thanh toán tự động cộng điểm và có thể thêm customer cho order.
- **Report**: báo cáo top khách hàng & lịch sử mua hàng sử dụng dữ liệu từ đây.
- **Voucher**: loyalty point có thể dùng trong chiến dịch voucher.

## Các tệp liên quan trong BE
- DTO: `CustomerDTO`, `CustomerPurchaseHistoryResponseDTO`, `CustomerPurchaseHistoryItemDTO`, `CustomerAnalyticsDTO`.
- Mapper: `CustomerMapper`, `CustomerPurchaseHistoryMapper`.
- Repository: `CustomerRepository`, `OrderRepository`.
- Service: `CustomerService`, `PaymentService` (gọi update điểm).
