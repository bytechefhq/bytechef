/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditEvent;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditPublisher;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Deleting a workspace ends every membership in it, so it must leave the same audit trail the per-member removals do.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    private static final long WORKSPACE_ID = 5L;
    private static final long FIRST_USER_ID = 11L;
    private static final long SECOND_USER_ID = 12L;

    @Mock
    private PermissionService permissionService;

    @Mock
    private UserService userService;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceUserAuditPublisher workspaceUserAuditPublisher;

    @Mock
    private WorkspaceUserRepository workspaceUserRepository;

    @InjectMocks
    private WorkspaceServiceImpl workspaceService;

    @Captor
    private ArgumentCaptor<Map<String, Object>> dataArgumentCaptor;

    @Test
    void testDeleteAuditsEveryMembershipItRemoves() {
        when(workspaceUserRepository.findAllByWorkspaceId(WORKSPACE_ID)).thenReturn(
            List.of(
                WorkspaceUser.forRole(FIRST_USER_ID, WORKSPACE_ID, WorkspaceRole.ADMIN),
                WorkspaceUser.forRole(SECOND_USER_ID, WORKSPACE_ID, WorkspaceRole.EDITOR)));

        workspaceService.delete(WORKSPACE_ID);

        verify(workspaceUserAuditPublisher, times(2)).publish(
            eq(WorkspaceUserAuditEvent.WORKSPACE_USER_REMOVED), dataArgumentCaptor.capture());

        // Same payload shape removeWorkspaceUser publishes, so the two routes out of a workspace are indistinguishable
        // to anything reading the trail.
        assertThat(dataArgumentCaptor.getAllValues()).containsExactly(
            Map.of("workspaceId", String.valueOf(WORKSPACE_ID), "userId", String.valueOf(FIRST_USER_ID)),
            Map.of("workspaceId", String.valueOf(WORKSPACE_ID), "userId", String.valueOf(SECOND_USER_ID)));
    }

    @Test
    void testDeleteOfAnEmptyWorkspacePublishesNothing() {
        when(workspaceUserRepository.findAllByWorkspaceId(WORKSPACE_ID)).thenReturn(List.of());

        workspaceService.delete(WORKSPACE_ID);

        verify(workspaceRepository).deleteById(WORKSPACE_ID);
        verify(workspaceUserAuditPublisher, never()).publish(any(), any());
    }
}
