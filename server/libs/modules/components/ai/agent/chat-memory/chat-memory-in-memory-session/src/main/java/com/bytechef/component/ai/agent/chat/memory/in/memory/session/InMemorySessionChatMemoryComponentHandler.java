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

package com.bytechef.component.ai.agent.chat.memory.in.memory.session;

import static com.bytechef.component.ai.agent.chat.memory.in.memory.session.constant.InMemorySessionChatMemoryConstants.IN_MEMORY_SESSION_CHAT_MEMORY;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.chat.memory.in.memory.session.cluster.InMemorySessionChatMemory;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(IN_MEMORY_SESSION_CHAT_MEMORY + "_v1_ComponentHandler")
public class InMemorySessionChatMemoryComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition = component(IN_MEMORY_SESSION_CHAT_MEMORY)
        .title("In-memory Session Repository")
        .description("In-memory storage backend for Session Chat Memory.")
        .icon("path:assets/in-memory-session-chat-memory.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .clusterElements(InMemorySessionChatMemory.of());

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
