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
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction.CHAT_MEMORY;

import com.bytechef.component.ai.agent.chat.memory.builtin.util.ChatMemoryUtils;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;

/**
 * @author Ivica Cardic
 */
public class ChatMemory {

    public static ClusterElementDefinition<ChatMemoryFunction> of(ChatMemoryRepository chatMemoryRepository) {
        return ComponentDsl.<ChatMemoryFunction>clusterElement("chatMemory")
            .title("Chat Memory")
            .description(
                "Memory is retrieved from the application database and added as prior messages in the conversation.")
            .properties(
                string(CONVERSATION_ID)
                    .label("Conversation ID")
                    .description("The unique identifier for the conversation.")
                    .options(ChatMemoryUtils.getFirstMessages(chatMemoryRepository))
                    .required(true))
            .type(CHAT_MEMORY)
            .object(() -> (inputParameters, connectionParameters, extensions, componentConnections) -> apply(
                inputParameters, chatMemoryRepository));
    }

    private ChatMemory() {
    }

    protected static MessageChatMemoryAdvisor
        apply(Parameters inputParameters, ChatMemoryRepository chatMemoryRepository) {
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(chatMemoryRepository)
            .build();

        return MessageChatMemoryAdvisor.builder(chatMemory)
            .order(BaseAdvisor.HIGHEST_PRECEDENCE + 200)
            .build();
    }
}
