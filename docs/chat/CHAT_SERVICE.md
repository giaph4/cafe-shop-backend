# Tài liệu Tích hợp Chat Service

> **Mục tiêu**: cung cấp tài liệu chi tiết, rõ ràng để FE triển khai đầy đủ mà không cần hỏi thêm BE. Tài liệu này phản ánh chính xác mã nguồn (`main`) tính tới 2025-11-19.

---

## 1. Tổng Quan Hệ Thống

- **Ngôn ngữ/Framework**: Spring Boot 3.5.6, Java 21.
- **Chức năng chính**: hội thoại (1-1, nhóm), gửi/nhận tin nhắn (text, emoji, file), realtime qua WebSocket STOMP, quản lý thành viên, seen/recall.
- **Bảo mật**: JWT (`Authorization: Bearer <access_token>`), refresh token qua REST.
- **Realtime**: WebSocket endpoint `/ws`, sử dụng STOMP (SockJS fallback).
- **File**: multipart upload trực tiếp về backend, lưu local storage (folder `file.storage.upload-dir`).

### Kiến trúc tổng quan

```
Frontend (React/Vue) <-> REST (HTTP/JSON) & WebSocket (STOMP) <-> Spring Boot Chat Module <-> Database (MySQL)
```

---

## 2. Authentication & Authorization

### 2.1. Flow

| Bước | Endpoint | Mô tả |
| --- | --- | --- |
| Đăng nhập | `POST /api/v1/auth/login` | Nhận `accessToken`, `refreshToken`. |
| Sử dụng API | Kèm header `Authorization: Bearer <accessToken>` | Access token hết hạn sau `application.jwt.expirationMs` (24h). |
| Làm mới | `POST /api/v1/auth/refresh` | Gửi refresh token để lấy cặp token mới. Refresh token hết hạn (config trong DB - mặc định 7 ngày). |
| Đăng xuất | `POST /api/v1/auth/logout` | Thu hồi refresh token hiện tại. |

- **Lưu ý**: Nếu access token hết hạn, BE trả `401` với code `AUTH-EXPIRED`. FE phải tự động gọi refresh, sau đó retry request ban đầu.
- **Refresh token** lưu phía FE (secure storage). Nếu refresh thất bại (`401`), FE bắt buộc chuyển user về màn login.

### 2.2. Headers yêu cầu

```
Authorization: Bearer <access_token>
Content-Type: application/json (trừ multipart)
```

### 2.3. Ví dụ

**Đăng nhập**

```bash
curl -X POST https://api.coffeeshop.local/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john.doe","password":"secret123"}'
```

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "7c056d3f-...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "refreshExpiresIn": 604800,
  "user": {
    "id": 101,
    "username": "john.doe",
    "fullName": "John Doe",
    "avatarUrl": null,
    "statusMessage": "Xin chào",
    "lastSeenAt": "2025-11-19T02:12:00Z"
  }
}
```

**Làm mới token**

```bash
curl -X POST https://api.coffeeshop.local/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"7c056d3f-..."}'
```

### 2.4. Error Codes liên quan

| HTTP | Code | Message | Hướng dẫn FE |
| --- | --- | --- | --- |
| 401 | AUTH-INVALID | Token không hợp lệ | Đăng xuất và chuyển về login. |
| 401 | AUTH-EXPIRED | Access token hết hạn | Gọi refresh, retry nếu thành công. |
| 401 | AUTH-REFRESH-EXPIRED | Refresh token hết hạn | Đăng xuất. |
| 403 | AUTH-FORBIDDEN | Không đủ quyền | Hiển thị toast "Bạn không có quyền". |

---

## 3. REST API

Tất cả endpoint, schema chi tiết nằm trong [`docs/chat/openapi-chat.yaml`](./openapi-chat.yaml). FE import trực tiếp vào Postman hoặc tool OpenAPI để xem chi tiết. Dưới đây là tóm tắt và example chính.

### 3.1. Danh sách hội thoại

- **GET** `/api/chat/conversations`
- Query: `page`, `size`
- Response: `ConversationPage` (xem schema)

```bash
curl "https://api.coffeeshop.local/api/chat/conversations?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

