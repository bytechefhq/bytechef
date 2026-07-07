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

package com.bytechef.ai.copilot.agent;

import com.agui.core.state.State;
import com.bytechef.platform.annotation.ConditionalOnCEVersion;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * CE default {@link OverrideChatClientResolver}. CE has no catalog-driven per-request (provider, model) override, so
 * this always returns {@code null}, letting Copilot agents fall back to their injected base
 * {@link org.springframework.ai.chat.model.ChatModel} (resolved for CE by {@code AiModelConfiguration} in
 * {@code ai-model-config}). The EE {@code CopilotChatClientResolver} takes precedence over this bean whenever it is
 * present.
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnCEVersion
@ConditionalOnMissingBean(OverrideChatClientResolver.class)
public class DefaultOverrideChatClientResolver implements OverrideChatClientResolver {

    @Override
    public @Nullable ChatClient resolve(State state) {
        return null;
    }
}
