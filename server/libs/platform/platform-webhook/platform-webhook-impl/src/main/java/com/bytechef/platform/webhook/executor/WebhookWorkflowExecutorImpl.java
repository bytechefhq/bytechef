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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.HttpStatus;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.domain.WebhookTriggerFlags;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.job.sync.SseStreamBridge;
import com.bytechef.platform.webhook.executor.SseStreamBridgeRegistry.Registration;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.event.TriggerWebhookEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerWebhookEvent.WebhookParameters;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.JobExecutionErrors;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
public class WebhookWorkflowExecutorImpl implements WebhookWorkflowExecutor {

    private static final Logger log = LoggerFactory.getLogger(WebhookWorkflowExecutorImpl.class);

    private final ApplicationEventPublisher eventPublisher;
    private final JobCompletionAwaiter jobCompletionAwaiter;
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final PrincipalJobFacade principalJobFacade;
    private final SseStreamBridgeRegistry sseStreamBridgeRegistry;
    private final Duration syncTimeout;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final TriggerDefinitionService triggerDefinitionService;
    private final WebhookWorkflowSyncExecutor webhookWorkflowSyncExecutor;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public WebhookWorkflowExecutorImpl(
        ApplicationEventPublisher eventPublisher, JobCompletionAwaiter jobCompletionAwaiter,
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry, PrincipalJobFacade principalJobFacade,
        SseStreamBridgeRegistry sseStreamBridgeRegistry, TaskExecutionService taskExecutionService,
        TaskFileStorage taskFileStorage, TriggerDefinitionService triggerDefinitionService,
        WebhookWorkflowSyncExecutor webhookWorkflowSyncExecutor, WorkflowService workflowService,
        Duration syncTimeout) {

        this.eventPublisher = eventPublisher;
        this.jobCompletionAwaiter = jobCompletionAwaiter;
        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.principalJobFacade = principalJobFacade;
        this.sseStreamBridgeRegistry = sseStreamBridgeRegistry;
        this.syncTimeout = syncTimeout;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
        this.triggerDefinitionService = triggerDefinitionService;
        this.webhookWorkflowSyncExecutor = webhookWorkflowSyncExecutor;
        this.workflowService = workflowService;
    }

