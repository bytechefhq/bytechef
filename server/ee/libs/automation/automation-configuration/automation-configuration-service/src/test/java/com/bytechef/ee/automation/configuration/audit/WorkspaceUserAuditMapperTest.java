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
import java.util.HashMap;
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
    void testAddedRecordsWorkspaceIdUserIdAndRole() {
        Map<String, String> data = workspaceUserAuditMapper.map(invocation(
            WorkspaceUserAuditEvents.WORKSPACE_USER_ADDED,
            arguments("userId", USER_ID, "workspaceId", WORKSPACE_ID, "workspaceRole", WorkspaceRole.EDITOR), null,
            null));

        assertThat(data).containsExactlyInAnyOrderEntriesOf(
            Map.of("workspaceId", "1", "userId", "2", "role", "EDITOR"));
    }

    @Test
    void testAddedCaptureReadsNothing() {
        Object captured = workspaceUserAuditMapper.capture(invocation(
            WorkspaceUserAuditEvents.WORKSPACE_USER_ADDED,
            arguments("userId", USER_ID, "workspaceId", WORKSPACE_ID, "workspaceRole", WorkspaceRole.EDITOR), null,
            null));

        assertThat(captured).isNull();
        verifyNoInteractions(workspaceUserRepository);
    }

    @Test
    void testRoleUpdatedCapturesThePreviousRoleAndMapsTheNewRole() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
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
    void testRemovedCapturesThePreviousRole() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(WorkspaceUser.forRole(USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR)));

        Map<String, Object> arguments = arguments("userId", USER_ID, "workspaceId", WORKSPACE_ID);

        Object captured = workspaceUserAuditMapper.capture(
            invocation(WorkspaceUserAuditEvents.WORKSPACE_USER_REMOVED, arguments, null, null));

        Map<String, String> data = workspaceUserAuditMapper.map(
            invocation(WorkspaceUserAuditEvents.WORKSPACE_USER_REMOVED, arguments, true, captured));

        assertThat(data).containsEntry("previousRole", "EDITOR")
            .containsEntry("workspaceId", "1")
            .containsEntry("userId", "2");
    }

    @Test
    void testCustomRolePreviousRoleIsDescribedByItsId() {
        when(workspaceUserRepository.findByUserIdAndWorkspaceId(USER_ID, WORKSPACE_ID))
            .thenReturn(Optional.of(WorkspaceUser.forCustomRole(USER_ID, WORKSPACE_ID, 5L)));

        Map<String, Object> arguments = arguments(
            "userId", USER_ID, "workspaceId", WORKSPACE_ID, "workspaceRole", WorkspaceRole.EDITOR);

        Object captured = workspaceUserAuditMapper.capture(
            invocation(WorkspaceUserAuditEvents.WORKSPACE_USER_ROLE_UPDATED, arguments, null, null));

        Map<String, String> data = workspaceUserAuditMapper.map(
            invocation(WorkspaceUserAuditEvents.WORKSPACE_USER_ROLE_UPDATED, arguments, null, captured));

        assertThat(data).containsEntry("previousRole", "customRole:5");
    }

    @Test
    void testDeniedInvocationMapsWithoutErrorAndRecordsNoPreviousRole() {
        Map<String, String> data = workspaceUserAuditMapper.map(new AuditInvocation(
            WorkspaceUserAuditEvents.WORKSPACE_USER_ROLE_UPDATED,
            arguments("userId", USER_ID, "workspaceId", WORKSPACE_ID, "workspaceRole", WorkspaceRole.ADMIN), null,
            null, AuditOutcome.DENIED, null));

        assertThat(data).containsEntry("workspaceId", "1")
            .containsEntry("userId", "2")
            .containsEntry("role", "ADMIN")
            .doesNotContainKey("previousRole");
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
