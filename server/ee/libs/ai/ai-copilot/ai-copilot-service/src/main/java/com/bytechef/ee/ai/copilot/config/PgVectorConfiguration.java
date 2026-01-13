package com.bytechef.ee.ai.copilot.config;

import com.bytechef.config.ApplicationProperties;
import com.zaxxer.hikari.HikariDataSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@EnableJdbcRepositories(
    basePackages = "com.bytechef.ee.ai.copilot.repository",
    jdbcOperationsRef = "pgVectorNamedParameterJdbcTemplate",
    dataAccessStrategyRef = "pgVectorDataAccessStrategy",
    transactionManagerRef = "pgVectorTxManager"
)
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class PgVectorConfiguration {
    private final ApplicationProperties.Ai.Copilot.Vectorstore.PgVector pgVector;
    private final ApplicationProperties.Datasource datasource;

    @SuppressFBWarnings("EI")
    public PgVectorConfiguration(ApplicationProperties applicationProperties){
        this.datasource = applicationProperties.getDatasource();
        this.pgVector = applicationProperties.getAi()
            .getCopilot()
            .getVectorstore()
            .getPgVector();
    }

    @Bean
    @Primary
    DataSource dataSource(DataSourceProperties properties) {
        return DataSourceBuilder.create(properties.getClassLoader())
            .type(HikariDataSource.class)
            .url(datasource.getUrl())
            .username(datasource.getUsername())
            .password(datasource.getPassword())
            .build();
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

    @Bean
    @Primary
    JdbcClient jdbcClient(DataSource dataSource) {
        return JdbcClient.create(dataSource);
    }

    @Bean
    @Primary
    JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
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

    @Bean
    @Primary
    NamedParameterJdbcOperations jdbcOperations(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    @Primary
    PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }

    @Bean
    @Primary
    DataAccessStrategy dataAccessStrategy(
        NamedParameterJdbcOperations ops,
        JdbcConverter jdbcConverter,
        RelationalMappingContext context,
        Dialect jdbcDialect) {

        return new DefaultDataAccessStrategy(new SqlGeneratorSource(context, jdbcConverter, jdbcDialect), context, jdbcConverter, ops,
            new SqlParametersFactory(context, jdbcConverter), new InsertStrategyFactory(ops, jdbcDialect), QueryMappingConfiguration.EMPTY);
    }

    @Bean(name = "pgVectorDataAccessStrategy")
    DataAccessStrategy pgVectorDataAccessStrategy(
        @Qualifier("pgVectorNamedParameterJdbcTemplate") NamedParameterJdbcOperations ops,
        JdbcConverter jdbcConverter,
        RelationalMappingContext context,
        Dialect jdbcDialect) {

        return new DefaultDataAccessStrategy(new SqlGeneratorSource(context, jdbcConverter, jdbcDialect), context, jdbcConverter, ops,
            new SqlParametersFactory(context, jdbcConverter), new InsertStrategyFactory(ops, jdbcDialect), QueryMappingConfiguration.EMPTY);
    }

    @Bean
    public PgVectorStore vectorStore(
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
