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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

/**
 * Mints and validates short-lived single-use tokens that authorize a browser to open a workflow-test voice WebSocket.
 *
 * <p>
 * The token is issued via {@code POST /internal/workflow-tests/{workflowId}/voice-session-token} (gated by the standard
 * session cookie) and consumed by the WS upgrade handler. It is not an auth token — it is a one-shot anti-collision
 * guard so that opening the panel in two tabs against the same draft workflow produces two distinct sessions.
 *
 * @author Ivica Cardic
 */
@Service
public class WorkflowTestVoiceSessionTokenService {

    private static final long TOKEN_TTL_SECONDS = 60L;

    private final Cache<String, String> tokenToWorkflowId = Caffeine.newBuilder()
        .expireAfterWrite(TOKEN_TTL_SECONDS, TimeUnit.SECONDS)
        .maximumSize(10000)
        .build();

    public Token issue(String workflowId) {
        String token = UUID.randomUUID()
            .toString();

        tokenToWorkflowId.put(token, workflowId);

        return new Token(token, TOKEN_TTL_SECONDS);
    }

    /**
     * Consume the token and verify it was issued for {@code workflowId}. Returns true on first call with a matching
     * pair; subsequent calls (and mismatched workflowIds) return false.
     */
    public boolean consume(String token, String workflowId) {
        if (token == null || workflowId == null) {
            return false;
        }

        String issuedFor = tokenToWorkflowId.getIfPresent(token);

        if (issuedFor == null || !issuedFor.equals(workflowId)) {
            return false;
        }

        tokenToWorkflowId.invalidate(token);

        return true;
    }

    public record Token(String token, long expiresInSeconds) {
    }
}
