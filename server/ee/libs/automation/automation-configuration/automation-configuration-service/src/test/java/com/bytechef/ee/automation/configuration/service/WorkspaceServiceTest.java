/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Test
    void testDeleteOfAnEmptyWorkspaceDeletesTheWorkspace() {
        when(workspaceUserRepository.findAllByWorkspaceId(WORKSPACE_ID)).thenReturn(List.of());

        workspaceService.delete(WORKSPACE_ID);

        verify(workspaceRepository).deleteById(WORKSPACE_ID);
    }
}
