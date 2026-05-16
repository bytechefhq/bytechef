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

package com.bytechef.component.ai.agent.chat.memory.redis.cluster;

import static com.bytechef.component.ai.agent.chat.memory.redis.constant.RedisChatMemoryConstants.CONVERSATION_ID;
import static com.bytechef.component.ai.agent.chat.memory.redis.util.RedisChatMemoryUtils.getChatMemoryRepository;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction.CHAT_MEMORY;

import com.bytechef.component.ai.agent.chat.memory.redis.util.RedisChatMemoryUtils;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import java.util.Map;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;

/**
 * @author Ivica Cardic
 */
public class RedisChatMemory {

    public static final ClusterElementDefinition<ChatMemoryFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ChatMemoryFunction>clusterElement("chatMemory")
            .title("Redis Chat Memory")
            .description("Memory is retrieved from Redis and added as prior messages in the conversation.")
            .properties(
                string(CONVERSATION_ID)
                    .label("Conversation ID")
                    .description("The unique identifier for the conversation.")
                    .options(RedisChatMemoryUtils.getFirstMessages())
                    .required(true))
            .type(CHAT_MEMORY)
            .object(() -> RedisChatMemory::apply);

    protected static MessageChatMemoryAdvisor apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) {

        ChatMemoryRepository chatMemoryRepository = getChatMemoryRepository(connectionParameters);

        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(chatMemoryRepository)
            .build();

        return MessageChatMemoryAdvisor.builder(chatMemory)
            .order(BaseAdvisor.HIGHEST_PRECEDENCE + 200)
            .build();
    }

    private RedisChatMemory() {
    }
}
