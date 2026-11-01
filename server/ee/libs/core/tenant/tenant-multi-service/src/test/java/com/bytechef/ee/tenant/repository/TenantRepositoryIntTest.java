/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.tenant.config.TenantMultiServiceIntTestConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = TenantMultiServiceIntTestConfiguration.class,
    properties = {
        "bytechef.edition=ee", "bytechef.tenant.mode=multi"
    })
@Import(PostgreSQLContainerConfiguration.class)
class TenantRepositoryIntTest {

    private static final String SENTINEL_SCHEMA = "bytechef_000001";

    private JdbcTemplate jdbcTemplate;
    private List<String> schemasBefore;
    private List<String> tablesBefore;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TenantRepository tenantRepository;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("CREATE SCHEMA " + SENTINEL_SCHEMA);
        jdbcTemplate.execute("CREATE TABLE " + SENTINEL_SCHEMA + ".users (id BIGINT)");
        jdbcTemplate.execute("CREATE TABLE public.users (id BIGINT)");

        schemasBefore = fetchSchemas();
        tablesBefore = fetchTables();
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    @AfterEach
    void afterEach() {
        for (String schema : fetchSchemas()) {
            if (schema.startsWith("bytechef_")) {
                jdbcTemplate.execute("DROP SCHEMA " + schema + " CASCADE");
            }
        }

        jdbcTemplate.execute("DROP TABLE IF EXISTS public.users");
    }

    @Test
    void testCreateTenantWithValidId() {
        tenantRepository.createTenant("tenant123");

        assertThat(fetchSchemas()).contains("bytechef_tenant123");
    }

    @Test
    void testCreateTenantWithAlphanumericId() {
        tenantRepository.createTenant("Tenant123ABC");

        assertThat(fetchSchemas()).contains("bytechef_tenant123abc");
    }

    @Test
    void testCreateTenantWithUnderscoreId() {
        tenantRepository.createTenant("tenant_123");

        assertThat(fetchSchemas()).contains("bytechef_tenant_123");
    }

    @Test
    void testCreateTenantWithHyphenId() {
        // The hyphen passes validation, but an unquoted schema identifier cannot contain one, so Postgres rejects the
        // statement and no schema is created.
        assertThatThrownBy(() -> tenantRepository.createTenant("tenant-123"))
            .isInstanceOf(RuntimeException.class)
            .isNotInstanceOf(IllegalArgumentException.class);

        assertSchemasAndTablesUnchanged();
    }

    @Test
    void testCreateTenantWithSqlInjection() {
        assertThatThrownBy(() -> tenantRepository.createTenant("tenant'; DROP SCHEMA bytechef_000001; --"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID");

        assertSchemasAndTablesUnchanged();
    }

    @Test
    void testCreateTenantWithSqlInjectionInDropSchema() {
        assertThatThrownBy(() -> tenantRepository.createTenant("000001; DROP TABLE users; --"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID");

        assertSchemasAndTablesUnchanged();
    }

    @Test
    void testCreateTenantWithSpaces() {
        assertThatThrownBy(() -> tenantRepository.createTenant("tenant 123"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID");

        assertSchemasAndTablesUnchanged();
    }

    @Test
    void testCreateTenantWithSpecialCharacters() {
        assertThatThrownBy(() -> tenantRepository.createTenant("tenant$123"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID");

        assertSchemasAndTablesUnchanged();
    }

    @Test
    void testCreateTenantWithQuotes() {
        assertThatThrownBy(() -> tenantRepository.createTenant("tenant'123"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID");

        assertSchemasAndTablesUnchanged();
    }

    @Test
    void testCreateTenantWithDoubleQuotes() {
        assertThatThrownBy(() -> tenantRepository.createTenant("tenant\"123"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID");

        assertSchemasAndTablesUnchanged();
    }

    @Test
    void testCreateTenantWithNull() {
        assertThatThrownBy(() -> tenantRepository.createTenant(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID");

        assertSchemasAndTablesUnchanged();
    }

    @Test
    void testCreateTenantWithEmpty() {
        assertThatThrownBy(() -> tenantRepository.createTenant(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID");

        assertSchemasAndTablesUnchanged();
    }

    @Test
    void testDeleteTenantWithValidId() {
        tenantRepository.createTenant("tenant123");

        tenantRepository.deleteTenant("tenant123");

        assertThat(fetchSchemas()).doesNotContain("bytechef_tenant123");
        assertSchemasAndTablesUnchanged();
    }

    @Test
    void testDeleteTenantWithValidIdOfMissingSchema() {
        assertThatThrownBy(() -> tenantRepository.deleteTenant("tenant123"))
            .isInstanceOf(RuntimeException.class)
            .isNotInstanceOf(IllegalArgumentException.class);

        assertSchemasAndTablesUnchanged();
    }

    @Test
    void testDeleteTenantWithSqlInjection() {
        assertThatThrownBy(() -> tenantRepository.deleteTenant("tenant'; DROP DATABASE bytechef; --"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID");

        assertSchemasAndTablesUnchanged();
    }

    @Test
    void testDeleteTenantWithSemicolon() {
        assertThatThrownBy(() -> tenantRepository.deleteTenant("000001;"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID");

        assertSchemasAndTablesUnchanged();
    }

    @Test
    void testDeleteTenantWithParentheses() {
        assertThatThrownBy(() -> tenantRepository.deleteTenant("tenant()"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID");

        assertSchemasAndTablesUnchanged();
    }

    @Test
    void testDeleteTenantWithNull() {
        assertThatThrownBy(() -> tenantRepository.deleteTenant(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid tenant ID");

        assertSchemasAndTablesUnchanged();
    }

    private void assertSchemasAndTablesUnchanged() {
        assertThat(fetchSchemas()).containsExactlyElementsOf(schemasBefore);
        assertThat(fetchTables()).containsExactlyElementsOf(tablesBefore);
        assertThat(tablesBefore).contains(SENTINEL_SCHEMA + ".users", "public.users");
    }

    private List<String> fetchSchemas() {
        return jdbcTemplate.queryForList(
            "SELECT schema_name FROM information_schema.schemata ORDER BY schema_name", String.class);
    }

    private List<String> fetchTables() {
        return jdbcTemplate.queryForList(
            "SELECT table_schema || '.' || table_name FROM information_schema.tables "
                + "WHERE table_schema NOT IN ('pg_catalog', 'information_schema') ORDER BY 1",
            String.class);
    }
}
