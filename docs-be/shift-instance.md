# Chức năng: Ca làm cụ thể (Shift Instance)

## Vai trò trong hệ thống
- Tạo và quản lý các ca làm thực tế dựa trên mẫu ca.
- Cho phép lọc danh sách ca theo ngày, trạng thái, cập nhật giờ, ghi chú, trạng thái.
- Liên kết với phân công nhân viên (`ShiftAssignment`) và chấm công (`Attendance`).

## Luồng xử lý backend
1. **Danh sách ca** (`GET /api/v1/shifts/instances`): lọc theo `from`, `to` (ISO DATE), `status`, phân trang (size=20) @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftInstanceController.java#39-47 @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftInstanceService.java#40-63.
2. **Chi tiết ca** (`GET /{id}`): lấy thông tin đầy đủ, bao gồm template liên kết, status @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftInstanceController.java#50-53 @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftInstanceService.java#65-69.
3. **Tạo ca** (`POST`): từ template + danh sách ngày, validate thời gian, tránh trùng template-date, lưu ca mới @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftInstanceController.java#55-61 @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftInstanceService.java#71-102.
4. **Cập nhật ca** (`PUT /{id}`): chỉnh sửa ca nếu chưa `LOCKED/DONE`, validate giờ, cập nhật ghi chú @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftInstanceController.java#63-69 @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftInstanceService.java#104-119.
5. **Đổi trạng thái** (`PATCH /{id}/status`): cập nhật `ShiftStatus`, không cho hủy nếu có phân công, đặt `lockedAt` khi chuyển LOCKED @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftInstanceController.java#72-78 @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftInstanceService.java#121-138.
6. **Xóa ca** (`DELETE /{id}`): chỉ cho xóa khi chưa có phân công nhân viên @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftInstanceController.java#81-84 @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftInstanceService.java#141-147.
7. **Liên kết**: ca chứa danh sách phân công (`ShiftAssignment`), chấm công, được dùng khi tạo bảng lương.

## Thành phần liên quan
- **Controller**: `ShiftInstanceController` @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftInstanceController.java#1-87
- **Service**: `ShiftInstanceService` @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftInstanceService.java#1-163
- **Repository**: `ShiftInstanceRepository`, `ShiftTemplateRepository`, `ShiftAssignmentRepository`
- **DTO**: `ShiftInstanceCreateRequestDTO`, `ShiftInstanceResponseDTO`, `ShiftInstanceStatusUpdateRequestDTO` @src/main/java/com/giapho/coffee_shop_backend/dto/shift/ShiftInstanceCreateRequestDTO.java#1-30 @src/main/java/com/giapho/coffee_shop_backend/dto/shift/ShiftInstanceResponseDTO.java#1-49 @src/main/java/com/giapho/coffee_shop_backend/dto/shift/ShiftInstanceStatusUpdateRequestDTO.java#1-16
- **Entity**: `ShiftInstance`, `ShiftTemplate`, `ShiftAssignment`
- **Mapper**: `ShiftInstanceMapper`
- **Security**: toàn bộ endpoint yêu cầu `hasAnyRole('MANAGER','ADMIN')` (class-level `@PreAuthorize`).

## Danh sách API
| Method | URL | Mô tả | Role |
| --- | --- | --- | --- |
| GET | `/api/v1/shifts/instances` | Lọc danh sách ca theo ngày/trạng thái | `MANAGER`,`ADMIN` |
| GET | `/api/v1/shifts/instances/{id}` | Chi tiết ca | `MANAGER`,`ADMIN` |
| POST | `/api/v1/shifts/instances` | Tạo ca mới (1 hoặc nhiều ngày) | `MANAGER`,`ADMIN` |
| PUT | `/api/v1/shifts/instances/{id}` | Cập nhật ca | `MANAGER`,`ADMIN` |
| PATCH | `/api/v1/shifts/instances/{id}/status` | Đổi trạng thái ca | `MANAGER`,`ADMIN` |
| DELETE | `/api/v1/shifts/instances/{id}` | Xóa ca | `MANAGER`,`ADMIN` |

