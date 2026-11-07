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

package com.bytechef.configuration.config;

import com.bytechef.atlas.configuration.converter.StringToWorkflowTaskConverter;
import com.bytechef.atlas.configuration.converter.WorkflowTaskToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedMapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.EncryptedStringToMapWrapperConverter;
import com.bytechef.commons.data.jdbc.converter.FileEntryToStringConverter;
import com.bytechef.commons.data.jdbc.converter.MapWrapperToStringConverter;
import com.bytechef.commons.data.jdbc.converter.StringToFileEntryConverter;
import com.bytechef.commons.data.jdbc.converter.StringToMapWrapperConverter;
import com.bytechef.encryption.Encryption;
import com.bytechef.tenant.annotation.ConditionalOnSingleTenant;
import com.zaxxer.hikari.HikariDataSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
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

    private final Encryption encryption;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI2")
    public JdbcConfiguration(Encryption encryption, ObjectMapper objectMapper) {
        this.encryption = encryption;
        this.objectMapper = objectMapper;
    }

    @Bean
    @Primary
    DataAccessStrategy dataAccessStrategy(
        NamedParameterJdbcOperations operations, JdbcConverter jdbcConverter, RelationalMappingContext context,
        Dialect jdbcDialect) {

        return new DefaultDataAccessStrategy(
            new SqlGeneratorSource(context, jdbcConverter, jdbcDialect), context, jdbcConverter, operations,
            new SqlParametersFactory(context, jdbcConverter), new InsertStrategyFactory(operations, jdbcDialect),
            QueryMappingConfiguration.EMPTY);
    }

    @Bean
    @Primary
    @ConditionalOnSingleTenant
    @ConfigurationProperties("spring.datasource.hikari")
    HikariDataSource dataSource(
        DataSourceProperties dataSourceProperties,
        ObjectProvider<JdbcConnectionDetails> jdbcConnectionDetailsProvider) {

        JdbcConnectionDetails jdbcConnectionDetails = jdbcConnectionDetailsProvider.getIfAvailable();

        if (jdbcConnectionDetails == null) {
            return dataSourceProperties.initializeDataSourceBuilder()
                .type(HikariDataSource.class)
                .build();
        }

        return DataSourceBuilder.create(dataSourceProperties.getClassLoader())
            .type(HikariDataSource.class)
            .driverClassName(jdbcConnectionDetails.getDriverClassName())
            .url(jdbcConnectionDetails.getJdbcUrl())
            .username(jdbcConnectionDetails.getUsername())
            .password(jdbcConnectionDetails.getPassword())
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
        return Arrays.asList(
            new EncryptedMapWrapperToStringConverter(encryption, objectMapper),
            new EncryptedStringToMapWrapperConverter(encryption, objectMapper),
            new FileEntryToStringConverter(objectMapper),
            new MapWrapperToStringConverter(objectMapper),
            new StringToFileEntryConverter(objectMapper),
            new StringToMapWrapperConverter(objectMapper),
            new StringToWorkflowTaskConverter(objectMapper),
            new WorkflowTaskToStringConverter(objectMapper));
    }
}
