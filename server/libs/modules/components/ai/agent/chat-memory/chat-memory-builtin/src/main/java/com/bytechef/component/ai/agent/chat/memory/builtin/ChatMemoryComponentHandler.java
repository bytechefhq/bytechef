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
import com.bytechef.component.ai.agent.chat.memory.jdbc.util.JdbcChatMemoryUtils;
import com.bytechef.component.ai.agent.chat.memory.jdbc.util.OrderedJdbcChatMemoryRepository;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(CHAT_MEMORY + "_v1_ComponentHandler")
public class ChatMemoryComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public ChatMemoryComponentHandler(
        ChatMemoryRepository chatMemoryRepository,
        @Autowired(required = false) @Nullable JdbcTemplate jdbcTemplate) {

        ChatMemoryRepository orderedRepository = chatMemoryRepository;

        if (jdbcTemplate != null) {
            DataSource dataSource = jdbcTemplate.getDataSource();

            if (dataSource != null) {
                JdbcChatMemoryRepositoryDialect dialect = JdbcChatMemoryRepositoryDialect.from(dataSource);

                orderedRepository = new OrderedJdbcChatMemoryRepository(
                    chatMemoryRepository, jdbcTemplate,
                    JdbcChatMemoryUtils.getSelectConversationIdsOrderedSql(dialect));
            }
        }

        this.componentDefinition = component(CHAT_MEMORY)
            .title("Chat Memory")
            .description("Built-in chat memory.")
            .icon("path:assets/chat-memory.svg")
            .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
            .actions(
                ChatMemoryAddMessagesAction.of(orderedRepository),
                ChatMemoryGetMessagesAction.of(orderedRepository),
                ChatMemoryDeleteAction.of(orderedRepository),
                ChatMemoryListConversationsAction.of(orderedRepository))
            .clusterElements(ChatMemory.of(orderedRepository));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
