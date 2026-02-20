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

package com.bytechef.component.ai.agent.chat.memory.jdbc;

import static com.bytechef.component.ai.agent.chat.memory.jdbc.JdbcChatMemoryComponentHandler.JDBC_CHAT_MEMORY;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.chat.memory.jdbc.action.JdbcChatMemoryAddMessagesAction;
import com.bytechef.component.ai.agent.chat.memory.jdbc.action.JdbcChatMemoryDeleteAction;
import com.bytechef.component.ai.agent.chat.memory.jdbc.action.JdbcChatMemoryGetMessagesAction;
import com.bytechef.component.ai.agent.chat.memory.jdbc.action.JdbcChatMemoryListConversationsAction;
import com.bytechef.component.ai.agent.chat.memory.jdbc.cluster.JdbcChatMemory;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.JdbcChatMemoryComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(JDBC_CHAT_MEMORY + "_v1_ComponentHandler")
public class JdbcChatMemoryComponentHandler implements ComponentHandler {

    public static final String JDBC_CHAT_MEMORY = "jdbcChatMemory";

    private final JdbcChatMemoryComponentDefinition componentDefinition;

    public JdbcChatMemoryComponentHandler(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new JdbcChatMemoryComponentDefinitionImpl(
            component(JDBC_CHAT_MEMORY)
                .title("JDBC Chat Memory")
                .description("JDBC Chat Memory stores conversation history in a relational database.")
                .icon("path:assets/jdbc-chat-memory.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .actions(
                    JdbcChatMemoryAddMessagesAction.getActionDefinition(clusterElementDefinitionService),
                    JdbcChatMemoryGetMessagesAction.getActionDefinition(clusterElementDefinitionService),
                    JdbcChatMemoryDeleteAction.getActionDefinition(clusterElementDefinitionService),
                    JdbcChatMemoryListConversationsAction.getActionDefinition(clusterElementDefinitionService))
                .clusterElements(JdbcChatMemory.of(clusterElementDefinitionService)));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class JdbcChatMemoryComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements JdbcChatMemoryComponentDefinition {

        public JdbcChatMemoryComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
