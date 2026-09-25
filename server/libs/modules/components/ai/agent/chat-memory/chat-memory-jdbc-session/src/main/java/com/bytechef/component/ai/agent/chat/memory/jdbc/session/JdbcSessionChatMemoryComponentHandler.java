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

package com.bytechef.component.ai.agent.chat.memory.jdbc.session;

import static com.bytechef.component.ai.agent.chat.memory.jdbc.session.constant.JdbcSessionChatMemoryConstants.JDBC_SESSION_CHAT_MEMORY;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.ai.agent.chat.memory.jdbc.session.cluster.JdbcSessionChatMemory;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.JdbcSessionChatMemoryComponentDefinition;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component(JDBC_SESSION_CHAT_MEMORY + "_v1_ComponentHandler")
public class JdbcSessionChatMemoryComponentHandler implements ComponentHandler {

    private final JdbcSessionChatMemoryComponentDefinition componentDefinition;

    public JdbcSessionChatMemoryComponentHandler(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new JdbcSessionChatMemoryComponentDefinitionImpl(
            component(JDBC_SESSION_CHAT_MEMORY)
                .title("JDBC Session Repository")
                .description("JDBC storage backend for Session Chat Memory.")
                .icon("path:assets/jdbc-session-chat-memory.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .clusterElements(JdbcSessionChatMemory.of(clusterElementDefinitionService)));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class JdbcSessionChatMemoryComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements JdbcSessionChatMemoryComponentDefinition {

        public JdbcSessionChatMemoryComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
