/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.tenant.multi.pgvector.MultiTenantPgVectorLoader;
import com.bytechef.tenant.service.TenantService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifies the conditional wiring of {@link MultiTenantKnowledgeBasePgVectorLoaderConfiguration}. The loader hard-
 * depends on the {@code pgVectorJdbcTemplate} bean, which only exists when a PgVector store is configured; the
 * configuration must therefore stay dormant otherwise, instead of failing context startup with an unsatisfied
 * dependency (regression for the {@code liquibase} migration run, which configures no PgVector store).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class MultiTenantKnowledgeBasePgVectorLoaderConfigurationTest {

    private static final String LOADER_BEAN_NAME = "knowledgeBaseMultiTenantPgVectorLoader";
    private static final String PG_VECTOR_URL = "jdbc:postgresql://localhost:5432/postgres";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(MultiTenantKnowledgeBasePgVectorLoaderConfiguration.class)
        .withBean(TenantService.class, () -> mock(TenantService.class))
        .withPropertyValues("bytechef.edition=ee", "bytechef.tenant.mode=multi");

    @Test
    void testLoaderSkippedWhenPgVectorProviderNotConfigured() {
        // No pgVectorJdbcTemplate bean is registered: if the configuration were active it would fail to start.
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(LOADER_BEAN_NAME);
        });
    }

    @Test
    void testLoaderSkippedWhenPgVectorUrlMissing() {
        contextRunner
            .withPropertyValues("bytechef.ai.vectorstore.provider=pgvector")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(LOADER_BEAN_NAME);
            });
    }

    @Test
    void testLoaderSkippedWhenLiquibaseDisabled() {
        contextRunner
            .withBean("pgVectorJdbcTemplate", JdbcTemplate.class, () -> mock(JdbcTemplate.class))
            .withPropertyValues(
                "bytechef.ai.vectorstore.provider=pgvector", "bytechef.ai.vectorstore.pgvector.url=" + PG_VECTOR_URL,
                "spring.liquibase.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(LOADER_BEAN_NAME);
            });
    }

    @Test
    void testLoaderRegisteredWhenPgVectorConfigured() {
        TenantService tenantService = mock(TenantService.class);

        when(tenantService.getTenantIds()).thenReturn(List.of());

        new ApplicationContextRunner()
            .withUserConfiguration(MultiTenantKnowledgeBasePgVectorLoaderConfiguration.class)
            .withBean(TenantService.class, () -> tenantService)
            .withBean("pgVectorJdbcTemplate", JdbcTemplate.class, () -> mock(JdbcTemplate.class))
            .withPropertyValues(
                "bytechef.edition=ee", "bytechef.tenant.mode=multi",
                "bytechef.ai.vectorstore.provider=pgvector", "bytechef.ai.vectorstore.pgvector.url=" + PG_VECTOR_URL)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasBean(LOADER_BEAN_NAME);
                assertThat(context).getBean(LOADER_BEAN_NAME)
                    .isInstanceOf(MultiTenantPgVectorLoader.class);
            });
    }

    @Test
    void testLoaderSkippedWhenNotMultiTenant() {
        new ApplicationContextRunner()
            .withUserConfiguration(MultiTenantKnowledgeBasePgVectorLoaderConfiguration.class)
            .withBean(TenantService.class, () -> mock(TenantService.class))
            .withBean("pgVectorJdbcTemplate", JdbcTemplate.class, () -> mock(JdbcTemplate.class))
            .withPropertyValues(
                "bytechef.edition=ee", "bytechef.ai.vectorstore.provider=pgvector",
                "bytechef.ai.vectorstore.pgvector.url=" + PG_VECTOR_URL)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(LOADER_BEAN_NAME);
            });
    }

    @Test
    void testLoaderSkippedWhenNotEEVersion() {
        new ApplicationContextRunner()
            .withUserConfiguration(MultiTenantKnowledgeBasePgVectorLoaderConfiguration.class)
            .withBean(TenantService.class, () -> mock(TenantService.class))
            .withBean("pgVectorJdbcTemplate", JdbcTemplate.class, () -> mock(JdbcTemplate.class))
            .withPropertyValues(
                "bytechef.tenant.mode=multi", "bytechef.ai.vectorstore.provider=pgvector",
                "bytechef.ai.vectorstore.pgvector.url=" + PG_VECTOR_URL)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(LOADER_BEAN_NAME);
            });
    }
}
