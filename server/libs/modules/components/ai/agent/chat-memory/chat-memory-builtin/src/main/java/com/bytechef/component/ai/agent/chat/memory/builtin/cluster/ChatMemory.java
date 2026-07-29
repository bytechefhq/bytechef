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

package com.bytechef.component.ai.agent.chat.memory.builtin.cluster;

import static com.bytechef.component.ai.agent.chat.memory.builtin.constant.ChatMemoryConstants.CONVERSATION_ID;
import static com.bytechef.component.ai.agent.chat.memory.builtin.constant.ChatMemoryConstants.DEFAULT_USER_ID;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction.CHAT_MEMORY;

import com.bytechef.component.ai.agent.chat.memory.builtin.util.ChatMemoryUtils;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;

/**
 * Built-in chat memory, backed by event-sourced session memory over the application's configured session backend (jdbc
 * by default). Session memory persists the full tool request/response transcript, so the advisor runs <b>inside</b> the
 * tool-calling loop (see {@code ChatMemoryFunction.TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER}) and the agent disables the
 * tool advisor's own in-loop history to avoid double-writing.
 *
 * @author Ivica Cardic
 */
public class ChatMemory {

    public static ClusterElementDefinition<ChatMemoryFunction> of(SessionRepository sessionRepository) {
        return ComponentDsl.<ChatMemoryFunction>clusterElement("chatMemory")
            .title("Chat Memory")
            .description(
                "Memory is retrieved from the application's session store and added as prior messages in the " +
                    "conversation, including prior tool calls and their results.")
            .properties(
                string(CONVERSATION_ID)
                    .label("Conversation ID")
                    .description("The unique identifier for the conversation.")
                    .options(ChatMemoryUtils.getFirstMessages(sessionRepository))
                    .required(true))
            .type(CHAT_MEMORY)
            .object(() -> (inputParameters, connectionParameters, extensions, componentConnections) -> apply(
                sessionRepository));
    }

    private ChatMemory() {
    }

    protected static ChatMemoryFunction.Result apply(SessionRepository sessionRepository) {
        SessionService sessionService = DefaultSessionService.builder()
            .sessionRepository(sessionRepository)
            .build();

        // The default user id keeps advisor-created sessions listable by the component's actions and options lookup,
        // which query by user id (SessionRepository has no list-all API).
        SessionMemoryAdvisor sessionMemoryAdvisor = SessionMemoryAdvisor.builder(sessionService)
            .defaultUserId(DEFAULT_USER_ID)
            .order(ChatMemoryFunction.TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER)
            .build();

        return new ChatMemoryFunction.Result(sessionMemoryAdvisor, null, true);
    }
}
