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
import com.bytechef.platform.configuration.domain.Environment;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Runs {@link WorkspaceUserAuditMapper} over real {@code workspace_user} rows, so the previous role it captures is read
 * from the database.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = WorkspacePermissionIntTestConfiguration.class,
    properties = {
        "bytechef.edition=ee", "spring.liquibase.contexts=mono,dev"
    })
class WorkspaceUserAuditMapperIntTest {

    private static final long USER_ID = 2L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void testAddedRecordsBuiltInRole() {
        Map<String, String> data = workspaceUserAuditMapper.map(invocation(
            WorkspaceUserAuditEvents.WORKSPACE_USER_ADDED,
            arguments("userId", USER_ID, "workspaceId", workspaceId, "workspaceRole", WorkspaceRole.EDITOR,
                "customRoleId", null),
            null, null));

        assertThat(data).containsExactlyInAnyOrderEntriesOf(
            Map.of("workspaceId", String.valueOf(workspaceId), "userId", "2", "role", "EDITOR"));
    }

    @Test
    void testInviteRecordsUserIdFromTheResultAndNeverTheEmail() {
        WorkspaceUser workspaceUser = WorkspaceUser.forCustomRole(9L, workspaceId, 5L);

        Map<String, String> data = workspaceUserAuditMapper.map(invocation(
            WorkspaceUserAuditEvents.WORKSPACE_USER_ADDED,
            arguments("workspaceId", workspaceId, "email", "someone@example.com", "workspaceRole", null,
                "customRoleId", 5L),
            workspaceUser, null));

        assertThat(data).containsExactlyInAnyOrderEntriesOf(
            Map.of(
                "workspaceId", String.valueOf(workspaceId), "userId", "9", "role", "customRole:5", "invited",
                "true"));
        assertThat(data.values()).noneMatch(value -> value.contains("@"));
    }

    @Test
    void testDeniedInviteRecordsNoUserId() {
        Map<String, String> data = workspaceUserAuditMapper.map(new AuditInvocation(
            WorkspaceUserAuditEvents.WORKSPACE_USER_ADDED,
            arguments("workspaceId", workspaceId, "email", "someone@example.com", "workspaceRole",
                WorkspaceRole.VIEWER),
            null, null, AuditOutcome.DENIED, null));

        assertThat(data).containsEntry("invited", "true")
            .doesNotContainKey("userId");
    }

    @Test
    void testRoleUpdatedCapturesThePreviousWorkspaceWideRole() {
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
    void testAssignCustomRoleRecordsTheCustomRole() {
        Map<String, String> data = workspaceUserAuditMapper.map(invocation(
            WorkspaceUserAuditEvents.WORKSPACE_USER_ROLE_UPDATED,
            arguments("userId", USER_ID, "workspaceId", workspaceId, "customRoleId", 5L), null, "EDITOR"));

        assertThat(data).containsEntry("role", "customRole:5")
            .containsEntry("previousRole", "EDITOR");
    }

    @Test
    void testRemovedCapturesEveryRow() {
        createCustomRole(5L);

        workspaceUserRepository.save(
            WorkspaceUser.forRole(USER_ID, workspaceId, WorkspaceRole.EDITOR, Environment.DEVELOPMENT));
        workspaceUserRepository.save(WorkspaceUser.forCustomRole(USER_ID, workspaceId, 5L, Environment.PRODUCTION));

        Map<String, Object> arguments = arguments("userId", USER_ID, "workspaceId", workspaceId);

        Object captured = workspaceUserAuditMapper.capture(
            invocation(WorkspaceUserAuditEvents.WORKSPACE_USER_REMOVED, arguments, null, null));

        assertThat(workspaceUserAuditMapper.map(
            invocation(WorkspaceUserAuditEvents.WORKSPACE_USER_REMOVED, arguments, true, captured)))
                .containsEntry("previousRole", "DEVELOPMENT:EDITOR,PRODUCTION:customRole:5");
    }

    @Test
    void testEnvironmentRoleUpdatedFallsBackToTheWorkspaceWideRole() {
        workspaceUserRepository.save(WorkspaceUser.forRole(USER_ID, workspaceId, WorkspaceRole.VIEWER));

        Map<String, Object> arguments = arguments(
            "userId", USER_ID, "workspaceId", workspaceId, "environment", Environment.STAGING, "workspaceRole",
            WorkspaceRole.EDITOR, "customRoleId", null);

        Object captured = workspaceUserAuditMapper.capture(
            invocation(WorkspaceUserAuditEvents.WORKSPACE_USER_ENVIRONMENT_ROLE_UPDATED, arguments, null, null));

        assertThat(workspaceUserAuditMapper.map(invocation(
            WorkspaceUserAuditEvents.WORKSPACE_USER_ENVIRONMENT_ROLE_UPDATED, arguments, null, captured)))
                .containsExactlyInAnyOrderEntriesOf(
                    Map.of(
                        "workspaceId", String.valueOf(workspaceId), "userId", "2", "environment", "STAGING",
                        "previousRole", "VIEWER", "role", "EDITOR"));
    }

    @Test
    void testEnvironmentRoleRemovedRecordsWideningOfTheLastRow() {
        workspaceUserRepository.save(
            WorkspaceUser.forRole(USER_ID, workspaceId, WorkspaceRole.EDITOR, Environment.PRODUCTION));

        Map<String, Object> arguments = arguments(
            "userId", USER_ID, "workspaceId", workspaceId, "environment", Environment.PRODUCTION);

        Object captured = workspaceUserAuditMapper.capture(
            invocation(WorkspaceUserAuditEvents.WORKSPACE_USER_ENVIRONMENT_ROLE_REMOVED, arguments, null, null));

        assertThat(workspaceUserAuditMapper.map(invocation(
            WorkspaceUserAuditEvents.WORKSPACE_USER_ENVIRONMENT_ROLE_REMOVED, arguments, null, captured)))
                .containsExactlyInAnyOrderEntriesOf(
                    Map.of(
                        "workspaceId", String.valueOf(workspaceId), "userId", "2", "environment", "PRODUCTION",
                        "previousRole", "EDITOR", "widenedToWorkspaceWide", "true"));
    }

    @Test
    void testCaptureOfAddedReadsNothing() {
        workspaceUserRepository.save(WorkspaceUser.forRole(USER_ID, workspaceId, WorkspaceRole.VIEWER));

        assertThat(workspaceUserAuditMapper.capture(invocation(
            WorkspaceUserAuditEvents.WORKSPACE_USER_ADDED, arguments("userId", USER_ID, "workspaceId", workspaceId),
            null, null))).isNull();
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

    private void createCustomRole(long customRoleId) {
        jdbcTemplate.update(
            "INSERT INTO custom_role (id, name, created_date, created_by, last_modified_date, last_modified_by, " +
                "version) VALUES (?, ?, CURRENT_TIMESTAMP, 'test', CURRENT_TIMESTAMP, 'test', 0) " +
                "ON CONFLICT (id) DO NOTHING",
            customRoleId, "Custom Role " + customRoleId);
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
