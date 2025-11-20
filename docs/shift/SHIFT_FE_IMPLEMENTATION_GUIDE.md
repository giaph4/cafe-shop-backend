# Sổ tay triển khai frontend – Shift Report & Chat Realtime

> Tài liệu này cung cấp đầy đủ đặc tả REST API, WebSocket, schema dữ liệu, mock, code snippet và checklist QA để đội FE triển khai mà không cần hỏi thêm backend.

---

## 1. Kiến trúc tổng quan

- **REST API**: tuân thủ JSON, base URL `https://<domain>/api` với header `Authorization: Bearer <JWT>`.
- **Realtime**: WebSocket STOMP trên `/ws`, sự kiện đẩy về `/topic/shifts/session-events` và `/topic/conversations/...`.
- **File upload**: multipart tới `/api/v1/files/upload`, backend trả URL file đã được chuẩn hóa.
- **Pagination**: page/size (default 20), endpoint chat hỗ trợ `beforeMessageId` kiểu cursor.
- **Schema**: tất cả JSON Schema, TypeScript interfaces, Postman, mock data, QA checklist nằm cùng thư mục:
  - `docs/shift/openapi/shift-platform.openapi.yaml`
  - `docs/shift/json-schema/*.schema.json`
  - `docs/shift/types/shift-platform.interfaces.ts`
  - `docs/shift/mock/shift-platform-mock.json`
  - `docs/shift/postman/shift-platform.postman_collection.json`
  - `docs/shift/QA_SHIFT_PLATFORM_CHECKLIST.md`

---

## 2. Authentication

| Endpoint | Method | Note |
| --- | --- | --- |
| `/api/v1/auth/login` | POST | Lấy JWT (không refresh token). |
| `/api/v1/auth/register` | POST | Tạo user mới (role mặc định STAFF nếu không khai báo). |

- **Header bắt buộc**: `Content-Type: application/json`.
- **Authorization**: sau khi đăng nhập, FE lưu `token` và gửi Bearer token với mọi request protected.
- **Refresh token**: **chưa hỗ trợ**. FE cần redirect login nếu backend trả 401 hoặc token gần hết hạn (local timer, expiration trong JWT claim `exp`).
- **Logout**: client-side (xóa token, clear state). Backend chưa cung cấp endpoint revoke.
- **Error codes**:
  - 400: validation (thiếu username/password, format sai).
  - 401: credential sai.
  - 423: account locked (mapping DisabledException → 423). FE hiển thị “Tài khoản bị khóa”.
  - 500: lỗi không xác định.

### 2.1 Flow đăng nhập

1. FE gửi POST `/api/v1/auth/login` với body `{ "username": "...", "password": "..." }`.
2. Backend xác thực → trả `{ token, username }`.
3. FE decode `token` để lấy `exp`, `userId`, `roles`.
4. Thiết lập interceptor gửi `Authorization: Bearer <token>`.
5. Khi nhận 401 → clear token, chuyển về trang đăng nhập.

### 2.2 Ví dụ

```bash
curl -X POST https://api.example.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"manager01","password":"Secret@123"}'
```

```typescript
import axios from "axios";

const login = async () => {
  const res = await axios.post("/api/v1/auth/login", {
    username: "manager01",
    password: "Secret@123",
  });
  const { token } = res.data;
  localStorage.setItem("authToken", token);
};
```

### 2.3 Bảng lỗi chuẩn

| HTTP | Code | Message | Gợi ý FE |
| --- | --- | --- | --- |
| 400 | `AUTH_VALIDATION` | Username or password must not be blank | Hiển thị toast, focus input |
| 401 | `AUTH_BAD_CREDENTIALS` | Invalid username or password | Shake form, xoá password |
| 423 | `AUTH_DISABLED` | Account is disabled or locked | Hiện banner, hướng dẫn liên hệ admin |
| 500 | `AUTH_INTERNAL` | Authentication failed | Retry tối đa 1 lần |

