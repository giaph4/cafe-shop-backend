# Use Case Diagram - Hướng Dẫn Sử Dụng

## 📁 Files

- **USE_CASE_DIAGRAM.puml**: File PlantUML chứa định nghĩa Use Case Diagram
- **USE_CASE_DIAGRAM_EXPLANATION.md**: Tài liệu giải thích chi tiết về diagram
- **USE_CASE_DIAGRAM_README.md**: File này - hướng dẫn nhanh

## 🚀 Cách Xem Diagram

### Option 1: Online (Khuyến nghị)
1. Truy cập: http://www.plantuml.com/plantuml/
2. Copy toàn bộ nội dung file `USE_CASE_DIAGRAM.puml`
3. Paste vào editor
4. Diagram sẽ tự động render

### Option 2: VS Code
1. Cài extension "PlantUML" trong VS Code
2. Mở file `USE_CASE_DIAGRAM.puml`
3. Nhấn `Alt + D` để preview
4. Export ra PNG/SVG nếu cần

### Option 3: IntelliJ IDEA
1. Cài plugin "PlantUML integration"
2. Mở file `USE_CASE_DIAGRAM.puml`
3. Nhấn `Ctrl + Shift + P` → "PlantUML: Show Diagram"
4. Export ra PNG/SVG nếu cần

### Option 4: Command Line
```bash
# Cài đặt PlantUML (cần Java)
# Windows: choco install plantuml
# Mac: brew install plantuml
# Linux: sudo apt-get install plantuml

# Generate PNG
plantuml USE_CASE_DIAGRAM.puml

# Generate SVG
plantuml -tsvg USE_CASE_DIAGRAM.puml
```

## ✅ Kiểm Tra Tiêu Chuẩn

Diagram này đã được kiểm tra và tuân thủ đầy đủ các tiêu chuẩn:

- ✅ **3 thành phần cơ bản**: Actor, Use Case, System Boundary
- ✅ **Tên Actor**: Danh từ chỉ vai trò (Admin, Manager, Staff, Customer)
- ✅ **Tên Use Case**: Động từ + Danh từ (Đăng nhập, Quản lý đơn hàng...)
- ✅ **System Boundary**: Hình chữ nhật bao quanh Use Case
- ✅ **Include**: Mũi tên từ Use Case chính → Use Case phụ (bắt buộc)
- ✅ **Extend**: Mũi tên từ Use Case tùy chọn → Use Case chính
- ✅ **Association**: Đường thẳng kết nối Actor với Use Case
- ✅ **Đầy đủ**: Bao phủ hết tác nhân và chức năng chính
- ✅ **Gom nhóm hợp lý**: Không vụn vặt, gom các chức năng liên quan

## 📊 Tổng Quan

- **6 Actors**: Admin, Manager, Staff, Customer, Payment Gateway, Google Gemini AI
- **25 Use Cases**: Bao phủ toàn bộ chức năng hệ thống
- **4 Include relationships**: Mối quan hệ bắt buộc
- **2 Extend relationships**: Mối quan hệ tùy chọn

## 🔍 Chi Tiết

Xem file **USE_CASE_DIAGRAM_EXPLANATION.md** để biết:
- Giải thích từng Actor và Use Case
- Mô tả chi tiết các mối quan hệ
- Phân quyền theo vai trò
- Checklist kiểm tra tiêu chuẩn

## 📝 Lưu Ý

1. Diagram này mô tả **toàn bộ hệ thống** ở mức tổng quát (high-level)
2. Không mô tả chi tiết luồng xử lý (flow) - đó là nhiệm vụ của Sequence Diagram hoặc Activity Diagram
3. Các Use Case đã được **gom nhóm** để tránh vụn vặt (ví dụ: "Quản lý sản phẩm" thay vì "Thêm sản phẩm", "Sửa sản phẩm", "Xóa sản phẩm")

## 🎯 Mục Đích Sử Dụng

Use Case Diagram này phục vụ:
- **Tài liệu hóa**: Mô tả chức năng hệ thống cho stakeholders
- **Phân tích yêu cầu**: Xác định các tác nhân và mục tiêu của họ
- **Thiết kế hệ thống**: Làm cơ sở cho việc thiết kế chi tiết
- **Giao tiếp**: Giúp team hiểu rõ phạm vi và chức năng hệ thống

