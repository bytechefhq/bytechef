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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.exception.ExecutionException;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.platform.plan.domain.PlanLimits;
import com.bytechef.platform.plan.domain.PlanTier;
import com.bytechef.platform.plan.provider.PlanLimitsProvider;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Unit tests for the distributed-coordinator synchronous webhook path. The behaviour under test is the post-hoc read of
 * the {@code WEBHOOK_RESPONSE}-tagged task output once {@link JobCompletionAwaiter} resolves the job: the last response
 * to complete (latest {@code endDate}, id as a deterministic tiebreaker) is the HTTP reply, the batch (collection)
 * trigger output produces one job per element, and a {@code FAILED} job surfaces its error.
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
public class WebhookWorkflowExecutorTest {

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
    public void setUp() {
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
    public void testExecuteSyncReturnsLastWebhookResponseByEndDate() throws Exception {
        Job job = job(1L, Job.Status.COMPLETED);

        stubTriggerAndJob("payload", false, 1L, job);

        FileEntry earlyOutput = mockFileEntry();
        FileEntry lateOutput = mockFileEntry();

        TaskExecution earlyTaskExecution = webhookResponseTaskExecution(10L, Instant.parse("2026-06-20T10:00:00Z"),
            earlyOutput);
        TaskExecution lateTaskExecution = webhookResponseTaskExecution(11L, Instant.parse("2026-06-20T10:05:00Z"),
            lateOutput);

        when(taskExecutionService.getJobTaskExecutions(1L))
            .thenReturn(List.of(earlyTaskExecution, lateTaskExecution));
        when(taskFileStorage.readTaskExecutionOutput(lateOutput)).thenReturn("late-response");

        stubJobOutputs(1L, "late-response");

        Object outputs = webhookWorkflowExecutor.executeSync(workflowExecutionId, mockWebhookRequest())
            .get();

        assertThat(outputs)
            .isEqualTo(Map.of(MetadataConstants.WEBHOOK_RESPONSE, "late-response"));

        verify(taskFileStorage).readTaskExecutionOutput(lateOutput);
        verify(taskFileStorage, never()).readTaskExecutionOutput(earlyOutput);
    }

    @Test
    public void testExecuteSyncBreaksEndDateTieByTaskId() throws Exception {
        Job job = job(2L, Job.Status.COMPLETED);

        stubTriggerAndJob("payload", false, 2L, job);

        Instant sharedEndDate = Instant.parse("2026-06-20T10:00:00Z");

        FileEntry lowerIdOutput = mockFileEntry();
        FileEntry higherIdOutput = mockFileEntry();

        TaskExecution lowerIdTaskExecution = webhookResponseTaskExecution(20L, sharedEndDate, lowerIdOutput);
        TaskExecution higherIdTaskExecution = webhookResponseTaskExecution(21L, sharedEndDate, higherIdOutput);

        when(taskExecutionService.getJobTaskExecutions(2L))
            .thenReturn(List.of(higherIdTaskExecution, lowerIdTaskExecution));
        when(taskFileStorage.readTaskExecutionOutput(higherIdOutput)).thenReturn("higher-id-response");

        stubJobOutputs(2L, "higher-id-response");

        Object outputs = webhookWorkflowExecutor.executeSync(workflowExecutionId, mockWebhookRequest())
            .get();

        assertThat(outputs)
            .isEqualTo(Map.of(MetadataConstants.WEBHOOK_RESPONSE, "higher-id-response"));

        verify(taskFileStorage).readTaskExecutionOutput(higherIdOutput);
        verify(taskFileStorage, never()).readTaskExecutionOutput(lowerIdOutput);
    }

    @Test
    public void testExecuteSyncBatchCollectionReturnsListPerElement() throws Exception {
        Job firstJob = job(3L, Job.Status.COMPLETED);
        Job secondJob = job(4L, Job.Status.COMPLETED);

        when(webhookWorkflowSyncExecutor.execute(eq(workflowExecutionId), any()))
            .thenReturn(new TriggerOutput(List.of("first", "second"), null, false));
        when(principalJobFacade.createJob(any(JobParametersDTO.class), eq(JOB_PRINCIPAL_ID),
            eq(PlatformType.AUTOMATION)))
                .thenReturn(3L, 4L);
        when(jobCompletionAwaiter.await(eq(3L), any())).thenReturn(CompletableFuture.completedFuture(firstJob));
        when(jobCompletionAwaiter.await(eq(4L), any())).thenReturn(CompletableFuture.completedFuture(secondJob));
        when(taskExecutionService.fetchLastJobTaskExecution(any(Long.class))).thenReturn(Optional.empty());

        FileEntry firstOutput = mockFileEntry();
        FileEntry secondOutput = mockFileEntry();

        when(taskExecutionService.getJobTaskExecutions(3L))
            .thenReturn(List.of(webhookResponseTaskExecution(30L, Instant.parse("2026-06-20T10:00:00Z"), firstOutput)));
        when(taskExecutionService.getJobTaskExecutions(4L))
            .thenReturn(
                List.of(webhookResponseTaskExecution(40L, Instant.parse("2026-06-20T10:00:00Z"), secondOutput)));
        when(taskFileStorage.readTaskExecutionOutput(firstOutput)).thenReturn("first-response");
        when(taskFileStorage.readTaskExecutionOutput(secondOutput)).thenReturn("second-response");

        stubJobOutputs(3L, "first-response");
        stubJobOutputs(4L, "second-response");

        Object outputs = webhookWorkflowExecutor.executeSync(workflowExecutionId, mockWebhookRequest())
            .get();

        assertThat(outputs)
            .isEqualTo(
                List.of(
                    Map.of(MetadataConstants.WEBHOOK_RESPONSE, "first-response"),
                    Map.of(MetadataConstants.WEBHOOK_RESPONSE, "second-response")));
    }

    @Test
    public void testExecuteSyncReturnsNullWhenNoWebhookResponse() throws Exception {
        Job job = job(5L, Job.Status.COMPLETED);

        stubTriggerAndJob("payload", false, 5L, job);

        when(taskExecutionService.getJobTaskExecutions(5L)).thenReturn(List.of());

        Object outputs = webhookWorkflowExecutor.executeSync(workflowExecutionId, mockWebhookRequest())
            .get();

        assertThat(outputs).isNull();
    }

    @Test
    public void testExecuteSyncSurfacesFailedJobError() {
        Job job = job(6L, Job.Status.FAILED);

        job.setError(new com.bytechef.error.ExecutionError("boom", List.of()));

        when(webhookWorkflowSyncExecutor.execute(eq(workflowExecutionId), any()))
            .thenReturn(new TriggerOutput("payload", null, false));
        when(principalJobFacade.createJob(any(JobParametersDTO.class), eq(JOB_PRINCIPAL_ID),
            eq(PlatformType.AUTOMATION)))
                .thenReturn(6L);
        when(jobCompletionAwaiter.await(eq(6L), any())).thenReturn(CompletableFuture.completedFuture(job));
        when(taskExecutionService.fetchLastJobTaskExecution(6L)).thenReturn(Optional.empty());

        CompletableFuture<?> future = webhookWorkflowExecutor.executeSync(workflowExecutionId, mockWebhookRequest());

        assertThatThrownBy(future::get)
            .hasRootCauseInstanceOf(ExecutionException.class)
            .hasRootCauseMessage("boom");
    }

    @Test
    public void testTighterPlanSyncRunTimeoutCapsAwaitTimeout() throws Exception {
        PlanLimits planLimits = new PlanLimits(
            PlanTier.FREE, null, null, null, null, PlanLimits.DEFAULT_BURST_MULTIPLIER, null,
            Duration.ofSeconds(1), null, null, null, null, null);

        when(planLimitsProviderObjectProvider.getIfAvailable()).thenReturn(tenantId -> planLimits);

        Job job = job(7L, Job.Status.COMPLETED);

        stubTriggerAndJob("payload", false, 7L, job);

        when(taskExecutionService.getJobTaskExecutions(7L)).thenReturn(List.of());

        webhookWorkflowExecutor.executeSync(workflowExecutionId, mockWebhookRequest())
            .get();

        verify(jobCompletionAwaiter).await(7L, Duration.ofSeconds(1));
    }

    @Test
    public void testLooserPlanSyncRunTimeoutKeepsConfiguredDefault() throws Exception {
        // Plan allows 10s but the executor was configured with 5s: the plan can only tighten, never extend.
        PlanLimits planLimits = new PlanLimits(
            PlanTier.PRO, null, null, null, null, PlanLimits.DEFAULT_BURST_MULTIPLIER, null,
            Duration.ofSeconds(10), null, null, null, null, null);

        when(planLimitsProviderObjectProvider.getIfAvailable()).thenReturn(tenantId -> planLimits);

        Job job = job(8L, Job.Status.COMPLETED);

        stubTriggerAndJob("payload", false, 8L, job);

        when(taskExecutionService.getJobTaskExecutions(8L)).thenReturn(List.of());

        webhookWorkflowExecutor.executeSync(workflowExecutionId, mockWebhookRequest())
            .get();

        verify(jobCompletionAwaiter).await(8L, Duration.ofSeconds(5));
    }

    private void stubTriggerAndJob(Object triggerValue, boolean batch, long jobId, Job job) {
        when(webhookWorkflowSyncExecutor.execute(eq(workflowExecutionId), any()))
            .thenReturn(new TriggerOutput(triggerValue, null, batch));
        when(principalJobFacade.createJob(any(JobParametersDTO.class), eq(JOB_PRINCIPAL_ID),
            eq(PlatformType.AUTOMATION)))
                .thenReturn(jobId);
        when(jobCompletionAwaiter.await(eq(jobId), any())).thenReturn(CompletableFuture.completedFuture(job));
        when(taskExecutionService.fetchLastJobTaskExecution(jobId)).thenReturn(Optional.empty());
    }

    private void stubJobOutputs(long jobId, String webhookResponse) {
        Map<String, Object> outputsMap = Map.of(MetadataConstants.WEBHOOK_RESPONSE, webhookResponse);

        FileEntry outputsFileEntry = mockFileEntry();

        when(taskFileStorage.storeJobOutputs(jobId, outputsMap)).thenReturn(outputsFileEntry);

        doReturn(outputsMap).when(taskFileStorage)
            .readJobOutputs(outputsFileEntry);
    }

    private static Job job(long id, Job.Status status) {
        Job job = new Job();

        job.setId(id);
        job.setStatus(status);

        return job;
    }

    private static TaskExecution webhookResponseTaskExecution(long id, Instant endDate, FileEntry output) {
        TaskExecution taskExecution = new TaskExecution();

        taskExecution.setId(id);
        taskExecution.setEndDate(endDate);
        taskExecution.putMetadata(MetadataConstants.WEBHOOK_RESPONSE, true);
        taskExecution.setOutput(output);

        return taskExecution;
    }

    private static FileEntry mockFileEntry() {
        return mock(FileEntry.class);
    }

    private static WebhookRequest mockWebhookRequest() {
        return mock(WebhookRequest.class);
    }
}
