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
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

/**
 * Per-request resolver of an override {@link ChatClient} for Copilot agents. Used to swap the LLM at runtime when the
 * client has supplied a user-selected (provider, model) pair via AG-UI state keys, instead of the workspace-default
 * {@code @Primary ChatModel} the agent was built against.
 *
 * @author Ivica Cardic
 */
public interface OverrideChatClientResolver {

    @Nullable
    ChatClient resolve(State state);

    /**
     * Resolves the environment-default {@link ChatModel} from the runtime provider catalog, honoring the configured
     * preferred provider. Returns {@code null} only when no catalog is active (CE); callers then use their
     * startup-configured model.
     *
     * @throws ChatModelUnavailableException if a catalog is active but no chat model is resolvable in the environment
     */
    default @Nullable ChatModel resolveDefaultChatModel(int environmentId) {
        return null;
    }
}
