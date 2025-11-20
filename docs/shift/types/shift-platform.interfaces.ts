/*
 * Bộ interface TypeScript dùng cho FE – đồng bộ với JSON Schema và OpenAPI.
 * Cập nhật khi backend thay đổi DTO hoặc schema.
 */

export type ISODateTimeString = string;
export type Nullable<T> = T | null;

export type ShiftSessionStatus = "ACTIVE" | "CLOSED" | "FORCED";
export type MessageType = "TEXT" | "EMOJI" | "ATTACHMENT";
export type MessageStatus = "PENDING" | "SENT" | "DELIVERED" | "SEEN" | "FAILED";
export type ShiftSessionEventType = "SESSION_STARTED" | "SESSION_ENDED" | "SESSION_FORCED";
export type PresenceStatus = "ONLINE" | "OFFLINE" | "AWAY" | "DO_NOT_DISTURB";

export interface UserDTO {
  id: number;
  username: string;
  fullName: string;
  email: Nullable<string>;
  phone: Nullable<string>;
  avatarUrl: Nullable<string>;
  roles: string[];
  status?: string;
}

export interface ConversationMemberDTO {
  userId: number;
  username: Nullable<string>;
  fullName: string;
  avatarUrl: Nullable<string>;
  role: "OWNER" | "ADMIN" | "MEMBER";
  pinned: boolean;
  muted: boolean;
  lastReadMessageId: Nullable<number>;
}

export interface MessageAttachmentDTO {
  id: number;
  originalName: string;
  storedUrl: string;
  previewUrl: Nullable<string>;
  mimeType: Nullable<string>;
  size: number;
}

export interface MessageDTO {
  id: number;
  conversationId: number;
  senderId: number;
  senderName: string;
  senderAvatar: Nullable<string>;
  content: string;
  contentType: MessageType;
  status: MessageStatus;
  metadata: Nullable<string>;
  createdAt: ISODateTimeString;
  updatedAt: Nullable<ISODateTimeString>;
  attachments: MessageAttachmentDTO[];
  seenByUserIds: Nullable<number[]>;
}

export interface ConversationDTO {
  id: number;
  title: Nullable<string>;
  avatarUrl: Nullable<string>;
  type: "DIRECT" | "GROUP";
  updatedAt: ISODateTimeString;
  lastMessage: Nullable<MessageDTO>;
  unreadCount: number;
  pinned: boolean;
  participants: ConversationMemberDTO[];
}

export interface ReactionDTO {
  id: number;
  messageId: number;
  userId: number;
  emoji: string;
  skinTone: Nullable<string>;
  createdAt: ISODateTimeString;
}

export interface PresenceEvent {
  userId: number;
  status: PresenceStatus;
  updatedAt: ISODateTimeString;
  conversationId: Nullable<number>;
  device: Nullable<string>;
}

export interface MessageSeenEvent {
  messageId: number;
  userId: number;
  seenAt: Nullable<ISODateTimeString>;
}

export interface ShiftSessionResponse {
  id: number;
  workShiftId: number;
  userId: number;
  username: string;
  fullName: Nullable<string>;
  startAt: ISODateTimeString;
  endAt: Nullable<ISODateTimeString>;
  status: ShiftSessionStatus;
  adminOverride: boolean;
  forceReason: Nullable<string>;
  forceByUserId: Nullable<number>;
  createdAt: ISODateTimeString;
  updatedAt: ISODateTimeString;
}

export interface ShiftReportPaymentBreakdown {
  paymentMethod: string;
  orderCount: number;
  totalAmount: number;
}

export interface ShiftReportProductSummary {
  productId: number;
  productName: string;
  quantity: number;
  totalAmount: number;
}

export interface ShiftReportResponse {
  reportId: number;
  sessionId: number;
  workShiftId: number;
  userId: number;
  username: string;
  status: ShiftSessionStatus;
  startAt: ISODateTimeString;
  endAt: Nullable<ISODateTimeString>;
  totalOrders: number;
  totalPaidAmount: number;
  totalUnpaidAmount: number;
  transferredOrders: number;
  paymentBreakdown: ShiftReportPaymentBreakdown[];
  topProducts: ShiftReportProductSummary[];
  generatedAt: ISODateTimeString;
}

export interface ShiftSessionEvent {
  eventType: ShiftSessionEventType;
  session: ShiftSessionResponse;
  report: Nullable<ShiftReportResponse>;
  publishedAt: Nullable<ISODateTimeString>;
}

export interface FileUploadResponse {
  fileName: string;
  fileUrl: string;
  fileSize: number;
  fileType: string;
  message?: string;
}

export interface AuthenticationResponse {
  token: string;
  username: string;
}

export interface ErrorResponse {
  code: string;
  message: string;
  details?: Record<string, unknown>;
}

export interface PageSortMetadata {
  sorted: boolean;
  unsorted: boolean;
  empty: boolean;
}

export interface PageMeta<T> {
  content: T[];
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
  first: boolean;
  last: boolean;
  numberOfElements?: number;
  sort?: Nullable<PageSortMetadata>;
}

export type ConversationPage = PageMeta<ConversationDTO>;
export type MessagePage = PageMeta<MessageDTO>;

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest extends LoginRequest {
  fullName: string;
  email: string;
  phone: string;
  roleIds?: number[];
}

export interface SendTextMessagePayload {
  conversationId: number;
  content: string;
}

export interface SendEmojiMessagePayload {
  conversationId: number;
  code: string;
}

export interface SendAttachmentMessagePayload {
  conversationId: number;
  messageText: Nullable<string>;
  files: File[];
}

export interface MarkMessageSeenPayload {
  conversationId: number;
  messageId: number;
}

export interface ShiftReportRegenerateOptions {
  sessionId: number;
  refresh?: boolean;
}

export interface ConversationListQuery {
  page?: number;
  size?: number;
}

export interface MessageListQuery {
  conversationId: number;
  beforeMessageId?: number;
  page?: number;
  size?: number;
}
