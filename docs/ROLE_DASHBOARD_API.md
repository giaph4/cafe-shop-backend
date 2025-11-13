# Tài liệu API Dashboard Theo Vai Trò

> **Phiên bản:** 1.0 – Cập nhật ngày 14/11/2025  
> **Phạm vi:** Backend Spring Boot 3.5.6 – Coffee Shop Management  
> **Ngôn ngữ:** Tài liệu dành cho frontend (React/Vue/…)

## 1. Tổng quan

Hệ thống cung cấp 3 dashboard chuyên biệt cho từng vai trò:

| Vai trò    | Endpoint                       | Phạm vi dữ liệu chính                                                  |
|------------|--------------------------------|-------------------------------------------------------------------------|
| **Admin**  | `GET /api/admin/dashboard`     | Doanh thu, đơn hàng, tồn kho, top nhân viên/sản phẩm/khách, cảnh báo  |
| **Manager**| `GET /api/manager/dashboard`   | Tổng quan ca, hiệu suất đội nhóm, tồn kho trọng điểm, payroll, cảnh báo|
| **Staff**  | `GET /api/staff/dashboard`     | Tóm tắt ca cá nhân, lịch, hiệu suất, chấm công, payroll, thông báo     |
| **Staff/Manager/Admin**| `GET /api/staff/dashboard/{userId}` | Dashboard cá nhân cho nhân viên cụ thể (Manager/Admin truy cập hộ) |

### 1.1 Yêu cầu xác thực & quyền hạn
- Tất cả endpoint yêu cầu header `Authorization: Bearer <JWT>`.
- Spring Security kiểm tra role theo `@PreAuthorize`:
  - Admin: `hasRole('ADMIN')`
  - Manager: `hasAnyRole('MANAGER','ADMIN')`
  - Staff (dashboard cá nhân): `hasAnyRole('STAFF','MANAGER','ADMIN')`
  - Staff dashboard theo userId: chỉ `MANAGER` hoặc `ADMIN`.

### 1.2 Format trả về
- Mặc định JSON UTF-8.
- Mọi giá trị tiền tệ dùng format Decimal, FE hiển thị theo locale.
- Các giá trị thời gian: `LocalDate` định dạng `YYYY-MM-DD`, `LocalDateTime` theo ISO 8601 (`YYYY-MM-DDTHH:mm:ss`).

---

## 2. Dashboard Admin – `GET /api/admin/dashboard`

### 2.1 Mô tả dữ liệu
```json
{
  "revenue": {
    "today": 12500000.0,
    "month": 235700000.0,
    "year": 1985000000.0,
    "averageOrderValue": 78000.0,
    "todayProfit": 3200000.0,
    "monthProfit": 61200000.0
  },
  "orders": {
    "today": 160,
    "month": 2980,
    "year": 24000,
    "cancelledToday": 4,
    "cancelledMonth": 58
  },
  "inventory": {
    "lowStockItems": 6,
    "totalSuppliers": 18,
    "pendingPurchaseOrders": 3
  },
  "topStaff": [
    {
      "staffId": 102,
      "staffName": "Nguyễn Thị Lan",
      "orders": 380,
      "revenue": 54200000.0
    }
  ],
  "topProducts": [
    {
      "productId": 21,
      "productName": "Caramel Macchiato",
      "quantity": 920,
      "revenue": 82800000.0
    }
  ],
  "topCustomers": [
    {
      "customerId": 55,
      "customerName": "Phạm Minh Tâm",
      "phone": "0987654321",
      "orders": 35,
      "spend": 15250000.0
    }
  ],
  "alerts": [
    {
      "type": "INVENTORY",
      "message": "Có 6 nguyên liệu dưới mức tồn kho an toàn",
      "severity": "HIGH"
    }
  ]
}
```

### 2.2 Ghi chú hiển thị
- `averageOrderValue`: nên format tiền tệ (vd: 78.000₫).
- `alerts.severity`: map màu sắc (INFO – xanh, MEDIUM – vàng, HIGH – đỏ).
- `top*` tối đa 10 phần tử, FE có thể cắt/bổ sung phân trang nếu cần.

---

## 3. Dashboard Manager – `GET /api/manager/dashboard`

