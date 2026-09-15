/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.audit.AuditInvocation;
import com.bytechef.platform.audit.AuditOutcome;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkspaceAuditMapperTest {

    private WorkspaceAuditMapper workspaceAuditMapper;
    private WorkspaceUserRepository workspaceUserRepository;

    @BeforeEach
    void beforeEach() {
        workspaceUserRepository = mock(WorkspaceUserRepository.class);
        workspaceAuditMapper = new WorkspaceAuditMapper(workspaceUserRepository);
    }

    @Test
    void testWorkspaceDeletedRecordsEachRemovedUserOnce() {
        when(workspaceUserRepository.findAllByWorkspaceId(4L)).thenReturn(List.of(
            WorkspaceUser.forRole(2L, 4L, WorkspaceRole.ADMIN, Environment.DEVELOPMENT),
            WorkspaceUser.forRole(2L, 4L, WorkspaceRole.VIEWER, Environment.PRODUCTION),
            WorkspaceUser.forRole(3L, 4L, WorkspaceRole.EDITOR)));

        Map<String, Object> arguments = Map.of("id", 4L);

        Object captured = workspaceAuditMapper.capture(
            invocation(WorkspaceAuditEvents.WORKSPACE_DELETED, arguments, null));

        assertThat(workspaceAuditMapper.map(invocation(WorkspaceAuditEvents.WORKSPACE_DELETED, arguments, captured)))
            .containsExactlyInAnyOrderEntriesOf(
                Map.of("workspaceId", "4", "removedUserIds", "2,3", "removedUserCount", "2"));
    }

    @Test
    void testMembershipsRemovedRecordsEachWorkspaceOnce() {
        when(workspaceUserRepository.findAllByUserId(2L)).thenReturn(List.of(
            WorkspaceUser.forRole(2L, 4L, WorkspaceRole.ADMIN, Environment.DEVELOPMENT),
            WorkspaceUser.forRole(2L, 4L, WorkspaceRole.VIEWER, Environment.PRODUCTION),
            WorkspaceUser.forRole(2L, 6L, WorkspaceRole.EDITOR)));

        Map<String, Object> arguments = Map.of("userId", 2L);

        Object captured = workspaceAuditMapper.capture(
            invocation(WorkspaceAuditEvents.WORKSPACE_MEMBERSHIPS_REMOVED, arguments, null));

        assertThat(workspaceAuditMapper.map(
            invocation(WorkspaceAuditEvents.WORKSPACE_MEMBERSHIPS_REMOVED, arguments, captured)))
                .containsExactlyInAnyOrderEntriesOf(
                    Map.of("userId", "2", "workspaceIds", "4,6", "workspaceCount", "2"));
    }

    @Test
    void testMembershipsRemovedOverTheLimitKeepsWholeIdsAndRecordsTheFullCount() {
        List<WorkspaceUser> workspaceUsers = new ArrayList<>();

        for (long workspaceId = 1000; workspaceId < 1100; workspaceId++) {
            workspaceUsers.add(WorkspaceUser.forRole(2L, workspaceId, WorkspaceRole.EDITOR));
        }

        when(workspaceUserRepository.findAllByUserId(2L)).thenReturn(workspaceUsers);

        Map<String, Object> arguments = Map.of("userId", 2L);

        Object captured = workspaceAuditMapper.capture(
            invocation(WorkspaceAuditEvents.WORKSPACE_MEMBERSHIPS_REMOVED, arguments, null));

        Map<String, String> data = workspaceAuditMapper.map(
            invocation(WorkspaceAuditEvents.WORKSPACE_MEMBERSHIPS_REMOVED, arguments, captured));

        assertThat(data.get("workspaceIds")).hasSizeLessThanOrEqualTo(256);
        assertThat(data.get("workspaceIds")
            .split(",")).allSatisfy(id -> assertThat(id).matches("\\d+"));
        assertThat(data).containsEntry("workspaceCount", "100");
    }

    @Test
    void testDeniedDeleteRecordsOnlyTheWorkspace() {
        assertThat(workspaceAuditMapper.map(invocation(WorkspaceAuditEvents.WORKSPACE_DELETED, Map.of("id", 4L), null)))
            .containsExactlyInAnyOrderEntriesOf(Map.of("workspaceId", "4"));
    }

    @Test
    void testShouldRecordIsFalseForANoOpMembershipsRemoved() {
        assertThat(workspaceAuditMapper.shouldRecord(
            invocation(WorkspaceAuditEvents.WORKSPACE_MEMBERSHIPS_REMOVED, Map.of("userId", 2L), List.of())))
                .isFalse();

        assertThat(workspaceAuditMapper.shouldRecord(
            invocation(WorkspaceAuditEvents.WORKSPACE_MEMBERSHIPS_REMOVED, Map.of("userId", 2L), null)))
                .isFalse();
    }

    @Test
    void testShouldRecordIsTrueForANonEmptyMembershipsRemoved() {
        assertThat(workspaceAuditMapper.shouldRecord(
            invocation(WorkspaceAuditEvents.WORKSPACE_MEMBERSHIPS_REMOVED, Map.of("userId", 2L), List.of(4L))))
                .isTrue();
    }

    @Test
    void testShouldRecordIsTrueForADeniedMembershipsRemovedEvenWithNothingCaptured() {
        assertThat(workspaceAuditMapper.shouldRecord(
            new AuditInvocation(
                WorkspaceAuditEvents.WORKSPACE_MEMBERSHIPS_REMOVED, Map.of("userId", 2L), null, null,
                AuditOutcome.DENIED, null))).isTrue();
    }

    @Test
    void testShouldRecordIsTrueForWorkspaceDeletedRegardlessOfCapturedMembers() {
        assertThat(workspaceAuditMapper.shouldRecord(
            invocation(WorkspaceAuditEvents.WORKSPACE_DELETED, Map.of("id", 4L), List.of())))
                .isTrue();
    }

    private static AuditInvocation invocation(String event, Map<String, Object> arguments, Object captured) {
        return new AuditInvocation(event, arguments, null, captured, AuditOutcome.SUCCESS, null);
    }
}
