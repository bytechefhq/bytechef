/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.audit.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.audit.config.AuditIntTestConfiguration;
import com.bytechef.ee.platform.user.scim.audit.ScimGroupAuditEvents;
import com.bytechef.ee.platform.user.scim.audit.ScimGroupAuditMapper;
import com.bytechef.ee.platform.user.scim.audit.ScimUserAuditEvents;
import com.bytechef.ee.platform.user.scim.audit.ScimUserAuditMapper;
import com.bytechef.ee.platform.user.scim.facade.ScimGroupFacade;
import com.bytechef.ee.platform.user.scim.facade.ScimUserFacade;
import com.bytechef.ee.platform.user.scim.web.filter.ScimBearerTokenAuthenticationFilter;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.ee.platform.user.service.IdentityProviderServiceImpl;
import com.bytechef.encryption.Encryption;
import com.bytechef.platform.user.callback.UserCallback;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.AuthorityServiceImpl;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.user.service.UserServiceImpl;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = {
    AuditIntTestConfiguration.class, ScimAuditIntTest.Config.class
})
@ActiveProfiles("testint")
@Import(PostgreSQLContainerConfiguration.class)
public class ScimAuditIntTest {

    private static final String DEFAULT_SCHEMA = "public";
    private static final long IDENTITY_PROVIDER_ID = 5L;
    private static final MediaType SCIM_JSON = MediaType.parseMediaType("application/scim+json");
    private static final String SCIM_API_KEY = "scim-api-key-value";
    private static final String SCIM_PRINCIPAL = "scim:Okta (5)";
    private static final String TENANT_ID = "000001";
    private static final String TENANT_SCHEMA = "bytechef_" + TENANT_ID;

    @Autowired
    private AuthorityService authorityService;

    @Autowired
    private IdentityProviderService identityProviderService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private UserService userService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public void beforeEach() {
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS " + TENANT_SCHEMA);

        List<String> tables = List.of(
            "\"user\"", "authority", "user_authority", "identity_provider", "identity_provider_domain",
            "persistent_audit_event", "persistent_audit_event_data");

        for (String table : tables) {
            jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS " + TENANT_SCHEMA + "." + table + " (LIKE public." + table +
                    " INCLUDING ALL)");
        }

        jdbcTemplate.update(
            "INSERT INTO " + TENANT_SCHEMA + ".identity_provider (id, name, issuer_uri, client_id, client_secret, " +
                "scim_api_key) VALUES (?, 'Okta', 'https://okta.example.com', 'client', 'secret', ?)",
            IDENTITY_PROVIDER_ID, SCIM_API_KEY);

        when(tenantService.getTenantIds()).thenReturn(List.of(TenantContext.DEFAULT_TENANT_ID, TENANT_ID));

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .addFilters(new ScimBearerTokenAuthenticationFilter(identityProviderService, tenantService))
            .build();
    }

