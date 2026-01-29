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

import com.bytechef.config.ApplicationProperties;
import com.zaxxer.hikari.HikariDataSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.sql.DataSource;
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
import org.springframework.data.relational.core.dialect.Dialect;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.TransactionManager;

/**
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.ai.vectorstore", name = "type", havingValue = "pgvector")
class PgVectorJdbcConfiguration {

    private final ApplicationProperties.Ai.Vectorstore.PgVector pgVector;

    @SuppressFBWarnings("EI")
    PgVectorJdbcConfiguration(ApplicationProperties applicationProperties) {
        this.pgVector = applicationProperties.getAi()
            .getVectorstore()
            .getPgVector();
    }

    @Bean
    @ConditionalOnProperty(prefix = "bytechef.ai.vectorstore.pgvector", name = "url")
    @ConditionalOnProperty(prefix = "bytechef.tenant", name = "mode", havingValue = "single", matchIfMissing = true)
    DataSource pgVectorDataSource() {
        return DataSourceBuilder.create()
            .type(HikariDataSource.class)
            .url(pgVector.getUrl())
            .username(pgVector.getUsername())
            .password(pgVector.getPassword())
            .build();
    }

    @Bean
    JdbcTemplate pgVectorJdbcTemplate(@Qualifier("pgVectorDataSource") DataSource pgVectorDataSource) {
        return new JdbcTemplate(pgVectorDataSource);
    }

    @Bean
    NamedParameterJdbcOperations pgVectorNamedParameterJdbcOperations(
        @Qualifier("pgVectorDataSource") DataSource pgVectorDataSource) {

        return new NamedParameterJdbcTemplate(pgVectorDataSource);
    }

    @Bean
    TransactionManager pgVectorTransactionManager(
        @Qualifier("pgVectorDataSource") DataSource pgVectorDataSource) {

        return new JdbcTransactionManager(pgVectorDataSource);
    }

    @Bean
    DataAccessStrategy pgVectorDataAccessStrategy(
        @Qualifier("pgVectorNamedParameterJdbcOperations") NamedParameterJdbcOperations operations,
        JdbcConverter jdbcConverter, RelationalMappingContext context, Dialect jdbcDialect) {

        return new DefaultDataAccessStrategy(
            new SqlGeneratorSource(context, jdbcConverter, jdbcDialect), context, jdbcConverter, operations,
            new SqlParametersFactory(context, jdbcConverter), new InsertStrategyFactory(operations, jdbcDialect),
            QueryMappingConfiguration.EMPTY);
    }

    @Bean
    JdbcAggregateTemplate pgVectorJdbcAggregateTemplate(
        ApplicationContext applicationContext, RelationalMappingContext context, JdbcConverter jdbcConverter,
        @Qualifier("pgVectorDataAccessStrategy") DataAccessStrategy dataAccessStrategy) {

        return new JdbcAggregateTemplate(applicationContext, context, jdbcConverter, dataAccessStrategy);
    }
}
