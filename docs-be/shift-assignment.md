# Chức năng: Phân công ca làm (Shift Assignment)

## Vai trò trong hệ thống
- Gán nhân viên vào ca cụ thể, quản lý thông tin thời gian, mức lương theo ca.
- Theo dõi trạng thái phân công (PENDING/IN_PROGRESS/COMPLETED/CANCELLED), số đơn, doanh thu, lương thực nhận.
- Cho phép cập nhật, hủy, xóa phân công; tự động tính toán lại chỉ số khi có chấm công hoặc điều chỉnh hiệu suất.

## Luồng xử lý backend
1. **Lấy phân công** (`GET /api/v1/shifts/assignments/{assignmentId}`): trả `ShiftAssignmentResponseDTO` @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftAssignmentController.java#32-36 @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftAssignmentService.java#57-61.
2. **Danh sách theo ca** (`GET /shift/{shiftId}`): trả danh sách phân công của ca @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftAssignmentController.java#38-42 @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftAssignmentService.java#63-68.
3. **Tạo phân công** (`POST`): validate ca (không bị hủy), nhân viên, giờ làm, tránh trùng thời gian; set thông tin tính lương mặc định từ template @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftAssignmentController.java#45-51 @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftAssignmentService.java#70-99.
4. **Cập nhật phân công** (`PUT /{assignmentId}`): chỉ cho chỉnh khi ca chưa LOCKED/DONE; kiểm tra trùng thời gian (bỏ qua chính mình), cập nhật giờ, lương, ghi chú @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftAssignmentController.java#54-61 @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftAssignmentService.java#101-133.
5. **Cập nhật trạng thái** (`PATCH /{assignmentId}/status`): đổi `ShiftAssignmentStatus`, ghi chú kèm theo; không cho hủy phân công đã hoàn thành @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftAssignmentController.java#64-71 @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftAssignmentService.java#135-150.
6. **Xóa phân công** (`DELETE /{assignmentId}`): chỉ khi chưa IN_PROGRESS/COMPLETED @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftAssignmentController.java#74-78 @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftAssignmentService.java#152-158.
7. **Tính toán lại**: `ShiftAssignmentService.recalculateAssignment` được gọi khi có attendance hoặc điều chỉnh; tính lại giờ công, doanh thu, lương, bonus/penalty @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftAssignmentService.java#160-224.
8. **Ngăn chặn trùng ca**: `ensureNoOverlap` và `ensureNoOverlapExcludingCurrent` dùng repository `hasOverlappingAssignment` @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftAssignmentService.java#249-260.

## Thành phần liên quan
- **Controller**: `ShiftAssignmentController` @src/main/java/com/giapho/coffee_shop_backend/controller/ShiftAssignmentController.java#1-81
- **Service**: `ShiftAssignmentService` @src/main/java/com/giapho/coffee_shop_backend/service/shift/ShiftAssignmentService.java#1-296
- **Repository**: `ShiftAssignmentRepository`, `ShiftInstanceRepository`, `UserRepository`, `OrderRepository`, `AttendanceRecordRepository`, `ShiftPerformanceAdjustmentRepository`
- **DTO**: `ShiftAssignmentRequestDTO`, `ShiftAssignmentUpdateRequestDTO`, `ShiftAssignmentStatusUpdateRequestDTO`, `ShiftAssignmentResponseDTO` @src/main/java/com/giapho/coffee_shop_backend/dto/shift/ShiftAssignmentRequestDTO.java#1-36 @src/main/java/com/giapho/coffee_shop_backend/dto/shift/ShiftAssignmentUpdateRequestDTO.java#1-26 @src/main/java/com/giapho/coffee_shop_backend/dto/shift/ShiftAssignmentStatusUpdateRequestDTO.java#1-15 @src/main/java/com/giapho/coffee_shop_backend/dto/shift/ShiftAssignmentResponseDTO.java#1-63
- **Entity**: `ShiftAssignment`, `ShiftInstance`, `User`, `ShiftPerformanceAdjustment`
- **Mapper**: `ShiftAssignmentMapper`
- **Utility**: `SecurityUtil`
- **Security**: tất cả endpoint yêu cầu `hasAnyRole('MANAGER','ADMIN')`.

## Danh sách API
| Method | URL | Mô tả | Role |
| --- | --- | --- | --- |
| GET | `/api/v1/shifts/assignments/{assignmentId}` | Chi tiết phân công | `MANAGER`,`ADMIN` |
| GET | `/api/v1/shifts/assignments/shift/{shiftId}` | Danh sách phân công của ca | `MANAGER`,`ADMIN` |
| POST | `/api/v1/shifts/assignments` | Tạo phân công mới | `MANAGER`,`ADMIN` |
| PUT | `/api/v1/shifts/assignments/{assignmentId}` | Cập nhật phân công | `MANAGER`,`ADMIN` |
| PATCH | `/api/v1/shifts/assignments/{assignmentId}/status` | Đổi trạng thái phân công | `MANAGER`,`ADMIN` |
| DELETE | `/api/v1/shifts/assignments/{assignmentId}` | Xóa phân công | `MANAGER`,`ADMIN` |

