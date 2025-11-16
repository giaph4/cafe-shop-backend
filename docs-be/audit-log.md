# Chức năng: Ghi nhật ký thao tác (Audit Log)

## Vai trò trong hệ thống
- Lưu lại mọi hành động quan trọng (tạo/sửa/xóa/điều chỉnh) để phục vụ audit và điều tra sự cố.
- Ghi nhận người thực hiện, quyền hạn, ngữ cảnh request (URI, phương thức, IP, User-Agent).
- Được sử dụng bởi các service khác khi cần ghi log.

## Thành phần liên quan
- **Service**: `AuditLogService` @src/main/java/com/giapho/coffee_shop_backend/service/AuditLogService.java#1-88
- **Repository**: `AuditLogRepository` @src/main/java/com/giapho/coffee_shop_backend/domain/repository/AuditLogRepository.java#1-9
- **Entity**: `AuditLog` (chứa action, resourceType, resourceId, success, summary, details, errorMessage, actor info, request info, timestamps).
- **Security**: sử dụng `SecurityContextHolder` để lấy user hiện tại; hỗ trợ cả `User` và `UserDetails`.
- **Request context**: dùng `RequestContextHolder` để lấy URI, HTTP method, IP, User-Agent.

## Cách sử dụng `AuditLogService`
### `recordAction(String action, String resourceType, String resourceId, boolean success, String summary, String details, String errorMessage)`
- **Workflow**:
  1. Tạo `AuditLog` với thời gian thực hiện (`LocalDateTime.now()`), action, resource, trạng thái thành công/thất bại, summary, details, errorMessage.
  2. Gọi `applySecurityContext` để ghi thông tin người thao tác:
     - Nếu principal là `User`: set `actorId`, `actorUsername`.
     - Nếu principal là `UserDetails`: set `actorUsername`.
     - Nếu không: dùng `authentication.getName()`.
     - Lấy danh sách quyền (`GrantedAuthority`) -> set `actorRoles` (chuỗi phân cách bằng dấu phẩy).
  3. Gọi `applyRequestContext` để ghi `requestUri`, `httpMethod`, `ipAddress`, `userAgent`.
  4. Lưu log qua `auditLogRepository.save(log)`.

### Ví dụ sử dụng
a. Điều chỉnh tồn kho (`IngredientService.adjustInventory`):
- Ghi log khi điều chỉnh thành công/thất bại (hành động `INGREDIENT_INVENTORY_ADJUSTED`, `INGREDIENT_INVENTORY_ADJUSTMENT_FAILED`).
- Summary/Details chứa số lượng cũ/mới, lý do, message lỗi.

b. Shift performance adjustment:
- Ghi lại khi tạo/thu hồi/xóa thưởng phạt (log thông qua `shiftAssignmentService.recalculateAssignment` có thể gọi audit).

## Dữ liệu lưu trong `AuditLog`
- `eventTime`: thời điểm xảy ra hành động.
- `action`: mã hành động (ví dụ: `INGREDIENT_INVENTORY_ADJUSTED`).
- `resourceType`: loại tài nguyên (INGREDIENT, ORDER, SUPPLIER...).
- `resourceId`: ID tài nguyên (chuỗi).
- `success`: true/false.
- `summary`: mô tả ngắn.
- `details`: JSON/text chi tiết thay đổi.
- `errorMessage`: thông điệp lỗi khi không thành công.
- `actorId`, `actorUsername`, `actorRoles`: thông tin người thực hiện.
- `requestUri`, `httpMethod`, `ipAddress`, `userAgent`: ngữ cảnh request.

## Điều kiện & lưu ý
- `AuditLogService` không tự expose API; các module cần log phải gọi service trực tiếp.
- Nên dùng summary ngắn gọn, details JSON nếu chứa nhiều thông tin.
- Nếu không có context (ví dụ worker, batch job), service vẫn ghi log với actor `null` hoặc `SYSTEM`.
- Đảm bảo `RequestContextHolder` có request attributes (chỉ log được trong context HTTP).

## Quan hệ với chức năng khác
- **Inventory**: ghi log khi điều chỉnh tồn kho (`IngredientService`).
- **Shift/Payroll**: ghi log khi tạo điều chỉnh thưởng/phạt, regen payroll.
- **Security/Authentication**: `AuthenticationService` có thể mở rộng để log login (hiện dùng LoginHistoryService).
- **Reporting**: audit log có thể dùng để tra cứu hành động quan trọng.

## Các tệp liên quan
- `AuditLogService.java`
- `AuditLogRepository.java`
- `AuditLog.java`
- Các service gọi `recordAction` (ví dụ `IngredientService`, `ShiftPerformanceAdjustmentService`)
