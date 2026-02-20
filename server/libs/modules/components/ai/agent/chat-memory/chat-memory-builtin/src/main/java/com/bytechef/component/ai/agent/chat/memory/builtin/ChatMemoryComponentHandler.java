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

package com.bytechef.component.ai.agent.chat.memory.builtin;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.chat.memory.builtin.action.ChatMemoryAddMessagesAction;
import com.bytechef.component.ai.agent.chat.memory.builtin.action.ChatMemoryDeleteAction;
import com.bytechef.component.ai.agent.chat.memory.builtin.action.ChatMemoryGetMessagesAction;
import com.bytechef.component.ai.agent.chat.memory.builtin.action.ChatMemoryListConversationsAction;
import com.bytechef.component.ai.agent.chat.memory.builtin.cluster.ChatMemory;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.google.auto.service.AutoService;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@AutoService(ComponentHandler.class)
public class ChatMemoryComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("chatMemory")
        .title("Chat Memory")
        .description("Built-in Chat Memory using application database.")
        .icon("path:assets/chat-memory.svg")
        .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
        .actions(
            ChatMemoryAddMessagesAction.ACTION_DEFINITION,
            ChatMemoryGetMessagesAction.ACTION_DEFINITION,
            ChatMemoryDeleteAction.ACTION_DEFINITION,
            ChatMemoryListConversationsAction.ACTION_DEFINITION)
        .clusterElements(ChatMemory.CLUSTER_ELEMENT_DEFINITION);

    public ChatMemoryComponentHandler() {
    }

    @Autowired(required = false)
    public ChatMemoryComponentHandler(ChatMemoryRepository chatMemoryRepository) {
        ChatMemoryAddMessagesAction.setChatMemoryRepository(chatMemoryRepository);
        ChatMemoryGetMessagesAction.setChatMemoryRepository(chatMemoryRepository);
        ChatMemoryDeleteAction.setChatMemoryRepository(chatMemoryRepository);
        ChatMemoryListConversationsAction.setChatMemoryRepository(chatMemoryRepository);
        ChatMemory.setChatMemoryRepository(chatMemoryRepository);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
