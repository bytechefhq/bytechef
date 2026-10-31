/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.user.scim.audit.ScimUserAuditEvents;
import com.bytechef.ee.platform.user.scim.audit.ScimUserAuditMapper;
import com.bytechef.ee.platform.user.scim.facade.ScimUserFacade;
import com.bytechef.ee.platform.user.scim.model.ScimEmail;
import com.bytechef.ee.platform.user.scim.model.ScimName;
import com.bytechef.ee.platform.user.scim.model.ScimUser;
import com.bytechef.platform.user.callback.UserCallback;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserServiceImpl;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.sql.BaseDataSource;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = {
    AuditIntTestConfiguration.class, ScimUserAuditIntTest.Config.class
})
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
public class ScimUserAuditIntTest {

    private static final String DEFAULT_SCHEMA = "public";
    private static final String SCIM_PRINCIPAL = "scim:Okta (5)";
    private static final String TENANT_ID = "000001";
    private static final String TENANT_SCHEMA = "bytechef_" + TENANT_ID;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ScimUserFacade scimUserFacade;

    @BeforeEach
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public void beforeEach() {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + TENANT_SCHEMA);

        List<String> tables = List.of(
            "\"user\"", "authority", "user_authority", "persistent_audit_event", "persistent_audit_event_data");

        for (String table : tables) {

            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS " + TENANT_SCHEMA + "." + table + " (LIKE public." + table +
                    " INCLUDING ALL)");
        }

        SecurityContext securityContext = SecurityContextHolder.getContext();

        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(SCIM_PRINCIPAL, null, List.of()));
    }

    @AfterEach
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public void afterEach() {
        SecurityContextHolder.clearContext();

        TenantContext.resetCurrentTenantId();

        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + TENANT_SCHEMA + " CASCADE");
        jdbcTemplate.update("DELETE FROM public.persistent_audit_event_data");
        jdbcTemplate.update("DELETE FROM public.persistent_audit_event");
    }

    @Test
    public void testCreatedUserAndItsAuditEventCommitInTheTenantSchema() {
        User user = TenantContext.callWithTenantId(
            TENANT_ID, () -> scimUserFacade.createUser(scimUser("jane", "jane@example.com")));

        assertThat(countUsers(TENANT_SCHEMA, "jane")).isEqualTo(1);
        assertThat(countUsers(DEFAULT_SCHEMA, "jane")).isZero();

        assertThat(findAuditEventData(TENANT_SCHEMA))
            .containsEntry("result", "SUCCESS")
            .containsEntry("userId", String.valueOf(user.getId()))
            .containsEntry("login", "jane");
        assertThat(countAuditEvents(DEFAULT_SCHEMA)).isZero();
    }

    @Test
    public void testFailedCreateRollsBackTheUserAndCommitsAnErrorAuditEventInTheTenantSchema() {
        TenantContext.runWithTenantId(
            TENANT_ID, () -> scimUserFacade.createUser(scimUser("jane", "jane@example.com")));

        jdbcTemplate.update("DELETE FROM " + TENANT_SCHEMA + ".persistent_audit_event_data");
        jdbcTemplate.update("DELETE FROM " + TENANT_SCHEMA + ".persistent_audit_event");

        assertThatThrownBy(() -> TenantContext.runWithTenantId(
            TENANT_ID, () -> scimUserFacade.createUser(scimUser("jane", "jane.other@example.com"))))
                .isInstanceOf(RuntimeException.class);

        assertThat(countUsers(TENANT_SCHEMA, "jane")).isEqualTo(1);
        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + TENANT_SCHEMA + ".\"user\" WHERE email = ?", Integer.class,
                "jane.other@example.com")).isZero();

        assertThat(findAuditEventData(TENANT_SCHEMA))
            .containsEntry("result", "ERROR")
            .containsEntry("login", "jane")
            .containsKey("errorClass");
        assertThat(countAuditEvents(DEFAULT_SCHEMA)).isZero();
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private Integer countAuditEvents(String schema) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + schema + ".persistent_audit_event WHERE event_type = ?", Integer.class,
            ScimUserAuditEvents.SCIM_USER_CREATED);
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private Integer countUsers(String schema, String login) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + schema + ".\"user\" WHERE login = ?", Integer.class, login);
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private Map<String, String> findAuditEventData(String schema) {
        Long eventId = jdbcTemplate.queryForObject(
            "SELECT id FROM " + schema + ".persistent_audit_event WHERE event_type = ? AND principal = ?", Long.class,
            ScimUserAuditEvents.SCIM_USER_CREATED, SCIM_PRINCIPAL);

        Map<String, String> data = new HashMap<>();

        jdbcTemplate.query(
            "SELECT key, value FROM " + schema + ".persistent_audit_event_data WHERE persistent_audit_event_id = ?",
            (RowCallbackHandler) resultSet -> data.put(resultSet.getString("key"), resultSet.getString("value")),
            eventId);

        return data;
    }

    private static ScimUser scimUser(String userName, String email) {
        return new ScimUser(
            List.of(ScimUser.SCHEMA), null, "00u-external", userName, new ScimName(null, "Doe", "Jane"), null,
            List.of(new ScimEmail(email, "work", true)), true, List.of(), null);
    }

    @TestConfiguration(proxyBeanMethods = false)
    @Import({
        ScimUserAuditMapper.class, ScimUserFacade.class, UserCallback.class, UserServiceImpl.class
    })
    static class Config {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder(4);
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