---

## 3. REST API chi tiết

*OpenAPI chi tiết tại `docs/shift/openapi/shift-platform.openapi.yaml`. Dưới đây là tóm tắt các endpoint chính.*

### 3.1 Báo cáo ca làm việc (Shift Report)

| Method | URL | Mô tả | Quyền |
| --- | --- | --- | --- |
| GET | `/api/v1/shifts/reports/sessions/{sessionId}` | Lấy báo cáo. Query `refresh=true` để tái tổng hợp. | MANAGER/ADMIN |
| POST | `/api/v1/shifts/reports/sessions/{sessionId}/regenerate` | Regenerate cưỡng bức. | MANAGER/ADMIN |
| GET | `/api/v1/shifts/reports/work-shifts/{workShiftId}` | Danh sách báo cáo theo ca mẫu. | MANAGER/ADMIN |

- **Headers**: `Authorization`, `Content-Type: application/json` (GET không bắt buộc nhưng khuyến nghị).
- **Response**: `ShiftReportResponseDTO` (JSON Schema + TS interface ở clip dưới).
- **HTTP codes**:
  - 200: success.
  - 201 (POST regenerate).
  - 400: sessionId invalid (<1).
  - 401/403: thiếu quyền.
  - 404: không tìm thấy báo cáo/session.
  - 500: lỗi tổng hợp.

#### Curl ví dụ

```bash
curl -X GET "https://api.example.com/api/v1/shifts/reports/sessions/300?refresh=false" \
  -H "Authorization: Bearer $TOKEN"
```

#### Fetch/axios ví dụ

```typescript
export async function fetchShiftReport(sessionId: number, { refresh = false } = {}) {
  const query = refresh ? "?refresh=true" : "";
  const res = await fetch(`/api/v1/shifts/reports/sessions/${sessionId}${query}`, {
    headers: {
      Authorization: `Bearer ${localStorage.getItem("authToken")}`,
    },
  });
  if (!res.ok) throw await res.json();
  return res.json() as Promise<ShiftReportResponse>;
}
```

### 3.2 File upload

| Method | URL | Nội dung |
| --- | --- | --- |
| POST | `/api/v1/files/upload` | Multipart upload một file |
| POST | `/api/v1/files/upload-multiple` | Multipart `files[]` |
| GET | `/api/v1/files/{fileName}` | Truy cập file (public) |
| DELETE | `/api/v1/files/{fileName}` | Xoá file | MANAGER/ADMIN |

- **Validation**: size ≤ 5MB, extension theo `FileStorageProperties.allowedExtensions`.
- **Error**: 400 `FILE_EMPTY`, 415 `FILE_TYPE_NOT_ALLOWED`, 413 `FILE_TOO_LARGE`.

#### Upload ví dụ

```bash
curl -X POST https://api.example.com/api/v1/files/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/image.png"
```

```typescript
const upload = async (file: File) => {
  const form = new FormData();
  form.append("file", file);
  const res = await axios.post("/api/v1/files/upload", form, {
    headers: {
      Authorization: `Bearer ${token}`,
      "Content-Type": "multipart/form-data",
    },
    onUploadProgress: (evt) => {
      const percent = Math.round((evt.loaded * 100) / (evt.total ?? 1));
      setProgress(percent);
    },
  });
  return res.data as FileUploadResponse;
};
```

### 3.3 Chat API (rút gọn)

