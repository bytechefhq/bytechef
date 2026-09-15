/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditOutcome;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkspaceUserAuditMapperTest {

    private static final long USER_ID = 2L;
    private static final long WORKSPACE_ID = 1L;

    private WorkspaceUserAuditMapper workspaceUserAuditMapper;
    private WorkspaceUserRepository workspaceUserRepository;

    @BeforeEach
    void beforeEach() {
        workspaceUserRepository = mock(WorkspaceUserRepository.class);
        workspaceUserAuditMapper = new WorkspaceUserAuditMapper(workspaceUserRepository);
    }

    @Test
    void testAddedRecordsBuiltInRole() {
        Map<String, String> data = workspaceUserAuditMapper.map(invocation(
            WorkspaceUserAuditEvents.WORKSPACE_USER_ADDED,
            arguments("userId", USER_ID, "workspaceId", WORKSPACE_ID, "workspaceRole", WorkspaceRole.EDITOR,
                "customRoleId", null),
            null, null));

        assertThat(data).containsExactlyInAnyOrderEntriesOf(
            Map.of("workspaceId", "1", "userId", "2", "role", "EDITOR"));
    }

    @Test
    void testInviteRecordsUserIdFromTheResultAndNeverTheEmail() {
        WorkspaceUser workspaceUser = WorkspaceUser.forCustomRole(9L, WORKSPACE_ID, 5L);

        Map<String, String> data = workspaceUserAuditMapper.map(invocation(
            WorkspaceUserAuditEvents.WORKSPACE_USER_ADDED,
            arguments("workspaceId", WORKSPACE_ID, "email", "someone@example.com", "workspaceRole", null,
                "customRoleId", 5L),
            workspaceUser, null));

        assertThat(data).containsExactlyInAnyOrderEntriesOf(
            Map.of("workspaceId", "1", "userId", "9", "role", "customRole:5", "invited", "true"));
        assertThat(data.values()).noneMatch(value -> value.contains("@"));
    }

    @Test
    void testDeniedInviteRecordsNoUserId() {
        Map<String, String> data = workspaceUserAuditMapper.map(new AuditInvocation(
            WorkspaceUserAuditEvents.WORKSPACE_USER_ADDED,
            arguments("workspaceId", WORKSPACE_ID, "email", "someone@example.com", "workspaceRole",
                WorkspaceRole.VIEWER),
            null, null, AuditOutcome.DENIED, null));

        assertThat(data).containsEntry("invited", "true")
            .doesNotContainKey("userId");
    }

    @Test
    void testRoleUpdatedCapturesThePreviousWorkspaceWideRole() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER)));

        Map<String, Object> arguments = arguments(
            "userId", USER_ID, "workspaceId", WORKSPACE_ID, "workspaceRole", WorkspaceRole.ADMIN);

        Object captured = workspaceUserAuditMapper.capture(
            invocation(WorkspaceUserAuditEvents.WORKSPACE_USER_ROLE_UPDATED, arguments, null, null));

        Map<String, String> data = workspaceUserAuditMapper.map(
            invocation(WorkspaceUserAuditEvents.WORKSPACE_USER_ROLE_UPDATED, arguments, null, captured));

        assertThat(data).containsExactlyInAnyOrderEntriesOf(
            Map.of("workspaceId", "1", "userId", "2", "previousRole", "VIEWER", "role", "ADMIN"));
    }

    @Test
    void testAssignCustomRoleRecordsTheCustomRole() {
        Map<String, String> data = workspaceUserAuditMapper.map(invocation(
            WorkspaceUserAuditEvents.WORKSPACE_USER_ROLE_UPDATED,
            arguments("userId", USER_ID, "workspaceId", WORKSPACE_ID, "customRoleId", 5L), null, "EDITOR"));

        assertThat(data).containsEntry("role", "customRole:5")
            .containsEntry("previousRole", "EDITOR");
    }

    @Test
    void testRemovedCapturesEveryRow() {
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(List.of(
                WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR, Environment.DEVELOPMENT),
                WorkspaceUser.forCustomRole(USER_ID, WORKSPACE_ID, 5L, Environment.PRODUCTION)));

        Map<String, Object> arguments = arguments("userId", USER_ID, "workspaceId", WORKSPACE_ID);

        Object captured = workspaceUserAuditMapper.capture(
            invocation(WorkspaceUserAuditEvents.WORKSPACE_USER_REMOVED, arguments, null, null));

        assertThat(workspaceUserAuditMapper.map(
            invocation(WorkspaceUserAuditEvents.WORKSPACE_USER_REMOVED, arguments, true, captured)))
                .containsEntry("previousRole", "DEVELOPMENT:EDITOR,PRODUCTION:customRole:5");
    }

    @Test
    void testEnvironmentRoleUpdatedFallsBackToTheWorkspaceWideRole() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            USER_ID, WORKSPACE_ID, Environment.STAGING.ordinal())).thenReturn(Optional.empty());
        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.VIEWER)));

        Map<String, Object> arguments = arguments(
            "userId", USER_ID, "workspaceId", WORKSPACE_ID, "environment", Environment.STAGING, "workspaceRole",
            WorkspaceRole.EDITOR, "customRoleId", null);

        Object captured = workspaceUserAuditMapper.capture(
            invocation(WorkspaceUserAuditEvents.WORKSPACE_USER_ENVIRONMENT_ROLE_UPDATED, arguments, null, null));

        assertThat(workspaceUserAuditMapper.map(invocation(
            WorkspaceUserAuditEvents.WORKSPACE_USER_ENVIRONMENT_ROLE_UPDATED, arguments, null, captured)))
                .containsExactlyInAnyOrderEntriesOf(
                    Map.of(
                        "workspaceId", "1", "userId", "2", "environment", "STAGING", "previousRole", "VIEWER", "role",
                        "EDITOR"));
    }

    @Test
    void testEnvironmentRoleRemovedRecordsWideningOfTheLastRow() {
        WorkspaceUser environmentWorkspaceUser = WorkspaceUser.forRole(
            USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR, Environment.PRODUCTION);

        when(workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            USER_ID, WORKSPACE_ID, Environment.PRODUCTION.ordinal())).thenReturn(Optional.of(environmentWorkspaceUser));
        when(workspaceUserRepository.findAllByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(List.of(environmentWorkspaceUser));

        Map<String, Object> arguments = arguments(
            "userId", USER_ID, "workspaceId", WORKSPACE_ID, "environment", Environment.PRODUCTION);

        Object captured = workspaceUserAuditMapper.capture(
            invocation(WorkspaceUserAuditEvents.WORKSPACE_USER_ENVIRONMENT_ROLE_REMOVED, arguments, null, null));

        assertThat(workspaceUserAuditMapper.map(invocation(
            WorkspaceUserAuditEvents.WORKSPACE_USER_ENVIRONMENT_ROLE_REMOVED, arguments, null, captured)))
                .containsExactlyInAnyOrderEntriesOf(
                    Map.of(
                        "workspaceId", "1", "userId", "2", "environment", "PRODUCTION", "previousRole", "EDITOR",
                        "widenedToWorkspaceWide", "true"));
    }

    @Test
    void testCaptureOfAddedReadsNothing() {
        assertThat(workspaceUserAuditMapper.capture(invocation(
            WorkspaceUserAuditEvents.WORKSPACE_USER_ADDED, arguments("userId", USER_ID, "workspaceId", WORKSPACE_ID),
            null, null))).isNull();

        verifyNoInteractions(workspaceUserRepository);
    }

    @Test
    void testRenamedArgumentFailsMappingInsteadOfRecordingNull() {
        AuditInvocation auditInvocation = invocation(
            WorkspaceUserAuditEvents.WORKSPACE_USER_REMOVED,
            arguments("memberId", USER_ID, "workspaceId", WORKSPACE_ID),
            true, null);

        assertThatThrownBy(() -> workspaceUserAuditMapper.map(auditInvocation))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("userId");
    }

    @Test
    void testRenamedArgumentFailsCaptureInsteadOfSkippingIt() {
        AuditInvocation auditInvocation = invocation(
            WorkspaceUserAuditEvents.WORKSPACE_USER_REMOVED,
            arguments("memberId", USER_ID, "workspaceId", WORKSPACE_ID),
            null, null);

        assertThatThrownBy(() -> workspaceUserAuditMapper.capture(auditInvocation))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("userId");

        verifyNoInteractions(workspaceUserRepository);
    }

    private static AuditInvocation invocation(
        String event, Map<String, Object> arguments, Object result, Object captured) {

        return new AuditInvocation(event, arguments, result, captured, AuditOutcome.SUCCESS, null);
    }

    private static Map<String, Object> arguments(Object... namesAndValues) {
        Map<String, Object> arguments = new HashMap<>();

        for (int index = 0; index < namesAndValues.length; index += 2) {
            arguments.put((String) namesAndValues[index], namesAndValues[index + 1]);
        }

        return arguments;
    }
}