```json
{
  "items": [
    {
      "id": 5001,
      "type": "DIRECT",
      "title": null,
      "avatarUrl": "https://cdn/.../user-102.png",
      "updatedAt": "2025-11-19T02:15:00Z",
      "lastMessage": {
        "id": 9001,
        "conversationId": 5001,
        "senderId": 101,
        "content": "Hello!",
        "contentType": "TEXT",
        "status": "SENT",
        "createdAt": "2025-11-19T02:14:58Z",
        "updatedAt": "2025-11-19T02:14:58Z",
        "attachments": [],
        "seenByUserIds": [101, 102]
      },
      "unreadCount": 0,
      "pinned": true,
      "participants": [
        {
          "userId": 101,
          "username": "john.doe",
          "fullName": "John Doe",
          "avatarUrl": null,
          "role": "OWNER",
          "pinned": true,
          "muted": false,
          "lastReadMessageId": 9001
        },
        {
          "userId": 102,
          "username": "jane.smith",
          "fullName": "Jane Smith",
          "avatarUrl": "https://cdn/.../user-102.png",
          "role": "MEMBER",
          "pinned": false,
          "muted": false,
          "lastReadMessageId": 9001
        }
      ]
    }
  ],
  "page": 0,
  "size": 20,
  "totalPages": 5,
  "totalElements": 92
}
```

**TypeScript interface** (FE nên dùng chung)

```ts
export interface ConversationSummary {
  id: number;
  type: 'DIRECT' | 'GROUP';
  title: string | null;
  avatarUrl: string | null;
  updatedAt: string; // ISO
  lastMessage: Message | null;
  unreadCount: number;
  pinned: boolean;
  participants: ConversationMember[];
}

export interface ConversationMember {
  userId: number;
  username?: string | null;
  fullName: string;
  avatarUrl?: string | null;
  role: 'OWNER' | 'ADMIN' | 'MEMBER';
  pinned: boolean;
  muted: boolean;
  lastReadMessageId?: number | null;
}
```

### 3.2. Tạo hội thoại 1-1 / nhóm

- **POST** `/api/chat/conversations/direct/{targetUserId}`
- **POST** `/api/chat/conversations/group`

Group request body:

```json
{
  "title": "Team Marketing",
  "memberIds": [102, 103, 104]
}
```

### 3.3. Tin nhắn

| Endpoint | Mô tả |
| --- | --- |
| `GET /api/chat/conversations/{id}/messages` | Phân trang tin nhắn, hỗ trợ `beforeMessageId` để load ngược. |
| `POST /api/chat/conversations/{id}/messages/text` | Gửi tin nhắn text. Body form `content=...`. |
| `POST /api/chat/conversations/{id}/messages/emoji` | Gửi emoji. `code=😀`. |
| `POST /api/chat/conversations/{id}/messages/attachments` | Multipart upload `files[]`, optional `messageText`. |
| `POST /api/chat/messages/{messageId}/recall` | Thu hồi tin nhắn (đổi status thành `RECALLED`). |
| `DELETE /api/chat/messages/{messageId}` | Xóa phía client (lưu trong bảng `MessageDeletion`). |
| `POST /api/chat/conversations/{id}/messages/{messageId}/seen` | Đánh dấu đã xem; server trả `204`. |

#### Example gửi tin nhắn text (axios)

```ts
await axios.post(
  `/api/chat/conversations/${conversationId}/messages/text`,
  new URLSearchParams({ content: inputValue }),
  { headers: { Authorization: `Bearer ${token}` } }
);
```

#### Example gửi tin đính kèm (fetch)

```ts
const form = new FormData();
form.append('messageText', caption ?? '');
files.forEach(f => form.append('files', f));

await fetch(`/api/chat/conversations/${conversationId}/messages/attachments`, {
  method: 'POST',
  headers: {
    Authorization: `Bearer ${token}`
  },
  body: form
});
```

**Validation chính**

| Field | Rule |
| --- | --- |
| `content` | bắt buộc, 1 - 4000 ký tự, cắt khoảng trắng đầu/cuối. |
| `emoji.code` | 1-16 ký tự Unicode, BE không giới hạn set emoji. |
| `messageText` | tối đa 4000 ký tự. |
| `files` | bắt buộc >=1; extension hợp lệ theo `file.storage.allowed-extensions`. size <= 5MB. |

**Error**