| Method | URL | Ghi chú |
| --- | --- | --- |
| GET | `/api/chat/conversations` | page/size (1-100) |
| GET | `/api/chat/conversations/{id}` | chi tiết |
| POST | `/api/chat/conversations/direct/{targetUserId}` | tạo direct |
| POST | `/api/chat/conversations/group` | tạo group |
| GET | `/api/chat/conversations/{id}/members` | danh sách member |
| POST | `/api/chat/conversations/{id}/members` | thêm member |
| DELETE | `/api/chat/conversations/{id}/members/{memberId}` | xoá |
| PATCH | `/api/chat/conversations/{id}/members/{memberId}/role?role=OWNER` | đổi role |
| PATCH | `/api/chat/conversations/{id}/pin?pinned=true` | pin |
| GET | `/api/chat/conversations/{id}/messages` | query `beforeMessageId`, `page`, `size` |
| POST | `/api/chat/conversations/{id}/messages/text` | param `content` |
| POST | `/api/chat/conversations/{id}/messages/emoji` | param `code` |
| POST | `/api/chat/conversations/{id}/messages/attachments` | multipart `files[]` + `messageText` |
| POST | `/api/chat/messages/{messageId}/recall` | thu hồi |
| DELETE | `/api/chat/messages/{messageId}` | xoá cho chính mình |
| POST | `/api/chat/conversations/{id}/messages/{messageId}/seen` | đánh dấu đã xem |

Chi tiết request/response bên trong OpenAPI và TypeScript interfaces.

---

## 4. WebSocket / STOMP

- **Endpoint**: `wss://<domain>/ws`
- **Protocol**: STOMP over SockJS.
- **Auth**: FE nối kèm header `Authorization: Bearer <token>` trong handshake (SockJS `beforeConnect`).
- **Reconnect**: exponential backoff (`1s, 2s, 4s, 8s, max 30s`). Nếu nhận 401 → clear token.
- **Fallback**: nếu WS không khả dụng → FE poll `GET /api/v1/shifts/reports/sessions/{id}` mỗi 15s và `GET /api/chat/conversations` mỗi 30s.

### 4.1 Topics & payload

| Topic | Event | Direction | JSON Schema |
| --- | --- | --- | --- |
| `/topic/shifts/session-events` | `SESSION_STARTED` / `SESSION_ENDED` / `SESSION_FORCED` | server → client | `shift-session-event.schema.json` |
| `/topic/conversations` | `CONVERSATION_UPDATED` | server → client | `conversation-summary.schema.json` |
| `/topic/conversations/{conversationId}` | `MESSAGE_NEW` | server → client | `message.schema.json` |
| `/topic/conversations/{conversationId}/seen` | `MESSAGE_SEEN` | server → client | `message-seen-event.schema.json` |

**Acknowledgement**: client không cần gửi ack, nhưng nên log.

#### Subscribe mẫu (TypeScript)

```typescript
import { Client } from "@stomp/stompjs";

const client = new Client({
  brokerURL: "wss://api.example.com/ws",
  connectHeaders: {
    Authorization: `Bearer ${token}`,
  },
  reconnectDelay: 1000,
});

client.onConnect = () => {
  client.subscribe("/topic/shifts/session-events", (msg) => {
    const payload: ShiftSessionEvent = JSON.parse(msg.body);
    handleShiftEvent(payload);
  });
};

client.activate();
```

#### Event contract mẫu

```json
{
  "eventType": "SESSION_ENDED",
  "session": {
    "$ref": "shift-session.schema.json"
  },
  "report": {
    "$ref": "shift-report.schema.json"
  }
}
```

---

## 5. Message & Data Schemas

Tất cả schema chuẩn nằm trong `docs/shift/json-schema`. FE import TypeScript interface từ `docs/shift/types/shift-platform.interfaces.ts`.

| Entity | JSON Schema | Interface |
| --- | --- | --- |
| User | `user.schema.json` | `UserDTO` |
| Conversation | `conversation.schema.json` | `ConversationDTO` |
| ConversationMember | `conversation-member.schema.json` | `ConversationMemberDTO` |
| Message | `message.schema.json` | `MessageDTO` |
| Attachment | `attachment.schema.json` | `MessageAttachmentDTO` |
| Reaction | `reaction.schema.json` | `ReactionDTO` (dự phòng, client-side) |
| Presence | `presence.schema.json` | `PresenceEvent` |
| ShiftReport | `shift-report.schema.json` | `ShiftReportResponse` |
| ShiftSessionEvent | `shift-session-event.schema.json` | `ShiftSessionEvent` |

