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
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * @author Ivica Cardic
 */
class WebhookTriggerControllerTest {

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

    private ResponseEntity<?> executeWorkflow(String method) {
        MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest(
            method, "/webhooks/" + workflowExecutionId);

        return webhookTriggerController.executeWorkflow(
            workflowExecutionId.toString(), mockHttpServletRequest, new MockHttpServletResponse());
    }
}
