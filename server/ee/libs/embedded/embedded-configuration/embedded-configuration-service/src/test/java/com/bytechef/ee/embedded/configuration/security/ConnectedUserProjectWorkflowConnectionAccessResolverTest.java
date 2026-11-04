/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.ConnectedUserProject;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserProjectService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.security.WorkflowConnectionAccessResolver.ConnectionAccess;
import java.util.List;
import java.util.Optional;
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
class ConnectedUserProjectWorkflowConnectionAccessResolverTest {

    private static final long CONNECTED_USER_ID = 84001L;
    private static final long PROJECT_ID = 84002L;
    private static final String WORKFLOW_ID = "workflow-of-a-removed-connected-user";

    @Mock
    private ConnectedUserConnectionMembership connectedUserConnectionMembership;

    @Mock
    private ConnectedUserProjectService connectedUserProjectService;

    @InjectMocks
    private ConnectedUserProjectWorkflowConnectionAccessResolver connectedUserProjectWorkflowConnectionAccessResolver;

    @Mock
    private ConnectedUserService connectedUserService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Test
    void testResolveConnectionAccessDeniesWhenTheConnectedUserNoLongerExists() {
        ConnectedUserProject connectedUserProject = new ConnectedUserProject();

        connectedUserProject.setConnectedUserId(CONNECTED_USER_ID);
        connectedUserProject.setProjectId(PROJECT_ID);

        when(projectWorkflowService.getWorkflowProjectWorkflows(List.of(WORKFLOW_ID)))
            .thenReturn(List.of(new ProjectWorkflow(PROJECT_ID, 1, WORKFLOW_ID)));
        when(connectedUserProjectService.fetchProjectConnectedUserProject(PROJECT_ID))
            .thenReturn(Optional.of(connectedUserProject));
        when(connectedUserService.fetchConnectedUser(CONNECTED_USER_ID))
            .thenReturn(Optional.empty());

        assertThat(connectedUserProjectWorkflowConnectionAccessResolver.resolveConnectionAccess(WORKFLOW_ID, 1L, 0L))
            .isEqualTo(ConnectionAccess.DENIED);

        verify(connectedUserConnectionMembership, never()).getConnectionIds(anyLong(), any());
    }
}
