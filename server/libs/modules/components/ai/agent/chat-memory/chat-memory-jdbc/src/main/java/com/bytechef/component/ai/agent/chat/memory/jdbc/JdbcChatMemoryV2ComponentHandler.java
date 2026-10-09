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
import com.bytechef.component.ai.agent.chat.memory.jdbc.util.SessionChatMemoryUtils;
import com.bytechef.component.ai.agent.chat.memory.session.SessionRepositoryResolver;
import com.bytechef.component.ai.agent.chat.memory.session.action.SessionChatMemoryActions;
import com.bytechef.component.ai.agent.chat.memory.session.cluster.SessionChatMemory;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.JdbcSessionChatMemoryComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(JDBC_CHAT_MEMORY + "_v2_ComponentHandler")
public class JdbcChatMemoryV2ComponentHandler implements ComponentHandler {

    private final JdbcSessionChatMemoryComponentDefinition componentDefinition;

    public JdbcChatMemoryV2ComponentHandler(ClusterElementDefinitionService clusterElementDefinitionService) {
        SessionRepositoryResolver sessionRepositoryResolver =
            (inputParameters, connectionParameters, extensions, componentConnections) -> SessionChatMemoryUtils
                .getSessionRepository(
                    SessionChatMemoryUtils.getDataSource(
                        extensions, componentConnections, clusterElementDefinitionService));

        this.componentDefinition = new JdbcChatMemoryV2ComponentDefinition(
            component(JDBC_CHAT_MEMORY)
                .title("JDBC Chat Memory")
                .description("JDBC Chat Memory stores session-based conversation history in a relational database.")
                .icon("path:assets/jdbc-chat-memory.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .version(2)
                .actions(SessionChatMemoryActions.of("jdbc-chat-memory", sessionRepositoryResolver, true))
                .clusterElements(
                    SessionChatMemory.of(
                        "JDBC Chat Memory", sessionRepositoryResolver, clusterElementDefinitionService)));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class JdbcChatMemoryV2ComponentDefinition extends AbstractComponentDefinitionWrapper
        implements JdbcSessionChatMemoryComponentDefinition {

        JdbcChatMemoryV2ComponentDefinition(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
