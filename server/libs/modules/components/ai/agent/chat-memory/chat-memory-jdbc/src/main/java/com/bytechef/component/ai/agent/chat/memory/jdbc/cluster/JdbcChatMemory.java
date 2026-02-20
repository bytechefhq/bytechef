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

package com.bytechef.component.ai.agent.chat.memory.jdbc.cluster;

import static com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction.CHAT_MEMORY;
import static com.bytechef.platform.component.definition.ai.agent.DataSourceFunction.DATA_SOURCE;

import com.bytechef.component.definition.ClusterElementDefinition;
import com.bytechef.component.definition.ComponentDsl;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import com.bytechef.platform.component.definition.ai.agent.DataSourceFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
public class JdbcChatMemory {

    private final ClusterElementDefinitionService clusterElementDefinitionService;

    public static ClusterElementDefinition<ChatMemoryFunction> of(
        ClusterElementDefinitionService clusterElementDefinitionService) {

        return new JdbcChatMemory(clusterElementDefinitionService).build();
    }

    private JdbcChatMemory(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.clusterElementDefinitionService = clusterElementDefinitionService;
    }

    private ClusterElementDefinition<ChatMemoryFunction> build() {
        return ComponentDsl.<ChatMemoryFunction>clusterElement("chatMemory")
            .title("JDBC Chat Memory")
            .description("Memory is retrieved from a JDBC database and added into the prompt's system text.")
            .type(CHAT_MEMORY)
            .object(() -> this::apply);
    }

    protected PromptChatMemoryAdvisor apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) throws Exception {

        ClusterElement clusterElement = ClusterElementMap.of(extensions)
            .getClusterElement(DATA_SOURCE);

        DataSourceFunction dataSourceFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        ComponentConnection componentConnection = componentConnections.get(clusterElement.getWorkflowNodeName());

        Map<String, ?> componentConnectionParameters = componentConnection.getParameters();

        DataSource dataSource = dataSourceFunction.apply(
            ParametersFactory.create(clusterElement.getParameters()),
            ParametersFactory.create(componentConnectionParameters),
            ParametersFactory.create(clusterElement.getExtensions()),
            componentConnections);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        JdbcChatMemoryRepositoryDialect dialect = JdbcChatMemoryRepositoryDialect.from(dataSource);

        JdbcChatMemoryRepository jdbcChatMemoryRepository = JdbcChatMemoryRepository.builder()
            .jdbcTemplate(jdbcTemplate)
            .dialect(dialect)
            .build();

        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(jdbcChatMemoryRepository)
            .build();

        return PromptChatMemoryAdvisor.builder(chatMemory)
            .build();
    }
}
