# Shift Management API

Tài liệu mô tả các endpoint quản lý ca làm, chấm công và thưởng/phạt dành cho FE.

> **Lưu ý chung**
>
> - Các endpoint yêu cầu xác thực JWT, gửi header `Authorization: Bearer <token>`.
> - Quyền truy cập dựa trên role: `STAFF`, `MANAGER`, `ADMIN`.
> - Tham số phân trang sử dụng chuẩn Spring: `page` (0-based), `size`, `sort`.
> - Trường thời gian dùng ISO 8601 (`yyyy-MM-dd`, `yyyy-MM-dd'T'HH:mm:ss`).

---

## 1. Ca mẫu (Shift Templates)

| Method | URL | Quyền | Mô tả |
| --- | --- | --- | --- |
| `GET` | `/api/v1/shifts/templates` | MANAGER, ADMIN | Danh sách template (phân trang). |
| `GET` | `/api/v1/shifts/templates/{id}` | MANAGER, ADMIN | Chi tiết template. |
| `POST` | `/api/v1/shifts/templates` | MANAGER, ADMIN | Tạo template mới. |
| `PUT` | `/api/v1/shifts/templates/{id}` | MANAGER, ADMIN | Cập nhật template. |
| `DELETE` | `/api/v1/shifts/templates/{id}` | MANAGER, ADMIN | Xóa template. |

### 1.1. Request body `ShiftTemplateRequestDTO`

```json
{
  "name": "Ca sáng",
  "description": "Ca quầy pha chế",
  "startTime": "07:00:00",
  "endTime": "11:00:00",
  "requiredRoles": ["BARISTA"],
  "defaultHourlyRate": 30000,
  "defaultFixedAllowance": 50000
}
```

### 1.2. Response `ShiftTemplateResponseDTO`

```json
{
  "id": 1,
  "name": "Ca sáng",
  "description": "Ca quầy pha chế",
  "startTime": "07:00:00",
  "endTime": "11:00:00",
  "requiredRoles": ["BARISTA"],
  "defaultHourlyRate": 30000,
  "defaultFixedAllowance": 50000,
  "createdBy": "manager01",
  "updatedBy": "manager01",
  "createdAt": "2025-01-01T08:00:00",
  "updatedAt": "2025-01-01T08:00:00"
}
```

---

## 2. Ca cụ thể (Shift Instances)

| Method | URL | Quyền | Mô tả |
| --- | --- | --- | --- |
| `GET` | `/api/v1/shifts/instances` | MANAGER, ADMIN | Lọc ca theo ngày, trạng thái. |
| `GET` | `/api/v1/shifts/instances/{id}` | MANAGER, ADMIN | Chi tiết ca. |
| `POST` | `/api/v1/shifts/instances` | MANAGER, ADMIN | Sinh ca từ template (hỗ trợ nhiều ngày). |
| `PUT` | `/api/v1/shifts/instances/{id}` | MANAGER, ADMIN | Chỉnh sửa ca (khi chưa LOCKED/DONE). |
| `PATCH` | `/api/v1/shifts/instances/{id}/status` | MANAGER, ADMIN | Đổi trạng thái ca (PLANNED/LOCKED/DONE/CANCELLED). |
| `DELETE` | `/api/v1/shifts/instances/{id}` | MANAGER, ADMIN | Xóa ca (không có assignment). |

### 2.1. Query filters

- `from`, `to` (`yyyy-MM-dd`): khoảng ngày.
- `status`: enum `PLANNED`, `LOCKED`, `IN_PROGRESS`, `DONE`, `CANCELLED`.

### 2.2. Request `ShiftInstanceCreateRequestDTO`

```json
{
  "templateId": 1,
  "dates": ["2025-01-10", "2025-01-11"],
  "startTime": "08:00:00",        // optional, lấy từ template nếu null
  "endTime": "12:00:00",          // optional
  "notes": "Ca cuối tuần"
}
```

### 2.3. Response `ShiftInstanceResponseDTO`

```json
{
  "id": 10,
  "templateId": 1,
  "templateName": "Ca sáng",
  "shiftDate": "2025-01-10",
  "startTime": "08:00:00",
  "endTime": "12:00:00",
  "status": "PLANNED",
  "lockedAt": null,
  "notes": "Ca cuối tuần",
  "createdBy": "manager01",
  "updatedBy": "manager01",
  "createdAt": "2025-01-05T09:00:00",
  "updatedAt": "2025-01-05T09:00:00",
  "assignments": []
}
```

---

## 3. Phân công nhân viên (Shift Assignments)

| Method | URL | Quyền | Mô tả |
| --- | --- | --- | --- |
| `GET` | `/api/v1/shifts/assignments/{assignmentId}` | MANAGER, ADMIN | Chi tiết phân công. |
| `GET` | `/api/v1/shifts/assignments/shift/{shiftId}` | MANAGER, ADMIN | Danh sách phân công theo ca. |
| `POST` | `/api/v1/shifts/assignments` | MANAGER, ADMIN | Tạo phân công. |
| `PUT` | `/api/v1/shifts/assignments/{assignmentId}` | MANAGER, ADMIN | Cập nhật thời gian/lương phân công. |
| `PATCH` | `/api/v1/shifts/assignments/{assignmentId}/status` | MANAGER, ADMIN | Đổi trạng thái (SCHEDULED/CONFIRMED/...). |
| `DELETE` | `/api/v1/shifts/assignments/{assignmentId}` | MANAGER, ADMIN | Xóa phân công (khi chưa in-progress hoặc completed). |

