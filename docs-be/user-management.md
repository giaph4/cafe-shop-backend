# Chức năng: Quản lý người dùng nội bộ

## Vai trò trong hệ thống
- Quản trị tài khoản nhân sự nội bộ (ADMIN/MANAGER/STAFF), bao gồm vai trò, trạng thái, thông tin cá nhân.
- Cho phép người dùng tự cập nhật hồ sơ và đổi mật khẩu khi đã đăng nhập.
- Cung cấp danh sách roles phục vụ UI cấu hình phân quyền.

## Luồng xử lý backend
1. **Lấy danh sách role**: `UserController.getAllRoles` trả dữ liệu từ `RoleRepository` qua `UserService.getAllRoles` để UI phân quyền @src/main/java/com/giapho/coffee_shop_backend/controller/UserController.java#29-34 @src/main/java/com/giapho/coffee_shop_backend/service/UserService.java#38-44.
2. **Danh sách tài khoản**: `UserController.getAllUsers` gọi `UserService.getAllUsers`, map entity sang DTO, phân trang/sắp xếp theo username @src/main/java/com/giapho/coffee_shop_backend/controller/UserController.java#36-43 @src/main/java/com/giapho/coffee_shop_backend/service/UserService.java#45-49.
3. **Xem chi tiết**: `UserController.getUserById` chỉ cho phép chính chủ hoặc ADMIN truy cập, service ném `EntityNotFoundException` nếu không có @src/main/java/com/giapho/coffee_shop_backend/controller/UserController.java#45-50 @src/main/java/com/giapho/coffee_shop_backend/service/UserService.java#51-56.
4. **Cập nhật hồ sơ**: `UserController.updateUser` kiểm tra quyền chính chủ/ADMIN, `UserService.updateUser` validate unique phone/email, bắt buộc ít nhất một role, xử lý avatar/address trimming và cập nhật roles @src/main/java/com/giapho/coffee_shop_backend/controller/UserController.java#52-59 @src/main/java/com/giapho/coffee_shop_backend/service/UserService.java#58-105.
5. **Đổi mật khẩu**: `UserController.changePassword` yêu cầu đăng nhập, `UserService.changePassword` xác thực mật khẩu hiện tại, so khớp mật khẩu mới/confirm, không cho trùng mật khẩu cũ, lưu mật khẩu đã mã hóa @src/main/java/com/giapho/coffee_shop_backend/controller/UserController.java#62-68 @src/main/java/com/giapho/coffee_shop_backend/service/UserService.java#107-134.

## Thành phần liên quan
- **Controller**: `UserController` @src/main/java/com/giapho/coffee_shop_backend/controller/UserController.java#1-72
- **Service**: `UserService` @src/main/java/com/giapho/coffee_shop_backend/service/UserService.java#1-143
- **Repository**: `UserRepository`, `RoleRepository` @src/main/java/com/giapho/coffee_shop_backend/domain/repository/UserRepository.java#1-46
- **DTO**: `UserResponseDTO`, `UserUpdateRequestDTO`, `ChangePasswordRequestDTO`, `RoleDTO` @src/main/java/com/giapho/coffee_shop_backend/dto/UserUpdateRequestDTO.java#1-42 @src/main/java/com/giapho/coffee_shop_backend/dto/ChangePasswordRequestDTO.java#1-19 @src/main/java/com/giapho/coffee_shop_backend/dto/RoleDTO.java#1-13
- **Entity**: `User`, `Role` @src/main/java/com/giapho/coffee_shop_backend/domain/entity/User.java#1-125
- **Validation**: Annotation trên DTO (`@NotBlank`, `@Pattern`, `@Email`, `@NotEmpty`, `@URL`, `@Size`).
- **Exception & Logging**: `UserService` ném `IllegalArgumentException`/`EntityNotFoundException`; `GlobalExceptionHandler` chuẩn hóa response @src/main/java/com/giapho/coffee_shop_backend/exception/GlobalExceptionHandler.java#1-296.

## Danh sách API
| Method | URL | Mô tả | Role yêu cầu |
| --- | --- | --- | --- |
| GET | `/api/v1/users/roles` | Lấy danh sách role hiện có | `ADMIN` |
| GET | `/api/v1/users` | Danh sách tài khoản (phân trang) | `ADMIN` |
| GET | `/api/v1/users/{id}` | Xem chi tiết tài khoản | Chính chủ hoặc `ADMIN` |
| PUT | `/api/v1/users/{id}` | Cập nhật thông tin người dùng | Chính chủ hoặc `ADMIN` |
| POST | `/api/v1/users/change-password` | Đổi mật khẩu cá nhân | Bất kỳ user đăng nhập |

Không có API thừa/ẩn khác trong module.

