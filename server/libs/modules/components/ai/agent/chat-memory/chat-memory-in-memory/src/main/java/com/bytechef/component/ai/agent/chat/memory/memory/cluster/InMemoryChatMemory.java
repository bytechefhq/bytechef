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

package com.bytechef.component.ai.agent.chat.memory.memory.cluster;

import static com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction.CHAT_MEMORY;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import java.util.Map;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;

/**
 * @author Ivica Cardic
 */
public class InMemoryChatMemory {

    private static final MessageWindowChatMemory inMemoryChatMemory =
        MessageWindowChatMemory.builder()
            .chatMemoryRepository(new InMemoryChatMemoryRepository())
            .build();

    public static final ClusterElementDefinition<ChatMemoryFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<ChatMemoryFunction>clusterElement("chatMemory")
            .title("In Memory Chat Memory")
            .description("Memory is retrieved added into the prompt's system text.")
            .type(CHAT_MEMORY)
            .object(() -> InMemoryChatMemory::apply);

    protected static PromptChatMemoryAdvisor apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) {

        return PromptChatMemoryAdvisor.builder(inMemoryChatMemory)
            .build();
    }
}
