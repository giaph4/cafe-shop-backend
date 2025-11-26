# Sequence Diagrams - Hệ Thống Xác Thực

## 📁 Files

Các file Sequence Diagram cho hệ thống xác thực:

1. **SEQUENCE_DIAGRAM_LOGIN.puml** - Đăng nhập
2. **SEQUENCE_DIAGRAM_REGISTER.puml** - Đăng ký
3. **SEQUENCE_DIAGRAM_CHANGE_PASSWORD.puml** - Đổi mật khẩu
4. **SEQUENCE_DIAGRAM_VIEW_LOGIN_HISTORY.puml** - Xem lịch sử đăng nhập
5. **SEQUENCE_DIAGRAM_VALIDATE_JWT.puml** - Xác thực JWT (Include Use Case)

## 🎯 Tiêu Chuẩn Đã Áp Dụng

### 1. Ký Hiệu Chuẩn UML
- ✅ **Lifeline**: Đường nét đứt dọc xuống cho mỗi đối tượng
- ✅ **Activation Bar**: Hình chữ nhật hẹp trên Lifeline khi đối tượng đang xử lý
- ✅ **Sync Message**: Mũi tên nét liền, đầu đặc (➤) cho method call đồng bộ
- ✅ **Return Message**: Mũi tên nét đứt, đầu hở (<--) cho kết quả trả về
- ✅ **Async Message**: Không sử dụng (hệ thống này dùng đồng bộ)

### 2. Sắp Xếp Đối Tượng
Theo thứ tự tương tác từ trái sang phải:
- **Actor** → **Boundary (Controller)** → **Control (Service)** → **Entity (Repository)** → **Database**

### 3. Combined Fragments (Logic Rẽ Nhánh)
- ✅ **alt**: Dùng cho if...else (Đăng nhập thành công/thất bại, Validation pass/fail)
- ✅ **opt**: Dùng cho if (không có else) - roleIds trống trong Đăng ký
- ✅ **loop**: Dùng cho vòng lặp - for each roleId trong Đăng ký, for each LoginHistory trong Xem lịch sử

### 4. Mức Độ Chi Tiết
- ✅ Không vẽ các hàm get/set đơn giản
- ✅ Chỉ vẽ các message mang tính nghiệp vụ quan trọng
- ✅ Đồng nhất với Use Case - các bước trong Use Case khớp với message trong Sequence Diagram
- ✅ Self-Call: Sử dụng khi một lớp gọi hàm nội bộ (normalize, extractClientIp, buildClaims)

### 5. Tránh Lỗi Thường Gặp
- ✅ Không có "God Class" - trách nhiệm được phân chia rõ ràng
- ✅ Focus of Control (Activation Bar) rõ ràng, không bị gãy vô lý
- ✅ Không có thao tác trực tiếp vào DB từ Controller - luôn đi qua Service và Repository
- ✅ Không quá tải - mỗi diagram mô tả một Use Case cụ thể

## 📊 Chi Tiết Từng Sequence Diagram

### 1. Đăng nhập (Login)
**Luồng chính:**
1. User gửi LoginRequest
2. Controller validate input
3. Service normalize và extract IP/UserAgent
4. AuthenticationManager xác thực
5. Kiểm tra tài khoản active
6. Ghi lịch sử đăng nhập thành công
7. Tạo JWT token
8. Trả về AuthenticationResponse

**Luồng lỗi:**
- Username/password rỗng → HTTP 401
- Sai username/password → HTTP 401
- Tài khoản bị khóa → HTTP 403

### 2. Đăng ký (Register)
**Luồng chính:**
1. Admin gửi RegisterRequest
2. SecurityConfig kiểm tra quyền ADMIN/MANAGER
3. Service validate input
4. Kiểm tra username/email trùng
5. Resolve roles (nếu trống → ROLE_STAFF)
6. Encode password
7. Tạo và lưu User
8. Tạo JWT token
9. Trả về AuthenticationResponse

**Luồng lỗi:**
- Không có quyền → HTTP 403
- Validation thất bại → HTTP 400
- Username/Email trùng → HTTP 400

### 3. Đổi mật khẩu (Change Password)
**Luồng chính:**
1. User gửi ChangePasswordRequest với JWT token
2. JwtAuthenticationFilter validate token
3. Service lấy current user từ SecurityContext
4. Kiểm tra mật khẩu hiện tại
5. Validate mật khẩu mới
6. Kiểm tra mật khẩu mới khác mật khẩu cũ
7. Encode và cập nhật mật khẩu
8. Trả về success

**Luồng lỗi:**
- Token không hợp lệ → HTTP 401
- Mật khẩu hiện tại sai → HTTP 400
- Mật khẩu mới không hợp lệ → HTTP 400
- Mật khẩu mới trùng cũ → HTTP 400

### 4. Xem lịch sử đăng nhập (View Login History)
**Luồng chính:**
1. Admin gửi request với JWT token
2. JwtAuthenticationFilter validate token và quyền ADMIN
3. Service build Specification từ query params
4. Repository query với filter
5. Map LoginHistory → DTO
6. Trả về Page<LoginHistoryResponseDTO>

**Luồng lỗi:**
- Token không hợp lệ → HTTP 401
- Không có quyền ADMIN → HTTP 403

### 5. Xác thực JWT (Validate JWT) - Include Use Case
**Luồng chính:**
1. JwtAuthenticationFilter extract token từ request
2. JwtService parse và verify signature
3. Kiểm tra expiration
4. Extract claims (userId, username, roles)
5. Load User từ database
6. Kiểm tra User active
7. Build Authentication object
8. Set SecurityContext

**Luồng lỗi:**
- Token không hợp lệ → HTTP 401
- Token hết hạn → HTTP 401
- User không tồn tại/bị khóa → HTTP 401

## 🚀 Cách Xem

1. **Online**: http://www.plantuml.com/plantuml/ - Paste nội dung file
2. **VS Code**: Extension "PlantUML" + `Alt + D`
3. **IntelliJ IDEA**: Plugin "PlantUML integration"

## 📝 Lưu Ý

- Mỗi diagram mô tả một Use Case cụ thể
- Các diagram tuân thủ đúng tiêu chuẩn UML 2.5
- Có thể đọc to diagram như kể chuyện để kiểm tra logic
- Tất cả các luồng lỗi đều được mô tả đầy đủ

