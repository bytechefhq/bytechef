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
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.webhook.web.websocket.CallSessionRegistry.CallSession;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class WebhookWebSocketHandlerTest {

    private static final String CALL_SID = "CA0001";
    private static final String TRIGGER_NAME = "trigger_1";
    private static final String WORKFLOW_ID = "workflow-id";
    private static final String WORKFLOW_UUID = "workflow-uuid";

    private final JobFacade jobFacade = mock(JobFacade.class);
    private final JobPrincipalAccessor jobPrincipalAccessor = mock(JobPrincipalAccessor.class);
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry = mock(JobPrincipalAccessorRegistry.class);
    private final WebhookWorkflowExecutor webhookWorkflowExecutor = mock(WebhookWorkflowExecutor.class);
    private final WorkflowContinuationHelper workflowContinuationHelper = mock(WorkflowContinuationHelper.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    private CallSessionRegistry callSessionRegistry;
    private WebhookWebSocketHandler webhookWebSocketHandler;

    @BeforeEach
    void beforeEach() {
        callSessionRegistry = new CallSessionRegistry();

        webhookWebSocketHandler = new WebhookWebSocketHandler(
            callSessionRegistry, jobFacade, jobPrincipalAccessorRegistry, JsonMapper.builder()
                .build(),
            webhookWorkflowExecutor, workflowContinuationHelper, workflowService);

        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.AUTOMATION))
            .thenReturn(jobPrincipalAccessor);
        when(jobPrincipalAccessor.getWorkflowId(1L, WORKFLOW_UUID)).thenReturn(WORKFLOW_ID);
    }

    @Test
    void testConnectionWithoutACallSidSendsTheConnectedEventWithTheWebhookId() throws Exception {
        WebSocketSession webSocketSession = mockWebSocketSession("session-1", "/webhooks/webhook-1");

        webhookWebSocketHandler.afterConnectionEstablished(webSocketSession);

        assertThat(getSentMessages(webSocketSession)).containsExactly(
            Map.of("event", "connected", "id", "webhook-1"));
        assertThat(callSessionRegistry.hasSession(CALL_SID)).isFalse();

        verifyNoInteractions(jobFacade, workflowService);
    }

    @Test
    void testConnectionWithACallSidRegistersTheCallSession() throws Exception {
        WebSocketSession webSocketSession = mockWebSocketSession("session-1", "/webhooks?callSid=" + CALL_SID);

        webhookWebSocketHandler.afterConnectionEstablished(webSocketSession);

        assertThat(callSessionRegistry.getWebSocketSession(CALL_SID)).contains(webSocketSession);

        List<Map<String, Object>> sentMessages = getSentMessages(webSocketSession);

        assertThat(sentMessages).hasSize(1);

        Map<String, Object> connectedMessage = sentMessages.getFirst();

        assertThat(connectedMessage).containsEntry("event", "connected")
            .containsEntry("callSid", CALL_SID)
            .doesNotContainKey("reused");

        verifyNoInteractions(jobFacade, workflowService);
    }

    @Test
    void testConnectionForACallSidWithAnOpenSessionIsClosedAndTheExistingSessionIsKept() throws Exception {
        WebSocketSession existingWebSocketSession = mockWebSocketSession("session-1", "/webhooks?callSid=" + CALL_SID);

        webhookWebSocketHandler.afterConnectionEstablished(existingWebSocketSession);

        WebSocketSession newWebSocketSession = mockWebSocketSession(
            "session-2", "/webhooks/webhook-1?callSid=" + CALL_SID);

        webhookWebSocketHandler.afterConnectionEstablished(newWebSocketSession);

        assertThat(getSentMessages(newWebSocketSession)).containsExactly(
            Map.of("callSid", CALL_SID, "event", "connected", "id", "webhook-1", "reused", true));

        ArgumentCaptor<CloseStatus> closeStatusArgumentCaptor = ArgumentCaptor.forClass(CloseStatus.class);

        verify(newWebSocketSession).close(closeStatusArgumentCaptor.capture());

        CloseStatus closeStatus = closeStatusArgumentCaptor.getValue();

        assertThat(closeStatus.getCode()).isEqualTo(CloseStatus.NORMAL.getCode());
        assertThat(callSessionRegistry.getWebSocketSession(CALL_SID)).contains(existingWebSocketSession);

        verifyNoInteractions(workflowService);
    }

    @Test
    void testConnectionForACallSidWithAClosedSessionReplacesTheExistingSession() throws Exception {
        WebSocketSession existingWebSocketSession = mockWebSocketSession("session-1", "/webhooks?callSid=" + CALL_SID);

        webhookWebSocketHandler.afterConnectionEstablished(existingWebSocketSession);

        when(existingWebSocketSession.isOpen()).thenReturn(false);

        WebSocketSession newWebSocketSession = mockWebSocketSession("session-2", "/webhooks?callSid=" + CALL_SID);

        webhookWebSocketHandler.afterConnectionEstablished(newWebSocketSession);

        assertThat(callSessionRegistry.getWebSocketSession(CALL_SID)).contains(newWebSocketSession);
        assertThat(callSessionRegistry.getWebSocketSessionById("session-1")).isEmpty();

        verify(newWebSocketSession, never()).close(any(CloseStatus.class));

        Map<String, Object> connectedMessage = getSentMessages(newWebSocketSession).getFirst();

        assertThat(connectedMessage).doesNotContainKey("reused");
    }

    @Test
    void testConnectionWithACallSidAndAWebhookIdStartsTheWebsocketSubflow() throws Exception {
        String webhookId = createWebhookId();
        String websocketSubflowDefinition = "{\"tasks\":[]}";
        Workflow workflow = mockWorkflowWithTrigger(Map.of("websocketTasks", websocketSubflowDefinition));
        Workflow subflowWorkflow = mock(Workflow.class);

        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);
        when(workflowService.create(websocketSubflowDefinition, Workflow.Format.JSON, Workflow.SourceType.JDBC))
            .thenReturn(subflowWorkflow);
        when(subflowWorkflow.getId()).thenReturn("subflow-id");
        when(jobFacade.createJob(any(JobParametersDTO.class))).thenReturn(99L);

        WebSocketSession webSocketSession = mockWebSocketSession(
            "session-1", "/webhooks/" + webhookId + "?callSid=" + CALL_SID);

        webhookWebSocketHandler.afterConnectionEstablished(webSocketSession);

        ArgumentCaptor<JobParametersDTO> jobParametersDTOArgumentCaptor =
            ArgumentCaptor.forClass(JobParametersDTO.class);

        verify(jobFacade, timeout(5000)).createJob(jobParametersDTOArgumentCaptor.capture());

        JobParametersDTO jobParametersDTO = jobParametersDTOArgumentCaptor.getValue();

        assertThat(jobParametersDTO.getWorkflowId()).isEqualTo("subflow-id");
        assertThat(jobParametersDTO.getInputs()).isEqualTo(
            Map.of("callSid", CALL_SID, "mainWorkflowExecutionId", webhookId));

        CallSession callSession = callSessionRegistry.getSessionByCallSid(CALL_SID)
            .orElseThrow();

        assertThat(callSession.getWorkflowExecutionId()).isEqualTo(webhookId);

        await().atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> assertThat(callSession.getSubJobId()).isEqualTo(99L));
    }

    @Test
    void testConnectionDoesNotStartASubflowWhenTheTriggerHasNoWebsocketTasks() throws Exception {
        String webhookId = createWebhookId();
        Workflow workflow = mockWorkflowWithTrigger(Map.of());

        when(workflowService.getWorkflow(WORKFLOW_ID)).thenReturn(workflow);

        WebSocketSession webSocketSession = mockWebSocketSession(
            "session-1", "/webhooks/" + webhookId + "?callSid=" + CALL_SID);

        webhookWebSocketHandler.afterConnectionEstablished(webSocketSession);

        verify(workflowService, timeout(5000)).getWorkflow(WORKFLOW_ID);
        verify(workflowService, after(200).never()).create(anyString(), any(), any());
        verify(jobFacade, never()).createJob(any(JobParametersDTO.class));
    }

    @Test
    void testTextMessageWithoutTheExecuteActionIsAcknowledged() throws Exception {
        WebSocketSession webSocketSession = mockWebSocketSession("session-1", "/webhooks/webhook-1");
        String payload = "{\"action\":\"ping\"}";

        webhookWebSocketHandler.handleTextMessage(webSocketSession, new TextMessage(payload));

        assertThat(getSentMessages(webSocketSession)).containsExactly(Map.of("data", payload, "event", "ack"));

        verifyNoInteractions(webhookWorkflowExecutor);
    }

    @Test
    void testTextMessageThatIsNotJsonIsAnsweredWithAnError() throws Exception {
        WebSocketSession webSocketSession = mockWebSocketSession("session-1", "/webhooks/webhook-1");

        webhookWebSocketHandler.handleTextMessage(webSocketSession, new TextMessage("not json"));

        List<Map<String, Object>> sentMessages = getSentMessages(webSocketSession);

        assertThat(sentMessages).hasSize(1);

        Map<String, Object> errorMessage = sentMessages.getFirst();

        assertThat(errorMessage).containsEntry("event", "error");
        assertThat((String) errorMessage.get("message")).startsWith("Error processing message: ");
    }

    @Test
    void testExecuteWithoutAWorkflowExecutionIdIsAnsweredWithAnError() throws Exception {
        WebSocketSession webSocketSession = mockWebSocketSession("session-1", "/webhooks/webhook-1");

        webhookWebSocketHandler.handleTextMessage(webSocketSession, new TextMessage("{\"action\":\"execute\"}"));

        assertThat(getSentMessages(webSocketSession)).containsExactly(
            Map.of("event", "start", "message", "Workflow execution started", "webhookId", "webhook-1"),
            Map.of("event", "error", "message", "workflowExecutionId is required"));

        verifyNoInteractions(webhookWorkflowExecutor);
    }

    @Test
    void testMessagesAreNotSentToAClosedSession() throws Exception {
        WebSocketSession webSocketSession = mockWebSocketSession("session-1", "/webhooks/webhook-1");

        when(webSocketSession.isOpen()).thenReturn(false);

        webhookWebSocketHandler.handleTextMessage(webSocketSession, new TextMessage("{\"action\":\"ping\"}"));

        verify(webSocketSession, never()).sendMessage(any());
    }

    @Test
    void testCloseOfASessionThatSignalsCompletionOnCloseStopsTheSubflowAndContinuesTheWorkflow() throws Exception {
        WebSocketSession webSocketSession = mockWebSocketSession("session-1", "/webhooks?callSid=" + CALL_SID);

        webhookWebSocketHandler.afterConnectionEstablished(webSocketSession);

        CallSession callSession = callSessionRegistry.getSessionByCallSid(CALL_SID)
            .orElseThrow();

        callSession.initCompletionLatch();
        callSession.setCallDuration(42);
        callSession.setSignalCompletionOnClose(true);
        callSession.setSubJobId(7L);
        callSession.setWorkflowExecutionId("workflow-execution-id");

        webhookWebSocketHandler.afterConnectionClosed(webSocketSession, CloseStatus.GOING_AWAY.withReason("bye"));

        verify(jobFacade).stopJob(7L);

        assertThat(callSession.awaitCompletion(0, TimeUnit.MILLISECONDS)).isTrue();

        verify(workflowContinuationHelper).createContinuationJob(
            "workflow-execution-id",
            Map.of(
                "callDuration", 42, "callSid", CALL_SID, "callStatus", "websocket_closed", "closeReason", "bye",
                "closeStatusCode", CloseStatus.GOING_AWAY.getCode()));

        assertThat(callSessionRegistry.hasSession(CALL_SID)).isFalse();
        assertThat(callSessionRegistry.getWebSocketSessionById("session-1")).isEmpty();
    }

    @Test
    void testCloseContinuesTheWorkflowEvenWhenStoppingTheSubflowFails() throws Exception {
        WebSocketSession webSocketSession = mockWebSocketSession("session-1", "/webhooks?callSid=" + CALL_SID);

        webhookWebSocketHandler.afterConnectionEstablished(webSocketSession);

        CallSession callSession = callSessionRegistry.getSessionByCallSid(CALL_SID)
            .orElseThrow();

        callSession.initCompletionLatch();
        callSession.setSignalCompletionOnClose(true);
        callSession.setSubJobId(7L);
        callSession.setWorkflowExecutionId("workflow-execution-id");

        doThrow(new IllegalStateException("job not found")).when(jobFacade)
            .stopJob(7L);

        webhookWebSocketHandler.afterConnectionClosed(webSocketSession, CloseStatus.NORMAL);

        assertThat(callSession.awaitCompletion(0, TimeUnit.MILLISECONDS)).isTrue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> afterCallDataArgumentCaptor = ArgumentCaptor.forClass(Map.class);

        verify(workflowContinuationHelper).createContinuationJob(
            eq("workflow-execution-id"), afterCallDataArgumentCaptor.capture());

        assertThat(afterCallDataArgumentCaptor.getValue()).doesNotContainKey("callDuration")
            .containsEntry("closeStatusCode", CloseStatus.NORMAL.getCode());
        assertThat(callSessionRegistry.hasSession(CALL_SID)).isFalse();
    }

    @Test
    void testCloseWithoutASubJobOrWorkflowExecutionIdOnlySignalsCompletion() throws Exception {
        WebSocketSession webSocketSession = mockWebSocketSession("session-1", "/webhooks?callSid=" + CALL_SID);

        webhookWebSocketHandler.afterConnectionEstablished(webSocketSession);

        CallSession callSession = callSessionRegistry.getSessionByCallSid(CALL_SID)
            .orElseThrow();

        callSession.initCompletionLatch();
        callSession.setSignalCompletionOnClose(true);

        webhookWebSocketHandler.afterConnectionClosed(webSocketSession, CloseStatus.NORMAL);

        assertThat(callSession.awaitCompletion(0, TimeUnit.MILLISECONDS)).isTrue();

        verify(jobFacade, never()).stopJob(anyLong());
        verifyNoInteractions(workflowContinuationHelper);
    }

    @Test
    void testCloseOfASessionThatDoesNotSignalCompletionOnCloseOnlyRemovesTheCallSession() throws Exception {
        WebSocketSession webSocketSession = mockWebSocketSession("session-1", "/webhooks?callSid=" + CALL_SID);

        webhookWebSocketHandler.afterConnectionEstablished(webSocketSession);

        CallSession callSession = callSessionRegistry.getSessionByCallSid(CALL_SID)
            .orElseThrow();

        callSession.initCompletionLatch();
        callSession.setSubJobId(7L);
        callSession.setWorkflowExecutionId("workflow-execution-id");

        webhookWebSocketHandler.afterConnectionClosed(webSocketSession, CloseStatus.NORMAL);

        assertThat(callSession.awaitCompletion(0, TimeUnit.MILLISECONDS)).isFalse();
        assertThat(callSessionRegistry.hasSession(CALL_SID)).isFalse();

        verify(jobFacade, never()).stopJob(anyLong());
        verifyNoInteractions(workflowContinuationHelper);
    }

    @Test
    void testCloseOfASessionWithoutACallSidLeavesOtherCallSessionsAlone() throws Exception {
        WebSocketSession callWebSocketSession = mockWebSocketSession("session-1", "/webhooks?callSid=" + CALL_SID);

        webhookWebSocketHandler.afterConnectionEstablished(callWebSocketSession);

        WebSocketSession webSocketSession = mockWebSocketSession("session-2", "/webhooks/webhook-1");

        webhookWebSocketHandler.afterConnectionEstablished(webSocketSession);

        webhookWebSocketHandler.afterConnectionClosed(webSocketSession, CloseStatus.NORMAL);

        assertThat(callSessionRegistry.hasSession(CALL_SID)).isTrue();

        verifyNoInteractions(jobFacade, workflowContinuationHelper);
    }

    private static String createWebhookId() {
        TenantContext.setCurrentTenantId("public");

        try {
            WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
                PlatformType.AUTOMATION, 1L, WORKFLOW_UUID, TRIGGER_NAME);

            return workflowExecutionId.toString();
        } finally {
            TenantContext.resetCurrentTenantId();
        }
    }

    private static List<Map<String, Object>> getSentMessages(WebSocketSession webSocketSession) throws Exception {
        ArgumentCaptor<TextMessage> textMessageArgumentCaptor = ArgumentCaptor.forClass(TextMessage.class);

        verify(webSocketSession, timeout(5000).atLeastOnce()).sendMessage(textMessageArgumentCaptor.capture());

        return textMessageArgumentCaptor.getAllValues()
            .stream()
            .map(textMessage -> JsonUtils.readMap(textMessage.getPayload(), Object.class))
            .toList();
    }

    private static WebSocketSession mockWebSocketSession(String id, String uri) {
        WebSocketSession webSocketSession = mock(WebSocketSession.class);

        when(webSocketSession.getId()).thenReturn(id);
        when(webSocketSession.getUri()).thenReturn(URI.create(uri));
        when(webSocketSession.isOpen()).thenReturn(true);

        return webSocketSession;
    }

    private static Workflow mockWorkflowWithTrigger(Map<String, Object> extensions) {
        Map<String, Object> triggerSource = new HashMap<>(extensions);

        triggerSource.put("name", TRIGGER_NAME);
        triggerSource.put("type", "twilio/v1/newCall");

        Workflow workflow = mock(Workflow.class);

        when(workflow.getExtensions(WorkflowExtConstants.TRIGGERS, WorkflowTrigger.class, List.of()))
            .thenReturn(List.of(new WorkflowTrigger(triggerSource)));

        return workflow;
    }
}
