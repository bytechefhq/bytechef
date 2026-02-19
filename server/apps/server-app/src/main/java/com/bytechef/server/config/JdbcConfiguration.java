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

package com.bytechef.server.config;

import com.bytechef.atlas.configuration.converter.StringToWorkflowTaskConverter;
import com.bytechef.atlas.configuration.converter.WorkflowTaskToStringConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.StringToWebhooksConverter;
import com.bytechef.atlas.execution.repository.jdbc.converter.WebhooksToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.commons.data.jdbc.converter.ExecutionErrorToStringConverter;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToExecutionErrorConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.ee.ai.copilot.repository.converter.ListDoubleToPGObjectConverter;
import com.bytechef.ee.ai.copilot.repository.converter.MapToPGObjectConverter;
import com.bytechef.ee.ai.copilot.repository.converter.PGObjectToListDoubleConverter;
import com.bytechef.ee.ai.copilot.repository.converter.PGobjectToMapConverter;
import com.bytechef.encryption.Encryption;
import com.bytechef.platform.data.storage.jdbc.repository.converter.DataEntryValueWrapperToStringConverter;
import com.bytechef.platform.data.storage.jdbc.repository.converter.StringToDataEntryValueWrapperConverter;
import com.bytechef.platform.workflow.execution.repository.converter.JobResumeIdToStringConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToJobResumeIdConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToTaskStateValueConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToTriggerStateValueConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToWorkflowExecutionIdConverter;
import com.bytechef.platform.workflow.execution.repository.converter.StringToWorkflowTriggerConverter;
import com.bytechef.platform.workflow.execution.repository.converter.TaskStateValueToStringConverter;
import com.bytechef.platform.workflow.execution.repository.converter.TriggerStateValueToStringConverter;
import com.bytechef.platform.workflow.execution.repository.converter.WorkflowExecutionIdToStringConverter;
import com.bytechef.platform.workflow.execution.repository.converter.WorkflowTriggerToStringConverter;
import com.bytechef.tenant.annotation.ConditionalOnSingleTenant;
import com.zaxxer.hikari.HikariDataSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.jdbc.core.convert.DataAccessStrategy;
import org.springframework.data.jdbc.core.convert.DefaultDataAccessStrategy;
import org.springframework.data.jdbc.core.convert.InsertStrategyFactory;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.core.convert.QueryMappingConfiguration;
import org.springframework.data.jdbc.core.convert.SqlGeneratorSource;
import org.springframework.data.jdbc.core.convert.SqlParametersFactory;
import org.springframework.data.jdbc.core.mapping.JdbcMappingContext;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
@Configuration
public class JdbcConfiguration extends AbstractJdbcConfiguration {

    private final DataSourceProperties dataSourceProperties;
    private final Encryption encryption;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI2")
    public JdbcConfiguration(
        DataSourceProperties dataSourceProperties, Encryption encryption, ObjectMapper objectMapper) {

        this.dataSourceProperties = dataSourceProperties;
        this.encryption = encryption;
        this.objectMapper = objectMapper;
    }

    @Bean
    @Primary
    DataAccessStrategy dataAccessStrategy(
        NamedParameterJdbcOperations operations, JdbcConverter jdbcConverter, RelationalMappingContext context,
        Dialect jdbcDialect) {

        return new DefaultDataAccessStrategy(new SqlGeneratorSource(context, jdbcConverter, jdbcDialect), context,
            jdbcConverter, operations, new SqlParametersFactory(context, jdbcConverter),
            new InsertStrategyFactory(operations, jdbcDialect), QueryMappingConfiguration.EMPTY);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "bytechef.datasource", name = "url")
    @ConditionalOnSingleTenant
    DataSource dataSource(DataSourceProperties properties) {
        return DataSourceBuilder.create(properties.getClassLoader())
            .type(HikariDataSource.class)
            .url(Objects.requireNonNull(dataSourceProperties.getUrl()))
            .username(dataSourceProperties.getUsername())
            .password(dataSourceProperties.getPassword())
            .build();
    }

    @Bean
    @Primary
    public JdbcAggregateTemplate jdbcAggregateTemplate(
        ApplicationContext applicationContext, JdbcMappingContext mappingContext, JdbcConverter converter,
        DataAccessStrategy dataAccessStrategy) {

        return new JdbcAggregateTemplate(applicationContext, mappingContext, converter, dataAccessStrategy);
    }

    @Bean
    @Primary
    JdbcClient jdbcClient(DataSource dataSource) {
        return JdbcClient.create(dataSource);
    }

    @Bean
    @Primary
    NamedParameterJdbcOperations jdbcOperations(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    @Primary
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    @Primary
    PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }

    @Override
    protected List<?> userConverters() {
        // TODO Use JsonUtils directly
        return Arrays.asList(
            new DataEntryValueWrapperToStringConverter(),
            new EncryptedMapWrapperToStringConverter(encryption, objectMapper),
            new EncryptedStringToMapWrapperConverter(encryption, objectMapper),
            new ExecutionErrorToStringConverter(objectMapper),
            new FileEntryToStringConverter(objectMapper),
            new ListDoubleToPGObjectConverter(),
            new MapToPGObjectConverter(objectMapper),
            new MapWrapperToStringConverter(objectMapper),
            new JobResumeIdToStringConverter(),
            new PGObjectToListDoubleConverter(),
            new PGobjectToMapConverter(objectMapper),
            new StringToDataEntryValueWrapperConverter(),
            new StringToExecutionErrorConverter(objectMapper),
            new StringToFileEntryConverter(objectMapper),
            new StringToMapWrapperConverter(objectMapper),
            new StringToJobResumeIdConverter(),
            new StringToTriggerStateValueConverter(objectMapper),
            new StringToTaskStateValueConverter(objectMapper),
            new StringToWebhooksConverter(objectMapper),
            new StringToWorkflowExecutionIdConverter(),
            new StringToWorkflowTaskConverter(objectMapper),
            new StringToWorkflowTriggerConverter(objectMapper),
            new TriggerStateValueToStringConverter(objectMapper),
            new TaskStateValueToStringConverter(objectMapper),
            new WebhooksToStringConverter(objectMapper),
            new WorkflowExecutionIdToStringConverter(),
            new WorkflowTaskToStringConverter(objectMapper),
            new WorkflowTriggerToStringConverter(objectMapper));
    }
}
