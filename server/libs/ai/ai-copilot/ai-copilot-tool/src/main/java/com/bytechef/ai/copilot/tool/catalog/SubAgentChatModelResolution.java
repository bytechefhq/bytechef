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

package com.bytechef.ai.copilot.tool.catalog;

import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;

/**
 * Shared fail-open resolution helper for every intelligent delegate {@code ToolCallback}: resolves the
 * {@link ChatModel} the caller picked from the forwarded parent tool context, tolerating both a missing
 * {@link SubAgentChatModelResolver} and a resolver that throws.
 *
 * <p>
 * A model preference must never fail a delegate call, so both cases fall back to {@code null} — the delegate's default
 * client. This is a single shared implementation rather than eight duplicated try/catch blocks, one per delegate
 * callback, since {@code ai-copilot-tool} (CE) and the EE {@code automation-ai-copilot} module both need it.
 * </p>
 *
 * @author Ivica Cardic
 */
public final class SubAgentChatModelResolution {

    private static final Logger log = LoggerFactory.getLogger(SubAgentChatModelResolution.class);

    private SubAgentChatModelResolution() {
    }

    /**
     * Resolves the {@link ChatModel} the caller picked, from the parent tool context a delegate forwards. Never throws
     * — a missing resolver or a resolver failure both fall back to {@code null}, which callers pass to
     * {@link IntelligentToolChatClientFactory#get(ChatModel)} to mean "use the contributor's default client".
     *
     * @param chatModelResolver the resolver to consult, or {@code null} when the surface never wired one
     * @param parentContext     the forwarded parent tool context map
     * @return the picked {@link ChatModel}, or {@code null} when there is no usable pick
     */
    @Nullable
    public static ChatModel resolve(
        @Nullable SubAgentChatModelResolver chatModelResolver, Map<String, Object> parentContext) {

        if (chatModelResolver == null) {
            return null;
        }

        try {
            return chatModelResolver.resolve(parentContext);
        } catch (RuntimeException exception) {
            log.warn("Subagent chat model resolution failed; using the default client", exception);

            return null;
        }
    }
}
