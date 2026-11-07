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
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

/**
 * @author Ivica Cardic
 */
@Service
public class WorkflowTestVoiceSessionTokenService {

    private static final long TOKEN_TTL_SECONDS = 60L;

    private final Cache<String, IssuedToken> issuedTokens = Caffeine.newBuilder()
        .expireAfterWrite(TOKEN_TTL_SECONDS, TimeUnit.SECONDS)
        .maximumSize(10000)
        .build();

    public Token issue(String workflowId, long environmentId) {
        String token = UUID.randomUUID()
            .toString();

        issuedTokens.put(token, new IssuedToken(workflowId, environmentId));

        return new Token(token, TOKEN_TTL_SECONDS);
    }

    public OptionalLong consume(String token, String workflowId) {
        if (token == null || workflowId == null) {
            return OptionalLong.empty();
        }

        IssuedToken issuedToken = issuedTokens.getIfPresent(token);

        if (issuedToken == null || !issuedToken.workflowId()
            .equals(workflowId)) {

            return OptionalLong.empty();
        }

        issuedTokens.invalidate(token);

        return OptionalLong.of(issuedToken.environmentId());
    }

    public record Token(String token, long expiresInSeconds) {
    }

    private record IssuedToken(String workflowId, long environmentId) {
    }
}
