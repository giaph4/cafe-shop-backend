# Chức năng: Mẫu ca làm (Shift Template)

## Vai trò trong hệ thống
- Định nghĩa khung ca chuẩn (giờ bắt đầu/kết thúc, lương theo giờ, phụ cấp) để tái sử dụng khi tạo ca cụ thể.
- Cho phép quản lý tạo, chỉnh sửa, xóa template; dùng làm mặc định khi phân công nhân viên.
- Hỗ trợ việc chuẩn hóa dữ liệu ca, giảm lỗi khi nhập thông tin thủ công.

## Luồng xử lý backend
1. **Danh sách template** (`GET /api/v1/shifts/templates`): phân trang (`size=20`, có thể custom), trả `Page<ShiftTemplateResponseDTO>` @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftTemplateController.java#31-36 @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftTemplateService.java#29-33.
2. **Xem chi tiết** (`GET /{id}`): lấy template theo ID, ném 404 nếu không thấy @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftTemplateController.java#38-41 @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftTemplateService.java#35-39.
3. **Tạo template** (`POST`): validate thời gian & mức lương, kiểm tra trùng tên (case-insensitive), lưu entity @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftTemplateController.java#43-47 @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftTemplateService.java#41-55.
4. **Cập nhật template** (`PUT /{id}`): kiểm tra trùng tên (khác ID), validate, cập nhật dữ liệu, ghi nhận người sửa @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftTemplateController.java#49-53 @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftTemplateService.java#57-73.
5. **Xóa template** (`DELETE /{id}`): xác minh tồn tại, xóa khỏi DB @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftTemplateController.java#56-59 @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftTemplateService.java#75-78.
6. **Áp dụng vào phân công**: khi tạo `ShiftAssignment`, nếu template có `defaultHourlyRate`/`defaultFixedAllowance` thì được dùng để set mặc định @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftAssignmentService.java#225-235.

## Thành phần liên quan
- **Controller**: `ShiftTemplateController` @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftTemplateController.java#1-62
- **Service**: `ShiftTemplateService` @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftTemplateService.java#1-103
- **Repository**: `ShiftTemplateRepository`
- **DTO**: `ShiftTemplateRequestDTO`, `ShiftTemplateResponseDTO` @src/main/java/com/giapho/coffee_shop_backend/dto/shift/ShiftTemplateRequestDTO.java#1-28 @src/main/java/com/giapho/coffee_shop_backend/dto/shift/ShiftTemplateResponseDTO.java#1-39
- **Entity**: `ShiftTemplate`
- **Mapper**: `ShiftTemplateMapper`
- **Utility**: `SecurityUtil` – lấy actor gán `createdBy/updatedBy`
- **Security**: toàn bộ endpoint yêu cầu `hasAnyRole('MANAGER','ADMIN')`.

## Danh sách API
| Method | URL | Mô tả | Role |
| --- | --- | --- | --- |
| GET | `/api/v1/shifts/templates` | Danh sách template (phân trang) | `MANAGER`,`ADMIN` |
| GET | `/api/v1/shifts/templates/{id}` | Chi tiết template | `MANAGER`,`ADMIN` |
| POST | `/api/v1/shifts/templates` | Tạo template mới | `MANAGER`,`ADMIN` |
| PUT | `/api/v1/shifts/templates/{id}` | Cập nhật template | `MANAGER`,`ADMIN` |
| DELETE | `/api/v1/shifts/templates/{id}` | Xóa template | `MANAGER`,`ADMIN` |

Không có API nào khác trong controller; danh sách trên đầy đủ.

## Chi tiết API

### POST `/api/v1/shifts/templates`
- **Request (`ShiftTemplateRequestDTO`)**:
  ```json
  {
    "name": "Ca sáng chuẩn",
    "startTime": "07:00",
    "endTime": "11:00",
    "defaultHourlyRate": 25000,
    "defaultFixedAllowance": 20000,
    "description": "Ca phục vụ buổi sáng"
  }
  ```
- **Validation**:
  - `startTime` và `endTime` không rỗng, `startTime < endTime`.
  - `defaultHourlyRate`, `defaultFixedAllowance` nếu có phải ≥ 0.
  - `name` không trùng (case-insensitive).
- **Logic**: gán `createdBy/updatedBy` theo user hiện tại (hoặc `SYSTEM`), lưu entity.
- **Response 201**: `ShiftTemplateResponseDTO` gồm id, thời gian, mức lương mặc định.
- **Lỗi 400**: thời gian không hợp lệ, lương âm, tên trùng.
- **Lỗi 404**: không xuất hiện trong tạo.

### PUT `/api/v1/shifts/templates/{id}`
- **Logic**: giống tạo nhưng cập nhật entity hiện có; tên mới nếu khác phải kiểm tra trùng.
- **Response 200**: template cập nhật.
- **Lỗi 404**: ID không tồn tại.
- **Lỗi 400**: tên trùng, thời gian không hợp lệ, lương âm.

### DELETE `/api/v1/shifts/templates/{id}`
- **Response 204**: xóa thành công.
- **Lỗi 404**: template không tồn tại.

## Điều kiện nghiệp vụ & validation
- Tên template duy nhất (ignore case).
- Giờ bắt đầu phải trước giờ kết thúc.
- Mức lương/ phụ cấp mặc định không được âm.
- Thông tin `createdBy/updatedBy` được cập nhật từ `SecurityUtil`.

## Luồng lỗi & thông điệp
| Exception | HTTP | Message |
| --- | --- | --- |
| `EntityNotFoundException` | 404 | "Không tìm thấy template ca ..." |
| `IllegalArgumentException` | 400 | "Tên ca đã tồn tại", "Giờ bắt đầu phải trước giờ kết thúc", "Lương theo giờ không được âm" |

## Role/Permission
- Tất cả API: `hasAnyRole('MANAGER','ADMIN')` (được áp dụng ở class-level `@PreAuthorize`).

## Quan hệ với chức năng khác
- **ShiftInstance**: khi tạo ca, có thể dựa trên template.
- **ShiftAssignment**: áp dụng `defaultHourlyRate` và `defaultFixedAllowance` từ template.
- **Payroll**: template ảnh hưởng lương cơ bản khi tổng hợp bảng lương.

## Các tệp liên quan trong BE
- Controller: `ShiftTemplateController.java`
- Service: `ShiftTemplateService.java`
- DTO: `ShiftTemplateRequestDTO.java`, `ShiftTemplateResponseDTO.java`
- Entity & Repository: `ShiftTemplate.java`, `ShiftTemplateRepository.java`
- Mapper: `ShiftTemplateMapper.java`
- Utility: `SecurityUtil.java`
