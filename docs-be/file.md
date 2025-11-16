# Chức năng: Quản lý tệp tin (File Storage)

## Vai trò trong hệ thống
- Cho phép quản lý (MANAGER/ADMIN) tải lên, tải xuống và xóa tệp (ví dụ ảnh sản phẩm).
- Cung cấp URL phục vụ hiển thị frontend.
- Đảm bảo quyền truy cập phù hợp (tải lên/xóa cần quyền, tải xuống public).

## Luồng xử lý backend
1. **Upload đơn tệp** (`POST /api/v1/files/upload`):
   - Controller nhận `MultipartFile`, gọi `FileStorageService.storeFile`, tạo URL và trả `FileUploadResponse` @src/main/java/com/giapho/coffee_shop_backend/controller/FileController.java#27-45.
2. **Upload nhiều tệp** (`POST /api/v1/files/upload-multiple`):
   - Lặp qua mảng `MultipartFile`, lưu từng file, trả mảng `FileUploadResponse` @src/main/java/com/giapho/coffee_shop_backend/controller/FileController.java#48-72.
3. **Tải tệp** (`GET /api/v1/files/{fileName}`):
   - Public endpoint, `FileStorageService.loadFileAsResource`, xác định content-type; nếu không biết -> `application/octet-stream` @src/main/java/com/giapho/coffee_shop_backend/controller/FileController.java#74-98.
4. **Xóa tệp** (`DELETE /api/v1/files/{fileName}`):
   - Yêu cầu `MANAGER/ADMIN`, gọi `FileStorageService.deleteFile` @src/main/java/com/giapho/coffee_shop_backend/controller/FileController.java#100-106.
5. **Service**:
   - `FileStorageService.storeFile`: lưu file vào thư mục cấu hình, sinh tên duy nhất, trả fileName.
   - `getFileUrl`: dựng URL công khai.
   - `loadFileAsResource`: kiểm tra tồn tại, trả `Resource`.
   - `deleteFile`: xóa file khỏi hệ thống.
   @src/main/java/com/giapho/coffee_shop_backend/service/FileStorageService.java (xem chi tiết)

## Thành phần liên quan
- **Controller**: `FileController`
- **Service**: `FileStorageService`
- **DTO**: `FileUploadResponse`
- **Config**: `FileStorageProperties` (đường dẫn lưu file), `ApplicationConfig` (bean hỗ trợ)
- **Security**: `SecurityConfig` cho phép GET public, POST/DELETE requires role
- **Exception**: `FileStorageException` (nếu lưu/xóa thất bại)

## Danh sách API
| Method | URL | Mô tả | Role |
| --- | --- | --- | --- |
| POST | `/api/v1/files/upload` | Upload một file | `MANAGER`,`ADMIN` |
| POST | `/api/v1/files/upload-multiple` | Upload nhiều file | `MANAGER`,`ADMIN` |
| GET | `/api/v1/files/{fileName}` | Tải file (public) | Public |
| DELETE | `/api/v1/files/{fileName}` | Xóa file | `MANAGER`,`ADMIN` |

## Chi tiết API

### POST `/api/v1/files/upload`
- **Request**: form-data `file`.
- **Response 200** (`FileUploadResponse`):
  ```json
  {
    "fileName": "uuid.jpg",
    "fileUrl": "https://.../api/v1/files/uuid.jpg",
    "fileSize": 12345,
    "fileType": "image/jpeg",
    "message": "File uploaded successfully"
  }
  ```
- **Lỗi 500**: `FileStorageException` (ví dụ không tạo được thư mục).

### POST `/api/v1/files/upload-multiple`
- **Request**: form-data `files` (array).
- **Response 200**: mảng `FileUploadResponse` cho từng file.

### GET `/api/v1/files/{fileName}`
- **Logic**: xác định content-type (dùng `ServletContext.getMimeType`), fallback `application/octet-stream`.
- **Response**: trả về `Resource` với header `Content-Disposition: inline; filename=...`.
- **Lỗi 404**: `FileStorageService.loadFileAsResource` ném khi file không tồn tại.

### DELETE `/api/v1/files/{fileName}`
- **Response 200**: thông điệp "File deleted successfully: ...".
- **Lỗi 404**: nếu file không tồn tại (service có thể ném `FileStorageException`).

## Điều kiện nghiệp vụ & validation
- Tên file lưu trữ được chuẩn hóa (UUID) để tránh đụng tên; logic implement trong `FileStorageService`.
- Đường dẫn upload do `FileStorageProperties` cấu hình.
- Tải xuống không yêu cầu auth (phục vụ hiển thị ảnh).

## Luồng lỗi & thông điệp
| Exception | HTTP | Message |
| --- | --- | --- |
| `FileStorageException` | 500/400 | "Could not store file ..." / "Could not delete file ..." |
| `IOException` | 200 (log warning) | Không xác định MIME type, fallback `application/octet-stream` |

## Role/Permission
- Upload/Xóa: `hasAnyRole('MANAGER','ADMIN')`.
- Download: public (không yêu cầu token).

## Quan hệ với chức năng khác
- **ProductService**: lưu ảnh sản phẩm (upload file và lấy URL), xóa ảnh khi update/delete.
- **Report**: có thể dùng khi xuất file (Excel) nhưng report controller tự stream.
- **SecurityConfig**: cho phép GET `/api/v1/files/**` public, POST/DELETE require role.

## Các tệp liên quan trong BE
- Controller: `FileController.java`
- Service: `FileStorageService.java`
- DTO: `FileUploadResponse.java`
- Config: `FileStorageProperties.java`, `SecurityConfig.java`
- Exception: `FileStorageException.java`
