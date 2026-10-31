/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.audit.repository.PersistenceAuditEventRepository;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.tenant.sql.BaseDataSource;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.Search;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = {
    AuditIntTestConfiguration.class, AuditEventRetentionJobIntTest.TenantSchemaDataSourceConfiguration.class
})
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
class AuditEventRetentionJobIntTest {

    private static final String DEFAULT_SCHEMA = "public";
    private static final String FAILING_TENANT_ID = "000002";
    private static final String FAILING_TENANT_SCHEMA = "bytechef_" + FAILING_TENANT_ID;
    private static final String TENANT_ID = "000001";
    private static final String TENANT_SCHEMA = "bytechef_" + TENANT_ID;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private TenantService tenantService;

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    @BeforeEach
    void beforeEach() {
        createTenantSchema(TENANT_SCHEMA);
        createBrokenTenantSchema(FAILING_TENANT_SCHEMA);
    }

    @AfterEach
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    void afterEach() {
        reset(tenantService);

        TenantContext.resetCurrentTenantId();

        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + TENANT_SCHEMA + " CASCADE");
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + FAILING_TENANT_SCHEMA + " CASCADE");
        jdbcTemplate.update("DELETE FROM public.persistent_audit_event_data");
        jdbcTemplate.update("DELETE FROM public.persistent_audit_event");
    }

    @Test
    void testPurgeDeletesOnlyEventsOlderThanTheRetentionPeriod() {
        LocalDateTime now = LocalDateTime.now();

        insertEvent(DEFAULT_SCHEMA, "expired", now.minusDays(31));
        insertEvent(DEFAULT_SCHEMA, "kept", now.minusDays(29));

        AuditEventRetentionJob auditEventRetentionJob = createJob(30L, "0 0 2 * * *");

        auditEventRetentionJob.purgeExpiredEvents();

        assertThat(findPrincipals(DEFAULT_SCHEMA)).containsExactly("kept");
        assertThat(counterValue(AuditEventRetentionJob.PURGED_METRIC)).isEqualTo(1.0);
    }

    @Test
    void testPurgeIsDisabledWhenRetentionDaysIsNotPositive() {
        LocalDateTime now = LocalDateTime.now();

        insertEvent(DEFAULT_SCHEMA, "expired", now.minusYears(5));

        AuditEventRetentionJob zeroDaysAuditEventRetentionJob = createJob(0L, "0 0 2 * * *");
        AuditEventRetentionJob negativeDaysAuditEventRetentionJob = createJob(-1L, "0 0 2 * * *");

        zeroDaysAuditEventRetentionJob.purgeExpiredEvents();
        negativeDaysAuditEventRetentionJob.purgeExpiredEvents();

        assertThat(findPrincipals(DEFAULT_SCHEMA)).containsExactly("expired");
    }

    @Test
    void testMultiTenantPurgeCoversTheDefaultSchemaAndEveryTenantDespiteAFailingTenant() {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime expiredDate = now.minusDays(31);

        insertEvent(DEFAULT_SCHEMA, "default-expired", expiredDate);
        insertEvent(TENANT_SCHEMA, "tenant-expired", expiredDate);
        insertEvent(FAILING_TENANT_SCHEMA, "failing-tenant-expired", expiredDate);

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIds()).thenReturn(List.of(FAILING_TENANT_ID, TENANT_ID));

        AuditEventRetentionJob auditEventRetentionJob = createJob(30L, "0 0 2 * * *");

        auditEventRetentionJob.purgeExpiredEvents();

        assertThat(findPrincipals(DEFAULT_SCHEMA)).isEmpty();
        assertThat(findPrincipals(TENANT_SCHEMA)).isEmpty();
        assertThat(findPrincipals(FAILING_TENANT_SCHEMA)).containsExactly("failing-tenant-expired");
        assertThat(counterValue(AuditEventRetentionJob.PURGED_METRIC)).isEqualTo(2.0);
        assertThat(counterValue(AuditEventRetentionJob.FAILURE_METRIC)).isEqualTo(1.0);
    }

    @Test
    void testSingleTenantPurgeFailureIsCountedAndDoesNotPropagate() {
        doAnswer(invocation -> {
            throw new IllegalStateException("database unavailable");
        }).when(persistenceAuditEventRepository)
            .deleteByEventDateBefore(any());

        AuditEventRetentionJob auditEventRetentionJob = createJob(30L, "0 0 2 * * *");

        assertThatCode(auditEventRetentionJob::purgeExpiredEvents).doesNotThrowAnyException();
        assertThat(counterValue(AuditEventRetentionJob.FAILURE_METRIC)).isEqualTo(1.0);
    }

    @Test
    void testConfigureTasksRegistersTheConfiguredCron() {
        ScheduledTaskRegistrar scheduledTaskRegistrar = new ScheduledTaskRegistrar();

        AuditEventRetentionJob auditEventRetentionJob = createJob(30L, "0 30 3 * * *");

        auditEventRetentionJob.configureTasks(scheduledTaskRegistrar);

        List<CronTask> cronTasks = scheduledTaskRegistrar.getCronTaskList();

        assertThat(cronTasks).singleElement()
            .extracting(CronTask::getExpression)
            .isEqualTo("0 30 3 * * *");
    }

    private AuditEventRetentionJob createJob(long retentionDays, String retentionCron) {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Audit audit = applicationProperties.getAudit();

        audit.setRetentionCron(retentionCron);
        audit.setRetentionDays(retentionDays);

        @SuppressWarnings("unchecked")
        ObjectProvider<MeterRegistry> meterRegistryProvider = mock(ObjectProvider.class);

        when(meterRegistryProvider.getIfAvailable()).thenReturn(meterRegistry);

        return new AuditEventRetentionJob(
            applicationProperties, meterRegistryProvider, persistenceAuditEventRepository, tenantService);
    }

    private double counterValue(String metricName) {
        Search search = meterRegistry.find(metricName);

        Counter counter = search.counter();

        assertThat(counter).isNotNull();

        return counter.count();
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void createTenantSchema(String schema) {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS " + schema + ".persistent_audit_event " +
                "(LIKE public.persistent_audit_event INCLUDING ALL)");
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS " + schema + ".persistent_audit_event_data " +
                "(LIKE public.persistent_audit_event_data INCLUDING ALL)");
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void createBrokenTenantSchema(String schema) {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS " + schema + ".persistent_audit_event " +
                "(LIKE public.persistent_audit_event INCLUDING ALL)");
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS " + schema + ".persistent_audit_event_data (id BIGINT)");
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private List<String> findPrincipals(String schema) {
        return jdbcTemplate.queryForList(
            "SELECT principal FROM " + schema + ".persistent_audit_event ORDER BY principal", String.class);
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void insertEvent(String schema, String principal, LocalDateTime eventDate) {
        jdbcTemplate.update(
            "INSERT INTO " + schema + ".persistent_audit_event (principal, event_date, event_type) VALUES (?, ?, ?)",
            principal, Timestamp.valueOf(eventDate), "PERMISSION_CHECK");
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
