/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.audit.AuditEventConverter;
import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.platform.security.web.authentication.TenantWebAuthenticationDetails;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.tenant.sql.BaseDataSource;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.boot.actuate.audit.listener.AuditListener;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.RememberMeAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = {
    AuditIntTestConfiguration.class, CustomAuditEventRepositoryIntTest.TenantSchemaDataSourceConfiguration.class
})
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
public class CustomAuditEventRepositoryIntTest {

    private static final String DEFAULT_SCHEMA = "public";
    private static final String OTHER_TENANT_ID = "000002";
    private static final String OTHER_TENANT_SCHEMA = "bytechef_" + OTHER_TENANT_ID;
    private static final String TENANT_ID = "000001";
    private static final String TENANT_SCHEMA = "bytechef_" + TENANT_ID;
    private static final String UNKNOWN_TENANT_ID = "000009";

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private AuditEventConverter auditEventConverter;

    @Autowired
    private CustomAuditEventRepository customAuditEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PersistenceAuditEventRepository persistenceAuditEventRepository;

    @Autowired
    private ObjectProvider<PlatformTransactionManager> transactionManagerProvider;

    @Autowired
    private TenantService tenantService;

    @BeforeEach
    public void beforeEach() {
        createTenantSchema(TENANT_SCHEMA);
        createTenantSchema(OTHER_TENANT_SCHEMA);

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIds()).thenReturn(List.of(TENANT_ID, OTHER_TENANT_ID));
        when(tenantService.getTenantIdsByUserEmail("test1@gmail.com")).thenReturn(List.of(TENANT_ID, OTHER_TENANT_ID));
        when(tenantService.getTenantIdsByUserEmail("unknown@gmail.com")).thenReturn(List.of());
        when(tenantService.getTenantIdsByUserLogin("admin")).thenReturn(List.of(TENANT_ID));
        when(tenantService.getTenantIdsByUserLogin("[credential]")).thenReturn(List.of());
    }

    @AfterEach
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public void afterEach() {
        reset(tenantService);

        TenantContext.resetCurrentTenantId();

        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + TENANT_SCHEMA + " CASCADE");
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + OTHER_TENANT_SCHEMA + " CASCADE");
        jdbcTemplate.update("DELETE FROM public.persistent_audit_event_data");
        jdbcTemplate.update("DELETE FROM public.persistent_audit_event");
    }

    @Test
    public void testAuthenticationSuccessEventIsPersistedInTheUsersTenantSchema() {
        applicationEventPublisher.publishEvent(
            new AuthenticationSuccessEvent(
                UsernamePasswordAuthenticationToken.authenticated("test1@gmail.com", null, List.of())));

        assertThat(countEvents(TENANT_SCHEMA, "test1@gmail.com", "AUTHENTICATION_SUCCESS")).isEqualTo(1);
        assertThat(countEvents(DEFAULT_SCHEMA, "test1@gmail.com", "AUTHENTICATION_SUCCESS")).isZero();
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
    }

    @Test
    public void testDetailsTenantIsPreferredOverThePrincipalsFirstTenant() {
        UsernamePasswordAuthenticationToken authentication =
            UsernamePasswordAuthenticationToken.authenticated("test1@gmail.com", null, List.of());

        authentication.setDetails(createDetails(OTHER_TENANT_ID));

        applicationEventPublisher.publishEvent(new AuthenticationSuccessEvent(authentication));

        assertThat(countEvents(OTHER_TENANT_SCHEMA, "test1@gmail.com", "AUTHENTICATION_SUCCESS")).isEqualTo(1);
        assertThat(countEvents(TENANT_SCHEMA, "test1@gmail.com", "AUTHENTICATION_SUCCESS")).isZero();
        assertThat(countEvents(DEFAULT_SCHEMA, "test1@gmail.com", "AUTHENTICATION_SUCCESS")).isZero();
        verify(tenantService, never()).getTenantIdsByUserEmail(anyString());
    }

    @Test
    public void testMcpJwtAuthenticationSuccessIsPersistedInTheRequestTenantSchema() {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "RS256")
            .subject("test1@gmail.com")
            .build();

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, List.of());

        authentication.setDetails(createDetails(OTHER_TENANT_ID));

        applicationEventPublisher.publishEvent(new AuthenticationSuccessEvent(authentication));

        assertThat(countEvents(OTHER_TENANT_SCHEMA, "test1@gmail.com", "AUTHENTICATION_SUCCESS")).isEqualTo(1);
        assertThat(countEvents(TENANT_SCHEMA, "test1@gmail.com", "AUTHENTICATION_SUCCESS")).isZero();
        assertThat(countEvents(DEFAULT_SCHEMA, "test1@gmail.com", "AUTHENTICATION_SUCCESS")).isZero();
    }

    @Test
    public void testRedactedMcpBearerTokenFailureIsPersistedInTheRequestTenantSchema() {
        BearerTokenAuthenticationToken authentication = new BearerTokenAuthenticationToken("secret-token");

        authentication.setDetails(createDetails(OTHER_TENANT_ID));

        applicationEventPublisher.publishEvent(
            new AuthenticationFailureBadCredentialsEvent(
                authentication, new InvalidBearerTokenException("Invalid token")));

        assertThat(countEvents(OTHER_TENANT_SCHEMA, "[credential]", "AUTHENTICATION_FAILURE")).isEqualTo(1);
        assertThat(countEvents(DEFAULT_SCHEMA, "[credential]", "AUTHENTICATION_FAILURE")).isZero();
        assertThat(countAllEvents(TENANT_SCHEMA)).isZero();
    }

    @Test
    public void testRememberMeAuthenticationSuccessIsPersistedInTheCookieTenantSchema() {
        RememberMeAuthenticationToken authentication = new RememberMeAuthenticationToken(
            "remember-me-key", new User("admin", "", List.of()), List.of());

        authentication.setDetails(createDetails(OTHER_TENANT_ID));

        applicationEventPublisher.publishEvent(new AuthenticationSuccessEvent(authentication));

        assertThat(countEvents(OTHER_TENANT_SCHEMA, "admin", "AUTHENTICATION_SUCCESS")).isEqualTo(1);
        assertThat(countEvents(TENANT_SCHEMA, "admin", "AUTHENTICATION_SUCCESS")).isZero();
        assertThat(countEvents(DEFAULT_SCHEMA, "admin", "AUTHENTICATION_SUCCESS")).isZero();
        verify(tenantService, never()).getTenantIdsByUserLogin(anyString());
    }

    @Test
    public void testUnknownDetailsTenantFallsBackToThePrincipalLookup() {
        UsernamePasswordAuthenticationToken authentication =
            UsernamePasswordAuthenticationToken.authenticated("test1@gmail.com", null, List.of());

        authentication.setDetails(createDetails(UNKNOWN_TENANT_ID));

        applicationEventPublisher.publishEvent(new AuthenticationSuccessEvent(authentication));

        assertThat(countEvents(TENANT_SCHEMA, "test1@gmail.com", "AUTHENTICATION_SUCCESS")).isEqualTo(1);
        assertThat(countEvents(OTHER_TENANT_SCHEMA, "test1@gmail.com", "AUTHENTICATION_SUCCESS")).isZero();
        assertThat(countEvents(DEFAULT_SCHEMA, "test1@gmail.com", "AUTHENTICATION_SUCCESS")).isZero();
    }

    @Test
    public void testSingleTenantIgnoresTheDetailsTenant() {
        when(tenantService.isMultiTenantEnabled()).thenReturn(false);

        UsernamePasswordAuthenticationToken authentication =
            UsernamePasswordAuthenticationToken.authenticated("test1@gmail.com", null, List.of());

        authentication.setDetails(createDetails(OTHER_TENANT_ID));

        applicationEventPublisher.publishEvent(new AuthenticationSuccessEvent(authentication));

        assertThat(countEvents(DEFAULT_SCHEMA, "test1@gmail.com", "AUTHENTICATION_SUCCESS")).isEqualTo(1);
        assertThat(countEvents(OTHER_TENANT_SCHEMA, "test1@gmail.com", "AUTHENTICATION_SUCCESS")).isZero();
        verify(tenantService, never()).getTenantIds();
    }

    @Test
    public void testAuthenticationFailureEventForUnknownUserIsPersistedInTheDefaultSchema() {
        applicationEventPublisher.publishEvent(
            new AuthenticationFailureBadCredentialsEvent(
                UsernamePasswordAuthenticationToken.unauthenticated("unknown@gmail.com", "secret"),
                new BadCredentialsException("Bad credentials")));

        assertThat(countEvents(DEFAULT_SCHEMA, "unknown@gmail.com", "AUTHENTICATION_FAILURE")).isEqualTo(1);
        assertThat(countEvents(TENANT_SCHEMA, "unknown@gmail.com", "AUTHENTICATION_FAILURE")).isZero();
    }

    @Test
    public void testEmailPrincipalIsPersistedInItsFirstTenantSchema() {
        customAuditEventRepository.add(createAuditEvent("test1@gmail.com", "LOGOUT_SUCCESS"));

        assertThat(countEvents(TENANT_SCHEMA, "test1@gmail.com", "LOGOUT_SUCCESS")).isEqualTo(1);
        assertThat(countEvents(OTHER_TENANT_SCHEMA, "test1@gmail.com", "LOGOUT_SUCCESS")).isZero();
        assertThat(countEvents(DEFAULT_SCHEMA, "test1@gmail.com", "LOGOUT_SUCCESS")).isZero();
        verify(tenantService, never()).getTenantIdsByUserLogin(anyString());
    }

    @Test
    public void testLoginPrincipalIsLowercasedAndPersistedInItsTenantSchema() {
        customAuditEventRepository.add(createAuditEvent("Admin", "AUTHENTICATION_SUCCESS"));

        assertThat(countEvents(TENANT_SCHEMA, "Admin", "AUTHENTICATION_SUCCESS")).isEqualTo(1);
        assertThat(countEvents(DEFAULT_SCHEMA, "Admin", "AUTHENTICATION_SUCCESS")).isZero();
        verify(tenantService, never()).getTenantIdsByUserEmail(anyString());
    }

    @Test
    public void testAlreadySetTenantIsKept() {
        TenantContext.runWithTenantId(
            OTHER_TENANT_ID,
            () -> customAuditEventRepository.add(createAuditEvent("test1@gmail.com", "AUTHENTICATION_SUCCESS")));

        assertThat(countEvents(OTHER_TENANT_SCHEMA, "test1@gmail.com", "AUTHENTICATION_SUCCESS")).isEqualTo(1);
        assertThat(countEvents(TENANT_SCHEMA, "test1@gmail.com", "AUTHENTICATION_SUCCESS")).isZero();
        verify(tenantService, never()).getTenantIdsByUserEmail(anyString());
        verify(tenantService, never()).getTenantIdsByUserLogin(anyString());
    }

    @Test
    public void testPrincipalWithoutTenantIsPersistedInTheDefaultSchema() {
        customAuditEventRepository.add(createAuditEvent("unknown@gmail.com", "AUTHENTICATION_FAILURE"));
        customAuditEventRepository.add(createAuditEvent("[credential]", "AUTHENTICATION_FAILURE"));

        assertThat(countEvents(DEFAULT_SCHEMA, "unknown@gmail.com", "AUTHENTICATION_FAILURE")).isEqualTo(1);
        assertThat(countEvents(DEFAULT_SCHEMA, "[credential]", "AUTHENTICATION_FAILURE")).isEqualTo(1);
    }

    @Test
    public void testFailingTenantResolutionStillPersistsInTheDefaultSchema() {
        when(tenantService.getTenantIdsByUserEmail("test1@gmail.com")).thenThrow(new IllegalStateException("down"));

        customAuditEventRepository.add(createAuditEvent("test1@gmail.com", "AUTHENTICATION_SUCCESS"));

        assertThat(countEvents(DEFAULT_SCHEMA, "test1@gmail.com", "AUTHENTICATION_SUCCESS")).isEqualTo(1);
        assertThat(countEvents(TENANT_SCHEMA, "test1@gmail.com", "AUTHENTICATION_SUCCESS")).isZero();
    }

    @Test
    public void testSingleTenantPersistsInTheDefaultSchemaWithoutLookup() {
        when(tenantService.isMultiTenantEnabled()).thenReturn(false);

        customAuditEventRepository.add(createAuditEvent("test1@gmail.com", "AUTHENTICATION_SUCCESS"));

        assertThat(countEvents(DEFAULT_SCHEMA, "test1@gmail.com", "AUTHENTICATION_SUCCESS")).isEqualTo(1);
        verify(tenantService, never()).getTenantIdsByUserEmail(anyString());
        verify(tenantService, never()).getTenantIdsByUserLogin(anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testMissingTenantServicePersistsInTheDefaultSchema() {
        ObjectProvider<TenantService> tenantServiceProvider = mock(ObjectProvider.class);

        CustomAuditEventRepository noTenantServiceAuditEventRepository = new CustomAuditEventRepository(
            persistenceAuditEventRepository, auditEventConverter, tenantServiceProvider, transactionManagerProvider);

        noTenantServiceAuditEventRepository.add(createAuditEvent("admin", "AUTHENTICATION_SUCCESS"));

        assertThat(countEvents(DEFAULT_SCHEMA, "admin", "AUTHENTICATION_SUCCESS")).isEqualTo(1);
        verify(tenantService, never()).getTenantIdsByUserLogin(anyString());
    }

    @Test
    public void testAuthorizationFailureAndAnonymousUserAreSkipped() {
        customAuditEventRepository.add(createAuditEvent("test1@gmail.com", "AUTHORIZATION_FAILURE"));
        customAuditEventRepository.add(
            createAuditEvent(CustomAuditEventRepository.ANONYMOUS_USER, "AUTHENTICATION_SUCCESS"));

        assertThat(countAllEvents(DEFAULT_SCHEMA)).isZero();
        assertThat(countAllEvents(TENANT_SCHEMA)).isZero();
        verify(tenantService, never()).getTenantIdsByUserEmail(anyString());
        verify(tenantService, never()).getTenantIdsByUserLogin(anyString());
    }

    private static TenantWebAuthenticationDetails createDetails(String tenantId) {
        return new TenantWebAuthenticationDetails("localhost", null, tenantId);
    }

    private static AuditEvent createAuditEvent(String principal, String type) {
        return new AuditEvent(principal, type, Map.of("sessionId", "session"));
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
    private Integer countAllEvents(String schema) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + schema + ".persistent_audit_event", Integer.class);
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
