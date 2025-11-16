# Chức năng: Điều chỉnh hiệu suất ca (Shift Performance Adjustment)

## Vai trò trong hệ thống
- Ghi nhận thưởng/phạt (bonus/penalty) cho phân công ca dựa trên hiệu suất.
- Cho phép quản lý tạo, thu hồi (revoke) và xóa điều chỉnh.
- Tự động cập nhật lương thực nhận của phân công sau mỗi điều chỉnh.

## Luồng xử lý backend
1. **Xem điều chỉnh** (`GET /api/v1/shifts/adjustments/{adjustmentId}`): trả `ShiftPerformanceAdjustmentResponseDTO` @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftPerformanceAdjustmentController.java#30-35 @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftPerformanceAdjustmentService.java#35-38.
2. **Danh sách theo phân công** (`GET /assignment/{assignmentId}`): trả danh sách điều chỉnh của một assignment @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftPerformanceAdjustmentController.java#37-43 @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftPerformanceAdjustmentService.java#40-44.
3. **Tạo điều chỉnh** (`POST`): nhận `ShiftPerformanceAdjustmentRequestDTO`, kiểm tra assignment tồn tại, map DTO -> entity, lưu và gọi `ShiftAssignmentService.recalculateAssignment` @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftPerformanceAdjustmentController.java#45-51 @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftPerformanceAdjustmentService.java#47-61.
4. **Thu hồi điều chỉnh** (`POST /{adjustmentId}/revoke`): đánh dấu `revoked=true`, ghi thời gian người thu hồi, lưu lý do nếu có, cập nhật assignment @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftPerformanceAdjustmentController.java#53-59 @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftPerformanceAdjustmentService.java#63-81.
5. **Xóa điều chỉnh** (`DELETE /{adjustmentId}`): xóa entity và tái tính assignment @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftPerformanceAdjustmentController.java#62-65 @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftPerformanceAdjustmentService.java#83-88.

## Thành phần liên quan
- **Controller**: `ShiftPerformanceAdjustmentController` @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftPerformanceAdjustmentController.java#1-68
- **Service**: `ShiftPerformanceAdjustmentService` @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftPerformanceAdjustmentService.java#1-100
- **Repository**: `ShiftPerformanceAdjustmentRepository`, `ShiftAssignmentRepository`
- **DTO**: `ShiftPerformanceAdjustmentRequestDTO`, `ShiftPerformanceAdjustmentRevokeRequestDTO`, `ShiftPerformanceAdjustmentResponseDTO` @src/main/java/com/giapho/coffee_shop_backend/dto/shift/ShiftPerformanceAdjustmentRequestDTO.java#1-24 @src/main/java/com/giapho/coffee_shop_backend/dto/shift/ShiftPerformanceAdjustmentRevokeRequestDTO.java#1-15 @src/main/java/com/giapho/coffee_shop_backend/dto/shift/ShiftPerformanceAdjustmentResponseDTO.java#1-45
- **Entity**: `ShiftPerformanceAdjustment`, `ShiftAssignment`
- **Mapper**: `ShiftPerformanceAdjustmentMapper`
- **Security**: toàn bộ endpoint yêu cầu `hasAnyRole('MANAGER','ADMIN')`.

## Danh sách API
| Method | URL | Mô tả | Role |
| --- | --- | --- | --- |
| GET | `/api/v1/shifts/adjustments/{adjustmentId}` | Chi tiết điều chỉnh | `MANAGER`,`ADMIN` |
| GET | `/api/v1/shifts/adjustments/assignment/{assignmentId}` | Danh sách điều chỉnh của phân công | `MANAGER`,`ADMIN` |
| POST | `/api/v1/shifts/adjustments` | Tạo điều chỉnh mới | `MANAGER`,`ADMIN` |
| POST | `/api/v1/shifts/adjustments/{adjustmentId}/revoke` | Thu hồi điều chỉnh | `MANAGER`,`ADMIN` |
| DELETE | `/api/v1/shifts/adjustments/{adjustmentId}` | Xóa điều chỉnh | `MANAGER`,`ADMIN` |

## Chi tiết API

### POST `/api/v1/shifts/adjustments`
- **Request (`ShiftPerformanceAdjustmentRequestDTO`)** (ví dụ):
  ```json
  {
    "assignmentId": 120,
    "type": "BONUS",
    "amount": 50000,
    "reason": "Khách hàng khen ngợi"
  }
  ```
- **Validation**: assignment tồn tại; type hợp lệ (`BONUS`/`PENALTY`); amount > 0.
- **Logic**: map DTO, set `revoked=false`, set actor, lưu; gọi `recalculateAssignment` để cập nhật lương.
- **Response 201**: `ShiftPerformanceAdjustmentResponseDTO` với `revoked=false`.
- **Lỗi 404**: assignment không tồn tại.

### POST `/api/v1/shifts/adjustments/{adjustmentId}/revoke`
- **Request (`ShiftPerformanceAdjustmentRevokeRequestDTO`)**: `{ "reason": "Nhập sai" }` (optional).
- **Logic**: nếu đã `revoked` -> lỗi; set `revoked=true`, `revokedAt`, `revokedBy`, lưu lý do nếu có; tái tính assignment.
- **Response 200**: DTO với `revoked=true`.
- **Lỗi 400**: điều chỉnh đã thu hồi trước đó.
- **Lỗi 404**: adjustment không tồn tại.

### DELETE `/api/v1/shifts/adjustments/{adjustmentId}`
- **Logic**: xóa record, tái tính assignment.
- **Response 204**.

## Điều kiện nghiệp vụ
- Mỗi adjustment gắn với 1 assignment; khi thay đổi (create/revoke/delete) phải cập nhật payroll assignment.
- Thưởng/phạt bị thu hồi không bị xóa, vẫn giữ lịch sử (trạng thái `revoked=true`).
- Actor (`createdBy/updatedBy/revokedBy`) lấy từ `SecurityUtil` hoặc `SYSTEM`.

## Luồng lỗi & thông điệp
| Exception | HTTP | Message |
| --- | --- | --- |
| `EntityNotFoundException` | 404 | "Không tìm thấy phân công ...", "Không tìm thấy điều chỉnh ..." |
| `IllegalStateException` | 400 | "Điều chỉnh đã bị thu hồi trước đó" |
| `IllegalArgumentException` | 400 | (validate amount/type nếu mapper/service kiểm tra thêm) |

## Role/Permission
- Tất cả API: `hasAnyRole('MANAGER','ADMIN')`.

## Quan hệ với chức năng khác
- **Shift Assignment**: thưởng/phạt trực tiếp ảnh hưởng tính lương assignment.
- **Payroll**: bảng lương tổng hợp đọc dữ liệu bonus/penalty từ assignment.
- **Audit**: service log tạo/thu hồi/xóa adjustment.

## Các tệp liên quan trong BE
- Controller: `ShiftPerformanceAdjustmentController.java`
- Service: `ShiftPerformanceAdjustmentService.java`, `ShiftAssignmentService.java`
- DTO: `ShiftPerformanceAdjustmentRequestDTO.java`, `ShiftPerformanceAdjustmentRevokeRequestDTO.java`, `ShiftPerformanceAdjustmentResponseDTO.java`
- Repository & Entity: `ShiftPerformanceAdjustmentRepository.java`, `ShiftAssignmentRepository.java`, `ShiftPerformanceAdjustment.java`
- Mapper: `ShiftPerformanceAdjustmentMapper.java`
- Utility: `SecurityUtil.java`
