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

package com.bytechef.platform.configuration.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.facade.WebhookTriggerTestFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeTestOutputFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.file.storage.EditorTempFileStorage;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * @author Ivica Cardic
 */
class WebhookTriggerTestControllerTest {

    private static final long ENVIRONMENT_ID = 0L;
    private static final WebhookTriggerFlags VALIDATION_FLAGS = new WebhookTriggerFlags(false, false, true, false);

    private final WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
        PlatformType.AUTOMATION, -1L, "workflow-uuid", "trigger_1");

    private WebhookTriggerTestController webhookTriggerTestController;
    private WebhookTriggerTestFacade webhookTriggerTestFacade;
    private WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade;

    @BeforeEach
    void beforeEach() {
        webhookTriggerTestFacade = mock(WebhookTriggerTestFacade.class);
        workflowNodeTestOutputFacade = mock(WorkflowNodeTestOutputFacade.class);

        when(webhookTriggerTestFacade.getWebhookTriggerFlags(any())).thenReturn(VALIDATION_FLAGS);
        when(webhookTriggerTestFacade.isWorkflowEnabled(any())).thenReturn(true);

        webhookTriggerTestController = new WebhookTriggerTestController(
            mock(EditorTempFileStorage.class), webhookTriggerTestFacade, workflowNodeTestOutputFacade);
    }

    @Test
    void testReturnsValidationResponseWithoutSavingTestOutputWhenValidationFails() {
        when(webhookTriggerTestFacade.validate(any(), any(), eq(ENVIRONMENT_ID)))
            .thenReturn(WebhookValidateResponse.badRequest());

        ResponseEntity<?> responseEntity = executeWorkflow();

        assertThat(responseEntity.getStatusCode()
            .value()).isEqualTo(400);

        verify(workflowNodeTestOutputFacade, never()).saveWorkflowNodeTestOutput(any(), anyLong(), any());
    }

    @Test
    void testSavesTestOutputWhenValidationSucceeds() {
        when(webhookTriggerTestFacade.validate(any(), any(), eq(ENVIRONMENT_ID)))
            .thenReturn(WebhookValidateResponse.ok());

        ResponseEntity<?> responseEntity = executeWorkflow();

        assertThat(responseEntity.getStatusCode()
            .value()).isEqualTo(200);

        ArgumentCaptor<WebhookRequest> webhookRequestArgumentCaptor = ArgumentCaptor.forClass(WebhookRequest.class);

        verify(workflowNodeTestOutputFacade).saveWorkflowNodeTestOutput(
            any(), eq(ENVIRONMENT_ID), webhookRequestArgumentCaptor.capture());

        assertThat(webhookRequestArgumentCaptor.getValue()
            .validated()).isTrue();
    }

    private ResponseEntity<?> executeWorkflow() {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest(
            "POST", "/webhooks/" + workflowExecutionId + "/test/environments/" + ENVIRONMENT_ID);

        return webhookTriggerTestController.executeWorkflow(
            workflowExecutionId.toString(), ENVIRONMENT_ID, mockHttpServletRequest);
    }
}
