/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.webhook.web.websocket;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket configuration registering the webhook WebSocket handler at {@code /webhooks/{id}/wss}.
 *
 * <p>
 * This keeps the exact relative endpoint the same as REST (/webhooks/{id}).
 *
 * @author ByteChef
 */
@Configuration
@EnableWebSocket
public class WebhookWebSocketConfiguration implements WebSocketConfigurer {

    private final WebhookWebSocketHandler webhookWebSocketHandler;

    public WebhookWebSocketConfiguration(WebhookWebSocketHandler webhookWebSocketHandler) {
        this.webhookWebSocketHandler = webhookWebSocketHandler;
    }

    /**
     * Registers WebSocket handlers for webhook endpoints.
     *
     * <p>
     * <b>Security Note:</b> CSRF protection is intentionally disabled for WebSocket connections used for webhook
     * handling. External services connecting via WebSocket for real-time webhook delivery cannot include CSRF tokens.
     * Security is maintained through webhook signature validation at the message level.
     */
    @SuppressFBWarnings("SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING")
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webhookWebSocketHandler, "/webhooks/*/wss")
            .setAllowedOriginPatterns("*");
    }
}
