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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.job.sync.SseStreamBridge;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.platform.plan.provider.PlanLimitsProvider;
import com.bytechef.platform.webhook.executor.SseStreamBridgeRegistry.Registration;
import com.bytechef.platform.workflow.JobInputConstants;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Pins that job creation on the webhook execution paths always seeds the reserved
 * {@link JobInputConstants#TRIGGER_NAME_INPUT} input alongside the trigger output, so downstream expressions can ask
 * "which trigger fired" portably.
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class WebhookWorkflowExecutorTriggerNameSeedingTest {

    private static final long JOB_PRINCIPAL_ID = 100L;
    private static final String TRIGGER_NAME = "trigger_1";
    private static final String WORKFLOW_ID = "workflow-id";
    private static final String WORKFLOW_UUID = "workflow-uuid";

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private JobCompletionAwaiter jobCompletionAwaiter;

    @Mock
    private JobPrincipalAccessor jobPrincipalAccessor;

    @Mock
    private JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;

    @Mock
    private JobSyncExecutor jobSyncExecutor;

    @Mock
    private PrincipalJobFacade principalJobFacade;

    @Mock
    private SseStreamBridgeRegistry sseStreamBridgeRegistry;

    @Mock
    private TaskExecutionService taskExecutionService;

    @Mock
    private TaskFileStorage taskFileStorage;

    @Mock
    private TaskFileStorage syncJobTaskFileStorage;

    @Mock
    private TriggerDefinitionService triggerDefinitionService;

    @Mock
    private WebhookWorkflowSyncExecutor webhookWorkflowSyncExecutor;

    @Mock
    private WorkflowService workflowService;

    private ObjectProvider<PlanLimitsProvider> planLimitsProviderObjectProvider;
    private WebhookWorkflowExecutorImpl webhookWorkflowExecutor;
    private WorkflowExecutionId workflowExecutionId;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        planLimitsProviderObjectProvider = (ObjectProvider<PlanLimitsProvider>) mock(ObjectProvider.class);

        webhookWorkflowExecutor = new WebhookWorkflowExecutorImpl(
            eventPublisher, jobCompletionAwaiter, jobPrincipalAccessorRegistry, jobSyncExecutor,
            planLimitsProviderObjectProvider, principalJobFacade, sseStreamBridgeRegistry, taskExecutionService,
            taskFileStorage, syncJobTaskFileStorage, triggerDefinitionService, webhookWorkflowSyncExecutor,
            workflowService, Duration.ofSeconds(5));

        workflowExecutionId = WorkflowExecutionId.of(
            PlatformType.AUTOMATION, JOB_PRINCIPAL_ID, WORKFLOW_UUID, TRIGGER_NAME);

        lenient()
            .when(jobPrincipalAccessorRegistry.getJobPrincipalAccessor(PlatformType.AUTOMATION))
            .thenReturn(jobPrincipalAccessor);
        lenient()
            .when(jobPrincipalAccessor.getInputMap(JOB_PRINCIPAL_ID, WORKFLOW_UUID))
            .thenReturn(Map.of());
        lenient()
            .when(jobPrincipalAccessor.getWorkflowId(JOB_PRINCIPAL_ID, WORKFLOW_UUID))
            .thenReturn(WORKFLOW_ID);
    }

    @Test
    void testCreateJobParametersSeedsTriggerName() {
        JobParametersDTO jobParametersDTO = WebhookWorkflowExecutorImpl.createJobParameters(
            workflowExecutionId, "workflow-1", Map.of("existing", "input"), Map.of("message", "hi"));

        assertThat(jobParametersDTO.getInputs())
            .containsEntry(JobInputConstants.TRIGGER_NAME_INPUT, TRIGGER_NAME)
            .containsEntry(TRIGGER_NAME, Map.of("message", "hi"))
            .containsEntry("existing", "input");
    }

    @Test
    void testExecuteAsyncWithSseStreamBridgeSeedsTriggerName() {
        when(webhookWorkflowSyncExecutor.execute(eq(workflowExecutionId), any()))
            .thenReturn(new TriggerOutput(Map.of("message", "hi"), null, false));

        long jobId = 42L;

        when(principalJobFacade.createJob(any(JobParametersDTO.class), eq(JOB_PRINCIPAL_ID),
            eq(PlatformType.AUTOMATION)))
                .thenReturn(jobId);

        CompletableFuture<Void> completionFuture = new CompletableFuture<>();

        when(sseStreamBridgeRegistry.register(eq(jobId), any()))
            .thenReturn(new Registration(() -> {}, completionFuture));

        SseStreamBridge sseStreamBridge = mock(SseStreamBridge.class);

        webhookWorkflowExecutor.executeAsync(workflowExecutionId, mock(WebhookRequest.class), sseStreamBridge);

        ArgumentCaptor<JobParametersDTO> jobParametersDTOArgumentCaptor = ArgumentCaptor.forClass(
            JobParametersDTO.class);

        verify(principalJobFacade).createJob(
            jobParametersDTOArgumentCaptor.capture(), eq(JOB_PRINCIPAL_ID), eq(PlatformType.AUTOMATION));

        assertThat(jobParametersDTOArgumentCaptor.getValue()
            .getInputs())
                .containsEntry(JobInputConstants.TRIGGER_NAME_INPUT, TRIGGER_NAME)
                .containsEntry(TRIGGER_NAME, Map.of("message", "hi"));
    }
}
