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

package com.bytechef.component.ai.agent.chat.memory.jdbc.util;

import static com.bytechef.platform.component.definition.ai.agent.DataSourceFunction.DATA_SOURCE;

import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.DataSourceFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import java.sql.DatabaseMetaData;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepositoryDialect;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcUtils;

/**
 * @author Ivica Cardic
 */
public class JdbcChatMemoryUtils {

    public static ChatMemoryRepository getChatMemoryRepository(
        Parameters extensions, Map<String, ComponentConnection> componentConnections,
        ClusterElementDefinitionService clusterElementDefinitionService) throws Exception {

        DataSource dataSource = getDataSource(extensions, componentConnections, clusterElementDefinitionService);

        initializeSchema(dataSource);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        JdbcChatMemoryRepositoryDialect dialect = JdbcChatMemoryRepositoryDialect.from(dataSource);

        return JdbcChatMemoryRepository.builder()
            .jdbcTemplate(jdbcTemplate)
            .dialect(dialect)
            .build();
    }

    private static void initializeSchema(DataSource dataSource) {
        String schemaScript = resolveSchemaScript(dataSource);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource(schemaScript));

        populator.setContinueOnError(true);

        DatabasePopulatorUtils.execute(populator, dataSource);
    }

    private static String resolveSchemaScript(DataSource dataSource) {
        String productName = null;

        try {
            productName = JdbcUtils.extractDatabaseMetaData(dataSource, DatabaseMetaData::getDatabaseProductName);
        } catch (Exception ignored) {
        }

        String schemaName = switch (productName != null ? productName : "") {
            case "MySQL" -> "schema-mysql.sql";
            case "MariaDB" -> "schema-mariadb.sql";
            case "Oracle" -> "schema-oracle.sql";
            default -> "schema-postgresql.sql";
        };

        return "org/springframework/ai/chat/memory/repository/jdbc/" + schemaName;
    }

    public static DataSource getDataSource(
        Parameters extensions, Map<String, ComponentConnection> componentConnections,
        ClusterElementDefinitionService clusterElementDefinitionService) throws Exception {

        ClusterElement clusterElement = ClusterElementMap.of(extensions)
            .getClusterElement(DATA_SOURCE);

        DataSourceFunction dataSourceFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        ComponentConnection componentConnection = componentConnections.get(clusterElement.getWorkflowNodeName());

        Map<String, ?> componentConnectionParameters = componentConnection.getParameters();

        return dataSourceFunction.apply(
            ParametersFactory.create(clusterElement.getParameters()),
            ParametersFactory.create(componentConnectionParameters),
            ParametersFactory.create(clusterElement.getExtensions()),
            componentConnections);
    }

    private JdbcChatMemoryUtils() {
    }
}
