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

import com.bytechef.tenant.service.TenantService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * Verifies the conditional wiring of {@link MultiTenantContextStorePgVectorConfiguration}. Every bean here depends on
 * the {@code pgVectorJdbcTemplate} bean, which only exists when a PgVector store is configured; enabling the Context
 * Store without a configured PgVector store must keep the configuration dormant instead of failing context startup with
 * an unsatisfied dependency.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class MultiTenantContextStorePgVectorConfigurationTest {

    private static final String LOADER_BEAN_NAME = "contextStoreMultiTenantPgVectorLoader";
    private static final String METADATA_SERVICE_BEAN_NAME = "contextStoreVectorStoreMetadataService";
    private static final String PG_VECTOR_URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String VECTOR_STORE_BEAN_NAME = "contextStorePgVectorStore";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(MultiTenantContextStorePgVectorConfiguration.class)
        .withBean(TenantService.class, () -> mock(TenantService.class))
        .withPropertyValues("bytechef.edition=ee", "bytechef.tenant.mode=multi");

    @Test
    void testConfigurationSkippedWhenContextStoreDisabled() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(LOADER_BEAN_NAME);
        });
    }

    @Test
    void testConfigurationSkippedWhenPgVectorProviderNotConfigured() {
        // No pgVectorJdbcTemplate bean is registered: if the configuration were active it would fail to start.
        contextRunner
            .withPropertyValues("bytechef.context-store.enabled=true")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(LOADER_BEAN_NAME);
            });
    }

    @Test
    void testConfigurationSkippedWhenPgVectorUrlMissing() {
        contextRunner
            .withPropertyValues("bytechef.context-store.enabled=true", "bytechef.ai.vectorstore.provider=pgvector")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(LOADER_BEAN_NAME);
            });
    }

    @Test
    void testConfigurationRegisteredWhenPgVectorConfigured() {
        TenantService tenantService = mock(TenantService.class);

        when(tenantService.getTenantIds()).thenReturn(List.of());

        new ApplicationContextRunner()
            .withUserConfiguration(MultiTenantContextStorePgVectorConfiguration.class)
            .withBean(TenantService.class, () -> tenantService)
            .withBean("pgVectorJdbcTemplate", JdbcTemplate.class, () -> mock(JdbcTemplate.class))
            .withBean(EmbeddingModel.class, () -> mock(EmbeddingModel.class))
            .withBean(BatchingStrategy.class, () -> mock(BatchingStrategy.class))
            .withBean(ObjectMapper.class, () -> mock(ObjectMapper.class))
            .withPropertyValues(
                "bytechef.edition=ee", "bytechef.tenant.mode=multi", "bytechef.context-store.enabled=true",
                "bytechef.ai.vectorstore.provider=pgvector", "bytechef.ai.vectorstore.pgvector.url=" + PG_VECTOR_URL)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasBean(VECTOR_STORE_BEAN_NAME);
                assertThat(context).hasBean(METADATA_SERVICE_BEAN_NAME);
                assertThat(context).hasBean(LOADER_BEAN_NAME);
            });
    }

    @Test
    void testConfigurationSkippedWhenNotMultiTenant() {
        new ApplicationContextRunner()
            .withUserConfiguration(MultiTenantContextStorePgVectorConfiguration.class)
            .withBean(TenantService.class, () -> mock(TenantService.class))
            .withBean("pgVectorJdbcTemplate", JdbcTemplate.class, () -> mock(JdbcTemplate.class))
            .withPropertyValues(
                "bytechef.edition=ee", "bytechef.context-store.enabled=true",
                "bytechef.ai.vectorstore.provider=pgvector", "bytechef.ai.vectorstore.pgvector.url=" + PG_VECTOR_URL)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(LOADER_BEAN_NAME);
            });
    }

    @Test
    void testConfigurationSkippedWhenNotEEVersion() {
        new ApplicationContextRunner()
            .withUserConfiguration(MultiTenantContextStorePgVectorConfiguration.class)
            .withBean(TenantService.class, () -> mock(TenantService.class))
            .withBean("pgVectorJdbcTemplate", JdbcTemplate.class, () -> mock(JdbcTemplate.class))
            .withPropertyValues(
                "bytechef.tenant.mode=multi", "bytechef.context-store.enabled=true",
                "bytechef.ai.vectorstore.provider=pgvector", "bytechef.ai.vectorstore.pgvector.url=" + PG_VECTOR_URL)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(LOADER_BEAN_NAME);
            });
    }
}
