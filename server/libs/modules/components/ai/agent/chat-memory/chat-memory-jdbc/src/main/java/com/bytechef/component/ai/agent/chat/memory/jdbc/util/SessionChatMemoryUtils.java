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

import com.bytechef.commons.util.ClientCacheSettings;
import com.bytechef.commons.util.ClientCacheUtils;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.DataSourceFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import com.github.benmanes.caffeine.cache.Cache;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.DatabaseMetaData;
import java.time.Duration;
import java.util.Map;
import javax.sql.DataSource;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.jdbc.JdbcSessionRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
public class SessionChatMemoryUtils {

    private static final Duration POOLED_CONNECTION_IDLE_TIMEOUT = Duration.ofMinutes(10);
    private static final String ORACLE = "Oracle";
    private static final String ORACLE_NOT_SUPPORTED_MESSAGE =
        "JDBC Chat Memory v2 does not support Oracle; use JDBC Chat Memory v1";
    private static final int POOL_MAXIMUM_SIZE = 5;

    private static final Cache<DataSourceKey, PooledSessionRepository> SESSION_REPOSITORIES =
        createSessionRepositoryCache(ClientCacheSettings.defaults());

    private SessionChatMemoryUtils() {
    }

    static <K> Cache<K, PooledSessionRepository> createSessionRepositoryCache(
        ClientCacheSettings clientCacheSettings) {

        return ClientCacheUtils.createClientCache(clientCacheSettings, PooledSessionRepository::close);
    }

    public static SessionRepository createSessionRepository(DataSource dataSource) {
        if (ORACLE.equals(getDatabaseProductName(dataSource))) {
            throw new IllegalStateException(ORACLE_NOT_SUPPORTED_MESSAGE);
        }

        return JdbcSessionRepository.builder()
            .dataSource(dataSource)
            .jsonMapper(JsonMapper.builder()
                .build())
            .build();
    }

    public static SessionRepository getSessionRepository(DataSource dataSource) {
        return getSessionRepository(dataSource, SESSION_REPOSITORIES);
    }

    static SessionRepository getSessionRepository(
        DataSource dataSource, Cache<DataSourceKey, PooledSessionRepository> sessionRepositories) {

        if (dataSource instanceof DriverManagerDataSource driverManagerDataSource) {
            DataSourceKey dataSourceKey = new DataSourceKey(
                driverManagerDataSource.getUrl(), driverManagerDataSource.getUsername(),
                driverManagerDataSource.getPassword());

            if (driverManagerDataSource instanceof SingleConnectionDataSource singleConnectionDataSource) {
                singleConnectionDataSource.destroy();
            }

            PooledSessionRepository pooledSessionRepository = sessionRepositories.get(
                dataSourceKey, SessionChatMemoryUtils::createPooledSessionRepository);

            return pooledSessionRepository.sessionRepository();
        }

        initializeSchema(dataSource);

        return createSessionRepository(dataSource);
    }

    public static DataSource getDataSource(
        Parameters extensions, Map<String, ComponentConnection> componentConnections,
        ClusterElementDefinitionService clusterElementDefinitionService) throws Exception {

        ClusterElement clusterElement = ClusterElementMap.of(extensions)
            .getClusterElement(DATA_SOURCE);

        DataSourceFunction dataSourceFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        String workflowNodeName = clusterElement.getWorkflowNodeName();

        ComponentConnection componentConnection = componentConnections.get(workflowNodeName);

        if (componentConnection == null) {
            throw new IllegalStateException(
                "The Data Source " + workflowNodeName + " of JDBC Chat Memory has no connection; select a database " +
                    "connection for it.");
        }

        return dataSourceFunction.apply(
            ParametersFactory.create(clusterElement.getParameters()),
            ParametersFactory.create(componentConnection.getParameters()),
            ParametersFactory.create(clusterElement.getExtensions()), componentConnections);
    }

    public static void initializeSchema(DataSource dataSource) {
        ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator(
            new ClassPathResource(resolveSchemaScript(dataSource)));

        DatabasePopulatorUtils.execute(resourceDatabasePopulator, dataSource);
    }

    private static PooledSessionRepository createPooledSessionRepository(DataSourceKey dataSourceKey) {
        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setJdbcUrl(dataSourceKey.url());
        hikariConfig.setUsername(dataSourceKey.username());
        hikariConfig.setPassword(dataSourceKey.password());
        hikariConfig.setMaximumPoolSize(POOL_MAXIMUM_SIZE);
        hikariConfig.setMinimumIdle(0);
        hikariConfig.setIdleTimeout(POOLED_CONNECTION_IDLE_TIMEOUT.toMillis());
        hikariConfig.setPoolName("session-chat-memory");

        HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);

        try {
            initializeSchema(hikariDataSource);
        } catch (RuntimeException runtimeException) {
            hikariDataSource.close();

            throw runtimeException;
        }

        return new PooledSessionRepository(createSessionRepository(hikariDataSource), hikariDataSource);
    }

    private static String resolveSchemaScript(DataSource dataSource) {
        String productName = getDatabaseProductName(dataSource);

        String schemaName = switch (productName) {
            case "MySQL", "MariaDB" -> "schema-mysql.sql";
            case "H2" -> "schema-h2.sql";
            case "PostgreSQL" -> "schema-postgresql.sql";
            case ORACLE -> throw new IllegalStateException(ORACLE_NOT_SUPPORTED_MESSAGE);
            default -> throw new IllegalStateException(
                "JDBC Chat Memory v2 does not support the " + productName + " database");
        };

        return "org/springframework/ai/session/jdbc/" + schemaName;
    }

    private static String getDatabaseProductName(DataSource dataSource) {
        try {
            return JdbcUtils.extractDatabaseMetaData(dataSource, DatabaseMetaData::getDatabaseProductName);
        } catch (MetaDataAccessException metaDataAccessException) {
            throw new IllegalStateException(
                "Failed to read the database product name for the JDBC Chat Memory schema", metaDataAccessException);
        }
    }

    record DataSourceKey(String url, @Nullable String username, @Nullable String password) {

        @Override
        public String toString() {
            return "DataSourceKey{url=" + redactUrl(url) + ", username=" + username + "}";
        }

        private static String redactUrl(String url) {
            String urlWithoutParameters = url.split("[?;]", 2)[0];

            int credentialsEndIndex = urlWithoutParameters.lastIndexOf('@');

            if (credentialsEndIndex < 0) {
                return urlWithoutParameters;
            }

            int subprotocolEndIndex = urlWithoutParameters.indexOf(':', "jdbc:".length());

            return urlWithoutParameters.substring(0, subprotocolEndIndex + 1) +
                urlWithoutParameters.substring(credentialsEndIndex + 1);
        }
    }

    record PooledSessionRepository(SessionRepository sessionRepository, HikariDataSource hikariDataSource) {

        void close() {
            hikariDataSource.close();
        }
    }
}
