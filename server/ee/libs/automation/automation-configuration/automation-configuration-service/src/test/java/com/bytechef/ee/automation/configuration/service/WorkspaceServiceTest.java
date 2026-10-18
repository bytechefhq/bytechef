/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    private static final long WORKSPACE_ID = 5L;

    @Mock
    private PermissionService permissionService;

    @Mock
    private UserService userService;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceUserRepository workspaceUserRepository;

    @InjectMocks
    private WorkspaceServiceImpl workspaceService;

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateMakesTheCreatorAWorkspaceWideAdmin() {
        User creator = new User();

        creator.setId(7L);
        creator.setLogin("creator");

        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken("creator", null, List.of()));

        when(workspaceRepository.save(any(Workspace.class))).thenReturn(new Workspace(WORKSPACE_ID, "workspace"));
        when(userService.getUser("creator")).thenReturn(creator);

        workspaceService.create(new Workspace("workspace"));

        ArgumentCaptor<WorkspaceUser> workspaceUserArgumentCaptor = ArgumentCaptor.forClass(WorkspaceUser.class);

        verify(workspaceUserRepository).save(workspaceUserArgumentCaptor.capture());

        WorkspaceUser workspaceUser = workspaceUserArgumentCaptor.getValue();

        assertThat(workspaceUser.getUserId()).isEqualTo(7L);
        assertThat(workspaceUser.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(workspaceUser.getWorkspaceRole()).isEqualTo(WorkspaceRole.ADMIN.ordinal());
        assertThat(workspaceUser.getEnvironment()).isNull();
    }

    @Test
    void testCreateWithoutAnAuthenticatedCreatorSeedsNoMembership() {
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(new Workspace(WORKSPACE_ID, "workspace"));

        Workspace workspace = workspaceService.create(new Workspace("workspace"));

        assertThat(workspace.getId()).isEqualTo(WORKSPACE_ID);

        verify(workspaceUserRepository, never()).save(any(WorkspaceUser.class));
    }

    @Test
    void testDeleteOfAnEmptyWorkspaceDeletesTheWorkspace() {
        when(workspaceUserRepository.findAllByWorkspaceId(WORKSPACE_ID)).thenReturn(List.of());

        workspaceService.delete(WORKSPACE_ID);

        verify(workspaceRepository).deleteById(WORKSPACE_ID);
    }
}