## Chi tiết API

### POST `/api/v1/shifts/assignments`
- **Request (`ShiftAssignmentRequestDTO`)** (ví dụ):
  ```json
  {
    "shiftId": 10,
    "userId": 45,
    "plannedStart": "07:00",
    "plannedEnd": "11:00",
    "hourlyRate": 25000,
    "fixedAllowance": 20000,
    "notes": "Phục vụ quầy"
  }
  ```
- **Validation**:
  - Ca tồn tại và chưa hủy.
  - Nhân viên tồn tại.
  - Giờ bắt đầu & kết thúc hợp lệ; không trùng với ca khác của nhân viên trong ngày (`ShiftAssignmentRepository.hasOverlappingAssignment`).
- **Logic**: áp dụng lương mặc định từ template nếu không truyền; tính `plannedMinutes`; lưu assignment với `createdBy/updatedBy`.
- **Response 201**: `ShiftAssignmentResponseDTO` với thông tin lương, doanh thu ban đầu = 0.
- **Lỗi 400**: ca đã hủy, nhân viên trùng ca, giờ sai.
- **Lỗi 404**: ca/nhân viên không tồn tại.

### PUT `/api/v1/shifts/assignments/{assignmentId}`
- **Logic**: không cho chỉnh nếu ca đã LOCKED/DONE; kiểm tra trùng ca (trừ chính mình); cập nhật giờ, lương, ghi chú; recalculation.
- **Response 200**: assignment cập nhật.
- **Lỗi 400**: ca khóa/hoàn thành, giờ trùng, giờ sai.
- **Lỗi 404**: assignment không tồn tại.

### PATCH `/api/v1/shifts/assignments/{assignmentId}/status`
- **Request**: `{ "status": "COMPLETED", "notes": "Hoàn thành ca" }`
- **Logic**: không cho hủy nếu đã COMPLETED; cập nhật notes.
- **Response 200**: assignment mới.

### DELETE `/api/v1/shifts/assignments/{assignmentId}`
- **Logic**: chỉ xóa khi trạng thái không phải IN_PROGRESS/COMPLETED.
- **Response 204**.

## Điều kiện nghiệp vụ & tính toán
- `ShiftAssignmentService.recalculateAssignment` được gọi sau cập nhật/attendance để tính:
  - Giờ thực tế (`actualMinutes`) từ attendance.
  - Đơn và doanh thu của nhân viên trong khoảng giờ phân công (`OrderRepository.findPaidOrdersForStaffBetween`).
  - Lương cơ bản (`hourlyRate` * giờ thực tế + phụ cấp), thưởng/phạt từ `ShiftPerformanceAdjustment`, tổng lương thực nhận.
- Overlap: repository `hasOverlappingAssignment` / `hasOverlappingAssignmentExcludingId` đảm bảo nhân viên không có ca chồng lấn.

## Luồng lỗi & thông điệp
| Exception | HTTP | Message |
| --- | --- | --- |
| `EntityNotFoundException` | 404 | "Không tìm thấy ca...", "Không tìm thấy nhân viên...", "Không tìm thấy phân công..." |
| `IllegalArgumentException` | 400 | "Nhân viên đã có ca khác trong khoảng thời gian này", "Giờ bắt đầu và kết thúc không được để trống", ... |
| `IllegalStateException` | 400 | "Không thể phân công nhân viên vào ca đã bị hủy", "Không thể cập nhật phân công khi ca đã khóa", "Không thể xóa phân công đang thực hiện..." |

## Role/Permission
- Tất cả API: `hasAnyRole('MANAGER','ADMIN')`.

## Quan hệ với chức năng khác
- **Shift Instance**: phân công thuộc về một ca cụ thể; đổi trạng thái ca có thể hạn chế chỉnh sửa assignment.
- **Attendance**: check-in/out gắn với assignment; mỗi assignment có thể có nhiều bản ghi attendance.
- **Shift Performance Adjustment**: thưởng/phạt cộng vào lương assignment.
- **Payroll**: bảng lương tổng hợp dựa trên assignment (giờ, doanh thu, thưởng/phạt, lương net).
- **Order**: doanh thu assignment dựa trên đơn `PAID` của nhân viên trong khoảng thời gian ca.

## Các tệp liên quan trong BE
- Controller: `ShiftAssignmentController.java`
- Service: `ShiftAssignmentService.java`
- DTO: `ShiftAssignmentRequestDTO.java`, `ShiftAssignmentUpdateRequestDTO.java`, `ShiftAssignmentStatusUpdateRequestDTO.java`, `ShiftAssignmentResponseDTO.java`
- Repository & Entity: `ShiftAssignmentRepository.java`, `ShiftInstanceRepository.java`, `AttendanceRecordRepository.java`, `OrderRepository.java`, `ShiftPerformanceAdjustmentRepository.java`, `ShiftAssignment.java`, `ShiftInstance.java`
- Mapper: `ShiftAssignmentMapper.java`
- Utility: `SecurityUtil.java`
