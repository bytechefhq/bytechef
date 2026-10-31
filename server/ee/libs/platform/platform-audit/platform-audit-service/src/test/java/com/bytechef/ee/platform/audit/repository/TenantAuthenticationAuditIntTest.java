/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.tenant.sql.BaseDataSource;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.audit.listener.AuditListener;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = {
    AuditIntTestConfiguration.class, TenantAuthenticationAuditIntTest.TenantSchemaDataSourceConfiguration.class
})
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
public class TenantAuthenticationAuditIntTest {

    private static final String TENANT_ID = "000001";
    private static final String TENANT_SCHEMA = "bytechef_" + TENANT_ID;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TenantService tenantService;

    @BeforeEach
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public void beforeEach() {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + TENANT_SCHEMA);
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS " + TENANT_SCHEMA + ".persistent_audit_event " +
                "(LIKE public.persistent_audit_event INCLUDING ALL)");
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS " + TENANT_SCHEMA + ".persistent_audit_event_data " +
                "(LIKE public.persistent_audit_event_data INCLUDING ALL)");

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIdsByUserEmail("test1@gmail.com")).thenReturn(List.of(TENANT_ID));
        when(tenantService.getTenantIdsByUserEmail("unknown@gmail.com")).thenReturn(List.of());
    }

    @AfterEach
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public void afterEach() {
        reset(tenantService);

        TenantContext.resetCurrentTenantId();

        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + TENANT_SCHEMA + " CASCADE");
        jdbcTemplate.update("DELETE FROM public.persistent_audit_event_data");
        jdbcTemplate.update("DELETE FROM public.persistent_audit_event");
    }

    @Test
    public void testAuthenticationSuccessIsPersistedInTheTenantSchema() {
        applicationEventPublisher.publishEvent(
            new AuthenticationSuccessEvent(
                UsernamePasswordAuthenticationToken.authenticated("test1@gmail.com", null, List.of())));

        assertThat(countEvents(TENANT_SCHEMA, "test1@gmail.com", "AUTHENTICATION_SUCCESS")).isEqualTo(1);
        assertThat(countEvents("public", "test1@gmail.com", "AUTHENTICATION_SUCCESS")).isZero();
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    public void testAuthenticationFailureForUnknownUserIsPersistedInTheDefaultSchema() {
        applicationEventPublisher.publishEvent(
            new AuthenticationFailureBadCredentialsEvent(
                UsernamePasswordAuthenticationToken.unauthenticated("unknown@gmail.com", "secret"),
                new BadCredentialsException("Bad credentials")));

        assertThat(countEvents("public", "unknown@gmail.com", "AUTHENTICATION_FAILURE")).isEqualTo(1);
        assertThat(countEvents(TENANT_SCHEMA, "unknown@gmail.com", "AUTHENTICATION_FAILURE")).isZero();
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private Integer countEvents(String schema, String principal, String eventType) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + schema + ".persistent_audit_event WHERE principal = ? AND event_type = ?",
            Integer.class, principal, eventType);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TenantSchemaDataSourceConfiguration {

        @Bean
        AuditListener auditListener(AuditEventRepository auditEventRepository) {
            return new AuditListener(auditEventRepository);
        }

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
