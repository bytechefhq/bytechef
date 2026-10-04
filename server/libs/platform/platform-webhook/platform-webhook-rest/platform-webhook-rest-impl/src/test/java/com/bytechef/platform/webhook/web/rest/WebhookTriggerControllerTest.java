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

package com.bytechef.platform.webhook.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.ai.constant.AiAgentSseEventType;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author Ivica Cardic
 */
class WebhookTriggerControllerTest {

    private static final WebhookTriggerFlags ASYNC_EXECUTION_FLAGS =
        new WebhookTriggerFlags(false, false, false, false);
    private static final WebhookTriggerFlags ASYNC_EXECUTION_WITH_VALIDATION_FLAGS =
        new WebhookTriggerFlags(false, false, true, false);
    private static final WebhookTriggerFlags SYNC_EXECUTION_WITH_VALIDATION_FLAGS =
        new WebhookTriggerFlags(false, true, true, false);

    private final WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
        PlatformType.AUTOMATION, 1L, "workflow-uuid", "trigger_1");

    private WebhookTriggerController webhookTriggerController;
    private WebhookWorkflowExecutor webhookWorkflowExecutor;

    @BeforeEach
    void beforeEach() {
        webhookWorkflowExecutor = mock(WebhookWorkflowExecutor.class);

        webhookTriggerController = new WebhookTriggerController(
            mock(ApplicationProperties.class), mock(TempFileStorage.class), webhookWorkflowExecutor);
    }

    @Test
    void testSyncTriggerReturnsValidationResponseWhenValidationFails() {
        when(webhookWorkflowExecutor.getWebhookTriggerFlags(any()))
            .thenReturn(SYNC_EXECUTION_WITH_VALIDATION_FLAGS);
        when(webhookWorkflowExecutor.validate(any(), any()))
            .thenReturn(WebhookValidateResponse.badRequest());

        ResponseEntity<?> responseEntity = executeWorkflow("POST");

        assertThat(responseEntity.getStatusCode()
            .value()).isEqualTo(400);

        verify(webhookWorkflowExecutor, never()).executeSync(any(), any());
    }

    @Test
    void testSyncTriggerExecutesWorkflowWhenValidationSucceeds() {
        when(webhookWorkflowExecutor.getWebhookTriggerFlags(any()))
            .thenReturn(SYNC_EXECUTION_WITH_VALIDATION_FLAGS);
        when(webhookWorkflowExecutor.validate(any(), any()))
            .thenReturn(WebhookValidateResponse.ok());
        when(webhookWorkflowExecutor.executeSync(any(), any()))
            .thenReturn(Map.of("result", "done"));

        ResponseEntity<?> responseEntity = executeWorkflow("POST");

        assertThat(responseEntity.getStatusCode()
            .value()).isEqualTo(200);
        assertThat(responseEntity.getBody()).isEqualTo(Map.of("result", "done"));
    }

    @Test
    void testDisabledWorkflowReturnsGoneForAsyncTrigger() {
        when(webhookWorkflowExecutor.getWebhookTriggerFlags(any())).thenReturn(ASYNC_EXECUTION_FLAGS);
        when(webhookWorkflowExecutor.isWorkflowDisabled(any())).thenReturn(true);

        ResponseEntity<?> responseEntity = executeWorkflow("POST");

        assertThat(responseEntity.getStatusCode()
            .value()).isEqualTo(410);
        assertThat(responseEntity.getBody()).isEqualTo(Map.of("detail", "Workflow is disabled."));

        verify(webhookWorkflowExecutor, never()).executeAsync(any(), any());
    }

    @Test
    void testDisabledWorkflowReturnsGoneForValidatingAsyncTrigger() {
        when(webhookWorkflowExecutor.getWebhookTriggerFlags(any())).thenReturn(ASYNC_EXECUTION_WITH_VALIDATION_FLAGS);
        when(webhookWorkflowExecutor.isWorkflowDisabled(any())).thenReturn(true);

        ResponseEntity<?> responseEntity = executeWorkflow("POST");

        assertThat(responseEntity.getStatusCode()
            .value()).isEqualTo(410);

        verify(webhookWorkflowExecutor, never()).validateAndExecuteAsync(any(), any());
    }

    @Test
    void testDisabledWorkflowReturnsGoneForSyncTrigger() {
        when(webhookWorkflowExecutor.getWebhookTriggerFlags(any())).thenReturn(SYNC_EXECUTION_WITH_VALIDATION_FLAGS);
        when(webhookWorkflowExecutor.isWorkflowDisabled(any())).thenReturn(true);

        ResponseEntity<?> responseEntity = executeWorkflow("POST");

        assertThat(responseEntity.getStatusCode()
            .value()).isEqualTo(410);

        verify(webhookWorkflowExecutor, never()).executeSync(any(), any());
    }

    @Test
    void testHeadRequestReturnsOkForDisabledAsyncTrigger() {
        when(webhookWorkflowExecutor.getWebhookTriggerFlags(any())).thenReturn(ASYNC_EXECUTION_FLAGS);
        when(webhookWorkflowExecutor.isWorkflowDisabled(any())).thenReturn(true);

        ResponseEntity<?> responseEntity = executeWorkflow("HEAD");

        assertThat(responseEntity.getStatusCode()
            .value()).isEqualTo(200);
    }

    @Test
    void testSyncTriggerExecutesWorkflowWithValidatedRequest() {
        when(webhookWorkflowExecutor.getWebhookTriggerFlags(any())).thenReturn(SYNC_EXECUTION_WITH_VALIDATION_FLAGS);
        when(webhookWorkflowExecutor.validate(any(), any())).thenReturn(WebhookValidateResponse.ok());

        executeWorkflow("POST");

        ArgumentCaptor<WebhookRequest> webhookRequestArgumentCaptor = ArgumentCaptor.forClass(WebhookRequest.class);

        verify(webhookWorkflowExecutor).validate(any(), webhookRequestArgumentCaptor.capture());

        assertThat(webhookRequestArgumentCaptor.getValue()
            .validated()).isFalse();

        verify(webhookWorkflowExecutor).executeSync(any(), webhookRequestArgumentCaptor.capture());

        assertThat(webhookRequestArgumentCaptor.getValue()
            .validated()).isTrue();
    }

    @Test
    void testStreamBridgeSendsEventTypeAsNamedEventWithoutDiscriminator() throws IOException {
        SseEmitter sseEmitter = mock(SseEmitter.class);

        WebhookTriggerController.WebhookSseStreamBridge webhookSseStreamBridge =
            new WebhookTriggerController.WebhookSseStreamBridge(sseEmitter);

        webhookSseStreamBridge.onEvent(
            Map.of(
                AiAgentSseEventType.EVENT_TYPE, AiAgentSseEventType.ASK_USER_QUESTION,
                "questions", List.of(Map.of("question", "Which library?")),
                "resumeUrl", "https://example.com/api/job/resume/abc"));

        List<Object> sentParts = getSentParts(sseEmitter);

        assertThat(sentParts.getFirst()).asString()
            .contains("event:" + AiAgentSseEventType.ASK_USER_QUESTION);
        assertThat(sentParts.get(1)).isEqualTo(
            Map.of(
                "questions", List.of(Map.of("question", "Which library?")),
                "resumeUrl", "https://example.com/api/job/resume/abc"));
    }

    @Test
    void testStreamBridgeSendsToolExecutionAsNamedEvent() throws IOException {
        SseEmitter sseEmitter = mock(SseEmitter.class);

        WebhookTriggerController.WebhookSseStreamBridge webhookSseStreamBridge =
            new WebhookTriggerController.WebhookSseStreamBridge(sseEmitter);

        webhookSseStreamBridge.onEvent(
            Map.of(AiAgentSseEventType.EVENT_TYPE, AiAgentSseEventType.TOOL_EXECUTION, "toolName", "search"));

        List<Object> sentParts = getSentParts(sseEmitter);

        assertThat(sentParts.getFirst()).asString()
            .contains("event:" + AiAgentSseEventType.TOOL_EXECUTION);
        assertThat(sentParts.get(1)).isEqualTo(Map.of("toolName", "search"));
    }

    @Test
    void testStreamBridgeSendsNonStringEventTypeAsStream() throws IOException {
        SseEmitter sseEmitter = mock(SseEmitter.class);

        WebhookTriggerController.WebhookSseStreamBridge webhookSseStreamBridge =
            new WebhookTriggerController.WebhookSseStreamBridge(sseEmitter);

        Map<String, Object> payload = Map.of(AiAgentSseEventType.EVENT_TYPE, 42, "text", "hello");

        webhookSseStreamBridge.onEvent(payload);

        List<Object> sentParts = getSentParts(sseEmitter);

        assertThat(sentParts.getFirst()).asString()
            .contains("event:stream");
        assertThat(sentParts.get(1)).isEqualTo(payload);
    }

    private static List<Object> getSentParts(SseEmitter sseEmitter) throws IOException {
        ArgumentCaptor<SseEmitter.SseEventBuilder> sseEventBuilderArgumentCaptor =
            ArgumentCaptor.forClass(SseEmitter.SseEventBuilder.class);

        verify(sseEmitter).send(sseEventBuilderArgumentCaptor.capture());

        SseEmitter.SseEventBuilder sseEventBuilder = sseEventBuilderArgumentCaptor.getValue();

        return sseEventBuilder.build()
            .stream()
            .map(ResponseBodyEmitter.DataWithMediaType::getData)
            .toList();
    }

    private ResponseEntity<?> executeWorkflow(String method) {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest(
            method, "/webhooks/" + workflowExecutionId);

        return webhookTriggerController.executeWorkflow(
            workflowExecutionId.toString(), mockHttpServletRequest, new MockHttpServletResponse());
    }
}
