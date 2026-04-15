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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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
import com.bytechef.platform.connection.audit.ConnectionAuditEvent;
import com.bytechef.platform.connection.audit.ConnectionAuditPublisher;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.service.ConnectionService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ProjectDeploymentJobPrincipalAccessorTest {

    @Mock
    private ConnectionAuditPublisher connectionAuditPublisher;

    @Mock
    private ConnectionService connectionService;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private ProjectDeploymentWorkflowService projectDeploymentWorkflowService;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Test
    void testValidateConnectionsForJobWhenAllActiveDoesNothing() {
        ProjectDeploymentJobPrincipalAccessor accessor = new ProjectDeploymentJobPrincipalAccessor(
            connectionAuditPublisher, connectionService, projectDeploymentService, projectDeploymentWorkflowService,
            projectWorkflowService);

        long jobPrincipalId = 1L;
        String workflowUuid = "uuid-abc";
        String workflowId = "wf-1";

        when(projectWorkflowService.getProjectWorkflowWorkflowId(jobPrincipalId, workflowUuid))
            .thenReturn(workflowId);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setConnections(List.of(
            new ProjectDeploymentWorkflowConnection(100L, "main", "nodeA"),
            new ProjectDeploymentWorkflowConnection(101L, "aux", "nodeB")));

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(jobPrincipalId, workflowId))
            .thenReturn(projectDeploymentWorkflow);

        when(connectionService.getInactiveConnections(anyList())).thenReturn(List.of());

        accessor.validateConnectionsForJob(jobPrincipalId, workflowUuid);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);

        verify(connectionService).getInactiveConnections(captor.capture());

        assertThat(captor.getValue()).containsExactlyInAnyOrder(100L, 101L);

        verify(connectionAuditPublisher, never()).publish(any(), any(Long.class), any());
        verify(connectionService, never()).validateConnectionsActive(anyList());
    }

    @Test
    void testValidateConnectionsForJobWhenInactivePublishesAuditAndThrows() {
        ProjectDeploymentJobPrincipalAccessor accessor = new ProjectDeploymentJobPrincipalAccessor(
            connectionAuditPublisher, connectionService, projectDeploymentService, projectDeploymentWorkflowService,
            projectWorkflowService);

        long jobPrincipalId = 1L;
        String workflowUuid = "uuid-abc";
        String workflowId = "wf-1";

        when(projectWorkflowService.getProjectWorkflowWorkflowId(jobPrincipalId, workflowUuid))
            .thenReturn(workflowId);

        ProjectDeploymentWorkflow projectDeploymentWorkflow = new ProjectDeploymentWorkflow();

        projectDeploymentWorkflow.setConnections(List.of(
            new ProjectDeploymentWorkflowConnection(100L, "main", "nodeA"),
            new ProjectDeploymentWorkflowConnection(101L, "aux", "nodeB")));

        when(projectDeploymentWorkflowService.getProjectDeploymentWorkflow(jobPrincipalId, workflowId))
            .thenReturn(projectDeploymentWorkflow);

        Connection inactiveConnection = new Connection();

        inactiveConnection.setId(100L);
        inactiveConnection.setStatus(ConnectionStatus.PENDING_REASSIGNMENT);

        when(connectionService.getInactiveConnections(anyList())).thenReturn(List.of(inactiveConnection));

        doThrow(new ConfigurationException("blocked", ConnectionErrorType.CONNECTION_NOT_ACTIVE))
            .when(connectionService)
            .validateConnectionsActive(anyList());

        assertThatThrownBy(() -> accessor.validateConnectionsForJob(jobPrincipalId, workflowUuid))
            .isInstanceOf(ConfigurationException.class);

        verify(connectionAuditPublisher).publish(
            eq(ConnectionAuditEvent.WORKFLOW_PAUSED), eq(100L),
            eq(Map.of(
                "projectDeploymentId", jobPrincipalId,
                "workflowId", workflowId,
                "connectionStatus", "PENDING_REASSIGNMENT")));
    }
}
