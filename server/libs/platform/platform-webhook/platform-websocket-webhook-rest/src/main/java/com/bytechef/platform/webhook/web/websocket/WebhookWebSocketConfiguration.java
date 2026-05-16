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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * WebSocket configuration registering all webhook-area WebSocket handlers.
 *
 * <p>
 * {@link WebhookWebSocketHandler} is registered at the production webhook upgrade path ({@code /webhooks/(id)/wss}) —
 * Twilio inbound, browser-voice, etc. Keeps the relative endpoint identical to the REST webhook path.
 *
 * <p>
 * {@link WorkflowTestWebSocketHandler} is registered at the in-editor voice test path
 * ({@code /internal/workflow-tests/(id)/wss}), gated by the standard session cookie. It resolves the draft workflow
 * definition and spawns its embedded {@code websocketTasks} sub-workflow against the live audio stream.
 *
 * <p>
 * Both share {@link CallSessionRegistry} so the worker-side {@code WebSocketEmitter} bridge resolves sessions for
 * either path by callSid.
 *
 * @author ByteChef
 */
@Configuration
@EnableWebSocket
public class WebhookWebSocketConfiguration implements WebSocketConfigurer {

    private final WebhookWebSocketHandler webhookWebSocketHandler;
    private final WorkflowTestWebSocketHandler workflowTestWebSocketHandler;

    @SuppressFBWarnings("EI")
    public WebhookWebSocketConfiguration(
        WebhookWebSocketHandler webhookWebSocketHandler,
        WorkflowTestWebSocketHandler workflowTestWebSocketHandler) {

        this.webhookWebSocketHandler = webhookWebSocketHandler;
        this.workflowTestWebSocketHandler = workflowTestWebSocketHandler;
    }

    /**
     * Registers WebSocket handlers for webhook endpoints.
     *
     * <p>
     * <b>Security Note:</b> CSRF protection is intentionally disabled for WebSocket connections used for webhook
     * handling. External services connecting via WebSocket for real-time webhook delivery cannot include CSRF tokens.
     * Security on the production path is maintained through webhook signature validation at the message level; the
     * in-editor test path lives under {@code /internal/**} which is gated by the standard session cookie, plus the
     * single-use session-token guard in {@link WorkflowTestWebSocketHandler}.
     */
    @SuppressFBWarnings("SPRING_CSRF_UNRESTRICTED_REQUEST_MAPPING")
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webhookWebSocketHandler, "/webhooks/*/wss")
            .setAllowedOriginPatterns("*");

        registry.addHandler(workflowTestWebSocketHandler, "/internal/workflow-tests/*/wss")
            .setAllowedOriginPatterns("*");
    }

    /**
     * Tunes the servlet container's WebSocket buffers for streaming PCM audio. The container defaults are conservative
     * (typically 8 KiB binary buffer, 60 s idle timeout) which is fine for control messages but fragments 20-ms PCM
     * frames at higher sample rates and drops idle voice sessions during long agent thinking pauses. Sizes are
     * deliberately generous — voice frames are ≤4 KiB but the buffer also has to hold reassembled fragmented messages
     * from misbehaving clients.
     */
    @Bean
    public ServletServerContainerFactoryBean voiceWebSocketContainerFactoryBean() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();

        container.setMaxBinaryMessageBufferSize(65536);
        container.setMaxTextMessageBufferSize(65536);
        container.setMaxSessionIdleTimeout(300_000L);
        container.setAsyncSendTimeout(10_000L);

        return container;
    }
}