## Chi tiết API

### POST `/api/v1/shifts/instances`
- **Request (`ShiftInstanceCreateRequestDTO`)** (ví dụ):
  ```json
  {
    "templateId": 3,
    "shiftDate": "2025-11-20",
    "dates": ["2025-11-20", "2025-11-21"],
    "startTime": "07:00",
    "endTime": "11:00",
    "notes": "Ca buổi sáng"
  }
  ```
- Nếu `dates` rỗng -> dùng `shiftDate`.
- **Logic**: kiểm tra template tồn tại, validate giờ, bỏ qua các ngày đã có ca cùng template.
- **Response 201**: danh sách `ShiftInstanceResponseDTO` được tạo.
- **Lỗi 400**: giờ không hợp lệ.
- **Lỗi 404**: template không tồn tại.

### PUT `/api/v1/shifts/instances/{id}`
- **Logic**: chỉ cho chỉnh khi trạng thái chưa LOCKED/DONE; validate giờ; mapper cập nhật; gán `updatedBy` bằng user hiện tại.
- **Response 200**: ca cập nhật.
- **Lỗi 400**: ca đã khóa/hoàn thành.
- **Lỗi 404**: id không tồn tại.

### PATCH `/api/v1/shifts/instances/{id}/status`
- **Request**:
  ```json
  { "status": "LOCKED", "notes": "Đã chốt nhân sự" }
  ```
- **Logic**: đặt `lockedAt` khi chuyển LOCKED; không cho hủy (`CANCELLED`) nếu đã có assignment.
- **Response 200**: `ShiftInstanceResponseDTO` mới.
- **Lỗi 400**: hủy ca có phân công.

### DELETE `/api/v1/shifts/instances/{id}`
- **Logic**: nếu ca đã có assignment -> lỗi; ngược lại xóa.
- **Response 204**.

## Điều kiện nghiệp vụ & validation
- Mỗi template + ngày chỉ có một ca (service bỏ qua ngày trùng khi tạo nhiều ca cùng lúc).
- Giờ bắt đầu < giờ kết thúc.
- Không cho chỉnh sửa ca đã LOCKED/DONE.
- Không cho hủy/xóa ca khi đã có nhân viên được phân.

## Luồng lỗi & thông điệp
| Exception | HTTP | Message |
| --- | --- | --- |
| `EntityNotFoundException` | 404 | "Không tìm thấy ca ..." / "Không tìm thấy template ..." |
| `IllegalArgumentException` | 400 | "Giờ bắt đầu và kết thúc không được để trống", "Giờ bắt đầu phải trước giờ kết thúc" |
| `IllegalStateException` | 400 | "Không thể chỉnh sửa ca đã khóa hoặc hoàn thành", "Không thể hủy ca vì đã có nhân viên" |

## Role/Permission
- Tất cả API: `hasAnyRole('MANAGER','ADMIN')`.

## Quan hệ với chức năng khác
- **Shift Template**: dùng làm cơ sở khởi tạo ca.
- **Shift Assignment**: ca chứa danh sách phân công; khi xóa/hủy ca phải đảm bảo không còn phân công.
- **Attendance**: chấm công gắn với phân công thuộc ca; khi khóa ca, việc chỉnh sửa phân công/attendance bị hạn chế.
- **Payroll**: dữ liệu ca (giờ/áp dụng template) ảnh hưởng tính lương.

## Các tệp liên quan trong BE
- Controller: `ShiftInstanceController.java`
- Service: `ShiftInstanceService.java`
- DTO: `ShiftInstanceCreateRequestDTO.java`, `ShiftInstanceResponseDTO.java`, `ShiftInstanceStatusUpdateRequestDTO.java`
- Repository & Entity: `ShiftInstanceRepository.java`, `ShiftTemplateRepository.java`, `ShiftAssignmentRepository.java`, `ShiftInstance.java`, `ShiftAssignment.java`
- Mapper: `ShiftInstanceMapper.java`
- Utility: `SecurityUtil.java`
