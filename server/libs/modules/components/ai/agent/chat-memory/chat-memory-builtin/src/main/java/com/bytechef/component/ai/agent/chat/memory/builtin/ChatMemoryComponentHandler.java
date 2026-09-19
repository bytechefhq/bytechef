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
import com.bytechef.component.ai.agent.chat.memory.builtin.session.util.BuiltInSessionRepositoryFactory;
import com.bytechef.component.ai.agent.chat.memory.builtin.session.util.BuiltInSessionRepositoryFactory.BuiltInSessionRepository;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.PreDestroy;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.session.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(CHAT_MEMORY + "_v1_ComponentHandler")
public class ChatMemoryComponentHandler implements ComponentHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatMemoryComponentHandler.class);

    private final ComponentDefinition componentDefinition;
    private final AutoCloseable closeable;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public ChatMemoryComponentHandler(
        @Autowired(required = false) @Nullable JdbcTemplate jdbcTemplate, Environment environment) {

        // Both the memory cluster element and the actions operate on session memory (event-sourced, persists tool
        // messages) over the application's configured session backend — bytechef.ai.memory.provider, jdbc by default.
        BuiltInSessionRepository builtInSessionRepository = BuiltInSessionRepositoryFactory.create(
            environment, jdbcTemplate);

        this.closeable = builtInSessionRepository.closeable();

        SessionRepository sessionRepository = builtInSessionRepository.sessionRepository();

        this.componentDefinition = component(CHAT_MEMORY)
            .title("Chat Memory")
            .description("Built-in chat memory.")
            .icon("path:assets/chat-memory.svg")
            .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
            .actions(
                ChatMemoryAddMessagesAction.of(sessionRepository),
                ChatMemoryGetMessagesAction.of(sessionRepository),
                ChatMemoryDeleteAction.of(sessionRepository),
                ChatMemoryListConversationsAction.of(sessionRepository))
            .clusterElements(ChatMemory.of(sessionRepository));
    }

    @PreDestroy
    public void destroy() {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception exception) {
                log.warn("Failed to close built-in session repository client", exception);
            }
        }
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
