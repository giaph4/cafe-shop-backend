package com.giapho.coffee_shop_backend.chat.security;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.http.server.ServerHttpRequest;

import java.security.Principal;
import java.util.Map;

@Component
public class StompHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        Object userIdAttr = attributes.get("userId");
        Object usernameAttr = attributes.get("username");

        if (!(userIdAttr instanceof Long userId) || !(usernameAttr instanceof String username)) {
            return null;
        }
        if (!StringUtils.hasText(username)) {
            return null;
        }

        return new StompPrincipal(userId, username);
    }
}
