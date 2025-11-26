# Use Case Diagram - Hệ Thống Quản Lý Ca Làm Việc và Lương (Subsystem)

## 📁 File

- **USE_CASE_SHIFT_PAYROLL.puml**: File PlantUML chứa Use Case Diagram chi tiết cho hệ thống ca làm việc và lương

## 🎯 Mục Đích Phân Rã

Use Case "Quản lý ca làm việc và lương" đã được phân rã thành subsystem riêng vì:

### 1. Tuân Thủ Nguyên Tắc "Một Mục Tiêu Duy Nhất"
Mỗi Use Case con có một mục tiêu nghiệp vụ riêng biệt:
- **Quản lý mẫu ca**: Mục tiêu là định nghĩa khung ca chuẩn để tái sử dụng
- **Quản lý ca làm việc**: Mục tiêu là tạo và quản lý các ca làm thực tế
- **Phân công ca**: Mục tiêu là gán nhân viên vào ca cụ thể
- **Chấm công**: Mục tiêu là ghi nhận thời gian check-in/check-out
- **Điều chỉnh hiệu suất**: Mục tiêu là ghi nhận thưởng/phạt
- **Quản lý chu kỳ lương**: Mục tiêu là quản trị chu kỳ lương
- **Xem bảng lương**: Mục tiêu là xem tổng hợp lương nhân viên

### 2. Tính Tái Sử Dụng (Include)
- **Tính toán lại phân công**: Logic chung được sử dụng bởi nhiều Use Case:
  - Chấm công cần tính toán lại phân công sau khi check-in/out
  - Điều chỉnh hiệu suất cần tính toán lại phân công sau khi thêm/sửa/xóa điều chỉnh
  - Quản lý chu kỳ lương cần tính toán lại khi tái tạo bảng lương

### 3. Phân Quyền Khác Biệt
- **Chấm công**: Tất cả actors (Staff, Manager, Admin) - nhân viên tự chấm công
- **Các Use Case khác**: Chỉ Manager và Admin - quản lý và điều hành

## 📊 Các Use Cases

### 1. Quản lý mẫu ca (UC_ShiftTemplate)
- **Actors**: Admin, Manager
- **Mục tiêu**: Định nghĩa khung ca chuẩn (giờ bắt đầu/kết thúc, lương theo giờ, phụ cấp)
- **Đặc điểm**: Tái sử dụng khi tạo ca cụ thể, giảm lỗi nhập thủ công

### 2. Quản lý ca làm việc (UC_ShiftInstance)
- **Actors**: Admin, Manager
- **Mục tiêu**: Tạo và quản lý các ca làm thực tế dựa trên mẫu ca
- **Đặc điểm**: Liên kết với phân công nhân viên và chấm công

### 3. Phân công ca (UC_ShiftAssignment)
- **Actors**: Admin, Manager
- **Mục tiêu**: Gán nhân viên vào ca cụ thể, quản lý thông tin thời gian và mức lương
- **Đặc điểm**: Theo dõi trạng thái, số đơn, doanh thu, lương thực nhận

### 4. Chấm công (UC_Attendance)
- **Actors**: Admin, Manager, Staff
- **Mục tiêu**: Ghi nhận thời gian check-in/check-out của nhân viên
- **Include**: Tính toán lại phân công (tự động cập nhật sau khi chấm công)
- **Đặc điểm**: Tự động tính phút đi trễ/về sớm, cập nhật trạng thái phân công

### 5. Điều chỉnh hiệu suất (UC_PerformanceAdjustment)
- **Actors**: Admin, Manager
- **Mục tiêu**: Ghi nhận thưởng/phạt (bonus/penalty) cho phân công ca
- **Include**: Tính toán lại phân công (tự động cập nhật lương sau điều chỉnh)
- **Đặc điểm**: Cho phép tạo, thu hồi và xóa điều chỉnh

### 6. Quản lý chu kỳ lương (UC_PayrollCycle)
- **Actors**: Admin, Manager
- **Mục tiêu**: Quản trị chu kỳ lương (tạo, cập nhật, phê duyệt)
- **Include**: Tính toán lại phân công (khi tái tạo bảng lương)
- **Đặc điểm**: Tổng hợp bảng lương theo ca/nhân viên

### 7. Xem bảng lương (UC_PayrollSummary)
- **Actors**: Admin, Manager
- **Mục tiêu**: Xem tổng hợp bảng lương nhân viên
- **Đặc điểm**: Dựa trên dữ liệu phân công, chấm công, điều chỉnh hiệu suất

### 8. Tính toán lại phân công (UC_RecalculateAssignment) - Use Case Chung
- **Mục đích**: Logic tái sử dụng để tính toán lại giờ công, doanh thu, lương, bonus/penalty
- **Được include bởi**: Chấm công, Điều chỉnh hiệu suất, Quản lý chu kỳ lương
- **Đặc điểm**: Không có actor trực tiếp, chỉ được gọi bởi các Use Case khác

## 🔗 Mối Quan Hệ

### Include (Bắt buộc)
- `Chấm công` include `Tính toán lại phân công` (sau khi check-in/out)
- `Điều chỉnh hiệu suất` include `Tính toán lại phân công` (sau khi thêm/sửa/xóa điều chỉnh)
- `Quản lý chu kỳ lương` include `Tính toán lại phân công` (khi tái tạo bảng lương)

## ✅ Tiêu Chuẩn Phân Rã Đã Áp Dụng

- ✅ **Một mục tiêu duy nhất**: Mỗi Use Case có mục tiêu nghiệp vụ riêng
- ✅ **Tính tái sử dụng**: Tính toán lại phân công được tách ra làm logic chung
- ✅ **Phân quyền rõ ràng**: Mỗi Use Case có actors riêng (Staff chỉ có quyền chấm công)
- ✅ **Độ phức tạp hợp lý**: Mỗi Use Case có 5-15 bước, không quá dài
- ✅ **Giá trị nghiệp vụ**: Mỗi Use Case đứng một mình vẫn có ý nghĩa

## 🚀 Cách Xem

Xem tương tự như file USE_CASE_DIAGRAM.puml:
1. Online: http://www.plantuml.com/plantuml/
2. VS Code: Extension "PlantUML" + `Alt + D`
3. IntelliJ IDEA: Plugin "PlantUML integration"

## 📝 Lưu Ý

- File này là **subsystem riêng** để quản lý Use Case ca làm việc và lương
- File chính (USE_CASE_DIAGRAM.puml) vẫn hiển thị subsystem này nhưng chi tiết nằm ở file riêng
- Khi cần cập nhật Use Case ca làm việc và lương, chỉnh sửa file này
- File chính sẽ tự động cập nhật vì dùng cùng Use Case names

