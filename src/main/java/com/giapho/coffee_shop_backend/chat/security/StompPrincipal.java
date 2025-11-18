package com.giapho.coffee_shop_backend.chat.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.security.Principal;

@Getter
@RequiredArgsConstructor
public class StompPrincipal implements Principal {

    private final Long userId;
    private final String name;

    @Override
    public String getName() {
        return name;
    }
}
