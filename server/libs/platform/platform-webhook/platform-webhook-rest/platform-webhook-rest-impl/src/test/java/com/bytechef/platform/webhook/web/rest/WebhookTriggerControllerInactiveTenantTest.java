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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.webhook.executor.WebhookWorkflowExecutor;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantDeletionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Ivica Cardic
 */
@SuppressWarnings("unchecked")
class WebhookTriggerControllerInactiveTenantTest {

    private WebhookTriggerController webhookTriggerController;
    private WebhookWorkflowExecutor webhookWorkflowExecutor;
    private WorkflowExecutionId workflowExecutionId;

    @BeforeEach
    void beforeEach() {
        TenantDeletionService tenantDeletionService = mock(TenantDeletionService.class);

        when(tenantDeletionService.isInactive("000001")).thenReturn(true);

        ObjectProvider<TenantDeletionService> tenantDeletionServiceProvider = mock(ObjectProvider.class);

        when(tenantDeletionServiceProvider.getIfAvailable()).thenReturn(tenantDeletionService);

        webhookWorkflowExecutor = mock(WebhookWorkflowExecutor.class);

        webhookTriggerController = new WebhookTriggerController(
            mock(ApplicationProperties.class), mock(TempFileStorage.class), webhookWorkflowExecutor,
            tenantDeletionServiceProvider);

        workflowExecutionId = TenantContext.callWithTenantId(
            "000001", () -> WorkflowExecutionId.of(PlatformType.AUTOMATION, 1L, "workflow-uuid", "trigger_1"));
    }

    @Test
    void testWebhookForInactiveTenantReturns410AndDoesNotExecute() {
        ResponseEntity<?> responseEntity = webhookTriggerController.executeWorkflow(
            workflowExecutionId.toString(), new MockHttpServletRequest("POST", "/webhooks/x"),
            new MockHttpServletResponse());

        assertThat(responseEntity.getStatusCode()
            .value()).isEqualTo(410);

        verifyNoInteractions(webhookWorkflowExecutor);
    }

    @Test
    void testSseWebhookForInactiveTenantReturns410AndDoesNotExecute() {
        assertThatThrownBy(
            () -> webhookTriggerController.sseStreamWorkflow(
                workflowExecutionId.toString(), new MockHttpServletRequest("GET", "/webhooks/x/sse")))
                    .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()
                            .value()).isEqualTo(410));

        verifyNoInteractions(webhookWorkflowExecutor);
    }
}
