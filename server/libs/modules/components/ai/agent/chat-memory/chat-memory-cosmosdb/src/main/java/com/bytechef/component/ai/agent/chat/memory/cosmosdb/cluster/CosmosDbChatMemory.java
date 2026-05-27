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

package com.bytechef.component.ai.agent.chat.memory.cosmosdb.cluster;

import static com.bytechef.component.ai.agent.chat.memory.cosmosdb.constant.CosmosDbChatMemoryConstants.CONVERSATION_ID;
import static com.bytechef.component.ai.agent.chat.memory.cosmosdb.util.CosmosDbChatMemoryUtils.getChatMemoryRepository;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction.CHAT_MEMORY;

import com.bytechef.component.ai.agent.chat.memory.cosmosdb.util.CosmosDbChatMemoryUtils;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import java.util.Map;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;

/**
 * @author Ivica Cardic
 */
public class CosmosDbChatMemory {

    private CosmosDbChatMemory() {
    }

    public static final ClusterElementDefinition<ChatMemoryFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ChatMemoryFunction>clusterElement("chatMemory")
            .title("Cosmos DB Chat Memory")
            .description("Memory is retrieved from Azure Cosmos DB and added as prior messages in the conversation.")
            .properties(
                string(CONVERSATION_ID)
                    .label("Conversation ID")
                    .description("The unique identifier for the conversation.")
                    .options(CosmosDbChatMemoryUtils.getFirstMessages())
                    .required(true))
            .type(CHAT_MEMORY)
            .object(() -> CosmosDbChatMemory::apply);

    protected static ChatMemoryFunction.Result apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) {

        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(getChatMemoryRepository(connectionParameters))
            .build();

        return new ChatMemoryFunction.Result(
            MessageChatMemoryAdvisor.builder(chatMemory)
                .order(BaseAdvisor.HIGHEST_PRECEDENCE + 200)
                .build(),
            chatMemory);
    }
}
