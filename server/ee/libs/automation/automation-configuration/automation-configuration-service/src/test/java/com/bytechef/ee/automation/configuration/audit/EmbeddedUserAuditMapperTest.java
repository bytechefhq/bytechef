/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.configuration.domain.EmbeddedUser;
import com.bytechef.ee.automation.configuration.repository.EmbeddedUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditOutcome;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedUserAuditMapperTest {

    private static final long USER_ID = 2L;

    private EmbeddedUserAuditMapper embeddedUserAuditMapper;
    private EmbeddedUserRepository embeddedUserRepository;

    @BeforeEach
    void beforeEach() {
        embeddedUserRepository = mock(EmbeddedUserRepository.class);
        embeddedUserAuditMapper = new EmbeddedUserAuditMapper(embeddedUserRepository);
    }

    @Test
    void testEventsAreExactlyTheFiveEmbeddedUserEvents() {
        assertThat(embeddedUserAuditMapper.events()).isEqualTo(
            Set.of(
                "EMBEDDED_USER_ADDED", "EMBEDDED_USER_ENVIRONMENT_ROLE_REMOVED",
                "EMBEDDED_USER_ENVIRONMENT_ROLE_UPDATED", "EMBEDDED_USER_REMOVED", "EMBEDDED_USER_ROLE_UPDATED"));
    }

    @Test
    void testAddedRecordsUserAndRole() {
        Map<String, Object> arguments = arguments("userId", USER_ID, "embeddedRole", WorkspaceRole.EDITOR);

        Object captured = embeddedUserAuditMapper.capture(
            invocation(EmbeddedUserAuditEvents.EMBEDDED_USER_ADDED, arguments, null));

        assertThat(captured).isNull();
        assertThat(
            embeddedUserAuditMapper.map(invocation(EmbeddedUserAuditEvents.EMBEDDED_USER_ADDED, arguments, null)))
                .containsExactlyInAnyOrderEntriesOf(Map.of("userId", "2", "role", "EDITOR"));

        verifyNoInteractions(embeddedUserRepository);
    }

    @Test
    void testAddedByEmailRecordsTheResolvedUserFromTheResult() {
        Map<String, Object> arguments = arguments("email", "jane@example.com", "embeddedRole", WorkspaceRole.EDITOR);

        AuditInvocation auditInvocation = new AuditInvocation(
            EmbeddedUserAuditEvents.EMBEDDED_USER_ADDED, arguments, EmbeddedUser.forRole(USER_ID, WorkspaceRole.EDITOR),
            null, AuditOutcome.SUCCESS, null);

        assertThat(embeddedUserAuditMapper.map(auditInvocation))
            .containsExactlyInAnyOrderEntriesOf(Map.of("userId", "2", "role", "EDITOR"));
    }

    @Test
    void testRoleUpdatedRecordsThePreviousRole() {
        when(embeddedUserRepository.findByUserIdAndEnvironmentIsNull(USER_ID))
            .thenReturn(Optional.of(EmbeddedUser.forRole(USER_ID, WorkspaceRole.VIEWER)));

        Map<String, Object> arguments = arguments("userId", USER_ID, "embeddedRole", WorkspaceRole.ADMIN);

        Object captured = embeddedUserAuditMapper.capture(
            invocation(EmbeddedUserAuditEvents.EMBEDDED_USER_ROLE_UPDATED, arguments, null));

        assertThat(
            embeddedUserAuditMapper.map(
                invocation(EmbeddedUserAuditEvents.EMBEDDED_USER_ROLE_UPDATED, arguments, captured)))
                    .containsExactlyInAnyOrderEntriesOf(
                        Map.of("userId", "2", "role", "ADMIN", "previousRole", "VIEWER"));
    }

    @Test
    void testEnvironmentRoleUpdatedFallsBackToTheEmbeddedWideRole() {
        when(embeddedUserRepository.findByUserIdAndEnvironment(USER_ID, Environment.STAGING.ordinal()))
            .thenReturn(Optional.empty());
        when(embeddedUserRepository.findByUserIdAndEnvironmentIsNull(USER_ID))
            .thenReturn(Optional.of(EmbeddedUser.forRole(USER_ID, WorkspaceRole.VIEWER)));

        Map<String, Object> arguments = arguments(
            "userId", USER_ID, "environment", Environment.STAGING, "embeddedRole", WorkspaceRole.EDITOR);

        Object captured = embeddedUserAuditMapper.capture(
            invocation(EmbeddedUserAuditEvents.EMBEDDED_USER_ENVIRONMENT_ROLE_UPDATED, arguments, null));

        assertThat(
            embeddedUserAuditMapper.map(
                invocation(EmbeddedUserAuditEvents.EMBEDDED_USER_ENVIRONMENT_ROLE_UPDATED, arguments, captured)))
                    .containsExactlyInAnyOrderEntriesOf(
                        Map.of("userId", "2", "environment", "STAGING", "role", "EDITOR", "previousRole", "VIEWER"));
    }

    @Test
    void testRemovedCapturesEveryRow() {
        when(embeddedUserRepository.findAllByUserId(USER_ID))
            .thenReturn(List.of(
                EmbeddedUser.forRole(USER_ID, WorkspaceRole.EDITOR, Environment.DEVELOPMENT),
                EmbeddedUser.forCustomRole(USER_ID, 5L, Environment.PRODUCTION)));

        Map<String, Object> arguments = arguments("userId", USER_ID);

        Object captured = embeddedUserAuditMapper.capture(
            invocation(EmbeddedUserAuditEvents.EMBEDDED_USER_REMOVED, arguments, null));

        assertThat(
            embeddedUserAuditMapper.map(invocation(EmbeddedUserAuditEvents.EMBEDDED_USER_REMOVED, arguments, captured)))
                .containsEntry("previousRole", "DEVELOPMENT:EDITOR,PRODUCTION:customRole:5");
    }

    @Test
    void testEnvironmentRoleRemovedRecordsWideningOnlyWhenItHappened() {
        EmbeddedUser lastRow = EmbeddedUser.forRole(USER_ID, WorkspaceRole.EDITOR, Environment.PRODUCTION);

        when(embeddedUserRepository.findByUserIdAndEnvironment(USER_ID, Environment.PRODUCTION.ordinal()))
            .thenReturn(Optional.of(lastRow));
        when(embeddedUserRepository.findAllByUserId(USER_ID)).thenReturn(List.of(lastRow));

        Map<String, Object> arguments = arguments("userId", USER_ID, "environment", Environment.PRODUCTION);

        Object captured = embeddedUserAuditMapper.capture(
            invocation(EmbeddedUserAuditEvents.EMBEDDED_USER_ENVIRONMENT_ROLE_REMOVED, arguments, null));

        assertThat(
            embeddedUserAuditMapper.map(
                invocation(EmbeddedUserAuditEvents.EMBEDDED_USER_ENVIRONMENT_ROLE_REMOVED, arguments, captured)))
                    .containsExactlyInAnyOrderEntriesOf(
                        Map.of(
                            "userId", "2", "environment", "PRODUCTION", "previousRole", "EDITOR",
                            "widenedToEmbeddedWide", "true"));

        EmbeddedUser developmentRow = EmbeddedUser.forRole(USER_ID, WorkspaceRole.EDITOR, Environment.DEVELOPMENT);

        when(embeddedUserRepository.findAllByUserId(USER_ID)).thenReturn(List.of(lastRow, developmentRow));

        Object capturedWithSibling = embeddedUserAuditMapper.capture(
            invocation(EmbeddedUserAuditEvents.EMBEDDED_USER_ENVIRONMENT_ROLE_REMOVED, arguments, null));

        assertThat(
            embeddedUserAuditMapper.map(
                invocation(
                    EmbeddedUserAuditEvents.EMBEDDED_USER_ENVIRONMENT_ROLE_REMOVED, arguments, capturedWithSibling)))
                        .containsEntry("widenedToEmbeddedWide", "false");
    }

    private static AuditInvocation invocation(String event, Map<String, Object> arguments, Object captured) {
        return new AuditInvocation(event, arguments, null, captured, AuditOutcome.SUCCESS, null);
    }

    private static Map<String, Object> arguments(Object... namesAndValues) {
        Map<String, Object> arguments = new HashMap<>();

        for (int index = 0; index < namesAndValues.length; index += 2) {
            arguments.put((String) namesAndValues[index], namesAndValues[index + 1]);
        }

        return arguments;
    }
}
