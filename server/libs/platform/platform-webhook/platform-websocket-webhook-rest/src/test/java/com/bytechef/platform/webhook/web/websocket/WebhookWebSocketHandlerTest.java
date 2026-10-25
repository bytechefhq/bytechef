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

package com.bytechef.platform.webhook.web.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WebhookWebSocketHandlerTest {

    private static final String CALL_SID = "call-1";
    private static final String TENANT_ID = "tenant1";
    private final CallSessionRegistry callSessionRegistry = new CallSessionRegistry();
    private final JobFacade jobFacade = mock(JobFacade.class);
    private final JobPrincipalAccessor jobPrincipalAccessor = mock(JobPrincipalAccessor.class);
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry = mock(JobPrincipalAccessorRegistry.class);
    private final List<String> tenantIds = new CopyOnWriteArrayList<>();
    private final WorkflowService workflowService = mock(WorkflowService.class);
    private final WebhookWebSocketHandler webhookWebSocketHandler = new WebhookWebSocketHandler(
        callSessionRegistry, jobFacade, jobPrincipalAccessorRegistry, mock(ObjectMapper.class),
        mock(WebhookWorkflowExecutor.class), mock(WorkflowContinuationHelper.class), workflowService);

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testAfterConnectionEstablishedStartsTheSubflowInTheTenantOfTheId() throws Exception {
        WebSocketSession webSocketSession = mockWebSocketSession(createWorkflowExecutionId());

        Workflow subflowWorkflow = mock(Workflow.class);
        Workflow workflow = mock(Workflow.class);

        WorkflowTrigger workflowTrigger = new WorkflowTrigger(
            Map.of("name", "trigger_1", "type", "webhook/v1/websocket", "websocketTasks", "{}"));

        when(subflowWorkflow.getId()).thenReturn("subflow-id");
        when(workflow.getExtensions(WorkflowExtConstants.TRIGGERS, WorkflowTrigger.class, List.of()))
            .thenReturn(List.of(workflowTrigger));
        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.AUTOMATION))
            .thenReturn(jobPrincipalAccessor);
        when(jobPrincipalAccessor.getWorkflowId(1L, "workflow-uuid"))
            .thenAnswer(invocation -> {
                tenantIds.add(TenantContext.getCurrentTenantId());

                return "workflow-id";
            });
        when(workflowService.getWorkflow("workflow-id"))
            .thenAnswer(invocation -> {
                tenantIds.add(TenantContext.getCurrentTenantId());

                return workflow;
            });
        when(workflowService.create(any(), any(), any()))
            .thenAnswer(invocation -> {
                tenantIds.add(TenantContext.getCurrentTenantId());

                return subflowWorkflow;
            });
        when(jobFacade.createJob(any()))
            .thenAnswer(invocation -> {
                tenantIds.add(TenantContext.getCurrentTenantId());

                return 2L;
            });

        webhookWebSocketHandler.afterConnectionEstablished(webSocketSession);

        verify(jobFacade, timeout(5000)).createJob(any());

        assertThat(tenantIds).containsExactly(TENANT_ID, TENANT_ID, TENANT_ID, TENANT_ID);
    }

    @Test
    void testAfterConnectionClosedStopsTheSubflowJobInTheTenantOfTheId() throws Exception {
        String workflowExecutionId = createWorkflowExecutionId();

        WebSocketSession webSocketSession = mockWebSocketSession(null);

        webhookWebSocketHandler.afterConnectionEstablished(webSocketSession);

        Optional<CallSessionRegistry.CallSession> callSessionOptional = callSessionRegistry.getSessionByCallSid(
            CALL_SID);

        assertThat(callSessionOptional).isPresent();

        CallSessionRegistry.CallSession callSession = callSessionOptional.get();

        callSession.setSignalCompletionOnClose(true);
        callSession.setSubJobId(2L);
        callSession.setWorkflowExecutionId(workflowExecutionId);

        doAnswer(invocation -> tenantIds.add(TenantContext.getCurrentTenantId()))
            .when(jobFacade)
            .stopJob(2L);

        webhookWebSocketHandler.afterConnectionClosed(webSocketSession, CloseStatus.NORMAL);

        assertThat(tenantIds).containsExactly(TENANT_ID);
    }

    private static String createWorkflowExecutionId() {
        WorkflowExecutionId workflowExecutionId = TenantContext.callWithTenantId(
            TENANT_ID, () -> WorkflowExecutionId.of(PlatformType.AUTOMATION, 1L, "workflow-uuid", "trigger_1"));

        return workflowExecutionId.toString();
    }

    private static WebSocketSession mockWebSocketSession(String webhookId) {
        WebSocketSession webSocketSession = mock(WebSocketSession.class);

        String path = webhookId == null ? "/webhooks" : "/webhooks/" + webhookId;

        when(webSocketSession.getId()).thenReturn("session-1");
        when(webSocketSession.getUri()).thenReturn(URI.create("ws://localhost" + path + "?callSid=" + CALL_SID));

        return webSocketSession;
    }
}