| HTTP | code | message |
| --- | --- | --- |
| 400 | MESSAGE-EMPTY | "Nội dung tin nhắn không được để trống" |
| 400 | ATTACHMENT-REQUIRED | "Tin nhắn đính kèm bắt buộc phải có tệp tải lên" |
| 403 | CHAT-FORBIDDEN | Không thuộc hội thoại. |
| 404 | MESSAGE-NOT-FOUND | Tin nhắn không tồn tại. |

---

## 4. WebSocket / STOMP

### 4.1. Thông tin kết nối

- **Endpoint**: `wss://api.coffeeshop.local/ws`
- **Protocol**: STOMP (SockJS fallback).
- **Auth**: gửi header `Authorization: Bearer <token>` hoặc query `?token=...`.
- **Handshake**: `JwtHandshakeInterceptor` xác thực; nếu token invalid => handshake bị từ chối.

### 4.2. Topic/Event

| Topic | Hướng | Payload schema | Mô tả |
| --- | --- | --- | --- |
| `/topic/conversations` | server → client | `ConversationSummary` | BE push cập nhật hội thoại khi có tin mới/đổi trạng thái. |
| `/topic/conversations/{conversationId}` | server → client | `Message` | Tin nhắn mới/thu hồi. |
| `/topic/conversations/{conversationId}/seen` | server → client | `{ messageId, userId }` | Sự kiện đã xem. |
| `/app/chat.typing` (gợi ý tương lai) | client → server | `{ conversationId, typing: true }` | (Chưa implement) |

```json
// Seen event payload
{
  "messageId": 9020,
  "userId": 105
}
```

**Subscribe example (browser)**

```ts
import { Client } from '@stomp/stompjs';

const client = new Client({
  brokerURL: 'wss://api.coffeeshop.local/ws',
  connectHeaders: {
    Authorization: `Bearer ${token}`
  },
  reconnectDelay: 5000
});

client.onConnect = () => {
  client.subscribe('/topic/conversations', message => {
    const conv = JSON.parse(message.body) as ConversationSummary;
    // update sidebar
  });

  client.subscribe(`/topic/conversations/${conversationId}`, message => {
    const msg = JSON.parse(message.body) as Message;
    // append to chat window
  });
};

client.activate();
```

**Reconnect**: sử dụng `reconnectDelay`, `maxReconnectAttempts`, exponential backoff. FE nên hiển thị toast "Đang kết nối lại" khi mất kết nối.

**Fallback REST**: nếu WebSocket không hoạt động, FE gọi `GET /api/chat/conversations/{id}/messages` định kỳ (polling) để cập nhật.

---

## 5. File Upload Flow

- FE gọi trực tiếp endpoint `/api/chat/conversations/{id}/messages/attachments` với multipart.
- BE lưu file tại `uploads/products` (cấu hình `file.storage.upload-dir`). Response chứa `storedUrl` (URL public theo `file.storage.base-url`).
- Không dùng presigned URL.
- MIME validation: theo `file.storage.allowed-extensions` (ảnh & media phổ biến). Nếu upload > 5MB => 400 `FILE-TOO-LARGE`.
- FE hiển thị progress bằng cách nghe `XMLHttpRequest.upload.onprogress` hoặc `fetch` + `ReadableStream` (khuyến nghị `axios` với `onUploadProgress`).

---

## 6. Pagination & Sorting

- Conversations: page/size (`Page<ConversationSummary>`). FE hiển thị `Load more` dùng `page+1`.
- Messages: page/size + optional `beforeMessageId`. Nếu FE muốn infinite scroll: lần đầu `page=0`, load `size`. Khi user cuộn lên => gửi `beforeMessageId` = message cũ nhất đã có => BE trả danh sách cũ hơn.
- Response `totalPages`, `totalElements` (dùng cho direct page). FE nên ưu tiên `hasMore = page < totalPages-1`.

Mock response xem trong [`mock-data/conversations.json`](./mock-data/conversations.json) (sẽ tạo bên dưới).

---

## 7. Validation Rules & Error Handling

- Toàn bộ rule chi tiết trong OpenAPI, schema.
- BE trả JSON error chuẩn:

```json
{
  "code": "MESSAGE-EMPTY",
  "message": "Nội dung tin nhắn không được để trống",
  "details": {
    "field": "content"
  }
}
```

