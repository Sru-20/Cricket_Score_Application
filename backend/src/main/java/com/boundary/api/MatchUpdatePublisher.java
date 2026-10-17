package com.boundary.api;

public interface MatchUpdatePublisher {
    void send(String destination, Object payload);
}
