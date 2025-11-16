# Sơ Đồ Lớp

## Mục lục
- [1. Tổng quan](#1-tổng-quan)
- [2. PlantUML sơ đồ lớp](#2-plantuml-sơ-đồ-lớp)
- [3. Mối quan hệ chính](#3-mối-quan-hệ-chính)
- [4. Tầng ứng dụng & lớp hỗ trợ](#4-tầng-ứng-dụng--lớp-hỗ-trợ)

## 1. Tổng quan
Sơ đồ lớp mô tả các entity trọng yếu, quan hệ giữa chúng và cách hệ thống tổ chức domain. Kiến trúc tuân theo layered architecture: Controller → Service → Repository → Entity. DTO và Mapper giúp tách payload API khỏi domain.

## 2. PlantUML sơ đồ lớp
```plantuml
@startuml
!theme plain

class User {
  +Long id
  +String username
  +String password
  +String fullName
  +String email
  +String phone
  +String status
}

class Role {
  +Long id
  +String name
}

class Order {
  +Long id
  +String type
  +String status
  +BigDecimal subTotal
  +BigDecimal discountAmount
  +BigDecimal totalAmount
  +LocalDateTime createdAt
  +LocalDateTime paidAt
}

class OrderDetail {
  +Long id
  +Integer quantity
  +BigDecimal unitPrice
  +BigDecimal lineTotal
}

class Product {
  +Long id
  +String name
  +String code
  +BigDecimal price
  +boolean available
}

class Category {
  +Long id
  +String name
  +String description
}

class Voucher {
  +Long id
  +String code
  +String type
  +BigDecimal discountValue
  +BigDecimal maxDiscount
  +LocalDate validFrom
  +LocalDate validTo
}

class Customer {
  +Long id
  +String fullName
  +String phone
  +String email
  +Integer loyaltyPoints
}

class CafeTable {
  +Long id
  +String code
  +TableStatus status
}

class LoginHistory {
  +Long id
  +String username
  +String ipAddress
  +String userAgent
  +boolean success
  +LocalDateTime attemptAt
}

class AttendanceRecord {
  +Long id
  +LocalDateTime checkInAt
  +LocalDateTime checkOutAt
  +String notes
}

class ShiftAssignment {
  +Long id
  +LocalDate assignDate
  +String status
}

class PayrollSummary {
  +Long id
  +BigDecimal totalHours
  +BigDecimal baseSalary
  +BigDecimal bonus
  +BigDecimal deductions
  +BigDecimal finalAmount
}

User "*" -- "*" Role : userRoles
Order "*" -- "1" User : createdBy
Order "*" -- "0..1" Customer : belongsTo
Order "*" -- "1" CafeTable : serveAt
Order "1" -- "*" OrderDetail : has
OrderDetail "*" -- "1" Product : references
Product "*" -- "1" Category : categorized
Order "0..1" -- "1" Voucher : applied
User "1" -- "*" LoginHistory : records
User "1" -- "*" AttendanceRecord : attendance
User "1" -- "*" ShiftAssignment : assignments
User "1" -- "*" PayrollSummary : payroll
ShiftAssignment "1" -- "1" AttendanceRecord : trackedBy
@enduml
```

## 3. Mối quan hệ chính
- **User ↔ Role**: Quan hệ nhiều-nhiều, ánh xạ qua bảng phụ `user_roles`.
- **Order ↔ OrderDetail ↔ Product**: Diễn tả cấu trúc đơn hàng và chi tiết món, lấy giá sản phẩm tại thời điểm đặt.
- **Order ↔ Voucher**: Mỗi order có thể gắn một voucher, ghi cả `voucherCode` và tham chiếu `Voucher` để truy vết.
- **AttendanceRecord/ShiftAssignment/PayrollSummary**: Bộ ba entity kết nối module nhân sự với bảng lương.
- **LoginHistory**: Lưu mọi lần đăng nhập (thành công/thất bại) phục vụ audit.

## 4. Tầng ứng dụng & lớp hỗ trợ
- **Repository**: Spring Data JPA (`UserRepository`, `OrderRepository`, ...), cung cấp truy vấn chuẩn và custom.
- **Service**: `OrderService`, `VoucherService`, `PayrollService`... triển khai nghiệp vụ và giao dịch.
- **Mapper**: MapStruct (`OrderMapper`, `UserMapper`) chuyển đổi giữa Entity ↔ DTO.
- **DTO**: `OrderResponseDTO`, `CustomerDetailDTO`, `VoucherRequestDTO` mô tả payload API.
- **Validation**: Annotation Jakarta Validation (`@NotNull`, `@Size`, `@Email`, ...), custom validator cho nghiệp vụ.

---
**Mức độ hoàn thiện:** 100%
**Hạng mục còn thiếu:** Không
