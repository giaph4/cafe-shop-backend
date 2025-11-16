# Chức năng: Chấm công ca làm (Attendance)

## Vai trò trong hệ thống
- Ghi nhận thời gian check-in/ check-out của nhân viên cho từng phân công ca.
- Tự động tính phút đi trễ/ về sớm, cập nhật trạng thái phân công.
- Cung cấp API cho quản lý truy vấn lịch sử chấm công theo phân công hoặc theo ca.
- Ghi nhật ký người thao tác khi điều chỉnh (Audit thông qua `AttendanceService`).

## Luồng xử lý backend
1. **Chấm công check-in** (`POST /api/v1/attendance/check-in`): controller nhận `AttendanceCheckRequestDTO`, `AttendanceService.checkIn` xác định phân công, kiểm tra trạng thái ca/ phân công, đảm bảo chưa check-out, tạo bản ghi attendance @src/main/java/com/giapho/coffee_shop_backend/controller/AttendanceController.java#27-34 @src/main/java/com/giapho/coffee_shop_backend/service/shift/AttendanceService.java#41-65.
2. **Chấm công check-out** (`POST /api/v1/attendance/check-out`): tương tự check-in nhưng tìm bản ghi chưa check-out, cập nhật thời gian và tính phút về sớm @src/main/java/com/giapho/coffee_shop_backend/controller/AttendanceController.java#36-42 @src/main/java/com/giapho/coffee_shop_backend/service/shift/AttendanceService.java#67-88.
3. **Danh sách theo phân công** (`GET /api/v1/attendance/assignment/{assignmentId}`): trả toàn bộ bản ghi chấm công của một assignment @src/main/java/com/giapho/coffee_shop_backend/controller/AttendanceController.java#45-52 @src/main/java/com/giapho/coffee_shop_backend/service/shift/AttendanceService.java#90-95.
4. **Danh sách theo ca** (`GET /shift/{shiftId}`): trả các bản ghi theo ca (tổng hợp nhiều phân công) @src/main/java/com/giapho/coffee_shop_backend/controller/AttendanceController.java#54-60 @src/main/java/com/giapho/coffee_shop_backend/service/shift/AttendanceService.java#97-101.
5. **Tính toán & cập nhật phân công**: sau mỗi check-in/out, `AttendanceService` gọi `ShiftAssignmentService.recalculateAssignment` để cập nhật trạng thái, thời lượng thực tế @src/main/java/com/giapho/coffee_shop_backend/service/shift/AttendanceService.java#62-63,#84-85.

## Thành phần liên quan
- **Controller**: `AttendanceController` @src/main/java/com/giapho/coffee_shop_backend/controller/AttendanceController.java#1-63
- **Service**: `AttendanceService` @src/main/java/com/giapho/coffee_shop_backend/service/shift/AttendanceService.java#1-178
- **Repository**: `AttendanceRecordRepository`, `ShiftAssignmentRepository`, `UserRepository`
- **DTO**: `AttendanceCheckRequestDTO`, `AttendanceRecordResponseDTO` @src/main/java/com/giapho/coffee_shop_backend/dto/shift/AttendanceCheckRequestDTO.java#1-21 @src/main/java/com/giapho/coffee_shop_backend/dto/shift/AttendanceRecordResponseDTO.java#1-37
- **Entity**: `AttendanceRecord`, `ShiftAssignment`, `ShiftInstance`
- **Mapper**: `AttendanceRecordMapper`
- **Enrich services**: `ShiftAssignmentService`, `SecurityUtil` (lấy user hiện tại)
- **Security**: tất cả endpoint yêu cầu `hasAnyRole('STAFF','MANAGER','ADMIN')` đối với check-in/out; truy vấn yêu cầu `MANAGER` hoặc `ADMIN`.

## Danh sách API
| Method | URL | Mô tả | Role |
| --- | --- | --- | --- |
| POST | `/api/v1/attendance/check-in` | Chấm công vào  | `STAFF`,`MANAGER`,`ADMIN` |
| POST | `/api/v1/attendance/check-out` | Chấm công ra | `STAFF`,`MANAGER`,`ADMIN` |
| GET | `/api/v1/attendance/assignment/{assignmentId}` | Xem chấm công theo phân công | `MANAGER`,`ADMIN` |
| GET | `/api/v1/attendance/shift/{shiftId}` | Xem chấm công theo ca | `MANAGER`,`ADMIN` |

