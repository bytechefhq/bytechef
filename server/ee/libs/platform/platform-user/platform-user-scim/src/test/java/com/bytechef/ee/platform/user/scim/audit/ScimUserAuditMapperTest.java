/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.user.scim.model.ScimPatchRequest;
import com.bytechef.ee.platform.user.scim.model.ScimUser;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditOutcome;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class ScimUserAuditMapperTest {

    private static final long USER_ID = 42L;

    private final UserService userService = mock(UserService.class);
    private final ScimUserAuditMapper scimUserAuditMapper = new ScimUserAuditMapper(userService);

    @Test
    void testCaptureSkipsCreatedEvent() {
        AuditInvocation auditInvocation = createAuditInvocation(
            ScimUserAuditEvents.SCIM_USER_CREATED, Map.of("scimUser", createScimUser("john.doe", "ext-1")), null, null);

        assertThat(scimUserAuditMapper.capture(auditInvocation)).isNull();

        verifyNoInteractions(userService);
    }

    @Test
    void testCaptureReturnsNullWhenUserDoesNotExist() {
        when(userService.fetchUser(USER_ID)).thenReturn(Optional.empty());

        AuditInvocation auditInvocation = createAuditInvocation(
            ScimUserAuditEvents.SCIM_USER_DELETED, Map.of("id", USER_ID), null, null);

        assertThat(scimUserAuditMapper.capture(auditInvocation)).isNull();
    }

    @Test
    void testCaptureRequiresIdArgumentForNonCreateEvents() {
        AuditInvocation auditInvocation = createAuditInvocation(
            ScimUserAuditEvents.SCIM_USER_UPDATED, Map.of(), null, null);

        assertThatThrownBy(() -> scimUserAuditMapper.capture(auditInvocation))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testMapCreatedEvent() {
        User user = createUser("john.doe", "john@example.com", "John", "Doe", true, List.of());

        AuditInvocation auditInvocation = createAuditInvocation(
            ScimUserAuditEvents.SCIM_USER_CREATED, Map.of("scimUser", createScimUser("john.doe", "ext-1")), user,
            null);

        Map<String, String> data = scimUserAuditMapper.map(auditInvocation);

        assertThat(data).containsExactly(
            Map.entry("userId", "42"),
            Map.entry("login", "john.doe"),
            Map.entry("email", "john@example.com"),
            Map.entry("active", "true"),
            Map.entry("externalId", "ext-1"));
    }

    @Test
    void testMapFailedCreateFallsBackToScimUserName() {
        AuditInvocation auditInvocation = new AuditInvocation(
            ScimUserAuditEvents.SCIM_USER_CREATED, Map.of("scimUser", createScimUser("john.doe", null)), null, null,
            AuditOutcome.ERROR, IllegalStateException.class);

        Map<String, String> data = scimUserAuditMapper.map(auditInvocation);

        assertThat(data).containsExactly(Map.entry("login", "john.doe"));
    }

    @Test
    void testMapUpdatedEventRecordsChangedFields() {
        User previousUser = createUser("john.doe", "john@example.com", "John", "Doe", true, List.of(2L, 1L));
        User updatedUser = createUser("jane.doe", "jane@example.com", "Jane", "Smith", false, List.of(3L));

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(previousUser));

        Map<String, Object> arguments = Map.of("id", USER_ID, "scimUser", createScimUser("jane.doe", "ext-2"));

        Object captured = scimUserAuditMapper.capture(
            createAuditInvocation(ScimUserAuditEvents.SCIM_USER_UPDATED, arguments, null, null));

        Map<String, String> data = scimUserAuditMapper.map(
            createAuditInvocation(
                ScimUserAuditEvents.SCIM_USER_UPDATED, arguments, Optional.of(updatedUser), captured));

        assertThat(data).containsEntry("userId", "42")
            .containsEntry("login", "jane.doe")
            .containsEntry("email", "jane@example.com")
            .containsEntry("active", "false")
            .containsEntry("externalId", "ext-2")
            .containsEntry("previousLogin", "john.doe")
            .containsEntry("previousEmail", "john@example.com")
            .containsEntry("previousActive", "true")
            .containsEntry("changedFields", "login,email,firstName,lastName,active,authorities");
    }

    @Test
    void testMapUpdatedEventIgnoresAuthorityOrder() {
        User previousUser = createUser("john.doe", "john@example.com", "John", "Doe", true, List.of(2L, 1L));
        User updatedUser = createUser("john.doe", "john@example.com", "John", "Doe", true, List.of(1L, 2L));

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(previousUser));

        Map<String, Object> arguments = Map.of("id", USER_ID);

        Object captured = scimUserAuditMapper.capture(
            createAuditInvocation(ScimUserAuditEvents.SCIM_USER_PATCHED, arguments, null, null));

        Map<String, String> data = scimUserAuditMapper.map(
            createAuditInvocation(
                ScimUserAuditEvents.SCIM_USER_PATCHED, arguments, Optional.of(updatedUser), captured));

        assertThat(data).containsEntry("changedFields", "")
            .doesNotContainKeys("previousLogin", "previousEmail", "previousActive", "externalId");
    }

    @Test
    void testMapDeletedEventUsesCapturedStateWithoutActiveFlag() {
        when(userService.fetchUser(USER_ID))
            .thenReturn(Optional.of(createUser("john.doe", "john@example.com", "John", "Doe", true, List.of())));

        Map<String, Object> arguments = Map.of("id", USER_ID);

        Object captured = scimUserAuditMapper.capture(
            createAuditInvocation(ScimUserAuditEvents.SCIM_USER_DELETED, arguments, null, null));

        Map<String, String> data = scimUserAuditMapper.map(
            createAuditInvocation(ScimUserAuditEvents.SCIM_USER_DELETED, arguments, Boolean.TRUE, captured));

        assertThat(data).containsExactly(
            Map.entry("userId", "42"),
            Map.entry("login", "john.doe"),
            Map.entry("email", "john@example.com"));
    }

    @Test
    void testMapDeactivatedEventWithPatchRequest() {
        User previousUser = createUser("john.doe", "john@example.com", "John", "Doe", true, List.of());
        User deactivatedUser = createUser("john.doe", "john@example.com", "John", "Doe", false, List.of());

        when(userService.fetchUser(USER_ID)).thenReturn(Optional.of(previousUser));

        Map<String, Object> arguments = Map.of(
            "id", USER_ID, "scimPatchRequest", new ScimPatchRequest(List.of(), List.of()));

        Object captured = scimUserAuditMapper.capture(
            createAuditInvocation(ScimUserAuditEvents.SCIM_USER_DEACTIVATED, arguments, null, null));

        Map<String, String> data = scimUserAuditMapper.map(
            createAuditInvocation(
                ScimUserAuditEvents.SCIM_USER_DEACTIVATED, arguments, Optional.of(deactivatedUser), captured));

        assertThat(data).containsEntry("active", "false")
            .containsEntry("previousActive", "true")
            .containsEntry("changedFields", "active");
    }

    @Test
    void testShouldRecordSkipsSuccessfulNoOpResults() {
        assertThat(scimUserAuditMapper.shouldRecord(createSuccessInvocation(Optional.empty()))).isFalse();
        assertThat(scimUserAuditMapper.shouldRecord(createSuccessInvocation(Boolean.FALSE))).isFalse();
    }

    @Test
    void testShouldRecordAcceptsSuccessfulResults() {
        User user = createUser("john.doe", "john@example.com", "John", "Doe", true, List.of());

        assertThat(scimUserAuditMapper.shouldRecord(createSuccessInvocation(Optional.of(user)))).isTrue();
        assertThat(scimUserAuditMapper.shouldRecord(createSuccessInvocation(user))).isTrue();
        assertThat(scimUserAuditMapper.shouldRecord(createSuccessInvocation(Boolean.TRUE))).isTrue();
    }

    @Test
    void testShouldRecordAlwaysRecordsFailures() {
        AuditInvocation errorAuditInvocation = new AuditInvocation(
            ScimUserAuditEvents.SCIM_USER_UPDATED, Map.of("id", USER_ID), null, null, AuditOutcome.ERROR,
            IllegalStateException.class);
        AuditInvocation deniedAuditInvocation = new AuditInvocation(
            ScimUserAuditEvents.SCIM_USER_UPDATED, Map.of("id", USER_ID), null, null, AuditOutcome.DENIED, null);

        assertThat(scimUserAuditMapper.shouldRecord(errorAuditInvocation)).isTrue();
        assertThat(scimUserAuditMapper.shouldRecord(deniedAuditInvocation)).isTrue();
    }

    private static AuditInvocation createAuditInvocation(
        String event, Map<String, Object> arguments, Object result, Object captured) {

        return new AuditInvocation(event, new HashMap<>(arguments), result, captured, AuditOutcome.SUCCESS, null);
    }

    private static AuditInvocation createSuccessInvocation(Object result) {
        return createAuditInvocation(ScimUserAuditEvents.SCIM_USER_UPDATED, Map.of("id", USER_ID), result, null);
    }

    private static ScimUser createScimUser(String userName, String externalId) {
        return new ScimUser(
            List.of(ScimUser.SCHEMA), null, externalId, userName, null, null, List.of(), true, List.of(), null);
    }

    private static User createUser(
        String login, String email, String firstName, String lastName, boolean activated, List<Long> authorityIds) {

        User user = new User();

        user.setActivated(activated);
        user.setAuthorityIds(authorityIds);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setId(USER_ID);
        user.setLastName(lastName);
        user.setLogin(login);

        return user;
    }
}
