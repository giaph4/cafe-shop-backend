# Checklist QA chi tiết – Shift Platform FE

> Mục tiêu: đảm bảo luồng Auth, Shift Report, Chat realtime, Upload hoạt động đúng theo đặc tả. Thực hiện trên staging với dữ liệu thực hoặc mock.

## 1. Chuẩn bị môi trường

- [ ] Base URL đã cấu hình (ENV/Config FE) đúng với server test.
- [ ] JWT token từ `/api/v1/auth/login` hoạt động, lưu vào Storage theo chuẩn.
- [ ] WebSocket `/ws` kết nối được với header `Authorization`.
- [ ] Load mock data `docs/shift/mock/shift-platform-mock.json` vào Storybook/test để so sánh UI.

## 2. Authentication

- [ ] Đăng nhập thành công với tài khoản hợp lệ → chuyển vào dashboard, lưu token, decode được `exp`.
- [ ] Đăng nhập sai mật khẩu → nhận 401 + code `AUTH_BAD_CREDENTIALS`, FE hiển thị thông báo chuẩn.
- [ ] Tài khoản disabled → 423 + `AUTH_DISABLED`, hiển thị banner hướng dẫn liên hệ admin.
- [ ] Token hết hạn hoặc bị xoá → redirect `/login`, clear state.

## 3. Shift Session & Report

- [ ] Trang danh sách ca hiển thị session ACTIVE/CLOSED từ API (compare với mock).
- [ ] Bấm “Kết ca” → gọi `POST /shifts/reports/sessions/{id}/regenerate` nếu cần refresh.
- [ ] Bảng báo cáo hiển thị đủ trường: tổng doanh thu, unpaid, chuyển giao, top products.
- [ ] WS nhận sự kiện `SESSION_STARTED` khi ca mới mở (dùng Postman/tạo session test) → FE cập nhật badge.
- [ ] WS nhận `SESSION_ENDED` → tự động refresh báo cáo.
- [ ] Trường hợp lỗi `SHIFT_SESSION_NOT_FOUND` → FE hiện empty state + nút quay lại.

## 4. File upload

- [ ] Chọn file <5MB → upload thành công, hiển thị preview/URL server trả về.
- [ ] Upload file >5MB → 413 `FILE_TOO_LARGE`, FE hiển thị lỗi và reset input.
- [ ] Upload định dạng không hợp lệ → 415 `FILE_TYPE_NOT_ALLOWED`, thông báo rõ ràng.
- [ ] Khi thu hồi tin nhắn có file → bubble biến mất, file bị xoá khỏi danh sách (theo response BE).

## 5. Chat – danh sách conversation

- [ ] `GET /api/chat/conversations` trả Page<Conversation>, FE phân trang đúng (page/size).
- [ ] Khi có event `CONVERSATION_UPDATED` → danh sách cập nhật realtime (pin/unread).
- [ ] UI hiển thị avatar, tên, lastMessage theo schema `conversation.schema.json`.
- [ ] Pin/Unpin conversation → FE gọi API tương ứng (nếu có) và UI phản ánh trạng thái.

## 6. Chat – chi tiết & message

- [ ] `GET /api/chat/conversations/{id}/messages` với `beforeMessageId` load thêm tin nhắn cũ.
- [ ] Gửi text → bubble hiển thị trạng thái `PENDING` → chuyển `SENT` khi BE trả `MessageDTO`.
- [ ] Gửi emoji → hiển thị đúng emoji Unicode.
- [ ] Gửi attachment → preview hình/label file, link dùng `storedUrl`.
- [ ] Thu hồi tin nhắn → bubble chuyển sang trạng thái recall theo response.
- [ ] Xoá cho chính mình → tin nhắn biến mất ở danh sách cá nhân.
- [ ] Event `MESSAGE_NEW` nhận qua WS → thêm vào cuối danh sách nếu conversation đang mở.
- [ ] Event `MESSAGE_SEEN` (topic `/seen`) → cập nhật tick seen/by danh sách user.
- [ ] Kiểm tra lỗi `MESSAGE_TEXT_EMPTY` (422) → disable nút gửi + cảnh báo.

## 7. Presence & Reaction (client-side)

- [ ] Khi nhận event presence → UI đổi trạng thái ONLINE/AWAY/…
- [ ] Reaction (nếu hiển thị) render đúng emoji/số lượng, xử lý skin tone nếu có.

## 8. Pagination & bộ lọc

- [ ] Conversation list: chuyển trang, giữ nguyên filter/pinned state.
- [ ] Message list: cuộn lên trên → gọi API với `beforeMessageId` chính xác.
- [ ] Đảm bảo `first`/`last` từ PageMeta được xử lý để disable nút navigation phù hợp.

## 9. Error handling & retry

- [ ] 401 trên bất kỳ API → logout, hiển thị toast “Phiên hết hạn”.
- [ ] 403 → hiển thị thông báo “Không có quyền”.
- [ ] 404 → hiển thị empty state/kèm nút back.
- [ ] 409 khi add member → rollback optimistic UI, thông báo giới hạn.
- [ ] 422 với `details.fieldErrors` → highlight input chính xác.
- [ ] 429 (nếu backend bật) → disable action trong 30s, countdown rõ.
- [ ] 500 → dialog retry, log error theo chuẩn.

## 10. Concurrency & realtime

- [ ] Khi gửi nhiều tin nhắn nhanh → metadata `clientMessageId` mapping đúng.
- [ ] Nếu WS disconnect → FE thực hiện backoff (1,2,4,8…s) và thông báo.
- [ ] Fallback polling: khi WS mất lâu → FE gọi lại `GET /shifts/reports/...` & `GET /conversations` theo chu kỳ.

## 11. Security

- [ ] Nội dung tin nhắn được escape HTML.
- [ ] Link file luôn dùng `fileUrl` từ backend, không dùng đường dẫn nội bộ.
- [ ] Không lưu token vào cookie; refresh logic dựa trên JWT `exp`.
- [ ] Áp dụng CSP khi render iframe/attachment nếu có.

## 12. Kiểm thử hồi quy

- [ ] Chạy lại toàn bộ test automation (nếu có) sau mỗi lần cập nhật BE/FE.
- [ ] Đảm bảo OpenAPI/JSON Schema/TS interfaces không bị lệch so với API thực tế.

---

**Hoàn thành:** đánh dấu 100% khi tất cả checkbox đã pass trên cả desktop & mobile view (nếu áp dụng).
