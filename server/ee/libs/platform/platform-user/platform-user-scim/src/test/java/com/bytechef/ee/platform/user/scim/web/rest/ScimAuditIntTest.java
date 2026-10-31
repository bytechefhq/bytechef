/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.audit.aspect.AuditAspect;
import com.bytechef.ee.platform.audit.aspect.AuditCaptureAspect;
import com.bytechef.ee.platform.audit.aspect.AuditMapperResolver;
import com.bytechef.ee.platform.audit.domain.PersistentAuditEvent;
import com.bytechef.ee.platform.audit.service.AuditEventService;
import com.bytechef.ee.platform.user.domain.IdentityProvider;
import com.bytechef.ee.platform.user.scim.audit.ScimGroupAuditEvents;
import com.bytechef.ee.platform.user.scim.audit.ScimGroupAuditMapper;
import com.bytechef.ee.platform.user.scim.audit.ScimUserAuditEvents;
import com.bytechef.ee.platform.user.scim.audit.ScimUserAuditMapper;
import com.bytechef.ee.platform.user.scim.facade.ScimGroupFacade;
import com.bytechef.ee.platform.user.scim.facade.ScimUserFacade;
import com.bytechef.ee.platform.user.scim.web.filter.ScimBearerTokenAuthenticationFilter;
import com.bytechef.ee.platform.user.service.IdentityProviderService;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
@SpringBootTest(classes = ScimAuditIntTest.Config.class, properties = "bytechef.edition=ee")
class ScimAuditIntTest {

    private static final MediaType SCIM_JSON = MediaType.parseMediaType("application/scim+json");
    private static final String SCIM_API_KEY = "scim-api-key-value";
    private static final String SCIM_PRINCIPAL = "scim:Okta (5)";
    private static final String TENANT_ID = "000001";
    private static final long USER_ID = 7L;
    private static final long GROUP_ID = 3L;
    private static final String PASSWORD_HASH = "$2a$10$" + UUID.randomUUID();

    private final List<SavedAuditEvents> savedAuditEvents = new CopyOnWriteArrayList<>();

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private AuthorityService authorityService;

    @Autowired
    private IdentityProviderService identityProviderService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private UserService userService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void beforeEach() {
        reset(auditEventService, authorityService, identityProviderService, tenantService, userService);

        savedAuditEvents.clear();

        IdentityProvider identityProvider = new IdentityProvider();

        identityProvider.setId(5L);
        identityProvider.setName("Okta");

        when(tenantService.getTenantIds()).thenReturn(List.of(TenantContext.DEFAULT_TENANT_ID, TENANT_ID));
        when(identityProviderService.fetchByScimApiKey(anyString())).thenAnswer(invocation -> {
            if (Objects.equals(TenantContext.getCurrentTenantId(), TENANT_ID)
                && SCIM_API_KEY.equals(invocation.getArgument(0))) {

                return Optional.of(identityProvider);
            }

            return Optional.empty();
        });

        doAnswer(invocation -> {
            List<PersistentAuditEvent> persistentAuditEvents = invocation.getArgument(0);

            savedAuditEvents.add(
                new SavedAuditEvents(List.copyOf(persistentAuditEvents), TenantContext.getCurrentTenantId()));

            return null;
        }).when(auditEventService)
            .saveAll(anyList());

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .addFilters(new ScimBearerTokenAuthenticationFilter(identityProviderService, tenantService))
            .build();
    }

    @Test
    void testCreateUserRecordsTheCreatedUserUnderTheScimClientInItsTenant() throws Exception {
        when(userService.create(any(AdminUserDTO.class))).thenAnswer(invocation -> {
            AdminUserDTO adminUserDTO = invocation.getArgument(0);

            return createUser(
                42L, adminUserDTO.getLogin(), adminUserDTO.getEmail(), adminUserDTO.getFirstName(),
                adminUserDTO.getLastName(), adminUserDTO.isActivated());
        });

        mockMvc
            .perform(
                scimRequest(MockMvcRequestBuilders.post("/api/scim/v2/Users"))
                    .content(userJson("jane", "jane@example.com", "Jane", "Doe", true)))
            .andExpect(MockMvcResultMatchers.status()
                .isCreated());

        PersistentAuditEvent persistentAuditEvent = getSingleSavedAuditEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(ScimUserAuditEvents.SCIM_USER_CREATED);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(SCIM_PRINCIPAL);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("userId", "42")
            .containsEntry("login", "jane")
            .containsEntry("email", "jane@example.com")
            .containsEntry("externalId", "00u-external")
            .containsEntry("active", "true")
            .doesNotContainKey("changedFields")
            .doesNotContainValue(PASSWORD_HASH)
            .doesNotContainValue(SCIM_API_KEY);
    }

