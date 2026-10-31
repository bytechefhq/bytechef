/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.ee.automation.configuration.config.WorkspacePermissionIntTestConfiguration;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditOutcome;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Runs {@link WorkspaceUserAuditMapper} over real {@code workspace_user} rows, so the previous role it captures is
 * read from the database.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = WorkspacePermissionIntTestConfiguration.class, properties = "bytechef.edition=ee")
class WorkspaceUserAuditMapperIntTest {

    private static final long USER_ID = 2L;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceUserAuditMapper workspaceUserAuditMapper;

    @Autowired
    private WorkspaceUserRepository workspaceUserRepository;

    private long workspaceId;

    @BeforeEach
    void beforeEach() {
        Workspace workspace = workspaceRepository.save(new Workspace("Workspace User Audit " + UUID.randomUUID()));

        workspaceId = workspace.getId();
    }

    @Test
    void testAddedRecordsWorkspaceIdUserIdAndRole() {
        Map<String, String> data = workspaceUserAuditMapper.map(invocation(
            WorkspaceUserAuditEvents.WORKSPACE_USER_ADDED,
            arguments("userId", USER_ID, "workspaceId", workspaceId, "workspaceRole", WorkspaceRole.EDITOR), null,
            null));

        assertThat(data).containsExactlyInAnyOrderEntriesOf(
            Map.of("workspaceId", String.valueOf(workspaceId), "userId", "2", "role", "EDITOR"));
    }

    @Test
    void testAddedCaptureReadsNothing() {
        // A membership row exists, so a capture that read the repository would return its role.
        workspaceUserRepository.save(WorkspaceUser.forRole(USER_ID, workspaceId, WorkspaceRole.VIEWER));

        Object captured = workspaceUserAuditMapper.capture(invocation(
            WorkspaceUserAuditEvents.WORKSPACE_USER_ADDED,
            arguments("userId", USER_ID, "workspaceId", workspaceId, "workspaceRole", WorkspaceRole.EDITOR), null,
            null));

        assertThat(captured).isNull();
    }

    @Test
    void testRoleUpdatedCapturesThePreviousRoleAndMapsTheNewRole() {
        workspaceUserRepository.save(WorkspaceUser.forRole(USER_ID, workspaceId, WorkspaceRole.VIEWER));

        Map<String, Object> arguments = arguments(
            "userId", USER_ID, "workspaceId", workspaceId, "workspaceRole", WorkspaceRole.ADMIN);

        Object captured = workspaceUserAuditMapper.capture(
            invocation(WorkspaceUserAuditEvents.WORKSPACE_USER_ROLE_UPDATED, arguments, null, null));

        Map<String, String> data = workspaceUserAuditMapper.map(
            invocation(WorkspaceUserAuditEvents.WORKSPACE_USER_ROLE_UPDATED, arguments, null, captured));

        assertThat(data).containsExactlyInAnyOrderEntriesOf(
            Map.of(
                "workspaceId", String.valueOf(workspaceId), "userId", "2", "previousRole", "VIEWER", "role",
                "ADMIN"));
    }

    @Test
    void testRemovedCapturesThePreviousRole() {
        workspaceUserRepository.save(WorkspaceUser.forRole(USER_ID, workspaceId, WorkspaceRole.EDITOR));

        Map<String, Object> arguments = arguments("userId", USER_ID, "workspaceId", workspaceId);

        Object captured = workspaceUserAuditMapper.capture(
            invocation(WorkspaceUserAuditEvents.WORKSPACE_USER_REMOVED, arguments, null, null));

        Map<String, String> data = workspaceUserAuditMapper.map(
            invocation(WorkspaceUserAuditEvents.WORKSPACE_USER_REMOVED, arguments, true, captured));

        assertThat(data).containsEntry("previousRole", "EDITOR")
            .containsEntry("workspaceId", String.valueOf(workspaceId))
            .containsEntry("userId", "2");
    }

    @Test
    void testCustomRolePreviousRoleIsDescribedByItsId() {
        workspaceUserRepository.save(WorkspaceUser.forCustomRole(USER_ID, workspaceId, 5L));

        Map<String, Object> arguments = arguments(
            "userId", USER_ID, "workspaceId", workspaceId, "workspaceRole", WorkspaceRole.EDITOR);

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
            arguments("userId", USER_ID, "workspaceId", workspaceId, "workspaceRole", WorkspaceRole.ADMIN), null,
            null, AuditOutcome.DENIED, null));

        assertThat(data).containsEntry("workspaceId", String.valueOf(workspaceId))
            .containsEntry("userId", "2")
            .containsEntry("role", "ADMIN")
            .doesNotContainKey("previousRole");
    }

    @Test
    void testRenamedArgumentFailsMappingInsteadOfRecordingNull() {
        AuditInvocation auditInvocation = invocation(
            WorkspaceUserAuditEvents.WORKSPACE_USER_REMOVED,
            arguments("memberId", USER_ID, "workspaceId", workspaceId),
            true, null);

        assertThatThrownBy(() -> workspaceUserAuditMapper.map(auditInvocation))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("userId");
    }

    @Test
    void testRenamedArgumentFailsCaptureInsteadOfSkippingIt() {
        workspaceUserRepository.save(WorkspaceUser.forRole(USER_ID, workspaceId, WorkspaceRole.VIEWER));

        AuditInvocation auditInvocation = invocation(
            WorkspaceUserAuditEvents.WORKSPACE_USER_REMOVED,
            arguments("memberId", USER_ID, "workspaceId", workspaceId),
            null, null);

        assertThatThrownBy(() -> workspaceUserAuditMapper.capture(auditInvocation))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("userId");
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
