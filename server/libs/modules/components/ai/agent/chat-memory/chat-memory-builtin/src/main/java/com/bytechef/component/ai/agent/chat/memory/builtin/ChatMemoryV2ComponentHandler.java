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
import com.bytechef.component.ai.agent.chat.memory.builtin.util.BuiltInSessionRepositoryFactory;
import com.bytechef.component.ai.agent.chat.memory.builtin.util.BuiltInSessionRepositoryFactory.BuiltInSessionStore;
import com.bytechef.component.ai.agent.chat.memory.session.SessionRepositoryResolver;
import com.bytechef.component.ai.agent.chat.memory.session.action.SessionChatMemoryActions;
import com.bytechef.component.ai.agent.chat.memory.session.cluster.SessionChatMemory;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.SessionChatMemoryComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
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
@Component(CHAT_MEMORY + "_v2_ComponentHandler")
public class ChatMemoryV2ComponentHandler implements ComponentHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatMemoryV2ComponentHandler.class);

    private final AutoCloseable closeable;
    private final SessionChatMemoryComponentDefinition componentDefinition;

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
    public ChatMemoryV2ComponentHandler(
        @Autowired(required = false) @Nullable JdbcTemplate jdbcTemplate, Environment environment,
        ClusterElementDefinitionService clusterElementDefinitionService) {

        BuiltInSessionStore builtInSessionStore = BuiltInSessionRepositoryFactory.create(environment, jdbcTemplate);

        SessionRepository sessionRepository = builtInSessionStore.sessionRepository();

        SessionRepositoryResolver sessionRepositoryResolver =
            (inputParameters, connectionParameters, extensions, componentConnections) -> sessionRepository;

        this.closeable = builtInSessionStore.closeable();
        this.componentDefinition = new ChatMemoryV2ComponentDefinition(
            component(CHAT_MEMORY)
                .title("Chat Memory")
                .description("Built-in session-based chat memory.")
                .icon("path:assets/chat-memory.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .version(2)
                .actions(SessionChatMemoryActions.of("chat-memory", sessionRepositoryResolver, false))
                .clusterElements(
                    SessionChatMemory.of("Chat Memory", sessionRepositoryResolver, clusterElementDefinitionService)));
    }

    @PreDestroy
    public void destroy() {
        try {
            closeable.close();
        } catch (Exception exception) {
            log.warn("Failed to close the built-in session repository client", exception);
        }
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class ChatMemoryV2ComponentDefinition extends AbstractComponentDefinitionWrapper
        implements SessionChatMemoryComponentDefinition {

        ChatMemoryV2ComponentDefinition(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