    @Test
    void testFailedCreateUserRecordsAnErrorWithTheRequestedLogin() {
        when(userService.create(any(AdminUserDTO.class))).thenThrow(new IllegalStateException("duplicate login"));

        assertThatThrownBy(() -> mockMvc.perform(
            scimRequest(MockMvcRequestBuilders.post("/api/scim/v2/Users"))
                .content(userJson("jane", "jane@example.com", "Jane", "Doe", true))))
                    .hasRootCauseInstanceOf(IllegalStateException.class);

        PersistentAuditEvent persistentAuditEvent = getSingleSavedAuditEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(ScimUserAuditEvents.SCIM_USER_CREATED);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(SCIM_PRINCIPAL);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "ERROR")
            .containsEntry("errorClass", IllegalStateException.class.getName())
            .containsEntry("login", "jane")
            .containsEntry("externalId", "00u-external");
    }

    @Test
    void testReplaceUserRecordsTheChangedFields() throws Exception {
        mockExistingUser(true);
        mockUserUpdate();

        mockMvc
            .perform(
                scimRequest(MockMvcRequestBuilders.put("/api/scim/v2/Users/" + USER_ID))
                    .content(userJson("jane.doe", "jane.doe@example.com", "Janet", "Doe", true)))
            .andExpect(MockMvcResultMatchers.status()
                .isOk());

        PersistentAuditEvent persistentAuditEvent = getSingleSavedAuditEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(ScimUserAuditEvents.SCIM_USER_UPDATED);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(SCIM_PRINCIPAL);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("userId", String.valueOf(USER_ID))
            .containsEntry("login", "jane.doe")
            .containsEntry("email", "jane.doe@example.com")
            .containsEntry("externalId", "00u-external")
            .containsEntry("active", "true")
            .containsEntry("changedFields", "login,email,firstName")
            .containsEntry("previousLogin", "jane")
            .containsEntry("previousEmail", "jane@example.com")
            .doesNotContainKey("previousActive")
            .doesNotContainValue(PASSWORD_HASH);
    }

    @Test
    void testReplaceUserThatDeactivatesTheUserRecordsOneDeactivation() throws Exception {
        mockExistingUser(true);
        mockUserUpdate();

        mockMvc
            .perform(
                scimRequest(MockMvcRequestBuilders.put("/api/scim/v2/Users/" + USER_ID))
                    .content(userJson("jane", "jane@example.com", "Jane", "Doe", false)))
            .andExpect(MockMvcResultMatchers.status()
                .isOk());

        PersistentAuditEvent persistentAuditEvent = getSingleSavedAuditEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(ScimUserAuditEvents.SCIM_USER_DEACTIVATED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("userId", String.valueOf(USER_ID))
            .containsEntry("login", "jane")
            .containsEntry("active", "false")
            .containsEntry("previousActive", "true")
            .containsEntry("changedFields", "active");
    }

    @Test
    void testPatchUserRecordsThePatchedFields() throws Exception {
        mockExistingUser(true);

        mockMvc
            .perform(
                scimRequest(MockMvcRequestBuilders.patch("/api/scim/v2/Users/" + USER_ID))
                    .content(patchJson("\"path\": \"name.familyName\", \"value\": \"Roe\"")))
            .andExpect(MockMvcResultMatchers.status()
                .isOk());

        PersistentAuditEvent persistentAuditEvent = getSingleSavedAuditEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(ScimUserAuditEvents.SCIM_USER_PATCHED);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(SCIM_PRINCIPAL);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("userId", String.valueOf(USER_ID))
            .containsEntry("login", "jane")
            .containsEntry("active", "true")
            .containsEntry("changedFields", "lastName")
            .doesNotContainKey("externalId");
    }

    @Test
    void testPatchUserThatDeactivatesTheUserRecordsOneDeactivation() throws Exception {
        mockExistingUser(true);

        mockMvc
            .perform(
                scimRequest(MockMvcRequestBuilders.patch("/api/scim/v2/Users/" + USER_ID))
                    .content(patchJson("\"path\": \"active\", \"value\": \"False\"")))
            .andExpect(MockMvcResultMatchers.status()
                .isOk());

        PersistentAuditEvent persistentAuditEvent = getSingleSavedAuditEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(ScimUserAuditEvents.SCIM_USER_DEACTIVATED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("userId", String.valueOf(USER_ID))
            .containsEntry("active", "false")
            .containsEntry("previousActive", "true")
            .containsEntry("changedFields", "active");
    }

    @Test
    void testPatchOfAnInactiveUserToInactiveIsRecordedAsAPatch() throws Exception {
        mockExistingUser(false);

        mockMvc
            .perform(
                scimRequest(MockMvcRequestBuilders.patch("/api/scim/v2/Users/" + USER_ID))
                    .content(patchJson("\"value\": {\"active\": false}")))
            .andExpect(MockMvcResultMatchers.status()
                .isOk());

        PersistentAuditEvent persistentAuditEvent = getSingleSavedAuditEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(ScimUserAuditEvents.SCIM_USER_PATCHED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("active", "false")
            .containsEntry("changedFields", "")
            .doesNotContainKey("previousActive");
    }

    @Test
    void testDeleteUserRecordsTheUserCapturedBeforeTheDelete() throws Exception {
        mockExistingUser(true);

        mockMvc
            .perform(scimRequest(MockMvcRequestBuilders.delete("/api/scim/v2/Users/" + USER_ID)))
            .andExpect(MockMvcResultMatchers.status()
                .isNoContent());

        verify(userService).delete("jane");

        PersistentAuditEvent persistentAuditEvent = getSingleSavedAuditEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(ScimUserAuditEvents.SCIM_USER_DELETED);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(SCIM_PRINCIPAL);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("userId", String.valueOf(USER_ID))
            .containsEntry("login", "jane")
            .containsEntry("email", "jane@example.com")
            .doesNotContainKey("active")
            .doesNotContainValue(PASSWORD_HASH);
    }

    @Test
    void testDeleteOfMissingUserRecordsNothing() throws Exception {
        when(userService.fetchUser(USER_ID)).thenReturn(Optional.empty());

        mockMvc
            .perform(scimRequest(MockMvcRequestBuilders.delete("/api/scim/v2/Users/" + USER_ID)))
            .andExpect(MockMvcResultMatchers.status()
                .isNotFound());

        verify(auditEventService, never()).saveAll(anyList());
    }

    @Test
    void testReadingUsersRecordsNothing() throws Exception {
        mockExistingUser(true);

        mockMvc
            .perform(scimRequest(MockMvcRequestBuilders.get("/api/scim/v2/Users/" + USER_ID)))
            .andExpect(MockMvcResultMatchers.status()
                .isOk());

        verify(auditEventService, never()).saveAll(anyList());
    }

    @Test
    void testRequestWithAnUnknownScimApiKeyRecordsNothing() throws Exception {
        mockMvc
            .perform(
                MockMvcRequestBuilders.delete("/api/scim/v2/Users/" + USER_ID)
                    .header("Authorization", "Bearer unknown"))
            .andExpect(MockMvcResultMatchers.status()
                .isUnauthorized());

        verify(userService, never()).delete(anyString());
        verify(auditEventService, never()).saveAll(anyList());
    }

    @Test
    void testCreateGroupRecordsTheCreatedGroup() throws Exception {
        when(authorityService.create(any(Authority.class))).thenAnswer(invocation -> {
            Authority authority = invocation.getArgument(0);

            authority.setId(GROUP_ID);

            return authority;
        });

        mockMvc
            .perform(
                scimRequest(MockMvcRequestBuilders.post("/api/scim/v2/Groups"))
                    .content(groupJson("ROLE_ENGINEERING")))
            .andExpect(MockMvcResultMatchers.status()
                .isCreated());

        PersistentAuditEvent persistentAuditEvent = getSingleSavedAuditEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(ScimGroupAuditEvents.SCIM_GROUP_CREATED);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(SCIM_PRINCIPAL);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("groupId", String.valueOf(GROUP_ID))
            .containsEntry("name", "ROLE_ENGINEERING");
    }

    @Test
    void testReplaceGroupRecordsThePreviousName() throws Exception {
        mockExistingGroup();

        when(authorityService.update(any(Authority.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc
            .perform(
                scimRequest(MockMvcRequestBuilders.put("/api/scim/v2/Groups/" + GROUP_ID))
                    .content(groupJson("ROLE_PLATFORM")))
            .andExpect(MockMvcResultMatchers.status()
                .isOk());

        PersistentAuditEvent persistentAuditEvent = getSingleSavedAuditEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(ScimGroupAuditEvents.SCIM_GROUP_UPDATED);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("groupId", String.valueOf(GROUP_ID))
            .containsEntry("name", "ROLE_PLATFORM")
            .containsEntry("previousName", "ROLE_ENGINEERING");
    }

    @Test
    void testDeleteGroupRecordsTheGroupCapturedBeforeTheDelete() throws Exception {
        mockExistingGroup();

        mockMvc
            .perform(scimRequest(MockMvcRequestBuilders.delete("/api/scim/v2/Groups/" + GROUP_ID)))
            .andExpect(MockMvcResultMatchers.status()
                .isNoContent());

        verify(authorityService).delete(GROUP_ID);

        PersistentAuditEvent persistentAuditEvent = getSingleSavedAuditEvent();

        assertThat(persistentAuditEvent.getEventType()).isEqualTo(ScimGroupAuditEvents.SCIM_GROUP_DELETED);
        assertThat(persistentAuditEvent.getPrincipal()).isEqualTo(SCIM_PRINCIPAL);
        assertThat(persistentAuditEvent.getData())
            .containsEntry("result", "SUCCESS")
            .containsEntry("groupId", String.valueOf(GROUP_ID))
            .containsEntry("name", "ROLE_ENGINEERING");
    }

    @Test
    void testDeleteOfMissingGroupRecordsNothing() throws Exception {
        when(authorityService.fetchAuthority(GROUP_ID)).thenReturn(Optional.empty());

        mockMvc
            .perform(scimRequest(MockMvcRequestBuilders.delete("/api/scim/v2/Groups/" + GROUP_ID)))
            .andExpect(MockMvcResultMatchers.status()
                .isNotFound());

        verify(authorityService, never()).delete(GROUP_ID);
        verify(auditEventService, never()).saveAll(anyList());
    }

    private static User createUser(
        long id, String login, String email, String firstName, String lastName, boolean activated) {

        User user = new User();

        user.setId(id);
        user.setLogin(login);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setActivated(activated);
        user.setPassword(PASSWORD_HASH);

        return user;
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

    private PersistentAuditEvent getSingleSavedAuditEvent() {
        assertThat(savedAuditEvents).hasSize(1);

        SavedAuditEvents firstSavedAuditEvents = savedAuditEvents.getFirst();

        assertThat(firstSavedAuditEvents.tenantId()).isEqualTo(TENANT_ID);
        List<PersistentAuditEvent> persistentAuditEvents = firstSavedAuditEvents.persistentAuditEvents();

        assertThat(persistentAuditEvents).hasSize(1);

        return persistentAuditEvents.getFirst();
    }

    private void mockExistingGroup() {
        when(authorityService.fetchAuthority(GROUP_ID)).thenAnswer(invocation -> {
            Authority authority = new Authority();

            authority.setId(GROUP_ID);
            authority.setName("ROLE_ENGINEERING");

            return Optional.of(authority);
        });
    }

    private void mockExistingUser(boolean activated) {
        User user = createUser(USER_ID, "jane", "jane@example.com", "Jane", "Doe", activated);

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(user));
    }

    private void mockUserUpdate() {
        when(userService.update(any(AdminUserDTO.class))).thenAnswer(invocation -> {
            AdminUserDTO adminUserDTO = invocation.getArgument(0);

            return Optional.of(
                createUser(
                    adminUserDTO.getId(), adminUserDTO.getLogin(), adminUserDTO.getEmail(),
                    adminUserDTO.getFirstName(), adminUserDTO.getLastName(), adminUserDTO.isActivated()));
        });
    }

    private record SavedAuditEvents(List<PersistentAuditEvent> persistentAuditEvents, String tenantId) {
    }

    @SpringBootConfiguration
    @EnableAspectJAutoProxy
    @EnableWebMvc
    @Import({
        AuditAspect.class, AuditCaptureAspect.class, AuditMapperResolver.class, ScimGroupAuditMapper.class,
        ScimGroupController.class, ScimGroupFacade.class, ScimUserAuditMapper.class, ScimUserController.class,
        ScimUserFacade.class
    })
    static class Config {

        @Bean
        AuditEventService auditEventService() {
            return mock(AuditEventService.class);
        }

        @Bean
        AuthorityService authorityService() {
            return mock(AuthorityService.class);
        }

        @Bean
        IdentityProviderService identityProviderService() {
            return mock(IdentityProviderService.class);
        }

        @Bean
        TenantService tenantService() {
            return mock(TenantService.class);
        }

        @Bean
        UserService userService() {
            return mock(UserService.class);
        }
    }
}
