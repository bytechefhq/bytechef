/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.ee.tenant.service.MultiTenantService;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
@SpringBootTest(classes = TenantRepositoryDeletionIntTest.IntTestConfiguration.class)
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
class TenantRepositoryDeletionIntTest {

    private static final String PENDING_EMAIL = "pending@x.com";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private TenantDeletionRepository tenantDeletionRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    void beforeEach() {
        jdbcTemplate = new JdbcTemplate(dataSource);

        for (String tenantId : new String[] {
            "000001", "000002"
        }) {
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS bytechef_" + tenantId + " CASCADE");
            jdbcTemplate.execute("CREATE SCHEMA bytechef_" + tenantId);
            jdbcTemplate.execute(
                "CREATE TABLE bytechef_" + tenantId + ".\"user\" (id BIGINT, email VARCHAR(255), login VARCHAR(255))");
        }

        jdbcTemplate.update("INSERT INTO bytechef_000001.\"user\" VALUES (1, 'live@x.com', 'live')");
        jdbcTemplate.update("INSERT INTO bytechef_000002.\"user\" VALUES (2, ?, 'pending')", PENDING_EMAIL);

        tenantDeletionRepository.markPendingDeletion("000002", "admin@x", Instant.now());
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS bytechef_000001 CASCADE");
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS bytechef_000002 CASCADE");
        jdbcTemplate.update("DELETE FROM public.tenant_deletion");
    }

    @Test
    void testEmailExistsIncludesPendingTenant() {
        MultiTenantService multiTenantService = new MultiTenantService(
            dataSource, mock(ApplicationEventPublisher.class), new LiquibaseProperties(), tenantRepository);

        assertThat(multiTenantService.tenantIdsByUserEmailExist(PENDING_EMAIL)).isTrue();
        assertThat(multiTenantService.tenantIdsByUserLoginExist("pending")).isTrue();
        assertThat(multiTenantService.getTenantIdsByUserEmail(PENDING_EMAIL)).isEmpty();
    }

    @Test
    void testFindMaxTenantIdCountsTombstones() {
        jdbcTemplate.execute("DROP SCHEMA bytechef_000002 CASCADE");

        tenantDeletionRepository.markPurged("000002");

        assertThat(tenantRepository.findMaxTenantId()).isEqualTo("000002");
    }

    @Test
    void testSchemaExists() {
        assertThat(tenantRepository.schemaExists("000001")).isTrue();
        assertThat(tenantRepository.schemaExists("000999")).isFalse();
    }

    @Test
    void testFindTenantIdsByUserEmailSkipsPendingTenant() {
        assertThat(tenantRepository.findTenantIdsByUserEmail(PENDING_EMAIL)).isEmpty();
    }

    @Test
    void testFindTenantIdsExcludesPendingTenant() {
        assertThat(tenantRepository.findTenantIds()).containsExactly("000001");
        assertThat(tenantRepository.findTenantIdsIncludingPendingDeletion()).containsExactlyInAnyOrder(
            "000001", "000002");
    }

    @Configuration
    @EnableAutoConfiguration
    @Import({
        LiquibaseConfiguration.class, TenantDeletionRepository.class, TenantRepository.class
    })
    static class IntTestConfiguration {
    }
}
