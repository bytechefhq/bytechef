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

package com.bytechef.ee.ai.copilot.config;

import com.bytechef.config.ApplicationProperties;
import com.zaxxer.hikari.HikariDataSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.observation.ObservationRegistry;
import javax.sql.DataSource;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.core.JdbcAggregateTemplate;
import org.springframework.data.jdbc.core.convert.DataAccessStrategy;
import org.springframework.data.jdbc.core.convert.DefaultDataAccessStrategy;
import org.springframework.data.jdbc.core.convert.InsertStrategyFactory;
import org.springframework.data.jdbc.core.convert.JdbcConverter;
import org.springframework.data.jdbc.core.convert.QueryMappingConfiguration;
import org.springframework.data.jdbc.core.convert.SqlGeneratorSource;
import org.springframework.data.jdbc.core.convert.SqlParametersFactory;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJdbcRepositories(
    basePackages = "com.bytechef.ee.ai.copilot.repository",
    jdbcAggregateOperationsRef = "pgVectorJdbcAggregateTemplate",
    transactionManagerRef = "pgVectorTxManager")
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class PgVectorConfiguration {

    private final ApplicationProperties.Ai.Copilot.Vectorstore.PgVector pgVector;

    @SuppressFBWarnings("EI")
    public PgVectorConfiguration(ApplicationProperties applicationProperties) {
        this.pgVector = applicationProperties.getAi()
            .getCopilot()
            .getVectorstore()
            .getPgVector();
    }

    @Bean(name = "pgVectorDataSource")
    DataSource pgVectorDataSource() {
        return DataSourceBuilder.create()
            .type(HikariDataSource.class)
            .url(pgVector.getUrl())
            .username(pgVector.getUsername())
            .password(pgVector.getPassword())
            .build();
    }

    @Bean(name = "pgVectorJdbcTemplate")
    JdbcTemplate pgVectorJdbcTemplate(@Qualifier("pgVectorDataSource") DataSource pgVectorDataSource) {
        return new JdbcTemplate(pgVectorDataSource);
    }

    @Bean(name = "pgVectorNamedParameterJdbcTemplate")
    NamedParameterJdbcOperations pgVectorNamedParameterJdbcTemplate(
        @Qualifier("pgVectorDataSource") DataSource pgVectorDataSource) {

        return new NamedParameterJdbcTemplate(pgVectorDataSource);
    }

    @Bean(name = "pgVectorTxManager")
    PlatformTransactionManager pgVectorTransactionManager(
        @Qualifier("pgVectorDataSource") DataSource pgVectorDataSource) {

        return new JdbcTransactionManager(pgVectorDataSource);
    }

    @Bean(name = "pgVectorDataAccessStrategy")
    DataAccessStrategy pgVectorDataAccessStrategy(
        @Qualifier("pgVectorNamedParameterJdbcTemplate") NamedParameterJdbcOperations operations,
        JdbcConverter jdbcConverter, RelationalMappingContext context, Dialect jdbcDialect) {

        return new DefaultDataAccessStrategy(new SqlGeneratorSource(context, jdbcConverter, jdbcDialect), context,
            jdbcConverter, operations, new SqlParametersFactory(context, jdbcConverter),
            new InsertStrategyFactory(operations, jdbcDialect), QueryMappingConfiguration.EMPTY);
    }

    @Bean(name = "pgVectorJdbcAggregateTemplate")
    JdbcAggregateTemplate pgVectorJdbcAggregateTemplate(
        ApplicationContext applicationContext, RelationalMappingContext context, JdbcConverter jdbcConverter,
        @Qualifier("pgVectorDataAccessStrategy") DataAccessStrategy dataAccessStrategy) {

        return new JdbcAggregateTemplate(applicationContext, context, jdbcConverter, dataAccessStrategy);
    }

    @Bean
    public PgVectorStore pgVectorStore(
        @Qualifier("pgVectorJdbcTemplate") JdbcTemplate pgVectorJdbcTemplate,
        EmbeddingModel embeddingModel, PgVectorStoreProperties properties,
        ObjectProvider<ObservationRegistry> observationRegistry,
        ObjectProvider<VectorStoreObservationConvention> customObservationConvention,
        BatchingStrategy batchingStrategy) {

        var initializeSchema = properties.isInitializeSchema();

        return PgVectorStore.builder(pgVectorJdbcTemplate, embeddingModel)
            .schemaName(properties.getSchemaName())
            .idType(properties.getIdType())
            .vectorTableName(properties.getTableName())
            .vectorTableValidationsEnabled(properties.isSchemaValidation())
            .dimensions(properties.getDimensions())
            .distanceType(properties.getDistanceType())
            .removeExistingVectorStoreTable(properties.isRemoveExistingVectorStoreTable())
            .indexType(properties.getIndexType())
            .initializeSchema(initializeSchema)
            .observationRegistry(observationRegistry.getIfUnique(() -> ObservationRegistry.NOOP))
            .customObservationConvention(customObservationConvention.getIfAvailable(() -> null))
            .batchingStrategy(batchingStrategy)
            .maxDocumentBatchSize(properties.getMaxDocumentBatchSize())
            .build();
    }
}
