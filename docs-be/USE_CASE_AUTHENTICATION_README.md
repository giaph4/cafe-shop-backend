# Use Case Diagram - Hệ Thống Xác Thực (Subsystem)

## 📁 File

- **USE_CASE_AUTHENTICATION.puml**: File PlantUML chứa Use Case Diagram chi tiết cho hệ thống xác thực

## 🎯 Mục Đích Phân Rã

Use Case "Quản lý xác thực" đã được phân rã thành subsystem riêng vì:

### 1. Tuân Thủ Nguyên Tắc "Một Mục Tiêu Duy Nhất"
Mỗi Use Case con có một mục tiêu nghiệp vụ riêng biệt:
- **Đăng nhập**: Mục tiêu là xác thực và nhận token để truy cập hệ thống
- **Đăng ký**: Mục tiêu là tạo tài khoản mới cho nhân viên
- **Đổi mật khẩu**: Mục tiêu là bảo mật tài khoản bằng cách thay đổi mật khẩu
- **Xem lịch sử đăng nhập**: Mục tiêu là audit và theo dõi bảo mật

### 2. Tính Tái Sử Dụng (Include)
- **Xác thực JWT**: Logic chung được sử dụng bởi nhiều Use Case:
  - Đăng nhập cần xác thực JWT để kiểm tra token
  - Đổi mật khẩu cần xác thực JWT để đảm bảo user đã đăng nhập
  - Xem lịch sử đăng nhập cần xác thực JWT để kiểm tra quyền Admin

### 3. Phân Quyền Khác Biệt
- **Đăng nhập**: Tất cả actors (Public)
- **Đăng ký**: Chỉ Admin và Manager
- **Đổi mật khẩu**: Tất cả actors (sau khi đăng nhập)
- **Xem lịch sử đăng nhập**: Chỉ Admin

## 📊 Các Use Cases

### 1. Đăng nhập (UC_Login)
- **Actors**: Admin, Manager, Staff
- **Mục tiêu**: Xác thực người dùng và cấp JWT token
- **Include**: Không có (đăng nhập là để TẠO JWT, không phải xác thực JWT)
- **Đặc điểm**: Public endpoint, không cần authentication

### 2. Đăng ký (UC_Register)
- **Actors**: Admin, Manager
- **Mục tiêu**: Tạo tài khoản mới cho nhân viên
- **Include**: Không có (dùng @PreAuthorize với role, không cần JWT token)
- **Đặc điểm**: Yêu cầu quyền ADMIN hoặc MANAGER qua Spring Security @PreAuthorize

### 3. Đổi mật khẩu (UC_ChangePassword)
- **Actors**: Admin, Manager, Staff
- **Mục tiêu**: Thay đổi mật khẩu của chính mình
- **Include**: Xác thực JWT (phải đăng nhập trước)
- **Đặc điểm**: Yêu cầu đã đăng nhập, kiểm tra mật khẩu cũ

### 4. Xem lịch sử đăng nhập (UC_ViewLoginHistory)
- **Actors**: Admin
- **Mục tiêu**: Xem nhật ký đăng nhập để audit bảo mật
- **Include**: Xác thực JWT (kiểm tra quyền Admin)
- **Đặc điểm**: Chỉ Admin, có phân trang và filter

### 5. Xác thực JWT (UC_ValidateJWT) - Use Case Chung
- **Mục đích**: Logic tái sử dụng để xác thực JWT token (kiểm tra token hợp lệ, chưa hết hạn, có quyền)
- **Được include bởi**: Đổi mật khẩu, Xem lịch sử đăng nhập
- **Đặc điểm**: Không có actor trực tiếp, chỉ được gọi bởi các Use Case cần authentication
- **Lưu ý**: Đăng nhập KHÔNG include Use Case này vì đăng nhập là để TẠO JWT, không phải xác thực JWT

## 🔗 Mối Quan Hệ

### Include (Bắt buộc)
- `Đổi mật khẩu` include `Xác thực JWT` (cần token để xác thực user đã đăng nhập)
- `Xem lịch sử đăng nhập` include `Xác thực JWT` (cần token và quyền Admin)

### Không Include
- `Đăng nhập` KHÔNG include `Xác thực JWT` (vì đăng nhập là để TẠO JWT, không phải xác thực)
- `Đăng ký` KHÔNG include `Xác thực JWT` (vì dùng @PreAuthorize với role, không cần JWT token)

## ✅ Tiêu Chuẩn Phân Rã Đã Áp Dụng

- ✅ **Một mục tiêu duy nhất**: Mỗi Use Case có mục tiêu nghiệp vụ riêng
- ✅ **Tính tái sử dụng**: Xác thực JWT được tách ra làm logic chung
- ✅ **Phân quyền rõ ràng**: Mỗi Use Case có actors riêng
- ✅ **Độ phức tạp hợp lý**: Mỗi Use Case có 5-10 bước, không quá dài
- ✅ **Giá trị nghiệp vụ**: Mỗi Use Case đứng một mình vẫn có ý nghĩa

## 🚀 Cách Xem

Xem tương tự như file USE_CASE_DIAGRAM.puml:
1. Online: http://www.plantuml.com/plantuml/
2. VS Code: Extension "PlantUML" + `Alt + D`
3. IntelliJ IDEA: Plugin "PlantUML integration"

## 📝 Lưu Ý

- File này là **subsystem riêng** để quản lý Use Case xác thực
- File chính (USE_CASE_DIAGRAM.puml) vẫn hiển thị subsystem này nhưng chi tiết nằm ở file riêng
- Khi cần cập nhật Use Case xác thực, chỉnh sửa file này
- File chính sẽ tự động cập nhật vì dùng cùng Use Case names