Mỗi schema chứa mô tả field (title, format). FE hiển thị chính xác theo `description`.

---

## 6. File upload flow

1. FE gọi `/api/v1/files/upload` gửi multipart `file`.
2. Backend lưu file, trả `{ fileName, fileUrl, fileSize, fileType, message }`.
3. FE sử dụng `fileUrl` hiển thị hoặc gửi tiếp trong message.
4. Khi thu hồi tin nhắn → backend tự xoá file, không cần FE gọi delete.
5. Validation: size ≤ 5MB, extension theo danh sách. Lỗi hiển thị message backend trả.
6. Progress: dùng `onUploadProgress` (axios) hoặc `xhr.upload.onprogress`.

---

## 7. Pagination & Sorting

- **Shift report list**: hiện không phân trang (trả toàn bộ). FE nên cache client.
- **Chat conversations**: `page`, `size`. Sort mới nhất theo `updatedAt DESC`.
- **Messages**: `beforeMessageId` để load thêm tin nhắn cũ (cursor). FE nên quản lý infinite scroll: khi user kéo lên, gọi `GET /messages?beforeMessageId=<oldest>`.
- **Response shape**: `Page<T>` của Spring → fields `content`, `totalElements`, `totalPages`, `number`, `size`, `first`, `last`.

---

## 8. Validation Rules & Error Mapping

| Field | Rules | Error code | UI phản hồi |
| --- | --- | --- | --- |
| Shift session `sessionId` | ≥ 1 | `SHIFT_SESSION_ID_INVALID` (400) | Hiện toast “Phiên ca không hợp lệ” |
| File upload | size ≤ 5MB | `FILE_TOO_LARGE` (413) | Thông báo & reset input |
| Chat message text | không rỗng sau trim | `MESSAGE_TEXT_EMPTY` (422) | Disable nút gửi + highlight |
| Emoji code | not blank | `MESSAGE_EMOJI_EMPTY` (422) | Show message “Chọn emoji” |
| Add member | list ≤ 100 id, distinct | `CHAT_MEMBER_LIMIT` (409) | Hiện snackbar “Vượt quá số thành viên” |
| Shift report regenerate | session phải ACTIVE/CLOSED | `SHIFT_SESSION_NOT_FOUND` (404) | Redirect danh sách báo cáo |

Error payload chuẩn: `{ "code": "...", "message": "...", "details": {...?} }`.

---

## 9. Error handling guideline

- 401 → clear token, chuyển `/login`.
- 403 → hiển thị “Bạn không có quyền truy cập”.
- 404 → show empty state + nút “Quay lại”.
- 409 → rollback optimistic UI (ví dụ add member), hiển thị message.
- 422 → highlight field lỗi theo `details.fieldErrors`.
- 429 → disable action 30s, hiển thị countdown.
- 500 → show dialog “Có lỗi xảy ra”, cung cấp nút retry.

---

## 10. UI mapping

| Component | Endpoint/Event | Fields | Loading | Success | Error | Optimistic |
| --- | --- | --- | --- | --- | --- | --- |
| `ShiftReportDashboard` | `GET /shifts/reports/sessions/{id}` | totalOrders, totalPaidAmount, ... | Skeleton chart 3s | Render cards & chart | Toast lỗi, nút retry | Không |
| `ShiftReportRegenerateButton` | `POST /shifts/reports/sessions/{id}/regenerate` | none | Disable + spinner | Toast “Đã cập nhật”, refetch | Rollback label & toast lỗi | Có (hiển thị kết quả dự kiến) |
| `ChatSidebarList` | `GET /api/chat/conversations` + event `CONVERSATION_UPDATED` | title, lastMessage, unreadCount | Placeholder rows | List update realtime | Show banner nếu 500 | Optimistic update pinned state |
| `ChatInput` | `POST /messages/text` | content | Disable gửi | Thêm message vào list | Rollback message, hiển thị badge lỗi | Có (status=pre-sent, update khi server trả message.id) |
| `AttachmentPicker` | `POST /files/upload`, `POST /messages/attachments` | fileUrl, previewUrl | Progress bar | Thêm bubble attachment | Xoá bubble, hiển thị lỗi file | Có (hiện preview ngay) |
| `ConversationMembersModal` | `POST /members`/`DELETE /members/{id}` | username, role | Loading overlay | Update list | Hiển thị error per user | Optimistic add/remove |

