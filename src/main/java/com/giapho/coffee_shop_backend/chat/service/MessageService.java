package com.giapho.coffee_shop_backend.chat.service;

import com.giapho.coffee_shop_backend.chat.dto.MessageDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MessageService {

    /**
     * Gửi tin nhắn văn bản thuần tuý.
     */
    MessageDTO sendText(Long conversationId, String content);

    /**
     * Gửi tin nhắn emoji với mã emoji chuẩn (ví dụ "😀").
     */
    MessageDTO sendEmoji(Long conversationId, String emojiCode);

    /**
     * Gửi tin nhắn đính kèm tệp phương tiện (có thể kèm nội dung text).
     */
    MessageDTO sendAttachments(Long conversationId,
                               String messageText,
                               List<MultipartFile> files);

    /**
     * Thu hồi (recall) tin nhắn bởi người gửi.
     */
    MessageDTO recall(Long messageId);

    /**
     * Đánh dấu xoá tin nhắn ở phía người dùng hiện tại (không ảnh hưởng tới người khác).
     */
    void deleteForCurrentUser(Long messageId);

    /**
     * Đánh dấu đã xem tới một tin nhắn trong cuộc trò chuyện và trả về ID người dùng đã xem.
     */
    Long markSeen(Long conversationId, Long messageId);
}
