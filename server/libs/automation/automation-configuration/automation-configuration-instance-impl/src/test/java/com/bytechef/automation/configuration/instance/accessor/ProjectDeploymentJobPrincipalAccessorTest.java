/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.configuration.instance.accessor;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflowConnection;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.service.ConnectionService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ProjectDeploymentJobPrincipalAccessorTest {

    @Mock
    private ConnectionService connectionService;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    private ProjectDeploymentJobPrincipalAccessor projectDeploymentJobPrincipalAccessor;

    @BeforeEach
    void setUp() {
        projectDeploymentJobPrincipalAccessor = new ProjectDeploymentJobPrincipalAccessor(
            connectionService, projectDeploymentService, projectDeploymentWorkflowService, projectWorkflowService);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setConnections(
            List.of(new ProjectDeploymentWorkflowConnection(10L, "connection", "node_1")));

        when(projectWorkflowService.getProjectWorkflowWorkflowId(1L, "workflow-uuid")).thenReturn("workflow-id");
        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(1L, "workflow-id"))
            .thenReturn(projectDeploymentWorkflow);
    }

    @Test
    void testValidateConnectionsForJobPassesWhenEveryConnectionIsActive() {
        when(connectionService.getInactiveConnections(List.of(10L))).thenReturn(List.of());

        projectDeploymentJobPrincipalAccessor.validateConnectionsForJob(1L, "workflow-uuid");

        verify(connectionService, never()).validateConnectionsActive(anyList());
    }

    @Test
    void testValidateConnectionsForJobRejectsAnInactiveConnection() {
        ConfigurationException inactiveConnections = new ConfigurationException(
            "inactive", ConnectionErrorType.CONNECTION_NOT_ACTIVE);

        when(connectionService.getInactiveConnections(List.of(10L))).thenReturn(List.of(new Connection()));
        doThrow(inactiveConnections).when(connectionService)
            .validateConnectionsActive(List.of(10L));

        assertThatThrownBy(() -> projectDeploymentJobPrincipalAccessor.validateConnectionsForJob(1L, "workflow-uuid"))
            .isSameAs(inactiveConnections);
    }
}