## Chi tiết API

### POST `/api/v1/attendance/check-in`
- **Request (`AttendanceCheckRequestDTO`)**:
  ```json
  {
    "assignmentId": 123,
    "shiftId": 456,
    "userId": 78,
    "source": "WEB",
    "note": "Đến đúng giờ"
  }
  ```
  - Nếu `assignmentId` null, service tự xác định qua `shiftId` + `userId` (hoặc user hiện tại qua JWT).
- **Logic kiểm tra**:
  1. Xác minh phân công tồn tại, ca không bị hủy (`ShiftStatus != CANCELLED`).
  2. Phân công không bị hủy (`ShiftAssignmentStatus != CANCELLED`).
  3. Không tồn tại bản ghi đang mở (check-in chưa check-out).
  4. Tính `lateMinutes` dựa trên giờ bắt đầu dự kiến.
- **Response 201**: `AttendanceRecordResponseDTO` gồm giờ check-in, phút trễ.
- **Lỗi 400**: thiếu thông tin (IllegalArgumentException), đã check-in chưa checkout (IllegalStateException).
- **Lỗi 404**: assignment/shift/user không tồn tại.

### POST `/api/v1/attendance/check-out`
- **Request**: giống check-in (đòi hỏi assignment hoặc shiftId+userId).
- **Logic**:
  1. Tìm bản ghi cuối chưa check-out.
  2. Tính `earlyLeaveMinutes` nếu rời sớm.
  3. Hợp nhất ghi chú check-out với ghi chú cũ.
- **Response 200**: record cập nhật.
- **Lỗi 400**: không có phiên check-in đang mở (IllegalStateException).

### GET `/api/v1/attendance/assignment/{assignmentId}`
- **Response 200**: danh sách `AttendanceRecordResponseDTO` (ordered theo repository default).
- **Lỗi 404**: assignment không tồn tại -> repository trả danh sách trống (không throw) nên FE cần xử lý.

### GET `/api/v1/attendance/shift/{shiftId}`
- **Response 200**: tổng hợp chấm công cho toàn ca.

## Điều kiện nghiệp vụ & validation
- Một phân công không thể check-in 2 lần liên tiếp nếu chưa check-out.
- Không thể chấm công cho ca/ phân công đã hủy.
- `shiftId` bắt buộc nếu không truyền `assignmentId`.
- Service sử dụng user trong JWT nếu `userId` không gửi.
- Các giá trị thời gian dùng `LocalDateTime.now()`, không hỗ trợ back-date.

## Luồng lỗi & thông điệp
| Exception | HTTP | Message |
| --- | --- | --- |
| `EntityNotFoundException` | 404 | "Không tìm thấy phân công ..." / "Không tìm thấy nhân viên ..." |
| `IllegalArgumentException` | 400 | "Cần cung cấp shiftId...", "Không xác định được nhân viên ..." |
| `IllegalStateException` | 400 | "Nhân viên đã check-in và chưa check-out", "Không có phiên check-in đang mở để check-out" |

## Role/Permission
- Check-in/out: `hasAnyRole('STAFF','MANAGER','ADMIN')`
- Query attendance theo assignment/shift: `hasAnyRole('MANAGER','ADMIN')`

## Quan hệ với chức năng khác
- **Shift Assignment**: Attendance gắn với assignment; sau khi chấm công, `ShiftAssignmentService` cập nhật trạng thái/giờ công.
- **Payroll**: thời gian làm thực tế là đầu vào tính lương.
- **Audit/Security**: sử dụng `SecurityUtil` lấy username, lưu `createdBy/updatedBy` cho audit trong DB.

## Các tệp liên quan trong BE
- Controller: `AttendanceController.java`
- Service: `AttendanceService.java`, `ShiftAssignmentService.java`
- DTO: `AttendanceCheckRequestDTO.java`, `AttendanceRecordResponseDTO.java`
- Repository: `AttendanceRecordRepository.java`, `ShiftAssignmentRepository.java`, `UserRepository.java`
- Entity: `AttendanceRecord.java`, `ShiftAssignment.java`, `ShiftInstance.java`
- Utils: `SecurityUtil.java`
