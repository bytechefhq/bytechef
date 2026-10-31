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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.facade.WebhookTriggerTestFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeTestOutputFacade;
import com.bytechef.platform.configuration.web.rest.config.PlatformConfigurationRestTestConfiguration;
import com.bytechef.platform.configuration.web.rest.config.WorkflowConfigurationRestTestConfigurationSharedMocks;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = PlatformConfigurationRestTestConfiguration.class)
@WebMvcTest(WebhookTriggerTestController.class)
@WorkflowConfigurationRestTestConfigurationSharedMocks
class WebhookTriggerTestControllerIntTest {

    private static final long ENVIRONMENT_ID = 0L;
    private static final WebhookTriggerFlags VALIDATION_FLAGS = new WebhookTriggerFlags(false, false, true, false);

    private final WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
        PlatformType.AUTOMATION, -1L, "workflow-uuid", "trigger_1");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebhookTriggerTestFacade webhookTriggerTestFacade;

    @Autowired
    private WorkflowNodeTestOutputFacade workflowNodeTestOutputFacade;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        when(webhookTriggerTestFacade.getWebhookTriggerFlags(any())).thenReturn(VALIDATION_FLAGS);
        when(webhookTriggerTestFacade.isWorkflowEnabled(any())).thenReturn(true);

        webTestClient = MockMvcWebTestClient.bindTo(mockMvc)
            .build();
    }

    @Test
    void testReturnsValidationResponseWithoutSavingTestOutputWhenValidationFails() {
        when(webhookTriggerTestFacade.validate(any(), any(), eq(ENVIRONMENT_ID)))
            .thenReturn(WebhookValidateResponse.badRequest());

        executeWorkflow()
            .expectStatus()
            .isBadRequest();

        verify(workflowNodeTestOutputFacade, never()).saveWorkflowNodeTestOutput(any(), anyLong(), any());
    }

    @Test
    void testSavesTestOutputWhenValidationSucceeds() {
        when(webhookTriggerTestFacade.validate(any(), any(), eq(ENVIRONMENT_ID)))
            .thenReturn(WebhookValidateResponse.ok());

        executeWorkflow()
            .expectStatus()
            .isOk();

        ArgumentCaptor<WorkflowExecutionId> workflowExecutionIdArgumentCaptor =
            ArgumentCaptor.forClass(WorkflowExecutionId.class);
        ArgumentCaptor<WebhookRequest> webhookRequestArgumentCaptor = ArgumentCaptor.forClass(WebhookRequest.class);

        verify(workflowNodeTestOutputFacade).saveWorkflowNodeTestOutput(
            workflowExecutionIdArgumentCaptor.capture(), eq(ENVIRONMENT_ID), webhookRequestArgumentCaptor.capture());

        WorkflowExecutionId savedWorkflowExecutionId = workflowExecutionIdArgumentCaptor.getValue();
        WebhookRequest webhookRequest = webhookRequestArgumentCaptor.getValue();

        assertThat(savedWorkflowExecutionId.getTriggerName()).isEqualTo("trigger_1");
        assertThat(webhookRequest.validated()).isTrue();
    }

    private WebTestClient.ResponseSpec executeWorkflow() {
        return webTestClient
            .post()
            .uri("/webhooks/{id}/test/environments/{environmentId}", workflowExecutionId.toString(), ENVIRONMENT_ID)
            .exchange();
    }
}