### 3.1 Response mẫu
```json
{
  "shiftOverview": {
    "scheduledToday": 8,
    "inProgress": 3,
    "completed": 12,
    "cancelled": 1,
    "upcomingShifts": [
      {
        "shiftId": 401,
        "shiftDate": "2025-11-15",
        "timeRange": "07:00 - 11:00",
        "status": "PLANNED",
        "assignedStaff": 5,
        "capacity": 6
      }
    ]
  },
  "teamPerformance": {
    "totalRevenue": 41500000.0,
    "totalOrders": 720,
    "averageOrderValue": 57638.9,
    "topStaff": [
      {
        "staffId": 102,
        "staffName": "Nguyễn Thị Lan",
        "orders": 120,
        "revenue": 16800000.0,
        "averageOrderValue": 70000.0
      }
    ]
  },
  "inventory": {
    "lowStockItems": 6,
    "criticalStockItems": 2,
    "alerts": [
      {
        "ingredientId": 11,
        "ingredientName": "Đường nâu",
        "quantityOnHand": 4.5,
        "reorderLevel": 10.0
      }
    ]
  },
  "payroll": {
    "estimatedPayroll": 56200000.0,
    "bonusTotal": 4300000.0,
    "penaltyTotal": 750000.0,
    "adjustmentNet": 1200000.0,
    "staffCount": 35
  },
  "pendingApprovals": [
    {
      "module": "PURCHASE_ORDER",
      "description": "Phiếu nhập 781 - Công ty ABC",
      "requestedBy": "manager1",
      "requestedAt": "2025-11-13",
      "status": "PENDING"
    }
  ],
  "attendanceAlerts": [
    {
      "assignmentId": 9001,
      "staffId": 201,
      "staffName": "Lê Hoàng",
      "issueType": "LATE_CHECK_IN",
      "note": "Có lần check-in trễ trong 24h qua"
    }
  ],
  "serviceIssues": [
    {
      "orderId": 12002,
      "tableName": "B12",
      "issue": "Đơn bị hủy",
      "severity": "MEDIUM",
      "createdDate": "2025-11-14"
    }
  ]
}
```

### 3.2 Lưu ý FE
- `timeRange` đã ghép sẵn string, FE có thể tách nếu cần hiển thị icon.
- `inventory.alerts`: `quantityOnHand` và `reorderLevel` dùng đơn vị như DB (kg/lít/pcs). Nên hiển thị kèm đơn vị nếu FE có metadata.
- `attendanceAlerts.issueType` các giá trị: `NO_CHECK_IN`, `LATE_CHECK_IN`, `EARLY_CHECK_OUT` – nên map sang tiếng Việt.
- `serviceIssues`: hiện chỉ theo đơn hủy, có thể mở rộng (ghi chú severity cố định `MEDIUM`).

---

## 4. Dashboard Staff – `GET /api/staff/dashboard`

### 4.1 Tham số tùy chọn
- `userId` (query param): nếu FE (MANAGER/ADMIN) muốn xem hộ nhân viên: `GET /api/staff/dashboard?userId=123`.
- Nếu Staff truy cập và không truyền `userId`, backend tự lấy theo JWT.

### 4.2 Response mẫu
```json
{
  "shiftSummary": {
    "shiftsThisWeek": 5,
    "completedShifts": 3,
    "pendingShifts": 2,
    "lateCheckIns": 1,
    "earlyCheckOuts": 0
  },
  "upcomingShifts": [
    {
      "assignmentId": 6001,
      "shiftDate": "2025-11-15",
      "timeRange": "07:00 - 11:00",
      "role": "BARISTA",
      "status": "SCHEDULED",
      "managerNote": "Chuẩn bị món seasonal"
    }
  ],
  "performance": {
    "totalRevenue": 18500000.0,
    "totalOrders": 240,
    "averageOrderValue": 77083.3,
    "positiveFeedbacks": 8,
    "negativeFeedbacks": 0
  },
  "attendance": {
    "currentlyCheckedIn": false,
    "lastCheckIn": "2025-11-14T07:58:00",
    "lastCheckOut": "2025-11-14T12:05:00",
    "consecutiveOnTimeDays": 4
  },
  "payroll": {
    "estimatedCurrentCycle": 6200000.0,
    "bonusTotal": 450000.0,
    "penaltyTotal": 0.0,
    "adjustmentNet": 150000.0,
    "lastCyclePaid": 5800000.0
  },
  "taskReminders": [],
  "announcements": []
}
```

