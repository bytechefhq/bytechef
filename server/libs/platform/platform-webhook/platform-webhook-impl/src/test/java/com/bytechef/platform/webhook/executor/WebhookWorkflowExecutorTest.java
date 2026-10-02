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

package com.bytechef.platform.webhook.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.event.TriggerWebhookEvent;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
public class WebhookWorkflowExecutorTest {

    private final WorkflowExecutionId workflowExecutionId = WorkflowExecutionId.of(
        PlatformType.AUTOMATION, 1L, "workflow-uuid", "trigger_1");
    private final WebhookRequest webhookRequest = new WebhookRequest(
        Map.of(), Map.of(), null, WebhookMethod.POST);

    private Evaluator evaluator;
    private ApplicationEventPublisher eventPublisher;
    private JobSyncExecutor jobSyncExecutor;
    private WebhookWorkflowExecutor webhookWorkflowExecutor;
    private WebhookWorkflowSyncExecutor webhookWorkflowSyncExecutor;
    private WorkflowService workflowService;

    @BeforeEach
    void beforeEach() {
        evaluator = mock(Evaluator.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        jobSyncExecutor = mock(JobSyncExecutor.class);
        webhookWorkflowSyncExecutor = mock(WebhookWorkflowSyncExecutor.class);
        workflowService = mock(WorkflowService.class);

        JobPrincipalAccessor jobPrincipalAccessor = mock(JobPrincipalAccessor.class);
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry = mock(JobPrincipalAccessorRegistry.class);

        when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(any())).thenReturn(jobPrincipalAccessor);
        when(jobPrincipalAccessor.getInputMap(anyLong(), anyString())).thenReturn(Map.of());
        when(jobPrincipalAccessor.getWorkflowId(anyLong(), anyString())).thenReturn("workflow-id");
        when(evaluator.evaluate(any(), any(), anyBoolean())).thenAnswer(invocation -> invocation.getArgument(0));

        webhookWorkflowExecutor = new WebhookWorkflowExecutorImpl(
            evaluator, eventPublisher, jobPrincipalAccessorRegistry, jobSyncExecutor, mock(PrincipalJobFacade.class),
            mock(SseStreamBridgeRegistry.class), webhookWorkflowSyncExecutor, mock(TaskFileStorage.class),
            mock(TriggerDefinitionService.class), workflowService);
    }

    @Test
    @Disabled
    public void testExecuteAsync() {
        // TODO
    }

    @Test
    @Disabled
    public void testExecuteAsyncSync() {
        // TODO
    }

    @Test
    public void testValidateReturnsTriggerValidationResponseWithoutExecutingWorkflow() {
        WebhookValidateResponse badRequestResponse = WebhookValidateResponse.badRequest();

        when(webhookWorkflowSyncExecutor.validate(any(), any())).thenReturn(badRequestResponse);

        assertThat(webhookWorkflowExecutor.validate(workflowExecutionId, webhookRequest))
            .isSameAs(badRequestResponse);

        verify(webhookWorkflowSyncExecutor).validate(workflowExecutionId, webhookRequest);
        verifyNoInteractions(eventPublisher, jobSyncExecutor);
    }

    @Test
    public void testExecuteSyncWaitsAtMostTriggerTimeout() {
        assertThat(executeSyncAndCaptureTimeout(Map.of("timeout", 1500))).isEqualTo(Duration.ofMillis(1500));
    }

    @Test
    public void testExecuteSyncWaitsAtMostEvaluatedTriggerTimeout() {
        when(evaluator.evaluate(any(), any(), anyBoolean())).thenReturn(
            Map.of(
                "name", "trigger_1", "type", "webhook/v1/awaitWorkflowAndRespond",
                "parameters", Map.of("timeout", 2000)));

        assertThat(executeSyncAndCaptureTimeout(Map.of("timeout", "${input.timeout}")))
            .isEqualTo(Duration.ofMillis(2000));
    }

    @Test
    public void testExecuteSyncAcceptsNumericTextTriggerTimeout() {
        assertThat(executeSyncAndCaptureTimeout(Map.of("timeout", "1500"))).isEqualTo(Duration.ofMillis(1500));
    }

    @Test
    public void testExecuteSyncUsesDefaultTimeoutWhenTriggerTimeoutIsNotSet() {
        assertThat(executeSyncAndCaptureTimeout(Map.of())).isNull();
    }

    @Test
    public void testExecuteSyncUsesDefaultTimeoutWhenTriggerTimeoutIsNotNumber() {
        assertThat(executeSyncAndCaptureTimeout(Map.of("timeout", "${input.timeout}"))).isNull();
    }

    @Test
    public void testValidateAndExecuteAsyncPublishesValidatedRequest() {
        when(webhookWorkflowSyncExecutor.validate(any(), any())).thenReturn(WebhookValidateResponse.ok());

        webhookWorkflowExecutor.validateAndExecuteAsync(workflowExecutionId, webhookRequest);

        ArgumentCaptor<TriggerWebhookEvent> triggerWebhookEventArgumentCaptor =
            ArgumentCaptor.forClass(TriggerWebhookEvent.class);

        verify(eventPublisher).publishEvent(triggerWebhookEventArgumentCaptor.capture());

        TriggerWebhookEvent triggerWebhookEvent = triggerWebhookEventArgumentCaptor.getValue();

        assertThat(triggerWebhookEvent.getWebhookRequest()
            .validated()).isTrue();
    }

    @Test
    public void testValidateAndExecuteAsyncDoesNotPublishInvalidRequest() {
        when(webhookWorkflowSyncExecutor.validate(any(), any())).thenReturn(WebhookValidateResponse.badRequest());

        webhookWorkflowExecutor.validateAndExecuteAsync(workflowExecutionId, webhookRequest);

        verifyNoInteractions(eventPublisher);
    }

    private Duration executeSyncAndCaptureTimeout(Map<String, ?> triggerParameters) {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getExtensions(any(), any(), any())).thenReturn(
            List.of(
                new WorkflowTrigger(
                    Map.of(
                        "name", "trigger_1", "type", "webhook/v1/awaitWorkflowAndRespond",
                        "parameters", triggerParameters))));
        when(workflowService.getWorkflow("workflow-id")).thenReturn(workflow);
        when(webhookWorkflowSyncExecutor.execute(any(), any())).thenReturn(new TriggerOutput(Map.of(), null, false));
        when(jobSyncExecutor.execute(any(), any(), anyBoolean(), any(), any())).thenReturn(new Job());

        webhookWorkflowExecutor.executeSync(workflowExecutionId, webhookRequest);

        ArgumentCaptor<Duration> timeoutArgumentCaptor = ArgumentCaptor.forClass(Duration.class);

        verify(jobSyncExecutor).execute(any(), any(), anyBoolean(), any(), timeoutArgumentCaptor.capture());

        return timeoutArgumentCaptor.getValue();
    }
}
