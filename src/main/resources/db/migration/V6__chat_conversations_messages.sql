-- Tạo cột bổ sung cho bảng users phục vụ module chat
ALTER TABLE users
    ADD COLUMN status_message VARCHAR(255) NULL,
    ADD COLUMN last_seen_at DATETIME NULL;

UPDATE users SET last_seen_at = NOW() WHERE last_seen_at IS NULL;

-- Bảng cuộc trò chuyện
CREATE TABLE chat_conversations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    type VARCHAR(20) NOT NULL,
    title VARCHAR(255),
    avatar_url VARCHAR(255),
    created_by BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NULL,
    last_message_id BIGINT NULL,
    PRIMARY KEY (id),
    INDEX idx_chat_conversations_updated_at (updated_at DESC),
    INDEX idx_chat_conversations_type (type)
) ENGINE=InnoDB;

-- Thành viên cuộc trò chuyện
CREATE TABLE chat_conversation_members (
    conversation_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    pinned BIT NOT NULL DEFAULT b'0',
    muted BIT NOT NULL DEFAULT b'0',
    last_read_message_id BIGINT NULL,
    joined_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NULL,
    PRIMARY KEY (conversation_id, user_id),
    INDEX idx_chat_members_user (user_id),
    INDEX idx_chat_members_pinned (user_id, pinned),
    CONSTRAINT fk_chat_member_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversations (id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_member_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Bảng tin nhắn
CREATE TABLE chat_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content LONGTEXT NULL,
    content_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    metadata LONGTEXT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NULL,
    PRIMARY KEY (id),
    INDEX idx_chat_messages_conversation_created_at (conversation_id, created_at DESC),
    INDEX idx_chat_messages_sender (sender_id),
    CONSTRAINT fk_chat_messages_conversation FOREIGN KEY (conversation_id) REFERENCES chat_conversations (id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Tệp đính kèm
CREATE TABLE chat_message_attachments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_url VARCHAR(512) NOT NULL,
    preview_url VARCHAR(512) NULL,
    mime_type VARCHAR(100) NULL,
    file_size BIGINT NULL,
    created_at DATETIME(3) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_chat_attachments_message (message_id),
    CONSTRAINT fk_chat_attachments_message FOREIGN KEY (message_id) REFERENCES chat_messages (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Các bản ghi xoá một phía
CREATE TABLE chat_message_deletions (
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    deleted_at DATETIME(3) NOT NULL,
    PRIMARY KEY (message_id, user_id),
    CONSTRAINT fk_chat_deletions_message FOREIGN KEY (message_id) REFERENCES chat_messages (id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_deletions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Đánh dấu đã xem
CREATE TABLE chat_message_seen (
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    seen_at DATETIME(3) NOT NULL,
    PRIMARY KEY (message_id, user_id),
    INDEX idx_chat_seen_user (user_id, seen_at DESC),
    CONSTRAINT fk_chat_seen_message FOREIGN KEY (message_id) REFERENCES chat_messages (id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_seen_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;