| Trạng thái | Code | Hướng xử lý FE |
| --- | --- | --- |
| 400 | MESSAGE-EMPTY | Highlight ô input, show text nhỏ dưới input. |
| 400 | ATTACHMENT-INVALID-TYPE | Toast "Định dạng file không hỗ trợ". |
| 401 | AUTH-EXPIRED | Trigger refresh token. |
| 403 | CHAT-FORBIDDEN | Toast và quay về danh sách. |
| 404 | MESSAGE-NOT-FOUND | Reload conversation. |
| 429 | RATE-LIMIT | Thông báo "Gửi quá nhanh" và disable nút 5s. |
| 500 | SERVER-ERROR | Retry option, log Sentry. |

`details` có thể chứa `field`, `expected`, `actual`. FE nên log detail vào monitoring.

---

## 8. UI Mapping & UX Guidelines

### 8.1. SidebarList (Danh sách hội thoại)

- **API**: `GET /api/chat/conversations`
- **Fields**: `avatarUrl`, `title`/`participants`, `lastMessage.content/attachments`, `updatedAt`, `unreadCount`, `pinned`.
- **Loading state**: skeleton rows.
- **Error**: nếu `401` => chuyển login; khác => toast "Không tải được danh sách" + retry button.
- **Optimistic**: khi pin/unpin (endpoint `PATCH pin`), update ngay UI. Nếu lỗi => rollback.

### 8.2. ChatWindow

- **API**: `GET /api/chat/conversations/{id}/messages`
- **Realtime**: subscribe `/topic/conversations/{id}`.
- **Message ordering**: Sắp xếp theo `createdAt` tăng dần. BE luôn trả list sorted. Nếu có message server-time < local, FE vẫn giữ theo `createdAt`.
- **Optimistic send**: hiển thị message local với status `sending`. Khi server push message (nhờ WebSocket) -> dùng `id` server để replace. Nếu server trả error -> show toast, chuyển message thành `failed` và cho phép retry.

### 8.3. ChatInput (Gửi tin)

- Validate `content` không rỗng (trim). Nếu fail => highlight ô.
- Khi attached file: preview thumbnail (nếu `previewUrl`), hiển thị progress.
- `Recall`: khi user recall, BE trả `Message` với `status=RECALLED`, `content=null`. FE hiển thị "Tin nhắn đã bị thu hồi".

### 8.4. AttachmentPicker

- Sử dụng `accept` theo danh sách extension.
- Trong quá trình upload, hiển thị progress bar. Nếu fail => show "Upload thất bại".
- Sau khi upload thành công, server push message -> hiển thị link download (sử dụng `storedUrl`).

### 8.5. Seen indicator

- Subscribe `/topic/conversations/{id}/seen`. Khi nhận `{ messageId, userId }`, FE cập nhật UI (ví dụ hiển thị avatar nhỏ). FE nên map `userId -> participant` để lấy avatar.

---

## 9. Concurrency & Race Conditions

- **Duplicate requests**: FE nên disable nút send khi đang gửi, hoặc idempotency key (chưa hỗ trợ). Nếu double click => BE vẫn tạo 2 message.
- **Optimistic update**: FE cần giữ queue message local, mark status `pending`. Khi server push message, match theo `content + timestamp close` hoặc chờ response API? BE returning message ngay trong REST -> FE dùng `id` trả về.
- **Ordering**: tin nhắn hiển thị theo `createdAt`. BE gán `createdAt` server-time. FE không dùng timestamp client.
- **Seen**: FE nên chỉ gửi mark seen khi user focus conversation và scroll tới bottom. Nếu duplicate mark => BE idempotent (vì `MessageSeenId` primary key).

---

## 10. Security Notes (FE)

- Escape mọi content trước khi render (`content` có thể chứa HTML). Sử dụng `DOMPurify` hoặc render plain text.
- Không tin tưởng timestamp client; dùng `createdAt` server.
- Không hiển thị preview file nếu `mimeType` không phải image/video an toàn.
- Không lưu token trong localStorage nếu có thể (dùng httpOnly cookie). Nếu bắt buộc -> encrypt & expiry.

---

## 11. Testing & Mocking

### 11.1. Mock Data

- `docs/chat/mock-data/` chứa:
  - `users.json`
  - `conversations.json`
  - `messages-conversation-5001.json`

FE có thể dùng để seed UI offline.

### 11.2. Postman Collection

