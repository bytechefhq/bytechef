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

package com.bytechef.platform.workflow.coordinator;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.event.TriggerListenerEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerPollEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerWebhookEvent;
import com.bytechef.platform.workflow.coordinator.trigger.completion.TriggerCompletionHandler;
import com.bytechef.platform.workflow.coordinator.trigger.dispatcher.TriggerDispatcher;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantDeletionService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
@SuppressWarnings("unchecked")
class TriggerCoordinatorInactiveTenantTest {

    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry = mock(JobPrincipalAccessorRegistry.class);
    private final TriggerCompletionHandler triggerCompletionHandler = mock(TriggerCompletionHandler.class);
    private final TriggerDispatcher triggerDispatcher = mock(TriggerDispatcher.class);
    private final TriggerExecutionService triggerExecutionService = mock(TriggerExecutionService.class);
    private final TriggerScheduler triggerScheduler = mock(TriggerScheduler.class);
    private final WorkflowService workflowService = mock(WorkflowService.class);

    private TriggerCoordinator triggerCoordinator;
    private WorkflowExecutionId workflowExecutionId;

    @BeforeEach
    void beforeEach() {
        TenantDeletionService tenantDeletionService = mock(TenantDeletionService.class);

        when(tenantDeletionService.isInactive("000001")).thenReturn(true);

        ObjectProvider<TenantDeletionService> tenantDeletionServiceProvider = mock(ObjectProvider.class);

        when(tenantDeletionServiceProvider.getIfAvailable()).thenReturn(tenantDeletionService);

        triggerCoordinator = new TriggerCoordinator(
            List.of(), List.of(), mock(Evaluator.class), mock(ApplicationEventPublisher.class),
            jobPrincipalAccessorRegistry, triggerCompletionHandler, triggerDispatcher, triggerExecutionService,
            mock(TriggerFileStorage.class), triggerScheduler, mock(TriggerStateService.class), workflowService,
            tenantDeletionServiceProvider);

        workflowExecutionId = TenantContext.callWithTenantId(
            "000001", () -> WorkflowExecutionId.of(PlatformType.AUTOMATION, 1L, "workflow-uuid", "trigger1"));
    }

    @Test
    void testListenerEventForInactiveTenantIsDropped() {
        triggerCoordinator.onTriggerListenerEvent(
            new TriggerListenerEvent(
                new TriggerListenerEvent.ListenerParameters(workflowExecutionId, Instant.now(), "output")));

        verifyNothingHappened();
    }

    @Test
    void testPollEventForInactiveTenantIsDropped() {
        triggerCoordinator.onTriggerPollEvent(new TriggerPollEvent(workflowExecutionId));

        verifyNothingHappened();
    }

    @Test
    void testWebhookEventForInactiveTenantIsDropped() {
        triggerCoordinator.onTriggerWebhookEvent(
            new TriggerWebhookEvent(
                new TriggerWebhookEvent.WebhookParameters(workflowExecutionId, mock(WebhookRequest.class))));

        verifyNothingHappened();
    }

    private void verifyNothingHappened() {
        verifyNoInteractions(
            jobPrincipalAccessorRegistry, triggerCompletionHandler, triggerDispatcher, triggerExecutionService,
            triggerScheduler, workflowService);
    }
}