---

## 11. Concurrency & race condition

- Khi gửi nhiều tin nhắn: attach `clientMessageId` (UUID) trong metadata phía FE để map reply (backend chưa lưu, FE tự giữ).
- Optimistic update: set `status = "PENDING"`, chuyển sang `SENT` khi server trả `MessageDTO`.
- Duplicate requests: disable nút gửi sau click 1s. Nếu backend trả 409 → hiển thị “Tin nhắn trùng”.
- Ordering: sử dụng `createdAt` từ server để sort; nếu trùng, fallback `id`.

---

## 12. Security notes (FE)

- Escape HTML khi render `message.content`.
- Không hiển thị đường dẫn file chưa qua proxy (luôn dùng `fileUrl` trả về).
- Không lưu token trong cookie (dùng localStorage + `HttpOnly` refresh chưa có).
- Không tin cậy timestamp từ client.
- Đặt CSP nếu render iframe/attachment.

---

## 13. Testing & Mocking

- Mock data sample: `docs/shift/mock/shift-platform-mock.json` (shift report, conversations, messages).
- Postman collection: `docs/shift/postman/shift-platform.postman_collection.json` (gồm folder Auth, Shift Report, Chat).
- Scripts seed (đề xuất): dùng collection + mock JSON.
- Đề xuất contract test: dùng `@openapitools/openapi-generator` hoặc Pact để verify schema `shift-platform.openapi.yaml`.

---

## 14. Acceptance criteria & QA checklist

- Chi tiết trong `docs/shift/QA_SHIFT_PLATFORM_CHECKLIST.md`.
- Các hạng mục chính:
  1. Đăng nhập thành công, token được lưu.
  2. WS connect, nhận `SESSION_ENDED` khi BE kết ca.
  3. Báo cáo hiển thị chính xác (đối chiếu mock).
  4. Chat gửi/nhận text, emoji, attachment OK.
  5. Upload file hiển thị progress & preview.
  6. Pagination/conversation load cũ hoạt động.
  7. Error hiển thị đúng message.
  8. Offline → hiển thị trạng thái + retry khi online.

---

## 15. Changelog & versioning

- **Version**: `v1.0.0` (lần đầu ban hành).
- **Breaking change policy**: mọi thay đổi schema phải bump minor/major và cập nhật OpenAPI + JSON Schema.
- **Migration**: nếu thay đổi payload, backend giữ backward compatibility 2 tuần; FE phải sẵn sàng fallback.

---

## 16. Artefact liên quan

| File | Nội dung |
| --- | --- |
| `docs/shift/openapi/shift-platform.openapi.yaml` | OpenAPI 3.0.3 spec đầy đủ |
| `docs/shift/json-schema/*.schema.json` | JSON Schema cho từng entity/event |
| `docs/shift/types/shift-platform.interfaces.ts` | TypeScript interface + helper |
| `docs/shift/mock/shift-platform-mock.json` | Mock data dùng Storybook/Postman |
| `docs/shift/postman/shift-platform.postman_collection.json` | Postman collection |
| `docs/shift/QA_SHIFT_PLATFORM_CHECKLIST.md` | Checklist QA chi tiết |

Vui lòng tham khảo các file đi kèm để implement nhanh chóng và đúng chuẩn.
