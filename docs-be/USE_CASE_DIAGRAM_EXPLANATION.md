# Use Case Diagram - Hệ Thống Quản Lý Quán Cà Phê

## 📋 Tổng Quan

Tài liệu này mô tả chi tiết Use Case Diagram của hệ thống quản lý quán cà phê, tuân thủ đúng các tiêu chuẩn UML và quy tắc vẽ Use Case Diagram.

## 🎯 Các Thành Phần Cơ Bản

### 1. Tác Nhân (Actors)

#### 1.1. Tác Nhân Con Người
- **Admin** (Quản trị viên): Người có quyền cao nhất, quản lý toàn bộ hệ thống
- **Manager** (Quản lý): Người quản lý vận hành hàng ngày, có quyền hạn trung bình
- **Staff** (Nhân viên): Người thực hiện các tác vụ cơ bản như tạo đơn, thanh toán
- **Customer** (Khách hàng): Người có tài khoản để tích điểm và xem lịch sử mua hàng

#### 1.2. Tác Nhân Hệ Thống Ngoại Lai (External Systems)
- **Payment Gateway**: Hệ thống thanh toán bên thứ 3 (có thể tích hợp trong tương lai)
- **Google Gemini AI**: Hệ thống AI hỗ trợ chat và các tính năng thông minh

### 2. Ca Sử Dụng (Use Cases)

Tất cả Use Case đều tuân thủ quy tắc: **Động từ + Danh từ**

#### 2.1. Nhóm Xác Thực & Quản Lý Người Dùng
- **Đăng nhập**: Xác thực người dùng vào hệ thống
- **Đăng ký**: Tạo tài khoản mới (chỉ Admin/Manager)
- **Quản lý người dùng**: CRUD người dùng (chỉ Admin)
- **Xem lịch sử đăng nhập**: Xem nhật ký đăng nhập (chỉ Admin)
- **Đổi mật khẩu**: Thay đổi mật khẩu của chính mình

#### 2.2. Nhóm Quản Lý Sản Phẩm & Danh Mục
- **Quản lý sản phẩm**: CRUD sản phẩm
- **Quản lý danh mục**: CRUD danh mục sản phẩm
- **Quản lý công thức sản phẩm**: Quản lý nguyên liệu và công thức cho từng sản phẩm

#### 2.3. Nhóm Quản Lý Đơn Hàng & Thanh Toán
- **Quản lý đơn hàng**: Tạo, xem, cập nhật, hủy đơn hàng
- **Thanh toán đơn hàng**: Xử lý thanh toán cho đơn hàng
- **Sử dụng voucher**: Áp dụng mã giảm giá (tùy chọn)
- **Xác thực thanh toán**: Xác thực với hệ thống thanh toán bên thứ 3

#### 2.4. Nhóm Quản Lý Khách Hàng & Bàn
- **Quản lý khách hàng**: CRUD khách hàng, tích điểm
- **Quản lý bàn**: CRUD bàn, quản lý trạng thái bàn
- **Quản lý voucher**: CRUD voucher, kiểm tra hiệu lực

#### 2.5. Nhóm Quản Lý Kho & Nhà Cung Cấp
- **Quản lý kho**: Quản lý nguyên liệu, tồn kho
- **Quản lý nhà cung cấp**: CRUD nhà cung cấp
- **Quản lý phiếu nhập hàng**: Tạo và quản lý phiếu nhập hàng
- **Quản lý chi phí**: Ghi nhận và quản lý chi phí

#### 2.6. Nhóm Báo Cáo & Phân Tích
- **Xem báo cáo**: Xem các báo cáo doanh thu, lợi nhuận, thống kê
- **Xuất báo cáo Excel**: Xuất báo cáo ra file Excel
- **Xem dashboard**: Xem tổng quan số liệu theo vai trò

#### 2.7. Nhóm Quản Lý Ca Làm Việc & Lương
- **Quản lý ca làm việc**: Tạo mẫu ca, ca thực tế, phân công
- **Chấm công**: Check-in/check-out ca làm việc
- **Quản lý lương**: Tính toán và quản lý lương nhân viên
- **Quản lý điều chỉnh hiệu suất**: Thưởng/phạt nhân viên

#### 2.8. Nhóm Khác
- **Quản lý file**: Upload, download, quản lý file ảnh sản phẩm
- **Chat**: Gửi/nhận tin nhắn giữa nhân viên

### 3. Biên Hệ Thống (System Boundary)

Hình chữ nhật bao quanh tất cả Use Case, phân định rõ phạm vi hệ thống. Tác nhân nằm bên ngoài, Use Case nằm bên trong.

## 🔗 Các Mối Quan Hệ

### 1. Association (Liên Kết)
- **Ký hiệu**: Đường thẳng đặc (không mũi tên hoặc mũi tên mở)
- **Ý nghĩa**: Kết nối Actor với Use Case, thể hiện sự tương tác trực tiếp
- **Ví dụ**: `Admin --> UC_ManageUsers` (Admin tương tác với "Quản lý người dùng")