### 4.3 Hướng dẫn UI
- `currentlyCheckedIn`: true => hiển thị badge “Đang làm việc”.
- `consecutiveOnTimeDays`: kết hợp icon streak.
- `positiveFeedbacks/negativeFeedbacks`: hiện tại backend đặt 0 (chưa tích hợp feedback service) – FE có thể ẩn nếu =0.
- `taskReminders`, `announcements`: đang là danh sách rỗng – backend sẽ bổ sung sau. FE nên xử lý gracefully (ví dụ skeleton).

### 4.4 Endpoint theo userId – `GET /api/staff/dashboard/{userId}`
- Trả dữ liệu giống hệt 4.2.
- Kiểm tra role: Manager/Admin.
- Nếu `userId` không tồn tại → HTTP 404.

---

## 5. Quy ước chung cho Frontend

### 5.1 Header yêu cầu
```
GET /api/admin/dashboard HTTP/1.1
Host: {{baseUrl}}
Authorization: Bearer {{jwtToken}}
Accept: application/json
```

### 5.2 Xử lý lỗi
- 401 – JWT hết hạn/không hợp lệ → dẫn về màn hình đăng nhập.
- 403 – Sai quyền → hiển thị thông báo “Bạn không có quyền truy cập dashboard này”.
- 404 – Dữ liệu không tồn tại (vd userId). FE nên hiển thị Empty State.
- 500 – Lỗi nội bộ → hiển thị thông báo chung + liên hệ hỗ trợ.

### 5.3 Best practice FE
- Cache dữ liệu dashboard trong 60s để tránh load lại quá nhiều.
- Khi cập nhật thủ công (ví dụ filter range), gọi lại API tương ứng.
- Hiển thị timestamp “Cập nhật lúc …” dựa trên giờ FE nhận response.

---

## 6. Test & Mock Data
- Các test unit sử dụng mẫu dữ liệu tương tự như JSON trong tài liệu.
- Để FE mock, có thể dùng các file JSON mẫu ở trên (copy trực tiếp).
- Nếu cần fake API local: dùng MSW (Mock Service Worker) hoặc MirageJS với payload tương ứng.

---

## 7. Lộ trình phát triển tiếp theo (FYI)
1. Bổ sung `taskReminders`, `announcements` cho Staff từ module Task/Announcement.
2. Mở rộng `serviceIssues` cho Manager (bao gồm review, feedback).
3. Admin dashboard sẽ thêm biểu đồ theo tuần/tháng – FE chuẩn bị layout linh hoạt.

### 7.1 Đề xuất tối ưu & cam kết không phá vỡ FE

| Hạng mục | Mô tả backend | Tác động tới FE | Hành động đề xuất cho FE |
|----------|---------------|-----------------|--------------------------|
| **Caching** | Bổ sung cache tạm thời (Redis/Caffeine) ở service `RoleDashboardService` nhằm giảm thời gian đáp ứng. Cache key phụ thuộc role + userId, TTL 30–60 giây. | Không đổi cấu trúc response, status code. FE không cần thay đổi. | Có thể hiển thị “Cập nhật lúc …” nhưng không bắt buộc. |
| **Filter thời gian** | Dự kiến mở rộng query param `range` (vd: `?range=today|week|month|custom&from=YYYY-MM-DD&to=YYYY-MM-DD`). Nếu không truyền, vẫn trả default như hiện tại. | Thay đổi ở dạng *tùy chọn*, không phá vỡ gọi cũ. FE hiện tại có thể bỏ qua. | Chuẩn bị giao diện chọn phạm vi thời gian; khi backend ra mắt, chỉ cần thêm query param. |
| **Biểu đồ nâng cao** | Backend sẽ bổ sung endpoint (vd: `/api/admin/dashboard/charts`) hoặc thêm trường mới (vd: `trend`, `series`). Mọi bổ sung sẽ ở dạng field mới, không xóa field cũ. | FE vẫn hoạt động như hiện tại. Khi muốn dùng biểu đồ, đọc thêm field mới; nếu chưa sẵn sàng có thể bỏ qua. | Thiết kế component biểu đồ nhận dữ liệu linh hoạt; fallback khi thiếu field. |

> **Nguyên tắc chung:** mọi tối ưu sẽ áp dụng theo triết lý backward-compatible. FE chỉ cần cập nhật khi muốn khai thác tính năng mới; các trường hiện tại sẽ không bị đổi tên hay thay đổi kiểu dữ liệu.

> Mọi thắc mắc liên quan API vui lòng liên hệ backend qua kênh #coffee-tech-backend trên Slack.