### 3.1. Request `ShiftAssignmentRequestDTO`

```json
{
  "shiftId": 10,
  "userId": 5,
  "roleName": "BARISTA",
  "plannedStart": "08:00:00",
  "plannedEnd": "12:00:00",
  "plannedMinutes": 240,
  "hourlyRate": 32000,
  "fixedAllowance": 50000,
  "notes": "Hỗ trợ buổi sáng"
}
```

### 3.2. Response `ShiftAssignmentResponseDTO` (rút gọn)

```json
{
  "id": 100,
  "shiftId": 10,
  "userId": 5,
  "username": "staff01",
  "fullName": "Nguyễn Văn A",
  "roleName": "BARISTA",
  "plannedStart": "08:00:00",
  "plannedEnd": "12:00:00",
  "plannedMinutes": 240,
  "actualMinutes": 230,
  "totalOrders": 18,
  "totalRevenue": 215000,
  "hourlyRate": 32000,
  "fixedAllowance": 50000,
  "bonusAmount": 20000,
  "penaltyAmount": 0,
  "basePayroll": 196000,
  "adjustmentTotal": 20000,
  "calculatedPayroll": 216000,
  "status": "COMPLETED",
  "notes": "Hỗ trợ tốt",
  "attendanceRecords": [...],
  "adjustments": [...]
}
```

---

## 4. Chấm công (Attendance)

| Method | URL | Quyền | Mô tả |
| --- | --- | --- | --- |
| `POST` | `/api/v1/attendance/check-in` | STAFF, MANAGER, ADMIN | Check-in ca. |
| `POST` | `/api/v1/attendance/check-out` | STAFF, MANAGER, ADMIN | Check-out ca. |
| `GET` | `/api/v1/attendance/assignment/{assignmentId}` | MANAGER, ADMIN | Xem lịch sử chấm công theo phân công. |
| `GET` | `/api/v1/attendance/shift/{shiftId}` | MANAGER, ADMIN | Xem chấm công theo ca. |

### 4.1. Request `AttendanceCheckRequestDTO`

```json
{
  "shiftId": 10,             // optional nếu truyền assignmentId
  "assignmentId": 100,       // optional, ưu tiên assignmentId
  "userId": 5,               // optional, mặc định lấy từ token
  "source": "QR",           // enum: QR, APP, WEB, MANUAL
  "note": "Check-in bằng QR"
}
```

### 4.2. Response `AttendanceRecordResponseDTO`

```json
{
  "id": 501,
  "assignmentId": 100,
  "checkInAt": "2025-01-10T07:58:12",
  "checkOutAt": "2025-01-10T12:05:00",
  "source": "QR",
  "lateMinutes": 0,
  "earlyLeaveMinutes": 0,
  "note": "Check-in bằng QR",
  "createdBy": "staff01",
  "updatedBy": "staff01",
  "createdAt": "2025-01-10T07:58:12",
  "updatedAt": "2025-01-10T12:05:00"
}
```

---

## 5. Thưởng/Phạt (Shift Performance Adjustments)

| Method | URL | Quyền | Mô tả |
| --- | --- | --- | --- |
| `GET` | `/api/v1/shifts/adjustments/{id}` | MANAGER, ADMIN | Chi tiết điều chỉnh. |
| `GET` | `/api/v1/shifts/adjustments/assignment/{assignmentId}` | MANAGER, ADMIN | Danh sách điều chỉnh theo phân công. |
| `POST` | `/api/v1/shifts/adjustments` | MANAGER, ADMIN | Tạo thưởng/phạt. |
| `POST` | `/api/v1/shifts/adjustments/{id}/revoke` | MANAGER, ADMIN | Thu hồi điều chỉnh. |
| `DELETE` | `/api/v1/shifts/adjustments/{id}` | MANAGER, ADMIN | Xóa điều chỉnh. |

### 5.1. Request `ShiftPerformanceAdjustmentRequestDTO`

```json
{
  "assignmentId": 100,
  "type": "BONUS",      // enum: BONUS, PENALTY
  "amount": 20000,
  "reason": "Doanh thu vượt chỉ tiêu"
}
```

### 5.2. Response `ShiftPerformanceAdjustmentResponseDTO`

```json
{
  "id": 9001,
  "assignmentId": 100,
  "type": "BONUS",
  "amount": 20000,
  "reason": "Doanh thu vượt chỉ tiêu",
  "effectiveAt": "2025-01-10T12:05:00",
  "revoked": false,
  "revokedAt": null,
  "revokedBy": null,
  "createdBy": "manager01",
  "updatedBy": "manager01",
  "createdAt": "2025-01-10T12:05:00",
  "updatedAt": "2025-01-10T12:05:00"
}
```

