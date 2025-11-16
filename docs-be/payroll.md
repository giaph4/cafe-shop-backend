# Chức năng: Chu kỳ lương & bảng lương ca (Payroll)

## Vai trò trong hệ thống
- Quản trị chu kỳ lương (tạo, cập nhật, phê duyệt) cho nhân viên làm ca.
- Tổng hợp bảng lương theo ca/nhân viên dựa trên dữ liệu phân công, chấm công, điều chỉnh hiệu suất.
- Cho phép quản lý xem, tái tạo và lọc bảng lương.

## Luồng xử lý backend
1. **Danh sách chu kỳ** (`GET /api/v1/shifts/payroll/cycles`): lọc theo trạng thái (`PayrollCycleStatus`) và khoảng ngày, trả về `PayrollCycleResponseDTO` @src/main/java/com/giapho/coffee_shop_backend/controller/PayrollController.java#33-41 @src/main/java/com/giapho/coffee_shop_backend/service/shift/PayrollService.java#120-125.
2. **Xem chi tiết chu kỳ** (`GET /cycles/{cycleId}`): gọi `PayrollService.getCycle`, trả DTO @src/main/java/com/giapho/coffee_shop_backend/controller/PayrollController.java#43-47 @src/main/java/com/giapho/coffee_shop_backend/service/shift/PayrollService.java#115-118.
3. **Tạo chu kỳ** (`POST /cycles`): validate ngày, kiểm tra mã code trùng, thiết lập trạng thái/phê duyệt và lưu @src/main/java/com/giapho/coffee_shop_backend/controller/PayrollController.java#49-54 @src/main/java/com/giapho/coffee_shop_backend/service/shift/PayrollService.java#48-77.
4. **Cập nhật chu kỳ** (`PUT /cycles/{cycleId}`): cập nhật thông tin, xử lý thay đổi trạng thái (phê duyệt/thu hồi) @src/main/java/com/giapho/coffee_shop_backend/controller/PayrollController.java#56-64 @src/main/java/com/giapho/coffee_shop_backend/service/shift/PayrollService.java#79-113.
5. **Tái tạo bảng lương** (`POST /cycles/{cycleId}/regenerate`): lấy phân công trong khoảng chu kỳ, gọi `ShiftAssignmentService.recalculateAssignment`, gom dữ liệu và lưu `PayrollSummary` @src/main/java/com/giapho/coffee_shop_backend/controller/PayrollController.java#66-70 @src/main/java/com/giapho/coffee_shop_backend/service/shift/PayrollService.java#127-169.
6. **Danh sách bảng lương** (`GET /summaries`): lọc theo `cycleId`/`userId`, trả về `PayrollSummaryDTO` @src/main/java/com/giapho/coffee_shop_backend/controller/PayrollController.java#73-79 @src/main/java/com/giapho/coffee_shop_backend/service/shift/PayrollService.java#171-177.
7. **Tính toán**: `PayrollService` sử dụng dữ liệu từ `ShiftAssignment`, `AttendanceRecord`, `ShiftPerformanceAdjustment`, `Order` để tính các chỉ số (giờ công, doanh thu, thưởng/phạt, lương thực nhận) @src/main/java/com/giapho/coffee_shop_backend/service/shift/PayrollService.java#171-248.

## Thành phần liên quan
- **Controller**: `PayrollController` @src/main/java/com/giapho/coffee_shop_backend/controller/PayrollController.java#1-81
- **Service**: `PayrollService` @src/main/java/com/giapho/coffee_shop_backend/service/shift/PayrollService.java#1-311
- **Repository**: `PayrollCycleRepository`, `PayrollSummaryRepository`, `ShiftAssignmentRepository`, `AttendanceRecordRepository`
- **DTO**: `PayrollCycleRequestDTO`, `PayrollCycleResponseDTO`, `PayrollSummaryDTO` @src/main/java/com/giapho/coffee_shop_backend/dto/shift/PayrollCycleRequestDTO.java#1-37 @src/main/java/com/giapho/coffee_shop_backend/dto/shift/PayrollCycleResponseDTO.java#1-50 @src/main/java/com/giapho/coffee_shop_backend/dto/shift/PayrollSummaryDTO.java#1-63
- **Entity**: `PayrollCycle`, `PayrollSummary`, `ShiftAssignment`, `ShiftInstance`, `ShiftPerformanceAdjustment`
- **Utility**: `SecurityUtil` (lấy actor), `ShiftAssignmentService.recalculateAssignment`
- **Security**: tất cả endpoint yêu cầu `hasAnyRole('MANAGER','ADMIN')`.

