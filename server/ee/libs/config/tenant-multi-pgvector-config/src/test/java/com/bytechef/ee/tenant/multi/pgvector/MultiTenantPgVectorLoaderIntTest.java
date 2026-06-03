/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.pgvector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.tenant.multi.sql.MultiTenantPgVectorDataSource;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.autoconfigure.PgVectorStoreProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Testcontainers
class MultiTenantPgVectorLoaderIntTest {

    private static final String TABLE_NAME = "kb_vector_store";
    private static final String TENANT_ID = "000001";
    private static final String VECTOR_SCHEMA = "bytechef_vectorstore_" + TENANT_ID;

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16")
            .asCompatibleSubstituteFor("postgres"));

    private HikariDataSource hikariDataSource;
    private JdbcTemplate plainJdbcTemplate;
    private JdbcTemplate tenantJdbcTemplate;

    @BeforeEach
    void setUp() {
        hikariDataSource = new HikariDataSource();

        hikariDataSource.setJdbcUrl(postgreSQLContainer.getJdbcUrl());
        hikariDataSource.setUsername(postgreSQLContainer.getUsername());
        hikariDataSource.setPassword(postgreSQLContainer.getPassword());

        // Tenant-aware template: each borrowed connection runs SET search_path TO bytechef_vectorstore_000001,
        // which does not exist until the loader creates it. This is the production wiring that triggered 3F000.
        tenantJdbcTemplate = new JdbcTemplate(new MultiTenantPgVectorDataSource(hikariDataSource));

        // Plain template (default search_path) used to assert the resulting catalog state.
        plainJdbcTemplate = new JdbcTemplate(hikariDataSource);
    }

    @AfterEach
    void tearDown() {
        TenantContext.resetCurrentTenantId();

        if (hikariDataSource != null) {
            hikariDataSource.close();
        }
    }

    @Test
    void testAfterPropertiesSetInstallsExtensionInPublicWhenTenantSchemaMissing() {
        TenantService tenantService = mock(TenantService.class);

        when(tenantService.getTenantIds()).thenReturn(List.of(TENANT_ID));

        PgVectorStoreProperties properties = new PgVectorStoreProperties();

        properties.setDimensions(1536);
        properties.setIdType(PgVectorStore.PgIdType.UUID);
        properties.setIndexType(PgVectorStore.PgIndexType.HNSW);
        properties.setDistanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE);

        MultiTenantPgVectorLoader loader = new MultiTenantPgVectorLoader(
            tenantJdbcTemplate, properties, TABLE_NAME, tenantService);

        assertThatCode(loader::afterPropertiesSet).doesNotThrowAnyException();

        String extensionSchema = plainJdbcTemplate.queryForObject(
            "SELECT n.nspname FROM pg_extension e JOIN pg_namespace n ON e.extnamespace = n.oid "
                + "WHERE e.extname = 'vector'",
            String.class);

        assertThat(extensionSchema).isEqualTo("public");

        Integer schemaCount = plainJdbcTemplate.queryForObject(
            "SELECT count(*) FROM information_schema.schemata WHERE schema_name = ?", Integer.class, VECTOR_SCHEMA);

        assertThat(schemaCount).isEqualTo(1);

        Integer tableCount = plainJdbcTemplate.queryForObject(
            "SELECT count(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?", Integer.class,
            VECTOR_SCHEMA, TABLE_NAME);

        assertThat(tableCount).isEqualTo(1);
    }
}
