package com.sonhoang2.notification_service.websocket;

import java.security.Principal;

/** The authenticated user of a STOMP session; its name is the user id used for /user destinations. */
public record StompPrincipal(String name) implements Principal {

    @Override
    public String getName() {
        return name;
    }
}