## Danh sách API
| Method | URL | Mô tả | Role |
| --- | --- | --- | --- |
| GET | `/api/v1/shifts/payroll/cycles` | Lọc danh sách chu kỳ lương | `MANAGER`,`ADMIN` |
| GET | `/api/v1/shifts/payroll/cycles/{cycleId}` | Chi tiết chu kỳ | `MANAGER`,`ADMIN` |
| POST | `/api/v1/shifts/payroll/cycles` | Tạo chu kỳ mới | `MANAGER`,`ADMIN` |
| PUT | `/api/v1/shifts/payroll/cycles/{cycleId}` | Cập nhật chu kỳ | `MANAGER`,`ADMIN` |
| POST | `/api/v1/shifts/payroll/cycles/{cycleId}/regenerate` | Tái tạo bảng lương cho chu kỳ | `MANAGER`,`ADMIN` |
| GET | `/api/v1/shifts/payroll/summaries` | Danh sách bảng lương nhân viên | `MANAGER`,`ADMIN` |

## Chi tiết API

### POST `/api/v1/shifts/payroll/cycles`
- **Request (`PayrollCycleRequestDTO`)**:
  ```json
  {
    "code": "CYCLE-2025-11",
    "name": "Chu kỳ 11/2025",
    "startDate": "2025-11-01",
    "endDate": "2025-11-30",
    "status": "DRAFT",
    "notes": "Đợt cuối năm"
  }
  ```
- **Validation**: ngày không rỗng, `endDate` ≥ `startDate`, mã chu kỳ không trùng.
- **Logic**: thiết lập `createdBy/updatedBy`, nếu status = `APPROVED` thì gán `approvedBy/approvedAt`.
- **Response 201**: `PayrollCycleResponseDTO` với thông tin trạng thái/phê duyệt.
- **Lỗi 400**: ngày sai, mã trùng, trạng thái không hợp lệ.

### PUT `/api/v1/shifts/payroll/cycles/{cycleId}`
- **Logic**: cập nhật thông tin, xử lý chuyển trạng thái (APPROVED -> set actor; khỏi phê duyệt -> reset `approvedBy`).
- **Response 200**: DTO cập nhật.
- **Lỗi 404**: chu kỳ không tồn tại.

### POST `/api/v1/shifts/payroll/cycles/{cycleId}/regenerate`
- **Logic**:
  1. Lấy chu kỳ và mọi phân công trong khoảng `startDate` – `endDate`.
  2. Gọi `ShiftAssignmentService.recalculateAssignment` để cập nhật giờ công, doanh thu, lương.
  3. Xóa bảng lương cũ của chu kỳ (`summaryRepository.deleteByCycleId`).
  4. Gom dữ liệu theo user và lưu `PayrollSummary` mới.
- **Response 200**: `List<PayrollSummaryDTO>` sắp xếp theo tên nhân viên.
- **Lỗi 404**: chu kỳ không tồn tại.

### GET `/api/v1/shifts/payroll/summaries`
- **Query**: `cycleId`, `userId` (tùy chọn).
- **Response 200**: danh sách bảng lương (giờ công, doanh thu, thưởng/phạt, lương net).

## Điều kiện nghiệp vụ & tính toán
- Chỉ chu kỳ `PENDING`/`DRAFT` mới nên cho phép chỉnh sửa; logic không chặn hoàn toàn nhưng cần kiểm soát phía FE.
- `ShiftAssignment` ở trạng thái `IN_PROGRESS`/`COMPLETED` mới có dữ liệu payroll chính xác.
- Giờ công thực tế lấy từ `AttendanceRecord` (check-in/out); doanh thu từ `OrderRepository.findPaidOrdersForStaffBetween`.
- Thưởng/phạt tổng hợp từ `ShiftPerformanceAdjustment` chưa bị revoke.
- Actor (người thao tác) lấy từ `SecurityUtil` (JWT) hoặc `SYSTEM` nếu không có.

## Luồng lỗi & thông điệp
| Exception | HTTP | Message |
| --- | --- | --- |
| `EntityNotFoundException` | 404 | "Không tìm thấy chu kỳ lương ..." |
| `IllegalArgumentException` | 400 | "Mã chu kỳ đã tồn tại", "Ngày kết thúc phải sau ...", "Không thể phân công ..." (từ service phụ) |
| `IllegalStateException` | 400 | Từ `ShiftAssignmentService` khi phân công/ cập nhật khi ca bị khóa (gián tiếp ảnh hưởng tái tạo) |

## Role/Permission
- Tất cả endpoint payroll yêu cầu `hasAnyRole('MANAGER','ADMIN')`.

## Quan hệ với chức năng khác
- **Shift Assignment & Attendance**: dữ liệu đầu vào tính lương.
- **Shift Performance Adjustment**: bonus/penalty áp dụng vào lương.
- **PayrollCycleStatus**: ảnh hưởng Dashboard báo cáo nhân sự.
- **Report**: số liệu payroll có thể dùng cho báo cáo chi phí nhân sự.

## Các tệp liên quan
- Controller: `PayrollController.java`
- Service: `PayrollService.java`, `ShiftAssignmentService.java`
- DTO: `PayrollCycleRequestDTO.java`, `PayrollCycleResponseDTO.java`, `PayrollSummaryDTO.java`
- Repository & Entity: `PayrollCycleRepository.java`, `PayrollSummaryRepository.java`, `ShiftAssignmentRepository.java`, `AttendanceRecordRepository.java`
- Tiện ích: `SecurityUtil.java`
