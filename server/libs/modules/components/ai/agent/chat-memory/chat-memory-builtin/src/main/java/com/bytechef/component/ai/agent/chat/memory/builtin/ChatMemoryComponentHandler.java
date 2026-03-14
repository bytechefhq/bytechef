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

import static com.bytechef.component.ai.agent.chat.memory.builtin.constant.ChatMemoryConstants.CHAT_MEMORY;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.chat.memory.builtin.action.ChatMemoryAddMessagesAction;
import com.bytechef.component.ai.agent.chat.memory.builtin.action.ChatMemoryDeleteAction;
import com.bytechef.component.ai.agent.chat.memory.builtin.action.ChatMemoryGetMessagesAction;
import com.bytechef.component.ai.agent.chat.memory.builtin.action.ChatMemoryListConversationsAction;
import com.bytechef.component.ai.agent.chat.memory.builtin.cluster.ChatMemory;
import com.bytechef.component.ai.agent.chat.memory.builtin.config.JdbcChatMemoryRepositoryFactory;
import com.bytechef.component.ai.agent.chat.memory.builtin.config.RedisChatMemoryRepositoryFactory;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.config.ApplicationProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.sql.DataSource;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(CHAT_MEMORY + "_v1_ComponentHandler")
public class ChatMemoryComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public ChatMemoryComponentHandler(
        ApplicationProperties applicationProperties, @Autowired(required = false) DataSource dataSource) {

        ChatMemoryRepository chatMemoryRepository = null;

        if (applicationProperties != null) {
            ApplicationProperties.Ai.Agent.Memory memoryProperties =
                applicationProperties.getAi()
                    .getAgent()
                    .getMemory();

            if (memoryProperties.isEnabled()) {
                chatMemoryRepository = switch (memoryProperties.getProvider()) {
                    case JDBC -> JdbcChatMemoryRepositoryFactory.create(applicationProperties, dataSource);
                    case REDIS -> RedisChatMemoryRepositoryFactory.create(applicationProperties);
                };
            }
        }

        this.componentDefinition = component(CHAT_MEMORY)
            .title("Chat Memory")
            .description("Built-in chat memory.")
            .icon("path:assets/chat-memory.svg")
            .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
            .actions(
                ChatMemoryAddMessagesAction.of(chatMemoryRepository),
                ChatMemoryGetMessagesAction.of(chatMemoryRepository),
                ChatMemoryDeleteAction.of(chatMemoryRepository),
                ChatMemoryListConversationsAction.of(chatMemoryRepository))
            .clusterElements(ChatMemory.of(chatMemoryRepository));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