### 2. Include (Bao Hàm) - Bắt Buộc
- **Ký hiệu**: Mũi tên nét đứt + `<<include>>`
- **Ý nghĩa**: Use Case A **bắt buộc phải có** Use Case B để hoàn thành
- **Hướng mũi tên**: Từ A trỏ sang B (A → B)
- **Ví dụ trong hệ thống**:
  - `Thanh toán đơn hàng` include `Xác thực thanh toán` (luôn phải xác thực khi thanh toán)
  - `Quản lý đơn hàng` include `Quản lý bàn` (tạo đơn phải quản lý trạng thái bàn)
  - `Thanh toán đơn hàng` include `Quản lý kho` (thanh toán phải trừ tồn kho)
  - `Xuất báo cáo Excel` include `Xem báo cáo` (phải có báo cáo trước khi xuất)

### 3. Extend (Mở Rộng) - Tùy Chọn
- **Ký hiệu**: Mũi tên nét đứt + `<<extend>>`
- **Ý nghĩa**: Use Case B chỉ xảy ra **khi có điều kiện cụ thể** trong Use Case A
- **Hướng mũi tên**: Từ B trỏ ngược về A (B → A)
- **Ví dụ trong hệ thống**:
  - `Sử dụng voucher` extend `Quản lý đơn hàng` (chỉ áp dụng voucher khi có đơn hàng và khách có mã)
  - `Quản lý khách hàng` extend `Thanh toán đơn hàng` (chỉ cộng điểm khách hàng khi thanh toán và có customer)

## 📊 Phân Quyền Theo Vai Trò

### Admin (Toàn Quyền)
Có quyền truy cập tất cả Use Case trong hệ thống, bao gồm:
- Quản lý người dùng và lịch sử đăng nhập
- Tất cả chức năng quản lý (sản phẩm, đơn hàng, kho, ca làm việc, lương...)
- Xem và xuất báo cáo
- Quản lý file

### Manager (Quản Lý)
Có quyền hạn trung bình, không bao gồm:
- Đăng ký tài khoản mới
- Xem lịch sử đăng nhập
- Quản lý người dùng

Có quyền:
- Tất cả chức năng quản lý nghiệp vụ (sản phẩm, đơn hàng, kho, ca làm việc, lương...)
- Xem và xuất báo cáo
- Quản lý file

### Staff (Nhân Viên)
Có quyền cơ bản:
- Đăng nhập, đổi mật khẩu
- Quản lý đơn hàng, thanh toán
- Quản lý khách hàng, bàn
- Xem dashboard cá nhân
- Chấm công
- Chat

### Customer (Khách Hàng)
Có quyền hạn chế:
- Đăng nhập, đổi mật khẩu
- Quản lý thông tin cá nhân (xem, cập nhật)

## ✅ Kiểm Tra Tiêu Chuẩn

### ✓ Đúng Cấu Trúc
- [x] Có đủ 3 thành phần: Actor, Use Case, System Boundary
- [x] Tên Actor là danh từ chỉ vai trò
- [x] Tên Use Case bắt đầu bằng Động từ + Danh từ
- [x] System Boundary bao quanh Use Case

### ✓ Đúng Mối Quan Hệ
- [x] Include: Mũi tên từ Use Case chính → Use Case phụ (bắt buộc)
- [x] Extend: Mũi tên từ Use Case tùy chọn → Use Case chính
- [x] Association: Đường thẳng kết nối Actor với Use Case

### ✓ Đầy Đủ Chức Năng
- [x] Bao phủ hết các tác nhân (Admin, Manager, Staff, Customer, External Systems)
- [x] Đủ các chức năng nghiệp vụ chính
- [x] Gom nhóm hợp lý, không vụn vặt

### ✓ Tránh Lỗi Thường Gặp
- [x] Không có mũi tên nối Use Case theo thứ tự thời gian (flowchart)
- [x] Không có "spiderweb" - sắp xếp hợp lý
- [x] Có System Boundary rõ ràng

## 📝 Ghi Chú

1. **Generalization cho Actor**: Không sử dụng trong diagram này vì mỗi vai trò có quyền hạn riêng biệt, không phải quan hệ kế thừa.

2. **Payment Gateway**: Hiện tại hệ thống chưa tích hợp payment gateway thực tế, nhưng đã có cấu trúc sẵn sàng cho việc tích hợp.

3. **Google Gemini AI**: Được sử dụng trong module Chat để hỗ trợ trả lời tự động.

4. **Use Case "Chat"**: Chỉ có trong hệ thống nếu module chat được kích hoạt.

## 🔧 Cách Sử Dụng File PlantUML

1. Cài đặt PlantUML plugin cho IDE (VS Code, IntelliJ IDEA) hoặc sử dụng online tại http://www.plantuml.com/plantuml/
2. Mở file `USE_CASE_DIAGRAM.puml`
3. Render để xem diagram
4. Export ra PNG/SVG nếu cần

## 📚 Tài Liệu Tham Khảo

- [UML Use Case Diagram Specification](https://www.uml-diagrams.org/use-case-diagrams.html)
- [PlantUML Use Case Diagram Guide](https://plantuml.com/use-case-diagram)