## Chi tiết API & dữ liệu

### GET `/api/v1/users/roles`
- **Controller**: `UserController.getAllRoles`
- **Response 200**: dạng `List<RoleDTO>`
  ```json
  [
    { "id": 1, "name": "ROLE_ADMIN" },
    { "id": 2, "name": "ROLE_MANAGER" },
    { "id": 3, "name": "ROLE_STAFF" }
  ]
  ```
- **Lỗi 401**: thiếu/invalid JWT.
- **Lỗi 403**: caller không phải ADMIN.

### GET `/api/v1/users`
- **Query mặc định**: `size=15`, `sort=username,asc`.
- **Response 200** (`Page<UserResponseDTO>` – xem cấu trúc DTO trong mapper):
  ```json
  {
    "content": [
      {
        "id": 10,
        "username": "staff001",
        "fullName": "Nguyen Van A",
        "phone": "0912345678",
        "email": "staff001@coffee.vn",
        "status": "ACTIVE",
        "avatarUrl": null,
        "address": "123 Nguyen Trai",
        "roles": ["ROLE_STAFF"],
        "createdAt": "2025-06-01T08:00:00",
        "updatedAt": "2025-06-10T12:00:00"
      }
    ],
    "pageable": { "pageNumber": 0, "pageSize": 15 },
    "totalElements": 42,
    "totalPages": 3
  }
  ```
- **Lỗi 400**: tham số phân trang sai định dạng -> `GlobalExceptionHandler` trả về.
- **Lỗi 401/403**: như trên.

### GET `/api/v1/users/{id}`
- **Điều kiện**: `@PreAuthorize` cho phép chính chủ (so sánh với `authentication.principal.id`) hoặc ADMIN.
- **Response 200**: `UserResponseDTO`.
- **Lỗi 404**: `EntityNotFoundException` khi không tìm thấy ID.
- **Lỗi 403**: caller không đủ quyền.

### PUT `/api/v1/users/{id}`
- **Request body (`UserUpdateRequestDTO`)**:
  ```json
  {
    "fullName": "Nguyen Van A",
    "phone": "0912345678",
    "email": "staff001@coffee.vn",
    "status": "ACTIVE",
    "roleIds": [3],
    "avatarUrl": "https://cdn.example.com/avatar.jpg",
    "address": "123 Nguyen Trai",
    "removeAvatar": false
  }
  ```
- **Validation**:
  - `fullName`, `phone`, `status` bắt buộc.
  - `phone` theo regex VN; `email` chuẩn RFC; `roleIds` không rỗng.
  - `avatarUrl` phải là URL hợp lệ ≤255 ký tự; `address` ≤255 ký tự.
- **Service logic**:
  1. Kiểm tra tồn tại user.
  2. Check unique phone/email nếu thay đổi.
  3. Tải roles theo `roleIds`, bắt buộc không rỗng.
  4. Mapper cập nhật trường cho entity, xử lý `avatarUrl`/`address` (trim, null nếu rỗng) và `removeAvatar`.
  5. Lưu entity và trả `UserResponseDTO`.
- **Response 200**: DTO cập nhật.
- **Lỗi 400**: trùng phone/email (`IllegalArgumentException`), validation fail.
- **Lỗi 404**: không tìm thấy user hoặc role bất kỳ.
- **Lỗi 403**: caller không phải chính chủ/ADMIN.

#### Upload/Thay đổi avatar
- **Bước 1**: gọi `POST /api/v1/files/upload` (xem [file.md](file.md)) để tải ảnh, nhận `fileUrl` trả về.
- **Bước 2**: gửi `PUT /api/v1/users/{id}` với `avatarUrl = fileUrl` để cập nhật avatar.
- **Xóa avatar**: set `removeAvatar = true` hoặc gửi `avatarUrl = null` → service sẽ xóa URL hiện tại.
- **Validation**: URL dài ≤255 ký tự và đúng định dạng; mọi thao tác upload yêu cầu quyền `MANAGER/ADMIN` (FileController), còn cập nhật avatar thì chính chủ hoặc ADMIN.

### POST `/api/v1/users/change-password`
- **Request (`ChangePasswordRequestDTO`)**:
  ```json
  {
    "currentPassword": "Current@123",
    "newPassword": "NewPass@2025",
    "confirmationPassword": "NewPass@2025"
  }
  ```
- **Service logic**:
  1. Lấy user hiện tại từ `SecurityContext`.
  2. So khớp mật khẩu hiện tại (`PasswordEncoder.matches`).
  3. Kiểm tra mật khẩu mới và confirm giống nhau, không trùng mật khẩu cũ.
  4. Mã hóa và lưu.
- **Response 200**: chuỗi `
