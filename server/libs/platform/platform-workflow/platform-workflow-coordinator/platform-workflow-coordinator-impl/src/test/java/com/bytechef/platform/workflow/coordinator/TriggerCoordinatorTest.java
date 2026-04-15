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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.exception.WorkflowErrorType;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.exception.ErrorType;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.file.storage.TriggerFileStorage;
import com.bytechef.platform.scheduler.TriggerScheduler;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.event.TriggerExecutionErrorEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerListenerEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerPollEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerWebhookEvent;
import com.bytechef.platform.workflow.coordinator.trigger.completion.TriggerCompletionHandler;
import com.bytechef.platform.workflow.coordinator.trigger.dispatcher.TriggerDispatcher;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.execution.service.TriggerStateService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class TriggerCoordinatorTest {

    @Mock
    private Evaluator evaluator;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private JobPrincipalAccessor jobPrincipalAccessor;

    @Mock
    private JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;

    @Mock
    private TriggerCompletionHandler triggerCompletionHandler;

    @Mock
    private TriggerDispatcher triggerDispatcher;

    @Mock
    private TriggerExecutionService triggerExecutionService;

    @Mock
    private TriggerFileStorage triggerFileStorage;

    @Mock
    private TriggerScheduler triggerScheduler;

    @Mock
    private TriggerStateService triggerStateService;

    @Mock
    private WebhookRequest webhookRequest;

    @Mock
    private WorkflowService workflowService;

    private TriggerCoordinator triggerCoordinator;

    @BeforeEach
    void setUp() {
        triggerCoordinator = new TriggerCoordinator(
            List.of(), List.of(), evaluator, eventPublisher, jobPrincipalAccessorRegistry,
            triggerCompletionHandler, triggerDispatcher, triggerExecutionService,
            triggerFileStorage, triggerScheduler, triggerStateService, workflowService);
    }

    @Test
    void testOnTriggerPollEventCancelsOrphanedTriggerOnIllegalArgumentException() {
        TenantContext.setCurrentTenantId("test-tenant");

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 1L, "workflow-uuid", "trigger1");

        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.AUTOMATION))
            .thenReturn(jobPrincipalAccessor);
        when(jobPrincipalAccessor.getWorkflowId(1L, "workflow-uuid"))
            .thenThrow(new IllegalArgumentException("ProjectWorkflow not found"));

        triggerCoordinator.onTriggerPollEvent(new TriggerPollEvent(workflowExecutionId));

        verify(triggerScheduler).cancelPollingTrigger(workflowExecutionId.toString());
        verifyNoInteractions(triggerDispatcher);
    }

    @Test
    void testOnTriggerPollEventCancelsOrphanedTriggerOnConfigurationException() {
        TenantContext.setCurrentTenantId("test-tenant");

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 1L, "workflow-uuid", "trigger1");

        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.AUTOMATION))
            .thenReturn(jobPrincipalAccessor);
        when(jobPrincipalAccessor.getWorkflowId(1L, "workflow-uuid"))
            .thenReturn("workflow-id");
        when(workflowService.getWorkflow("workflow-id"))
            .thenThrow(new ConfigurationException("Workflow not found", WorkflowErrorType.WORKFLOW_NOT_FOUND));

        triggerCoordinator.onTriggerPollEvent(new TriggerPollEvent(workflowExecutionId));

        verify(triggerScheduler).cancelPollingTrigger(workflowExecutionId.toString());
        verifyNoInteractions(triggerDispatcher);
    }

    @Test
    void testOnTriggerPollEventDoesNotDispatchWhenAConnectionIsInactive() {
        TenantContext.setCurrentTenantId("test-tenant");

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 1L, "workflow-uuid", "trigger1");
        Map<String, Object> workflowTriggerMap = Map.of("name", "trigger1", "type", "component/v1/trigger");
        Workflow workflow = mock(Workflow.class);
        WorkflowTrigger workflowTrigger = new WorkflowTrigger(workflowTriggerMap);

        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.AUTOMATION))
            .thenReturn(jobPrincipalAccessor);
        when(jobPrincipalAccessor.getWorkflowId(1L, "workflow-uuid"))
            .thenReturn("workflow-id");
        when(jobPrincipalAccessor.getInputMap(1L, "workflow-uuid"))
            .thenReturn(Map.of());
        when(workflowService.getWorkflow("workflow-id"))
            .thenReturn(workflow);
        when(workflow.getExtensions(eq(WorkflowExtConstants.TRIGGERS), eq(WorkflowTrigger.class), any()))
            .thenReturn(List.of(workflowTrigger));
        when(evaluator.evaluate(any(), any()))
            .thenReturn(workflowTriggerMap);
        when(triggerExecutionService.create(any(TriggerExecution.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        doThrow(
            new ConfigurationException("Workflow execution blocked: 1 non-ACTIVE connection(s)", mock(ErrorType.class)))
                .when(jobPrincipalAccessor)
                .validateConnectionsForJob(1L, "workflow-uuid");

        triggerCoordinator.onTriggerPollEvent(new TriggerPollEvent(workflowExecutionId));

        verifyNoInteractions(triggerDispatcher);
        verify(eventPublisher).publishEvent(any(TriggerExecutionErrorEvent.class));
        verify(triggerScheduler, never()).cancelPollingTrigger(anyString());
    }

    @Test
    void testOnTriggerListenerEventCancelsOrphanedTriggerOnIllegalArgumentException() {
        TenantContext.setCurrentTenantId("test-tenant");

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 1L, "workflow-uuid", "trigger1");

        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.AUTOMATION))
            .thenReturn(jobPrincipalAccessor);
        when(jobPrincipalAccessor.getWorkflowId(1L, "workflow-uuid"))
            .thenThrow(new IllegalArgumentException("ProjectWorkflow not found"));

        TriggerListenerEvent triggerListenerEvent = new TriggerListenerEvent(
            new TriggerListenerEvent.ListenerParameters(workflowExecutionId, Instant.now(), "output"));

        triggerCoordinator.onTriggerListenerEvent(triggerListenerEvent);

        verify(triggerScheduler).cancelScheduleTrigger(workflowExecutionId.toString());
        verifyNoInteractions(triggerExecutionService);
    }

    @Test
    void testOnTriggerListenerEventCancelsOrphanedTriggerOnConfigurationException() {
        TenantContext.setCurrentTenantId("test-tenant");

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 1L, "workflow-uuid", "trigger1");

        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.AUTOMATION))
            .thenReturn(jobPrincipalAccessor);
        when(jobPrincipalAccessor.getWorkflowId(1L, "workflow-uuid"))
            .thenReturn("workflow-id");
        when(workflowService.getWorkflow("workflow-id"))
            .thenThrow(new ConfigurationException("Workflow not found", WorkflowErrorType.WORKFLOW_NOT_FOUND));

        TriggerListenerEvent triggerListenerEvent = new TriggerListenerEvent(
            new TriggerListenerEvent.ListenerParameters(workflowExecutionId, Instant.now(), "output"));

        triggerCoordinator.onTriggerListenerEvent(triggerListenerEvent);

        verify(triggerScheduler).cancelScheduleTrigger(workflowExecutionId.toString());
        verifyNoInteractions(triggerExecutionService);
    }

    @Test
    void testOnTriggerWebhookEventCancelsOrphanedTriggerOnIllegalArgumentException() {
        TenantContext.setCurrentTenantId("test-tenant");

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 1L, "workflow-uuid", "trigger1");

        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.AUTOMATION))
            .thenReturn(jobPrincipalAccessor);
        when(jobPrincipalAccessor.getWorkflowId(1L, "workflow-uuid"))
            .thenThrow(new IllegalArgumentException("ProjectWorkflow not found"));

        TriggerWebhookEvent triggerWebhookEvent = new TriggerWebhookEvent(
            new TriggerWebhookEvent.WebhookParameters(workflowExecutionId, webhookRequest));

        triggerCoordinator.onTriggerWebhookEvent(triggerWebhookEvent);

        verify(triggerScheduler).cancelDynamicWebhookTriggerRefresh(workflowExecutionId.toString());
        verifyNoInteractions(triggerDispatcher);
    }

    @Test
    void testOnTriggerWebhookEventCancelsOrphanedTriggerOnConfigurationException() {
        TenantContext.setCurrentTenantId("test-tenant");

        WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, 1L, "workflow-uuid", "trigger1");

        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.AUTOMATION))
            .thenReturn(jobPrincipalAccessor);
        when(jobPrincipalAccessor.getWorkflowId(1L, "workflow-uuid"))
            .thenReturn("workflow-id");
        when(workflowService.getWorkflow("workflow-id"))
            .thenThrow(new ConfigurationException("Workflow not found", WorkflowErrorType.WORKFLOW_NOT_FOUND));

        TriggerWebhookEvent triggerWebhookEvent = new TriggerWebhookEvent(
            new TriggerWebhookEvent.WebhookParameters(workflowExecutionId, webhookRequest));

        triggerCoordinator.onTriggerWebhookEvent(triggerWebhookEvent);

        verify(triggerScheduler).cancelDynamicWebhookTriggerRefresh(workflowExecutionId.toString());
        verifyNoInteractions(triggerDispatcher);
    }
}
