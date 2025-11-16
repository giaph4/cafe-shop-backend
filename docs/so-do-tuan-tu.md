# Sơ Đồ Tuần Tự

## Mục lục
- [1. Giới thiệu](#1-giới-thiệu)
- [2. Đặt hàng và thanh toán tại quầy](#2-đặt-hàng-và-thanh-toán-tại-quầy)
- [3. Quy trình nhập kho](#3-quy-trình-nhập-kho)
- [4. Quy trình chấm công và bảng lương](#4-quy-trình-chấm-công-và-bảng-lương)
- [5. Ghi chú triển khai](#5-ghi-chú-triển-khai)

## 1. Giới thiệu
Sơ đồ tuần tự mô tả tương tác giữa client, service, repository và thành phần phụ trợ trong từng nghiệp vụ trọng điểm. Các sơ đồ sử dụng PlantUML, có thể biên dịch trực tiếp bằng PlantUML hoặc tích hợp CI để xuất hình tự động.

## 2. Đặt hàng và thanh toán tại quầy
```plantuml
@startuml
!theme plain
actor POS as "POS/Web"
participant Auth as "AuthenticationService"
participant OrderSvc as "OrderService"
participant VoucherSvc as "VoucherService"
participant PaymentSvc as "PaymentService"
database Repo as "OrderRepository"

POS -> Auth : POST /auth/login
Auth --> POS : JWT token

POS -> OrderSvc : POST /orders (payload, JWT)
OrderSvc -> Repo : findPendingByTable()
Repo --> OrderSvc : pendingOrder?
OrderSvc -> VoucherSvc : validate(order, voucherCode)
VoucherSvc --> OrderSvc : discountInfo
OrderSvc -> Repo : save(order, details)
Repo --> OrderSvc : savedOrder
OrderSvc --> POS : OrderResponseDTO (PENDING)

POS -> PaymentSvc : POST /orders/{id}/pay
PaymentSvc -> OrderSvc : verifyOrder(id)
OrderSvc -> Repo : findById(id)
Repo --> OrderSvc : order
OrderSvc --> PaymentSvc : orderInfo
PaymentSvc -> Repo : updateStatus(PAID)
Repo --> PaymentSvc : updatedOrder
PaymentSvc --> POS : PaymentResponse (PAID)
@enduml
```

## 3. Quy trình nhập kho
```plantuml
@startuml
!theme plain
actor Staff as "Nhân viên kho"
participant InventorySvc as "InventoryService"
participant SupplierSvc as "SupplierService"
database PORepo as "PurchaseOrderRepository"
database IngredientRepo as "IngredientRepository"

Staff -> InventorySvc : POST /purchase-orders (supplier, items)
InventorySvc -> SupplierSvc : validateSupplier(supplierId)
SupplierSvc --> InventorySvc : supplierInfo
InventorySvc -> PORepo : save(purchaseOrder)
PORepo --> InventorySvc : createdPO
InventorySvc --> Staff : PurchaseOrder (CREATED)

Staff -> InventorySvc : POST /purchase-orders/{id}/receive
InventorySvc -> PORepo : findById(id)
PORepo --> InventorySvc : purchaseOrder
InventorySvc -> IngredientRepo : increaseStock(itemList)
IngredientRepo --> InventorySvc : updatedStock
InventorySvc -> PORepo : updateStatus(RECEIVED)
PORepo --> InventorySvc : updatedPO
InventorySvc --> Staff : PurchaseOrder (RECEIVED)
@enduml
```

## 4. Quy trình chấm công và bảng lương
```plantuml
@startuml
!theme plain
actor Employee as "Nhân viên"
actor Manager as "Quản lý"
participant ShiftSvc as "ShiftAssignmentService"
participant AttendanceSvc as "AttendanceRecordService"
participant PayrollSvc as "PayrollService"
database ShiftRepo as "ShiftAssignmentRepository"
database AttendanceRepo as "AttendanceRepository"
database PayrollRepo as "PayrollSummaryRepository"

Employee -> ShiftSvc : GET /shifts/today
ShiftSvc -> ShiftRepo : findByUserAndDate()
ShiftRepo --> ShiftSvc : assignments
ShiftSvc --> Employee : assignments

Employee -> AttendanceSvc : POST /attendance/check-in
AttendanceSvc -> ShiftRepo : validateAssignment()
ShiftRepo --> AttendanceSvc : assignment
AttendanceSvc -> AttendanceRepo : save(checkIn)
AttendanceRepo --> AttendanceSvc : attendanceRecord
AttendanceSvc --> Employee : success

Employee -> AttendanceSvc : POST /attendance/check-out
AttendanceSvc -> AttendanceRepo : update(checkOut)
AttendanceRepo --> AttendanceSvc : attendanceRecord
AttendanceSvc --> Manager : notification (optional)

Manager -> PayrollSvc : POST /payroll/cycle/{id}/aggregate
PayrollSvc -> AttendanceRepo : fetchByCycle()
PayrollSvc -> ShiftRepo : fetchAdjustments()
PayrollSvc -> PayrollRepo : save(payrollSummary)
PayrollRepo --> PayrollSvc : summaries
PayrollSvc --> Manager : PayrollSummaryDTO
@enduml
```

## 5. Ghi chú triển khai
- Các bước kiểm tra điều kiện (validate supplier, validate assignment) nên throw exception rõ nghĩa để front-end xử lý.
- Sequence diagram hỗ trợ debug và onboarding: xem thêm `thiet-ke-module.md` để biết service nào chịu trách nhiệm.
- Có thể mở rộng diagram cho kịch bản lỗi (ví dụ: thanh toán thất bại, thiếu tồn kho) bằng cách thêm nhánh `alt` trong PlantUML.

---
**Mức độ hoàn thiện:** 100%
**Hạng mục còn thiếu:** Không