### 5.3. Revoke request `ShiftPerformanceAdjustmentRevokeRequestDTO`

```json
{
  "reason": "Nhập sai dữ liệu"
}
```

---

## 6. Bảng lương (Payroll)

| Method | URL | Quyền | Mô tả |
| --- | --- | --- | --- |
| `GET` | `/api/v1/shifts/payroll/cycles` | MANAGER, ADMIN | Danh sách chu kỳ lương, hỗ trợ lọc theo trạng thái/khoảng ngày. |
| `GET` | `/api/v1/shifts/payroll/cycles/{id}` | MANAGER, ADMIN | Chi tiết chu kỳ lương. |
| `POST` | `/api/v1/shifts/payroll/cycles` | MANAGER, ADMIN | Tạo chu kỳ lương mới (mặc định trạng thái `DRAFT`). |
| `PUT` | `/api/v1/shifts/payroll/cycles/{id}` | MANAGER, ADMIN | Cập nhật thông tin, trạng thái chu kỳ (khi chuyển `APPROVED` sẽ lưu người duyệt). |
| `POST` | `/api/v1/shifts/payroll/cycles/{id}/regenerate` | MANAGER, ADMIN | Gom lại dữ liệu lương cho chu kỳ theo khoảng ngày. |
| `GET` | `/api/v1/shifts/payroll/summaries` | MANAGER, ADMIN | Danh sách tổng hợp lương theo chu kỳ/nhân viên. |

### 6.1. Request `PayrollCycleRequestDTO`

```json
{
  "code": "JAN_2025",
  "name": "Lương tháng 01/2025",
  "startDate": "2025-01-01",
  "endDate": "2025-01-31",
  "status": "DRAFT",
  "notes": "Ghi chú tùy chọn"
}
```

### 6.2. Response `PayrollCycleResponseDTO`

```json
{
  "id": 10,
  "code": "JAN_2025",
  "name": "Lương tháng 01/2025",
  "startDate": "2025-01-01",
  "endDate": "2025-01-31",
  "status": "IN_PROGRESS",
  "notes": "Ghi chú",
  "approvedBy": "manager01",
  "approvedAt": "2025-02-02T09:30:00",
  "createdBy": "manager01",
  "updatedBy": "manager01",
  "createdAt": "2025-01-25T08:00:00",
  "updatedAt": "2025-02-02T09:30:00"
}
```

### 6.3. Response `PayrollSummaryDTO`

```json
{
  "cycleId": 10,
  "cycleCode": "JAN_2025",
  "cycleName": "Lương tháng 01/2025",
  "cycleStartDate": "2025-01-01",
  "cycleEndDate": "2025-01-31",
  "userId": 5,
  "username": "staff01",
  "fullName": "Nguyễn Văn A",
  "assignmentCount": 6,
  "attendanceCount": 12,
  "totalActualMinutes": 1440,
  "totalOrders": 85,
  "totalRevenue": 9500000,
  "totalBasePayroll": 4200000,
  "totalBonus": 250000,
  "totalPenalty": 50000,
  "totalAdjustment": 200000,
  "totalNetPayroll": 4400000,
  "notes": null
}
```

> **Ghi chú**: Backend luôn recalculated payroll mỗi khi có thay đổi attendance/thưởng phạt. Khi gọi regenerate summaries, hệ thống sẽ chắc chắn cập nhật lại các assignment trước khi tổng hợp.

---

## 7. Quy ước status & enum

- `ShiftStatus`: `PLANNED`, `LOCKED`, `IN_PROGRESS`, `DONE`, `CANCELLED`.
- `ShiftAssignmentStatus`: `SCHEDULED`, `CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`.
- `AttendanceSource`: `QR`, `APP`, `WEB`, `MANUAL`.
- `AdjustmentType`: `BONUS`, `PENALTY`.
- `PayrollCycleStatus`: `DRAFT`, `IN_PROGRESS`, `READY_FOR_APPROVAL`, `APPROVED`, `CLOSED`.

---

## 8. Ghi chú cho FE

1. **Xử lý lỗi**: các service trả về HTTP status chuẩn (`400`, `404`, `409`, `500`) kèm message tiếng Việt.
2. **Phân trang**: mặc định `size = 20`; truyền `sort=createdAt,desc` nếu cần.
3. **Chấm công**: FE ưu tiên gửi `assignmentId`. Nếu chỉ biết `shiftId`, backend sẽ tìm assignment theo user hiện tại, cần đảm bảo user đã được gán.
4. **Tự động cập nhật lương**: sau mỗi check-in/out hoặc thêm thưởng/phạt, backend tự recalculated assignment. Regenerate payroll cycle chỉ dùng khi cần tổng hợp lại toàn bộ chu kỳ.
5. **OpenAPI/Swagger**: truy cập `http://<host>:8088/swagger-ui.html` để xem schema đầy đủ.

Cập nhật tài liệu khi có thay đổi API mới để FE luôn đồng bộ với backend.
