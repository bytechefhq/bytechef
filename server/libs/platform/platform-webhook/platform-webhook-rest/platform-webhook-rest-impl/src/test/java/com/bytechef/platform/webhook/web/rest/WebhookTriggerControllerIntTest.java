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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @author Ivica Cardic
 */
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = WebhookTriggerController.class)
@ExtendWith(ObjectMapperSetupExtension.class)
@WebMvcTest(controllers = WebhookTriggerController.class)
class WebhookTriggerControllerIntTest {

    private static final WebhookTriggerFlags ASYNC_EXECUTION_FLAGS =
        new WebhookTriggerFlags(false, false, false, false);
    private static final WebhookTriggerFlags ASYNC_EXECUTION_WITH_VALIDATION_FLAGS =
        new WebhookTriggerFlags(false, false, true, false);
    private static final WebhookTriggerFlags SYNC_EXECUTION_WITH_VALIDATION_FLAGS =
        new WebhookTriggerFlags(false, true, true, false);

    private final WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
        PlatformType.AUTOMATION, 1L, "workflow-uuid", "trigger_1");

    @MockitoBean
    private ApplicationProperties applicationProperties;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TempFileStorage tempFileStorage;

    @MockitoBean
    private WebhookWorkflowExecutor webhookWorkflowExecutor;

    @Test
    void testSyncTriggerReturnsValidationResponseWhenValidationFails() throws Exception {
        when(webhookWorkflowExecutor.getWebhookTriggerFlags(any()))
            .thenReturn(SYNC_EXECUTION_WITH_VALIDATION_FLAGS);
        when(webhookWorkflowExecutor.validate(any(), any()))
            .thenReturn(WebhookValidateResponse.badRequest());

        mockMvc.perform(post("/webhooks/{id}", workflowExecutionId.toString()))
            .andExpect(status().isBadRequest());

        verify(webhookWorkflowExecutor, never()).executeSync(any(), any());
    }

    @Test
    void testSyncTriggerExecutesWorkflowWhenValidationSucceeds() throws Exception {
        when(webhookWorkflowExecutor.getWebhookTriggerFlags(any()))
            .thenReturn(SYNC_EXECUTION_WITH_VALIDATION_FLAGS);
        when(webhookWorkflowExecutor.validate(any(), any()))
            .thenReturn(WebhookValidateResponse.ok());
        when(webhookWorkflowExecutor.executeSync(any(), any()))
            .thenReturn(Map.of("result", "done"));

        String workflowExecutionIdString = workflowExecutionId.toString();

        mockMvc.perform(post("/webhooks/{id}", workflowExecutionIdString))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.result").value("done"));

        verify(webhookWorkflowExecutor).executeSync(
            argThat(executedWorkflowExecutionId -> workflowExecutionIdString.equals(
                executedWorkflowExecutionId.toString())),
            any());
    }

    @Test
    void testDisabledWorkflowReturnsGoneForAsyncTrigger() throws Exception {
        when(webhookWorkflowExecutor.getWebhookTriggerFlags(any())).thenReturn(ASYNC_EXECUTION_FLAGS);
        when(webhookWorkflowExecutor.isWorkflowDisabled(any())).thenReturn(true);

        mockMvc.perform(post("/webhooks/{id}", workflowExecutionId.toString()))
            .andExpect(status().isGone())
            .andExpect(jsonPath("$.detail").value("Workflow is disabled."));

        verify(webhookWorkflowExecutor, never()).executeAsync(any(), any());
    }

    @Test
    void testDisabledWorkflowReturnsGoneForValidatingAsyncTrigger() throws Exception {
        when(webhookWorkflowExecutor.getWebhookTriggerFlags(any())).thenReturn(ASYNC_EXECUTION_WITH_VALIDATION_FLAGS);
        when(webhookWorkflowExecutor.isWorkflowDisabled(any())).thenReturn(true);

        mockMvc.perform(post("/webhooks/{id}", workflowExecutionId.toString()))
            .andExpect(status().isGone());

        verify(webhookWorkflowExecutor, never()).validateAndExecuteAsync(any(), any());
    }

    @Test
    void testDisabledWorkflowReturnsGoneForSyncTrigger() throws Exception {
        when(webhookWorkflowExecutor.getWebhookTriggerFlags(any())).thenReturn(SYNC_EXECUTION_WITH_VALIDATION_FLAGS);
        when(webhookWorkflowExecutor.isWorkflowDisabled(any())).thenReturn(true);

        mockMvc.perform(post("/webhooks/{id}", workflowExecutionId.toString()))
            .andExpect(status().isGone());

        verify(webhookWorkflowExecutor, never()).executeSync(any(), any());
    }

    @Test
    void testHeadRequestReturnsOkForDisabledAsyncTrigger() throws Exception {
        when(webhookWorkflowExecutor.getWebhookTriggerFlags(any())).thenReturn(ASYNC_EXECUTION_FLAGS);
        when(webhookWorkflowExecutor.isWorkflowDisabled(any())).thenReturn(true);

        mockMvc.perform(head("/webhooks/{id}", workflowExecutionId.toString()))
            .andExpect(status().isOk());
    }

    @Test
    void testSyncTriggerExecutesWorkflowWithValidatedRequest() throws Exception {
        when(webhookWorkflowExecutor.getWebhookTriggerFlags(any())).thenReturn(SYNC_EXECUTION_WITH_VALIDATION_FLAGS);
        when(webhookWorkflowExecutor.validate(any(), any())).thenReturn(WebhookValidateResponse.ok());

        mockMvc.perform(post("/webhooks/{id}", workflowExecutionId.toString()));

        ArgumentCaptor<WebhookRequest> webhookRequestArgumentCaptor = ArgumentCaptor.forClass(WebhookRequest.class);

        verify(webhookWorkflowExecutor).validate(any(), webhookRequestArgumentCaptor.capture());

        WebhookRequest webhookRequestToValidate = webhookRequestArgumentCaptor.getValue();

        assertThat(webhookRequestToValidate.validated()).isFalse();

        verify(webhookWorkflowExecutor).executeSync(any(), webhookRequestArgumentCaptor.capture());

        WebhookRequest executedWebhookRequest = webhookRequestArgumentCaptor.getValue();

        assertThat(executedWebhookRequest.validated()).isTrue();
    }
}
