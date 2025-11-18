-- Seed dữ liệu mẫu cho module chat (nếu có đủ người dùng)

INSERT INTO chat_conversations (type, title, avatar_url, created_by, created_at, updated_at)
SELECT 'DIRECT', CONCAT('Seed Chat: ', u1.username, ' & ', u2.username), NULL, u1.id, NOW(3), NOW(3)
FROM (
         SELECT id, username FROM users ORDER BY id ASC LIMIT 1
     ) u1
         CROSS JOIN (
    SELECT id, username FROM users ORDER BY id DESC LIMIT 1
) u2
WHERE u1.id <> u2.id
  AND NOT EXISTS (
    SELECT 1 FROM chat_conversations WHERE type = 'DIRECT' AND created_by = u1.id
);

-- Chèn thành viên cho seed conversation vừa tạo
INSERT INTO chat_conversation_members (conversation_id, user_id, role, pinned, muted, last_read_message_id, joined_at, updated_at)
SELECT c.id, u.id,
       CASE WHEN u.id = c.created_by THEN 'OWNER' ELSE 'MEMBER' END,
       FALSE, FALSE, NULL, NOW(3), NOW(3)
FROM chat_conversations c
         JOIN users u ON u.id IN (c.created_by,
                                 (SELECT id FROM users WHERE id <> c.created_by ORDER BY id DESC LIMIT 1))
WHERE c.type = 'DIRECT'
  AND c.created_at >= NOW(3) - INTERVAL 1 SECOND
ON DUPLICATE KEY UPDATE updated_at = VALUES(updated_at);
