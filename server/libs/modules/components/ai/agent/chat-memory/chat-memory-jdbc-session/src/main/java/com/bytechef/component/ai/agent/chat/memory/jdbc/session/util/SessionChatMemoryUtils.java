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

package com.bytechef.component.ai.agent.chat.memory.jdbc.session.util;

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
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.jdbc.JdbcSessionRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcUtils;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
public class SessionChatMemoryUtils {

    private SessionChatMemoryUtils() {
    }

    public static SessionRepository getSessionRepository(DataSource dataSource) {
        initializeSchema(dataSource);

        return JdbcSessionRepository.builder()
            .dataSource(dataSource)
            .jsonMapper(JsonMapper.builder()
                .build())
            .build();
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

        return dataSourceFunction.apply(
            ParametersFactory.create(clusterElement.getParameters()),
            ParametersFactory.create(componentConnection.getParameters()),
            ParametersFactory.create(clusterElement.getExtensions()), componentConnections);
    }

    public static void initializeSchema(DataSource dataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
            new ClassPathResource(resolveSchemaScript(dataSource)));

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
            case "MySQL", "MariaDB" -> "schema-mysql.sql";
            case "H2" -> "schema-h2.sql";
            default -> "schema-postgresql.sql";
        };

        return "org/springframework/ai/session/jdbc/" + schemaName;
    }
}
