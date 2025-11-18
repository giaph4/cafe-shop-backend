package com.giapho.coffee_shop_backend.chat.domain.repository;

import com.giapho.coffee_shop_backend.chat.domain.entity.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long> {

    List<MessageAttachment> findByMessage_Id(Long messageId);
}
