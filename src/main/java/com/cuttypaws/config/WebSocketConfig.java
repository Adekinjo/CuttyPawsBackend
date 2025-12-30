package com.cuttypaws.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")     // CORS FIX
                .withSockJS()
                .setWebSocketEnabled(true)
                .setSessionCookieNeeded(false);    // IMPORTANT FOR JWT APPS
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // Channels frontend can SUBSCRIBE to
        registry.enableSimpleBroker(
                "/topic",      // public channels
                "/queue",      // private user notifications
                "/user"        // STOMP user prefix
        );

        // Channels frontend can SEND to
        registry.setApplicationDestinationPrefixes("/app");

        // User-specific queue for private messages
        registry.setUserDestinationPrefix("/user");
    }
}


