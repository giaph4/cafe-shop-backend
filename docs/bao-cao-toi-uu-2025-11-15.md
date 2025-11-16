# Báo Cáo Tối Ưu Mã Nguồn – 15/11/2025

## 1. Mục tiêu
- Giảm tải N+1 query và tránh tải dữ liệu thừa.
- Chuẩn hóa việc kết hợp Specification để cải thiện khả năng tái sử dụng và đọc hiểu.
- Giảm chi phí bộ nhớ cho các truy vấn phân trang trên bảng lớn.

## 2. Tóm tắt kết quả
```json
{
  "highlights": [
    {
      "issue": "N+1 query khi tải PurchaseOrderDetail ở tất cả màn hình",
      "severity": "HIGH",
      "impact": "Truy vấn liệt kê phiếu nhập bị nhân đôi số lượng query",
      "status": "RESOLVED"
    },
    {
      "issue": "Tải eager toàn bộ vai trò của người dùng",
      "severity": "HIGH",
      "impact": "Phân trang user tiêu tốn bộ nhớ và kéo dài thời gian phản hồi",
      "status": "RESOLVED"
    },
    {
      "issue": "Specification lẫn điều kiện khó bảo trì",
      "severity": "MEDIUM",
      "impact": "Khó tái sử dụng, tiềm ẩn lỗi khi bổ sung tiêu chí mới",
      "status": "RESOLVED"
    },
    {
      "issue": "Product code chưa chuẩn hóa và lọc sản phẩm thiếu guard",
      "severity": "HIGH",
      "impact": "Dễ trùng mã sản phẩm, truy vấn lọc sinh câu lệnh dư thừa và lỗi nhập liệu",
      "status": "RESOLVED"
    },
    {
      "issue": "Kiểm tra voucher chưa chặt chẽ, bộ lọc ngày sai phía Repository",
      "severity": "HIGH",
      "impact": "Rủi ro áp dụng voucher với dữ liệu không hợp lệ và kết quả tìm kiếm lệch",
      "status": "RESOLVED"
    }
  ]
}
```

## 3. Chi tiết thay đổi
### 3.1 PurchaseOrder lazy association
@src/main/java/com/giapho/coffee_shop_backend/domain/entity/PurchaseOrder.java#3-55
- Chuyển `fetch` từ `EAGER` sang `LAZY`, thêm `@BatchSize(20)` để giảm N+1 query.
- Lợi ích: Giảm truy vấn thừa khi chỉ cần danh sách phiếu nhập, vẫn đảm bảo hiệu năng khi cần tải chi tiết hàng loạt.

### 3.2 User roles lazy loading
@src/main/java/com/giapho/coffee_shop_backend/domain/entity/User.java#55-107
- Đổi `@ManyToMany(fetch = FetchType.LAZY)` cho tập Roles.
- Giảm chi phí bộ nhớ, hỗ trợ phân trang người dùng tốt hơn.

### 3.3 PurchaseOrderService tối ưu Specification và ánh xạ
@src/main/java/com/giapho/coffee_shop_backend/service/PurchaseOrderService.java#31-205
- Thu gọn builder điều kiện Specification bằng danh sách predicate.
- Đảm bảo chi tiết phiếu nhập gắn trực tiếp vào PO, loại bỏ truy vấn lại không cần thiết.

### 3.4 Role entity utility
@src/main/java/com/giapho/coffee_shop_backend/domain/entity/Role.java#9-43
- Bổ sung `asAuthority()` để thống nhất điểm truy cập quyền hạn.

### 3.5 ProductService chuẩn hóa code & lọc an toàn
@src/main/java/com/giapho/coffee_shop_backend/service/ProductService.java#5-310
- Chuẩn hóa mã sản phẩm (trim, upper-case) khi tạo/cập nhật, tái sử dụng helper tránh lặp.
- Bổ sung guard `StringUtils.hasText`, nâng cấp Specification để tránh điều kiện rỗng và kiểm tra danh mục tồn tại.
- Lợi ích: giảm nguy cơ trùng mã, truy vấn ổn định hơn khi người dùng nhập chuỗi trắng.

### 3.6 VoucherService siết chặt validation
@src/main/java/com/giapho/coffee_shop_backend/service/VoucherService.java#28-308
- Kiểm tra giá trị đơn hàng âm, chuẩn hóa mô tả với `safeTrim`.
- Sửa bộ lọc tìm kiếm dùng `validFrom/validTo` đúng chiều, tránh trả voucher sai phạm vi thời gian.
- Lợi ích: đảm bảo bảo mật và độ tin cậy cho quy trình áp dụng voucher.

## 4. Đề xuất tiếp theo
1. Rà soát các dịch vụ còn dùng EAGER mặc định với tập dữ liệu lớn.
2. Chuẩn hóa caching cho các bảng danh mục (Category, Supplier).
3. Bổ sung test tích hợp xác nhận không phát sinh N+1 với JPA.
4. Thêm unit test cho chuẩn hóa mã sản phẩm và kiểm tra filter voucher.

---
**Mức độ hoàn thiện:** 55%
**Hạng mục còn thiếu:** Caching danh mục, kiểm thử tự động bổ sung, unit test lọc voucher & mã sản phẩm