- `docs/chat/postman/ChatService.postman_collection.json`
- Bao gồm environment `Chat-Local.postman_environment.json`.

### 11.3. CLI scripts

- `scripts/mock-chat-data.sh` (cần tạo) seed DB qua REST (có thể dùng `curl` loop).

---

## 12. QA Checklist & Acceptance Criteria

| # | Tiêu chí | Cách kiểm |
| --- | --- | --- |
| 1 | Đăng nhập/refresh token | Login, chờ >24h (hoặc chỉnh clock) => refresh vẫn OK. |
| 2 | WebSocket connect | FE connect, subscribe; tắt server => FE retry backoff. |
| 3 | Gửi tin nhắn text | Tin hiển thị ở cả 2 user, realtime. |
| 4 | Gửi file 4MB | Progress hiển thị, preview đúng, link download hoạt động. |
| 5 | Recall tin nhắn | Tin biến thành "Đã thu hồi" cho cả 2 phía. |
| 6 | Seen indicator | Người B đọc => A nhận event seen. |
| 7 | Pagination | Scroll lên lấy thêm lịch sử, không trùng. |
| 8 | Lỗi 403 | Người lạ gọi API => hiển thị cảnh báo. |
| 9 | WS fallback | Tắt WS, FE fallback poll REST (manual). |
| 10 | Offline mode | Mất mạng khi send -> message pending, reconnect -> resend. |

Acceptance Criteria: tất cả test trên pass, không có error JS, contract REST/WS đúng schema.

---

## 13. Contract Testing (gợi ý)

- Sử dụng `pact-js` hoặc `openapi-enforcer` để verify response.
- CI: thêm step `npx openapi-enforcer verify docs/chat/openapi-chat.yaml --against http://localhost:8088`.
- WebSocket: sử dụng integration test mô phỏng STOMP client.

---

## 14. Versioning & Changelog

- **Phiên bản API**: v1 (prefix `/api/v1/auth`, `/api/chat`).
- **Breaking changes**: thông báo qua Slack + cập nhật changelog
  - `docs/chat/CHANGELOG.md`: liệt kê endpoint thay đổi, payload mới.
- **Migration**: nếu đổi schema message, cần cung cấp script migrate + backward-compatible (ví dụ field mới optional).

---

## 15. Phụ lục

### 15.1. TypeScript Interfaces (tổng hợp)

```ts
export interface MessageAttachment {
  id: number;
  originalName: string;
  storedUrl: string;
  previewUrl?: string | null;
  mimeType?: string | null;
  size?: number | null;
}

export type MessageStatus = 'SENT' | 'RECALLED';
export type MessageType = 'TEXT' | 'IMAGE' | 'VIDEO' | 'AUDIO' | 'FILE' | 'EMOJI';

export interface Message {
  id: number;
  conversationId: number;
  senderId: number;
  senderName: string;
  senderAvatar?: string | null;
  content: string | null;
  contentType: MessageType;
  status: MessageStatus;
  metadata?: string | null;
  createdAt: string;
  updatedAt: string;
  attachments: MessageAttachment[];
  seenByUserIds: number[];
}

export interface SeenEventPayload {
  messageId: number;
  userId: number;
}
```

### 15.2. Axios instance

```ts
import axios from 'axios';

export const chatApi = axios.create({
  baseURL: 'https://api.coffeeshop.local',
  timeout: 10000
});

chatApi.interceptors.request.use(config => {
  const token = authStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

### 15.3. cURL script gửi file

```bash
curl -X POST "https://api.coffeeshop.local/api/chat/conversations/5001/messages/attachments" \
  -H "Authorization: Bearer $TOKEN" \
  -F "messageText=Check file" \
  -F "files=@/path/to/image.png"
```

---

## 16. Tóm tắt hành động FE

1. Import OpenAPI + Postman.
2. Thiết lập axios client + refresh token logic.
3. Sử dụng TypeScript interfaces theo tài liệu.
4. Cài đặt STOMP client, subscribe topic.
5. Build UI mapping theo checklist.
6. Tạo mock data (dùng file trong `/docs/chat/mock-data`).
7. Viết test FE (MSW, Cypress) dựa trên mock JSON.
8. Đảm bảo QA checklist pass trước khi release.

---

> Mọi câu hỏi bổ sung vui lòng ping BE channel #chat-backend để review & cập nhật tài liệu.
