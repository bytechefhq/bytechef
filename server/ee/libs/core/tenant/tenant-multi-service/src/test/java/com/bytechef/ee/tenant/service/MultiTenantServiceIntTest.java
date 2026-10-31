/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.tenant.config.TenantMultiServiceIntTestConfiguration;
import com.bytechef.ee.tenant.repository.TenantRepository;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.Tenancy;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
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
class MultiTenantServiceIntTest {

    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private MultiTenantService multiTenantService;

    @Autowired
    private TenantRepository tenantRepository;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate = new JdbcTemplate(dataSource);

        tenantRepository.createTenant("000001");
        tenantRepository.createTenant("000002");
    }

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();

        tenantRepository.deleteTenant("000001");
        tenantRepository.deleteTenant("000002");
    }

    @Test
    void testLoadChangelogRunsEachTenantWithinItsTenantContext() {
        multiTenantService.loadChangelog(List.of("000001", "000002"), Tenancy.MULTITENANT);

        // The tenant id observed while each tenant's changelog runs must match the tenant being upgraded, not the
        // default "public" value (see GitHub issue bytechef-cloud#119).

        assertThat(fetchProbeTenantIds("bytechef_000001")).containsExactly("000001");
        assertThat(fetchProbeTenantIds("bytechef_000002")).containsExactly("000002");
        assertThat(countAppliedChangeSets("bytechef_000001")).isEqualTo(2);
        assertThat(countAppliedChangeSets("bytechef_000002")).isEqualTo(2);
    }

    @Test
    void testLoadChangelogRestoresPreviousTenantId() {
        TenantContext.setCurrentTenantId("000099");

        multiTenantService.loadChangelog(List.of("000001"), Tenancy.MULTITENANT);

        // The tenant id active before the upgrade must be restored afterward, not reset to the default "public".

        assertThat(TenantContext.getCurrentTenantId()).isEqualTo("000099");
        assertThat(fetchProbeTenantIds("bytechef_000001")).containsExactly("000001");
    }

    private int countAppliedChangeSets(String schema) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + schema + ".databasechangelog WHERE id LIKE '20261031000000%'", Integer.class);

        return count == null ? 0 : count;
    }

    private List<String> fetchProbeTenantIds(String schema) {
        return jdbcTemplate.queryForList("SELECT tenant_id FROM " + schema + ".tenant_changelog_probe", String.class);
    }
}
