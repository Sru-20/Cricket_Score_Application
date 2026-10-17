package com.boundary.api;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class StompMatchUpdatePublisher implements MatchUpdatePublisher {
    private final SimpMessagingTemplate messaging;

    public StompMatchUpdatePublisher(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @Override
    public void send(String destination, Object payload) {
        messaging.convertAndSend(destination, payload);
    }
}
