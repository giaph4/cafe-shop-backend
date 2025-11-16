# Sơ Đồ Use Case

## Mục lục
- [1. Giới thiệu](#1-giới-thiệu)
- [2. PlantUML sơ đồ use case](#2-plantuml-sơ-đồ-use-case)
- [3. Mô tả diễn viên](#3-mô-tả-diễn-viên)
- [4. Nhận xét nghiệp vụ](#4-nhận-xét-nghiệp-vụ)

## 1. Giới thiệu
Sơ đồ use case mô tả tương tác giữa các diễn viên chính với hệ thống backend quán cà phê. Sơ đồ giúp xác định phạm vi chức năng, phân bổ trách nhiệm và là cơ sở cho thiết kế chi tiết trong `use-case-chi-tiet.md`.

## 2. PlantUML sơ đồ use case
```plantuml
@startuml
!theme plain
left to right direction

actor "Quản lý" as Manager
actor "Nhân viên" as Staff
actor "Marketing" as Marketing
actor "Kế toán" as Accountant
actor "Khách hàng" as Customer

usecase "Quản lý tài khoản" as UC_UserAdmin
usecase "Quản lý sản phẩm" as UC_Product
usecase "Quản lý voucher" as UC_Voucher
usecase "Theo dõi dashboard" as UC_Dashboard
usecase "Xuất báo cáo" as UC_Report

usecase "Đăng nhập" as UC_Login
usecase "Tạo đơn hàng" as UC_CreateOrder
usecase "Thanh toán đơn" as UC_PayOrder
usecase "Cập nhật tồn kho" as UC_UpdateInventory
usecase "Chăm sóc khách" as UC_CustomerCare

usecase "Chiến dịch voucher" as UC_VoucherCampaign
usecase "Hiệu quả voucher" as UC_VoucherAnalytics

usecase "Quản lý chi phí" as UC_Expense
usecase "Xử lý bảng lương" as UC_Payroll
usecase "Đối soát doanh thu" as UC_Reconcile

usecase "Nhận voucher" as UC_ReceiveVoucher
usecase "Tham gia loyalty" as UC_Loyalty
usecase "Quản lý ca làm" as UC_Shift

Manager --> UC_UserAdmin
Manager --> UC_Product
Manager --> UC_Voucher
Manager --> UC_Dashboard
Manager --> UC_Report

Staff --> UC_Login
Staff --> UC_CreateOrder
Staff --> UC_PayOrder
Staff --> UC_UpdateInventory
Staff --> UC_CustomerCare

Marketing --> UC_VoucherCampaign
Marketing --> UC_VoucherAnalytics

Accountant --> UC_Expense
Accountant --> UC_Payroll
Accountant --> UC_Reconcile

Customer --> UC_ReceiveVoucher
Customer --> UC_Loyalty

UC_UserAdmin --> UC_Login : «include»
UC_CreateOrder --> UC_Login : «include»
UC_PayOrder --> UC_CreateOrder : «extend»
UC_Dashboard --> UC_Report : «include»
UC_VoucherAnalytics --> UC_VoucherCampaign : «include»
UC_Payroll --> UC_Shift : «include»
UC_Shift --> UC_Login : «include»
@enduml
```

## 3. Mô tả diễn viên
| Diễn viên | Mô tả |
|-----------|-------|
| Quản lý | Điều phối hoạt động cửa hàng, truy cập báo cáo, cấu hình danh mục, voucher. |
| Nhân viên | Thu ngân/phục vụ, thao tác bán hàng, cập nhật tồn kho, hỗ trợ khách tại quầy. |
| Marketing | Xây dựng chiến dịch khuyến mãi, phân tích hiệu quả voucher. |
| Kế toán | Ghi nhận chi phí, xử lý bảng lương, đối soát doanh thu cuối kỳ. |
| Khách hàng | Nhận voucher, tích điểm loyalty thông qua hệ thống. |

## 4. Nhận xét nghiệp vụ
- Use case "Đăng nhập" là tiền đề cho hầu hết nghiệp vụ nội bộ.
- "Tạo đơn hàng" và "Thanh toán đơn" là luồng chính ảnh hưởng đến báo cáo và tồn kho.
- Module nhân sự liên kết chặt với bảng lương thông qua ca làm.
- Chiến dịch voucher cung cấp dữ liệu cho phân tích hiệu quả marketing.

---
**Mức độ hoàn thiện:** 100%
**Hạng mục còn thiếu:** Không
