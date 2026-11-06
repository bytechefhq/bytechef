/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.external.invocation.log;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.external.invocation.log.config.ExternalInvocationLogIntTestConfiguration;
import com.bytechef.ee.platform.external.invocation.log.domain.ExternalInvocationLog;
import com.bytechef.ee.platform.external.invocation.log.job.ExternalInvocationLogRetentionJob;
import com.bytechef.ee.platform.external.invocation.log.repository.ExternalInvocationLogRepository;
import com.bytechef.ee.platform.external.invocation.log.service.ExternalInvocationLogService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.external.invocation.ExternalInvocationKind;
import com.bytechef.platform.external.invocation.ExternalInvocationOutcome;
import com.bytechef.platform.external.invocation.ExternalInvocationSurface;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.tenant.sql.BaseDataSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.sql.DataSource;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        ExternalInvocationLogIntTestConfiguration.class,
        ExternalInvocationLogRetentionJobIntTest.TenantSchemaDataSourceConfiguration.class
    },
    properties = {
        "bytechef.edition=ee", "bytechef.external-invocation-log.retention-days=30"
    })
class ExternalInvocationLogRetentionJobIntTest {

    private static final String BROKEN_TENANT_ID = "000003";
    private static final String BROKEN_TENANT_SCHEMA = "bytechef_" + BROKEN_TENANT_ID;
    private static final String FIRST_TENANT_ID = "000001";
    private static final String FIRST_TENANT_SCHEMA = "bytechef_" + FIRST_TENANT_ID;
    private static final String SECOND_TENANT_ID = "000002";
    private static final String SECOND_TENANT_SCHEMA = "bytechef_" + SECOND_TENANT_ID;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private TenantService tenantService;

    @Autowired
    private ExternalInvocationLogRepository externalInvocationLogRepository;

    @Autowired
    private ExternalInvocationLogRetentionJob externalInvocationLogRetentionJob;

    @Autowired
    private ExternalInvocationLogService externalInvocationLogService;

    @AfterEach
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    void afterEach() {
        TenantContext.resetCurrentTenantId();

        externalInvocationLogRepository.deleteAll();

        for (String schema : List.of(FIRST_TENANT_SCHEMA, SECOND_TENANT_SCHEMA, BROKEN_TENANT_SCHEMA)) {
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        }
    }

    @Test
    void testPurgeDeletesOnlyRowsOlderThanTheRetentionWindow() {
        Instant now = Instant.now();

        create("expired", now.minus(31, ChronoUnit.DAYS));
        create("retained", now.minus(29, ChronoUnit.DAYS));

        externalInvocationLogRetentionJob.purgeExpiredLogs();

        List<ExternalInvocationLog> externalInvocationLogs = externalInvocationLogRepository.findAll();

        assertThat(externalInvocationLogs)
            .extracting(ExternalInvocationLog::getToolName)
            .containsExactly("retained");
    }

    @Test
    void testPurgeDeletesExpiredRowsInEveryTenantSchema() {
        Instant now = Instant.now();

        for (String schema : List.of(FIRST_TENANT_SCHEMA, SECOND_TENANT_SCHEMA)) {
            createTenantSchema(schema);

            insert(schema, "expired", now.minus(31, ChronoUnit.DAYS));
            insert(schema, "retained", now.minus(29, ChronoUnit.DAYS));
        }

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIds()).thenReturn(List.of(FIRST_TENANT_ID, SECOND_TENANT_ID));

        externalInvocationLogRetentionJob.purgeExpiredLogs();

        assertThat(findToolNames(FIRST_TENANT_SCHEMA)).containsExactly("retained");
        assertThat(findToolNames(SECOND_TENANT_SCHEMA)).containsExactly("retained");
    }

    @Test
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    void testPurgeContinuesWithTheNextTenantWhenOneTenantFails() {
        Instant now = Instant.now();

        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + BROKEN_TENANT_SCHEMA);
        jdbcTemplate.execute("CREATE TABLE " + BROKEN_TENANT_SCHEMA + ".external_invocation_log (id BIGINT)");

        createTenantSchema(FIRST_TENANT_SCHEMA);

        insert(FIRST_TENANT_SCHEMA, "expired", now.minus(31, ChronoUnit.DAYS));

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIds()).thenReturn(List.of(BROKEN_TENANT_ID, FIRST_TENANT_ID));

        externalInvocationLogRetentionJob.purgeExpiredLogs();

        assertThat(findToolNames(FIRST_TENANT_SCHEMA)).isEmpty();
    }

    @Test
    void testPurgeRunsOnOneCoordinatorAtATime() throws NoSuchMethodException {
        SchedulerLock schedulerLock = ExternalInvocationLogRetentionJob.class
            .getMethod("purgeExpiredLogs")
            .getAnnotation(SchedulerLock.class);

        assertThat(schedulerLock.name()).isEqualTo("purgeExpiredExternalInvocationLogs");
        assertThat(schedulerLock.lockAtLeastFor()).isEqualTo("PT5M");
        assertThat(schedulerLock.lockAtMostFor()).isEqualTo("PT1H");
    }

    private void create(String toolName, Instant createdDate) {
        ExternalInvocationLog externalInvocationLog = new ExternalInvocationLog();

        externalInvocationLog.setType(PlatformType.AUTOMATION);
        externalInvocationLog.setSurface(ExternalInvocationSurface.MCP_AUTOMATION);
        externalInvocationLog.setKind(ExternalInvocationKind.COMPONENT);
        externalInvocationLog.setToolName(toolName);
        externalInvocationLog.setOutcome(ExternalInvocationOutcome.SUCCESS);
        externalInvocationLog.setDurationMs(5);
        externalInvocationLog.setCreatedDate(createdDate);

        externalInvocationLogService.create(externalInvocationLog);
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void createTenantSchema(String schema) {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
        jdbcTemplate.execute(
            "CREATE TABLE " + schema + ".external_invocation_log (LIKE public.external_invocation_log INCLUDING ALL)");
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private List<String> findToolNames(String schema) {
        return jdbcTemplate.queryForList(
            "SELECT tool_name FROM " + schema + ".external_invocation_log ORDER BY tool_name", String.class);
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void insert(String schema, String toolName, Instant createdDate) {
        jdbcTemplate.update(
            "INSERT INTO " + schema + ".external_invocation_log " +
                "(type, surface, kind, tool_name, outcome, duration_ms, created_date) VALUES (?, ?, ?, ?, ?, ?, ?)",
            PlatformType.AUTOMATION.ordinal(), ExternalInvocationSurface.MCP_AUTOMATION.ordinal(),
            ExternalInvocationKind.COMPONENT.ordinal(), toolName, ExternalInvocationOutcome.SUCCESS.ordinal(), 5,
            Timestamp.from(createdDate));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TenantSchemaDataSourceConfiguration {

        @Bean
        static BeanPostProcessor tenantSchemaDataSourceBeanPostProcessor() {
            return new BeanPostProcessor() {

                @Override
                public Object postProcessAfterInitialization(Object bean, String beanName) {
                    if (bean instanceof DataSource dataSource && !(bean instanceof TenantSchemaDataSource)) {
                        return new TenantSchemaDataSource(dataSource);
                    }

                    return bean;
                }
            };
        }
    }

    private static final class TenantSchemaDataSource extends BaseDataSource {

        private TenantSchemaDataSource(DataSource dataSource) {
            super(dataSource);
        }
    }
}
