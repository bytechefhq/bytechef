/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.ai.memory.inmemory.aiagent;

import static com.bytechef.platform.component.definition.aiagent.MemoryFunction.MEMORY;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.aiagent.MemoryFunction;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;

/**
 * @author Ivica Cardic
 */
public class InMemoryMemory {

    private static final InMemoryChatMemory inMemoryChatMemory = new InMemoryChatMemory();

    public static final ClusterElementDefinition<MemoryFunction> CLUSTER_ELEMENT_DEFINITION =
        ComponentDsl.<MemoryFunction>clusterElement("memory")
            .title("Memory")
            .description("Memory.")
            .type(MEMORY)
            .object(() -> InMemoryMemory::apply);

    protected static MessageChatMemoryAdvisor apply(Parameters inputParameters, Parameters connectionParameters) {
        return new MessageChatMemoryAdvisor(inMemoryChatMemory);
    }
}
