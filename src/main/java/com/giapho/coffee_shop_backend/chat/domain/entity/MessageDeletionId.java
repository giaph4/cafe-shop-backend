package com.giapho.coffee_shop_backend.chat.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class MessageDeletionId implements Serializable {

    @Column(name = "message_id")
    private Long messageId;

    @Column(name = "user_id")
    private Long userId;
}