    @AfterEach
    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    public void afterEach() {
        TenantContext.resetCurrentTenantId();

        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + TENANT_SCHEMA + " CASCADE");
        jdbcTemplate.update("DELETE FROM public.persistent_audit_event_data");
        jdbcTemplate.update("DELETE FROM public.persistent_audit_event");
    }

    @Test
    public void testCreateUserRecordsTheCreatedUserUnderTheScimClientInItsTenant() throws Exception {
        mockMvc
            .perform(
                scimRequest(MockMvcRequestBuilders.post("/api/scim/v2/Users"))
                    .content(userJson("jane", "jane@example.com", "Jane", "Doe", true)))
            .andExpect(MockMvcResultMatchers.status()
                .isCreated());

        assertThat(countUsers(TENANT_SCHEMA, "jane")).isEqualTo(1);
        assertThat(countUsers(DEFAULT_SCHEMA, "jane")).isZero();

        Long userId = findUserId("jane");
        String passwordHash = findUserColumn(userId, "password_hash");

        AuditEventRow auditEventRow = getSingleAuditEvent();

        assertThat(auditEventRow.eventType()).isEqualTo(ScimUserAuditEvents.SCIM_USER_CREATED);
        assertThat(auditEventRow.principal()).isEqualTo(SCIM_PRINCIPAL);
        assertThat(auditEventRow.data())
            .containsEntry("result", "SUCCESS")
            .containsEntry("userId", String.valueOf(userId))
            .containsEntry("login", "jane")
            .containsEntry("email", "jane@example.com")
            .containsEntry("externalId", "00u-external")
            .containsEntry("active", "true")
            .doesNotContainKey("changedFields")
            .doesNotContainValue(passwordHash)
            .doesNotContainValue(SCIM_API_KEY);
    }

    @Test
    public void testFailedCreateUserRollsBackAndRecordsAnErrorWithTheRequestedLogin() {
        createUser("jane", "jane@example.com", true);

        assertThatThrownBy(() -> mockMvc.perform(
            scimRequest(MockMvcRequestBuilders.post("/api/scim/v2/Users"))
                .content(userJson("jane", "jane.other@example.com", "Jane", "Doe", true))))
                    .rootCause()
                    .hasMessageContaining("user_login_key");

        assertThat(countUsers(TENANT_SCHEMA, "jane")).isEqualTo(1);
        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + TENANT_SCHEMA + ".\"user\" WHERE email = ?", Integer.class,
                "jane.other@example.com")).isZero();

        AuditEventRow auditEventRow = getSingleAuditEvent();

        assertThat(auditEventRow.eventType()).isEqualTo(ScimUserAuditEvents.SCIM_USER_CREATED);
        assertThat(auditEventRow.principal()).isEqualTo(SCIM_PRINCIPAL);
        assertThat(auditEventRow.data())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", DuplicateKeyException.class.getName())
            .containsEntry("login", "jane")
            .containsEntry("externalId", "00u-external");
    }

    @Test
    public void testReplaceUserRecordsTheChangedFields() throws Exception {
        long groupId = createGroup("ROLE_ENGINEERING");
        long userId = createUser("jane", "jane@example.com", true);

        assignGroup(userId, groupId);

        mockMvc
            .perform(
                scimRequest(MockMvcRequestBuilders.put("/api/scim/v2/Users/" + userId))
                    .content(userJson("jane.doe", "jane.doe@example.com", "Janet", "Doe", true)))
            .andExpect(MockMvcResultMatchers.status()
                .isOk());

        assertThat(findUserColumn(userId, "login")).isEqualTo("jane.doe");
        assertThat(findUserColumn(userId, "first_name")).isEqualTo("Janet");
        assertThat(findGroupIds(userId)).containsExactly(groupId);

        AuditEventRow auditEventRow = getSingleAuditEvent();

        assertThat(auditEventRow.eventType()).isEqualTo(ScimUserAuditEvents.SCIM_USER_UPDATED);
        assertThat(auditEventRow.principal()).isEqualTo(SCIM_PRINCIPAL);
        assertThat(auditEventRow.data())
            .containsEntry("result", "SUCCESS")
            .containsEntry("userId", String.valueOf(userId))
            .containsEntry("login", "jane.doe")
            .containsEntry("email", "jane.doe@example.com")
            .containsEntry("externalId", "00u-external")
            .containsEntry("active", "true")
            .containsEntry("changedFields", "login,email,firstName")
            .containsEntry("previousLogin", "jane")
            .containsEntry("previousEmail", "jane@example.com")
            .doesNotContainKey("previousActive");
    }

    @Test
    public void testReplaceUserThatDeactivatesTheUserRecordsOneDeactivation() throws Exception {
        long userId = createUser("jane", "jane@example.com", true);

        mockMvc
            .perform(
                scimRequest(MockMvcRequestBuilders.put("/api/scim/v2/Users/" + userId))
                    .content(userJson("jane", "jane@example.com", "Jane", "Doe", false)))
            .andExpect(MockMvcResultMatchers.status()
                .isOk());

        assertThat(findUserColumn(userId, "activated")).isEqualTo("false");

        AuditEventRow auditEventRow = getSingleAuditEvent();

        assertThat(auditEventRow.eventType()).isEqualTo(ScimUserAuditEvents.SCIM_USER_DEACTIVATED);
        assertThat(auditEventRow.principal()).isEqualTo(SCIM_PRINCIPAL);
        assertThat(auditEventRow.data())
            .containsEntry("result", "SUCCESS")
            .containsEntry("userId", String.valueOf(userId))
            .containsEntry("login", "jane")
            .containsEntry("active", "false")
            .containsEntry("previousActive", "true")
            .containsEntry("changedFields", "active");
    }

    @Test
    public void testPatchUserRecordsThePatchedFields() throws Exception {
        long userId = createUser("jane", "jane@example.com", true);

        mockMvc
            .perform(
                scimRequest(MockMvcRequestBuilders.patch("/api/scim/v2/Users/" + userId))
                    .content(patchJson("\"path\": \"name.familyName\", \"value\": \"Roe\"")))
            .andExpect(MockMvcResultMatchers.status()
                .isOk());

        assertThat(findUserColumn(userId, "last_name")).isEqualTo("Roe");

        AuditEventRow auditEventRow = getSingleAuditEvent();

        assertThat(auditEventRow.eventType()).isEqualTo(ScimUserAuditEvents.SCIM_USER_PATCHED);
        assertThat(auditEventRow.principal()).isEqualTo(SCIM_PRINCIPAL);
        assertThat(auditEventRow.data())
            .containsEntry("result", "SUCCESS")
            .containsEntry("userId", String.valueOf(userId))
            .containsEntry("login", "jane")
            .containsEntry("active", "true")
            .containsEntry("changedFields", "lastName")
            .doesNotContainKey("externalId");
    }

    @Test
    public void testPatchUserThatDeactivatesTheUserRecordsOneDeactivation() throws Exception {
        long userId = createUser("jane", "jane@example.com", true);

        mockMvc
            .perform(
                scimRequest(MockMvcRequestBuilders.patch("/api/scim/v2/Users/" + userId))
                    .content(patchJson("\"path\": \"active\", \"value\": \"False\"")))
            .andExpect(MockMvcResultMatchers.status()
                .isOk());

        assertThat(findUserColumn(userId, "activated")).isEqualTo("false");

        AuditEventRow auditEventRow = getSingleAuditEvent();

        assertThat(auditEventRow.eventType()).isEqualTo(ScimUserAuditEvents.SCIM_USER_DEACTIVATED);
        assertThat(auditEventRow.principal()).isEqualTo(SCIM_PRINCIPAL);
        assertThat(auditEventRow.data())
            .containsEntry("result", "SUCCESS")
            .containsEntry("userId", String.valueOf(userId))
            .containsEntry("active", "false")
            .containsEntry("previousActive", "true")
            .containsEntry("changedFields", "active");
    }

    @Test
    public void testPatchOfAnInactiveUserToInactiveIsRecordedAsAPatch() throws Exception {
        long userId = createUser("jane", "jane@example.com", false);

        mockMvc
            .perform(
                scimRequest(MockMvcRequestBuilders.patch("/api/scim/v2/Users/" + userId))
                    .content(patchJson("\"value\": {\"active\": false}")))
            .andExpect(MockMvcResultMatchers.status()
                .isOk());

        assertThat(findUserColumn(userId, "activated")).isEqualTo("false");

        AuditEventRow auditEventRow = getSingleAuditEvent();

        assertThat(auditEventRow.eventType()).isEqualTo(ScimUserAuditEvents.SCIM_USER_PATCHED);
        assertThat(auditEventRow.data())
            .containsEntry("active", "false")
            .containsEntry("changedFields", "")
            .doesNotContainKey("previousActive");
    }

    @Test
    public void testDeleteUserRecordsTheUserCapturedBeforeTheDelete() throws Exception {
        long userId = createUser("jane", "jane@example.com", true);

        mockMvc
            .perform(scimRequest(MockMvcRequestBuilders.delete("/api/scim/v2/Users/" + userId)))
            .andExpect(MockMvcResultMatchers.status()
                .isNoContent());

        assertThat(countUsers(TENANT_SCHEMA, "jane")).isZero();

        AuditEventRow auditEventRow = getSingleAuditEvent();

        assertThat(auditEventRow.eventType()).isEqualTo(ScimUserAuditEvents.SCIM_USER_DELETED);
        assertThat(auditEventRow.principal()).isEqualTo(SCIM_PRINCIPAL);
        assertThat(auditEventRow.data())
            .containsEntry("result", "SUCCESS")
            .containsEntry("userId", String.valueOf(userId))
            .containsEntry("login", "jane")
            .containsEntry("email", "jane@example.com")
            .doesNotContainKey("active");
    }

    @Test
    public void testDeleteOfMissingUserRecordsNothing() throws Exception {
        mockMvc
            .perform(scimRequest(MockMvcRequestBuilders.delete("/api/scim/v2/Users/" + Long.MAX_VALUE)))
            .andExpect(MockMvcResultMatchers.status()
                .isNotFound());

        assertNoAuditEvents();
    }

    @Test
    public void testReadingUsersRecordsNothing() throws Exception {
        long userId = createUser("jane", "jane@example.com", true);

        mockMvc
            .perform(scimRequest(MockMvcRequestBuilders.get("/api/scim/v2/Users/" + userId)))
            .andExpect(MockMvcResultMatchers.status()
                .isOk());

        assertNoAuditEvents();
    }

    @Test
    public void testRequestWithAnUnknownScimApiKeyRecordsNothing() throws Exception {
        long userId = createUser("jane", "jane@example.com", true);

        mockMvc
            .perform(
                MockMvcRequestBuilders.delete("/api/scim/v2/Users/" + userId)
                    .header("Authorization", "Bearer unknown"))
            .andExpect(MockMvcResultMatchers.status()
                .isUnauthorized());

        assertThat(countUsers(TENANT_SCHEMA, "jane")).isEqualTo(1);

        assertNoAuditEvents();
    }

    @Test
    public void testCreateGroupRecordsTheCreatedGroup() throws Exception {
        mockMvc
            .perform(
                scimRequest(MockMvcRequestBuilders.post("/api/scim/v2/Groups"))
                    .content(groupJson("ROLE_ENGINEERING")))
            .andExpect(MockMvcResultMatchers.status()
                .isCreated());

        assertThat(countAuthorities(DEFAULT_SCHEMA, "ROLE_ENGINEERING")).isZero();

        Long groupId = jdbcTemplate.queryForObject(
            "SELECT id FROM " + TENANT_SCHEMA + ".authority WHERE name = ?", Long.class, "ROLE_ENGINEERING");

        AuditEventRow auditEventRow = getSingleAuditEvent();

        assertThat(auditEventRow.eventType()).isEqualTo(ScimGroupAuditEvents.SCIM_GROUP_CREATED);
        assertThat(auditEventRow.principal()).isEqualTo(SCIM_PRINCIPAL);
        assertThat(auditEventRow.data())
            .containsEntry("result", "SUCCESS")
            .containsEntry("groupId", String.valueOf(groupId))
            .containsEntry("name", "ROLE_ENGINEERING");
    }

    @Test
    public void testReplaceGroupRecordsThePreviousName() throws Exception {
        long groupId = createGroup("ROLE_ENGINEERING");

        mockMvc
            .perform(
                scimRequest(MockMvcRequestBuilders.put("/api/scim/v2/Groups/" + groupId))
                    .content(groupJson("ROLE_PLATFORM")))
            .andExpect(MockMvcResultMatchers.status()
                .isOk());

        assertThat(countAuthorities(TENANT_SCHEMA, "ROLE_PLATFORM")).isEqualTo(1);
        assertThat(countAuthorities(TENANT_SCHEMA, "ROLE_ENGINEERING")).isZero();

        AuditEventRow auditEventRow = getSingleAuditEvent();

        assertThat(auditEventRow.eventType()).isEqualTo(ScimGroupAuditEvents.SCIM_GROUP_UPDATED);
        assertThat(auditEventRow.principal()).isEqualTo(SCIM_PRINCIPAL);
        assertThat(auditEventRow.data())
            .containsEntry("result", "SUCCESS")
            .containsEntry("groupId", String.valueOf(groupId))
            .containsEntry("name", "ROLE_PLATFORM")
            .containsEntry("previousName", "ROLE_ENGINEERING");
    }

    @Test
    public void testDeleteGroupRecordsTheGroupCapturedBeforeTheDelete() throws Exception {
        long groupId = createGroup("ROLE_ENGINEERING");

        mockMvc
            .perform(scimRequest(MockMvcRequestBuilders.delete("/api/scim/v2/Groups/" + groupId)))
            .andExpect(MockMvcResultMatchers.status()
                .isNoContent());

        assertThat(countAuthorities(TENANT_SCHEMA, "ROLE_ENGINEERING")).isZero();

        AuditEventRow auditEventRow = getSingleAuditEvent();

        assertThat(auditEventRow.eventType()).isEqualTo(ScimGroupAuditEvents.SCIM_GROUP_DELETED);
        assertThat(auditEventRow.principal()).isEqualTo(SCIM_PRINCIPAL);
        assertThat(auditEventRow.data())
            .containsEntry("result", "SUCCESS")
            .containsEntry("groupId", String.valueOf(groupId))
            .containsEntry("name", "ROLE_ENGINEERING");
    }

    @Test
    public void testDeleteOfMissingGroupRecordsNothing() throws Exception {
        mockMvc
            .perform(scimRequest(MockMvcRequestBuilders.delete("/api/scim/v2/Groups/" + Long.MAX_VALUE)))
            .andExpect(MockMvcResultMatchers.status()
                .isNotFound());

        assertNoAuditEvents();
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private void assignGroup(long userId, long groupId) {
        jdbcTemplate.update(
            "INSERT INTO " + TENANT_SCHEMA + ".user_authority (user_id, authority_id) VALUES (?, ?)", userId, groupId);
    }

    private void assertNoAuditEvents() {
        assertThat(countAuditEvents(TENANT_SCHEMA)).isZero();
        assertThat(countAuditEvents(DEFAULT_SCHEMA)).isZero();
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private Integer countAuditEvents(String schema) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + schema + ".persistent_audit_event", Integer.class);
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private Integer countAuthorities(String schema, String name) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + schema + ".authority WHERE name = ?", Integer.class, name);
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private Integer countUsers(String schema, String login) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM " + schema + ".\"user\" WHERE login = ?", Integer.class, login);
    }

    private long createGroup(String name) {
        Authority authority = new Authority();

        authority.setName(name);

        Authority createdAuthority =
            TenantContext.callWithTenantId(TENANT_ID, () -> authorityService.create(authority));

        return createdAuthority.getId();
    }

    private long createUser(String login, String email, boolean activated) {
        AdminUserDTO adminUserDTO = new AdminUserDTO();

        adminUserDTO.setEmail(email);
        adminUserDTO.setFirstName("Jane");
        adminUserDTO.setLastName("Doe");
        adminUserDTO.setLogin(login);

        User user = TenantContext.callWithTenantId(TENANT_ID, () -> userService.create(adminUserDTO));

        if (!activated) {
            user.setActivated(false);

            TenantContext.runWithTenantId(TENANT_ID, () -> userService.save(user));
        }

        return user.getId();
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private List<Long> findGroupIds(long userId) {
        return jdbcTemplate.queryForList(
            "SELECT authority_id FROM " + TENANT_SCHEMA + ".user_authority WHERE user_id = ?", Long.class, userId);
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private String findUserColumn(long userId, String column) {
        return jdbcTemplate.queryForObject(
            "SELECT " + column + "::text FROM " + TENANT_SCHEMA + ".\"user\" WHERE id = ?", String.class, userId);
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private Long findUserId(String login) {
        return jdbcTemplate.queryForObject(
            "SELECT id FROM " + TENANT_SCHEMA + ".\"user\" WHERE login = ?", Long.class, login);
    }

    @SuppressFBWarnings("SQL_INJECTION_SPRING_JDBC")
    private AuditEventRow getSingleAuditEvent() {
        assertThat(countAuditEvents(DEFAULT_SCHEMA)).isZero();
        assertThat(countAuditEvents(TENANT_SCHEMA)).isEqualTo(1);

        Map<String, Object> auditEvent = jdbcTemplate.queryForMap(
            "SELECT id, event_type, principal FROM " + TENANT_SCHEMA + ".persistent_audit_event");

        Map<String, String> data = new HashMap<>();

        jdbcTemplate.query(
            "SELECT key, value FROM " + TENANT_SCHEMA
                + ".persistent_audit_event_data WHERE persistent_audit_event_id = ?",
            (RowCallbackHandler) resultSet -> data.put(resultSet.getString("key"), resultSet.getString("value")),
            auditEvent.get("id"));

        return new AuditEventRow((String) auditEvent.get("event_type"), (String) auditEvent.get("principal"), data);
    }

    private static String groupJson(String displayName) {
        return """
            { \
              "schemas": ["urn:ietf:params:scim:schemas:core:2.0:Group"], \
              "displayName": "%s", \
              "members": [] \
            } \
            """.formatted(displayName);
    }

    private static String patchJson(String operationBody) {
        return """
            { \
              "schemas": ["urn:ietf:params:scim:api:messages:2.0:PatchOp"], \
              "Operations": [{"op": "Replace", %s}] \
            } \
            """.formatted(operationBody);
    }

    private static MockHttpServletRequestBuilder scimRequest(MockHttpServletRequestBuilder requestBuilder) {
        return requestBuilder.header("Authorization", "Bearer " + SCIM_API_KEY)
            .contentType(SCIM_JSON);
    }

    private static String userJson(
        String userName, String email, String givenName, String familyName, boolean active) {

        return """
            { \
              "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"], \
              "externalId": "00u-external", \
              "userName": "%s", \
              "name": {"givenName": "%s", "familyName": "%s"}, \
              "emails": [{"value": "%s", "type": "work", "primary": true}], \
              "active": %s \
            } \
            """.formatted(userName, givenName, familyName, email, active);
    }

    private record AuditEventRow(String eventType, String principal, Map<String, String> data) {
    }

    @ComponentScan("com.bytechef.ee.platform.user.scim.web.rest")
    @EnableWebMvc
    @TestConfiguration(proxyBeanMethods = false)
    @Import({
        AuthorityServiceImpl.class, IdentityProviderServiceImpl.class, ScimGroupAuditMapper.class,
        ScimGroupFacade.class, ScimUserAuditMapper.class, ScimUserFacade.class, UserCallback.class,
        UserServiceImpl.class
    })
    static class Config {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        Encryption encryption() {
            return mock(Encryption.class);
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