    @Override
    public void executeAsync(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {
        eventPublisher.publishEvent(
            new TriggerWebhookEvent(new WebhookParameters(workflowExecutionId, webhookRequest)));
    }

    @Override
    public CompletableFuture<Void> executeAsync(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest, SseStreamBridge sseStreamBridge) {

        TriggerOutput triggerOutput = webhookWorkflowSyncExecutor.execute(workflowExecutionId, webhookRequest);

        String workflowId = getWorkflowId(workflowExecutionId);
        Map<String, ?> inputMap = getInputMap(workflowExecutionId);

        if (!triggerOutput.batch() && triggerOutput.value() instanceof Collection<?>) {
            throw new IllegalStateException("Trigger output value must not be a collection");
        }

        Map<String, Object> inputs = new java.util.HashMap<>(inputMap);

        inputs.put(workflowExecutionId.getTriggerName(), triggerOutput.value());

        long jobId = TenantContext.callWithTenantId(
            workflowExecutionId.getTenantId(),
            () -> principalJobFacade.createJob(
                new JobParametersDTO(workflowId, inputs), workflowExecutionId.getJobPrincipalId(),
                workflowExecutionId.getType()));

        Registration registration = sseStreamBridgeRegistry.register(jobId, sseStreamBridge);

        sseStreamBridge.onEvent(Map.of("event", "start", "payload", Map.of("jobId", String.valueOf(jobId))));

        CompletableFuture<Void> completion = registration.completion();

        return completion.whenComplete((result, throwable) -> {
            try {
                AutoCloseable handle = registration.handle();

                handle.close();
            } catch (Exception exception) {
                if (log.isTraceEnabled()) {
                    log.trace(exception.getMessage(), exception);
                }
            }
        });
    }

    @Override
    public CompletableFuture<@Nullable Object> executeSync(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {

        TriggerOutput triggerOutput = webhookWorkflowSyncExecutor.execute(workflowExecutionId, webhookRequest);

        Map<String, ?> inputMap = getInputMap(workflowExecutionId);
        String workflowId = getWorkflowId(workflowExecutionId);

        if (!triggerOutput.batch() && triggerOutput.value() instanceof Collection<?> triggerOutputValues) {
            List<CompletableFuture<@Nullable Object>> futures = new ArrayList<>();

            for (Object triggerOutputValue : triggerOutputValues) {
                futures.add(runJob(workflowExecutionId, workflowId, inputMap, triggerOutputValue));
            }

            return CompletableFuture
                .allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(unused -> futures.stream()
                    .map(CompletableFuture::join)
                    .toList());
        }

        return runJob(workflowExecutionId, workflowId, inputMap, triggerOutput.value());
    }

    @Override
    public CompletableFuture<Void> stream(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest, SseStreamBridge sseStreamBridge) {

        if (isWorkflowDisabled(workflowExecutionId)) {
            sseStreamBridge.onError(new IllegalStateException("Workflow is disabled."));

            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = executeAsync(workflowExecutionId, webhookRequest, sseStreamBridge);

        future.whenComplete((unused, throwable) -> {
            if (throwable != null) {
                sseStreamBridge.onError(throwable);
            } else {
                sseStreamBridge.onComplete();
            }
        });

        return future;
    }

    @Override
    public WebhookTriggerFlags getWebhookTriggerFlags(WorkflowExecutionId workflowExecutionId) {
        WorkflowNodeType workflowNodeType = getComponentOperation(workflowExecutionId);

        return triggerDefinitionService.getWebhookTriggerFlags(
            workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation());
    }

    @Override
    public boolean hasStreamingTask(WorkflowExecutionId workflowExecutionId) {
        Workflow workflow = workflowService.getWorkflow(getWorkflowId(workflowExecutionId));

        return workflow.getTasks(true)
            .stream()
            .map(WorkflowTask::getType)
            .filter(Objects::nonNull)
            .anyMatch(type -> type.toLowerCase()
                .contains("stream"));
    }

    @Override
    public boolean isWorkflowDisabled(WorkflowExecutionId workflowExecutionId) {
        JobPrincipalAccessor jobPrincipalAccessor =
            jobPrincipalAccessorRegistry.getJobPrincipalAccessor(workflowExecutionId.getType());

        return !jobPrincipalAccessor.isWorkflowEnabled(
            workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());
    }

    @Override
    public WebhookValidateResponse validateAndExecuteAsync(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {

        WebhookValidateResponse response = webhookWorkflowSyncExecutor.validate(workflowExecutionId, webhookRequest);

        if (response.status() == HttpStatus.OK.getValue()) {
            executeAsync(workflowExecutionId, webhookRequest);
        }

        return response;
    }

    @Override
    public WebhookValidateResponse validateOnEnable(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {

        return webhookWorkflowSyncExecutor.validateOnEnable(workflowExecutionId, webhookRequest);
    }

    /**
     * Creates the job on the distributed coordinator and returns a future that collects the workflow outputs once the
     * job reaches a terminal status. Replaces the embedded {@code JobSyncExecutor}: the job runs on the worker fleet
     * and completion is signalled through {@link JobCompletionAwaiter}, while the webhook response is read back from
     * the tagged task output after completion.
     */
    private CompletableFuture<@Nullable Object> runJob(
        WorkflowExecutionId workflowExecutionId, String workflowId, Map<String, ?> inputMap,
        Object triggerOutputValue) {

        long jobId = TenantContext.callWithTenantId(
            workflowExecutionId.getTenantId(),
            () -> principalJobFacade.createJob(
                createJobParameters(workflowExecutionId, workflowId, inputMap, triggerOutputValue),
                workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getType()));

        return jobCompletionAwaiter.await(jobId, syncTimeout)
            .thenApply(job -> TenantContext.callWithTenantId(
                workflowExecutionId.getTenantId(), () -> collectOutputs(job)));
    }

    private @Nullable Object collectOutputs(Job job) {
        JobExecutionErrors.checkForError(job, taskExecutionService);

        long jobId = Validate.notNull(job.getId(), "id");

        Object webhookResponse = readWebhookResponse(jobId);

        if (webhookResponse != null) {
            job.setOutputs(
                taskFileStorage.storeJobOutputs(jobId, Map.of(MetadataConstants.WEBHOOK_RESPONSE, webhookResponse)));
        }

        return job.getOutputs() == null ? null : taskFileStorage.readJobOutputs(job.getOutputs());
    }

    /**
     * Reads the {@code WEBHOOK_RESPONSE}-tagged task output for a completed job. A run may execute multiple
     * {@code WebhookResponse} actions (usually sequentially, possibly in racing parallel branches); the HTTP reply is
     * the last-to-complete one, so the task with the latest {@code endDate} wins, with id as a deterministic
     * tiebreaker.
     */
    private @Nullable Object readWebhookResponse(long jobId) {
        TaskExecution lastTaskExecution = taskExecutionService.getJobTaskExecutions(jobId)
            .stream()
            .filter(taskExecution -> taskExecution.getMetadata()
                .containsKey(MetadataConstants.WEBHOOK_RESPONSE))
            .filter(taskExecution -> taskExecution.getOutput() != null)
            .max(Comparator
                .comparing(TaskExecution::getEndDate, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(TaskExecution::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
            .orElse(null);

        if (lastTaskExecution == null) {
            return null;
        }

        FileEntry outputFileEntry = lastTaskExecution.getOutput();

        return outputFileEntry == null ? null : taskFileStorage.readTaskExecutionOutput(outputFileEntry);
    }

    @SuppressWarnings("unchecked")
    private static JobParametersDTO createJobParameters(
        WorkflowExecutionId workflowExecutionId, String workflowId, Map<String, ?> inputMap,
        Object triggerOutputValue) {

        Map<String, Object> concat = MapUtils.concat(
            (Map<String, Object>) inputMap, Map.of(workflowExecutionId.getTriggerName(), triggerOutputValue));

        return new JobParametersDTO(workflowId, concat);
    }

    private Map<String, ?> getInputMap(WorkflowExecutionId workflowExecutionId) {
        JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(
            workflowExecutionId.getType());

        return jobPrincipalAccessor.getInputMap(
            workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());
    }

    private WorkflowNodeType getComponentOperation(WorkflowExecutionId workflowExecutionId) {
        Workflow workflow = workflowService.getWorkflow(getWorkflowId(workflowExecutionId));

        WorkflowTrigger workflowTrigger = WorkflowTrigger.of(workflowExecutionId.getTriggerName(), workflow);

        return WorkflowNodeType.ofType(workflowTrigger.getType());
    }

    private String getWorkflowId(WorkflowExecutionId workflowExecutionId) {
        JobPrincipalAccessor jobPrincipalAccessor = jobPrincipalAccessorRegistry.getJobPrincipalAccessor(
            workflowExecutionId.getType());

        if (workflowExecutionId.getJobPrincipalId() == -1) {
            return jobPrincipalAccessor.getLastWorkflowId(workflowExecutionId.getWorkflowUuid());
        }

        return jobPrincipalAccessor.getWorkflowId(
            workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());
    }
}
