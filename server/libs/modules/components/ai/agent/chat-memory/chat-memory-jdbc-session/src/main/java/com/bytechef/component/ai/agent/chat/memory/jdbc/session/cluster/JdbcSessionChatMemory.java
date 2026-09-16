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

package com.bytechef.component.ai.agent.chat.memory.jdbc.session.cluster;

import static com.bytechef.platform.component.definition.ai.agent.SessionRepositoryFunction.SESSION_REPOSITORY;

import com.bytechef.component.ai.agent.chat.memory.jdbc.session.util.SessionChatMemoryUtils;
import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ai.agent.SessionRepositoryFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.ai.session.SessionRepository;

/**
 * @author Ivica Cardic
 */
public class JdbcSessionChatMemory {

    private final ClusterElementDefinitionService clusterElementDefinitionService;

    public static ClusterElementDefinition<SessionRepositoryFunction> of(
        ClusterElementDefinitionService clusterElementDefinitionService) {

        return new JdbcSessionChatMemory(clusterElementDefinitionService).build();
    }

    private JdbcSessionChatMemory(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    private ClusterElementDefinition<SessionRepositoryFunction> build() {
        return ComponentDsl.<SessionRepositoryFunction>clusterElement("sessionRepository")
            .title("JDBC Session Repository")
            .description("Stores session events in a relational database.")
            .type(SESSION_REPOSITORY)
            .object(() -> this::apply);
    }

    protected SessionRepository apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) throws Exception {

        DataSource dataSource = SessionChatMemoryUtils.getDataSource(
            extensions, componentConnections, clusterElementDefinitionService);

        return SessionChatMemoryUtils.getSessionRepository(dataSource);
    }
}
