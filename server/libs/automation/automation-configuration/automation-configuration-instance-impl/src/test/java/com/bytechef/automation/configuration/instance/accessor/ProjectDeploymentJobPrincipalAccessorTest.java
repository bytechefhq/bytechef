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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.event.ConnectionWorkflowPausedEvent;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.service.ConnectionService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ProjectDeploymentJobPrincipalAccessorTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

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
            applicationEventPublisher, connectionService, projectDeploymentService, projectDeploymentWorkflowService,
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

        verify(applicationEventPublisher, never()).publishEvent(any());
        verify(connectionService, never()).validateConnectionsActive(anyList());
    }

    @Test
    void testValidateConnectionsForJobWhenInactivePublishesAuditAndThrows() {
        ProjectDeploymentJobPrincipalAccessor accessor = new ProjectDeploymentJobPrincipalAccessor(
            applicationEventPublisher, connectionService, projectDeploymentService, projectDeploymentWorkflowService,
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

        verify(applicationEventPublisher).publishEvent(
            eq(new ConnectionWorkflowPausedEvent(
                100L,
                Map.of(
                    "projectDeploymentId", jobPrincipalId,
                    "workflowId", workflowId,
                    "connectionStatus", "PENDING_REASSIGNMENT"))));
    }

    /**
     * A webhook URL carries {@code jobPrincipalId} as a plain, unsigned field (see
     * {@code WorkflowExecutionId#toString()}, which is base64 of a colon-joined string, not a signed token), so anyone
     * holding a webhook URL can edit that field to a deployment that does not exist — including {@code -1}, the
     * test-mode sentinel whose branch in {@code WebhookWorkflowExecutorImpl#getWorkflowId} resolves a workflow by uuid
     * alone and would therefore bypass the deployment's pinned workflow version.
     *
     * <p>
     * That branch is unreachable from the public {@code /webhooks/{id}} path only because this method reports "not
     * enabled" for an unknown principal, which makes the controller answer 410 GONE before it ever resolves a workflow.
     * Two implementation details carry that guarantee, and neither is stated anywhere else: the deployment lookup
     * returns {@code false} for a missing row rather than throwing, and {@code &&} short-circuits so the throwing
     * uuid-resolution is never evaluated. Reordering the operands or making the lookup throw would turn a clean deny
     * into a 500 that has already reached workflow resolution.
     */
    @Test
    void testIsWorkflowEnabledFailsClosedForUnknownJobPrincipal() {
        ProjectDeploymentJobPrincipalAccessor accessor = new ProjectDeploymentJobPrincipalAccessor(
            applicationEventPublisher, connectionService, projectDeploymentService, projectDeploymentWorkflowService,
            projectWorkflowService);

        long unknownJobPrincipalId = -1L;

        when(projectDeploymentService.isProjectDeploymentEnabled(unknownJobPrincipalId)).thenReturn(false);

        assertThat(accessor.isWorkflowEnabled(unknownJobPrincipalId, "uuid-abc")).isFalse();

        verify(projectWorkflowService, never()).getProjectWorkflowWorkflowId(anyLong(), anyString());
        verify(projectDeploymentWorkflowService, never()).isProjectDeploymentWorkflowEnabled(anyLong(), anyString());
    }
}
