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

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.HttpStatus;
import com.bytechef.component.definition.TriggerDefinition.WebhookValidateResponse;
import com.bytechef.platform.component.trigger.TriggerOutput;
import com.bytechef.platform.component.trigger.WebhookRequest;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessor;
import com.bytechef.platform.configuration.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.coordinator.job.JobSyncExecutor;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import com.bytechef.platform.workflow.coordinator.event.TriggerWebhookEvent;
import com.bytechef.platform.workflow.coordinator.event.TriggerWebhookEvent.WebhookParameters;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * @author Ivica Cardic
 */
public class WebhookWorkflowExecutorImpl implements WebhookWorkflowExecutor {

    private static final Logger logger = LoggerFactory.getLogger(WebhookWorkflowExecutorImpl.class);

    private final ApplicationEventPublisher eventPublisher;
    private final JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry;
    private final PrincipalJobFacade principalJobFacade;
    private final JobSyncExecutor jobSyncExecutor;
    private final WebhookWorkflowSyncExecutor webhookWorkflowSyncExecutor;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public WebhookWorkflowExecutorImpl(
        ApplicationEventPublisher eventPublisher, JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry,
        PrincipalJobFacade principalJobFacade, JobSyncExecutor jobSyncExecutor,
        WebhookWorkflowSyncExecutor webhookWorkflowSyncExecutor, TaskFileStorage taskFileStorage) {

        this.jobPrincipalAccessorRegistry = jobPrincipalAccessorRegistry;
        this.principalJobFacade = principalJobFacade;
        this.jobSyncExecutor = jobSyncExecutor;
        this.eventPublisher = eventPublisher;
        this.webhookWorkflowSyncExecutor = webhookWorkflowSyncExecutor;
        this.taskFileStorage = taskFileStorage;
    }

    @Override
    public void execute(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {
        eventPublisher.publishEvent(
            new TriggerWebhookEvent(new WebhookParameters(workflowExecutionId, webhookRequest)));
    }

    @Override
    public @Nullable Object executeSync(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {
        Object outputs;

        TriggerOutput triggerOutput = webhookWorkflowSyncExecutor.execute(workflowExecutionId, webhookRequest);

        Map<String, ?> inputMap = getInputMap(workflowExecutionId);
        String workflowId = getWorkflowId(workflowExecutionId);

        if (!triggerOutput.batch() && triggerOutput.value() instanceof Collection<?> triggerOutputValues) {
            List<Map<String, ?>> outputsList = new ArrayList<>();

            for (Object triggerOutputValue : triggerOutputValues) {
                Job job = jobSyncExecutor.execute(
                    createJobParameters(workflowExecutionId, workflowId, inputMap, triggerOutputValue),
                    jobParameters -> principalJobFacade.createSyncJob(
                        jobParameters, workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getType()),
                    true);

                outputsList.add(taskFileStorage.readJobOutputs(job.getOutputs()));
            }

            outputs = outputsList;
        } else {
            Job job = jobSyncExecutor.execute(
                createJobParameters(workflowExecutionId, workflowId, inputMap, triggerOutput.value()),
                jobParameters -> principalJobFacade.createSyncJob(
                    jobParameters, workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getType()),
                true);

            outputs = job.getOutputs() == null ? null : taskFileStorage.readJobOutputs(job.getOutputs());
        }

        return outputs;
    }

    @Override
    public SseEmitter executeSseStream(WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {
        final SseEmitter emitter = new SseEmitter(TimeUnit.MINUTES.toMillis(30));

        try {
            TriggerOutput triggerOutput = webhookWorkflowSyncExecutor.execute(workflowExecutionId, webhookRequest);

            String workflowId = getWorkflowId(workflowExecutionId);
            Map<String, ?> inputMap = getInputMap(workflowExecutionId);
            List<Long> jobIds = new ArrayList<>();

            if (!triggerOutput.batch() && triggerOutput.value() instanceof Collection<?> values) {
                for (Object value : values) {
                    Map<String, Object> inputs = new java.util.HashMap<>(inputMap);

                    inputs.put(workflowExecutionId.getTriggerName(), value);

                    long jobId = jobSyncExecutor.startJob(
                        new JobParametersDTO(workflowId, inputs),
                        jobParameters -> principalJobFacade.createSyncJob(
                            jobParameters, workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getType()));

                    jobIds.add(jobId);

                    CompletableFuture.runAsync(
                        () -> sendEvent(emitter, "start", Map.of("jobId", String.valueOf(jobId))));
                }
            } else {
                Map<String, Object> inputs = new java.util.HashMap<>(inputMap);

                inputs.put(workflowExecutionId.getTriggerName(), triggerOutput.value());

                long jobId = jobSyncExecutor.startJob(
                    new JobParametersDTO(workflowId, inputs),
                    jobParameters -> principalJobFacade.createSyncJob(
                        jobParameters, workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getType()));

                jobIds.add(jobId);

                sendEvent(emitter, "start", Map.of("jobId", String.valueOf(jobId)));
            }

            List<AutoCloseable> handles = new ArrayList<>();

            for (Long jobId : jobIds) {
                handles.add(
                    jobSyncExecutor.addSseStreamBridge(jobId, payload -> sendEvent(emitter, "stream", payload)));
            }

            String currentTenant = TenantContext.getCurrentTenantId();

            CompletableFuture.runAsync(() -> TenantContext.runWithTenantId(currentTenant, () -> {
                try {
                    for (Long jobId : jobIds) {
                        jobSyncExecutor.awaitJob(jobId, false);
                    }
                } finally {
                    try {
                        emitter.complete();
                    } catch (Exception exception) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(exception.getMessage(), exception);
                        }
                    }

                    for (AutoCloseable handle : handles) {
                        try {
                            handle.close();
                        } catch (Exception exception) {
                            if (logger.isTraceEnabled()) {
                                logger.trace(exception.getMessage(), exception);
                            }
                        }
                    }
                }
            }));

            emitter.onCompletion(() -> {
                for (AutoCloseable handle : handles) {
                    try {
                        handle.close();
                    } catch (Exception exception) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(exception.getMessage(), exception);
                        }
                    }
                }
            });

            emitter.onTimeout(() -> {
                for (AutoCloseable handle : handles) {
                    try {
                        handle.close();
                    } catch (Exception exception) {
                        if (logger.isTraceEnabled()) {
                            logger.trace(exception.getMessage(), exception);
                        }
                    }
                }
            });
        } catch (Exception e) {
            try {
                sendEvent(emitter, "error", e.getMessage());
            } finally {
                emitter.complete();
            }
        }

        return emitter;
    }

    @Override
    public WebhookValidateResponse validateAndExecuteAsync(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {

        WebhookValidateResponse response = webhookWorkflowSyncExecutor.validate(workflowExecutionId, webhookRequest);

        if (response.status() == HttpStatus.OK.getValue()) {
            execute(workflowExecutionId, webhookRequest);
        }

        return response;
    }

    @Override
    public WebhookValidateResponse validateOnEnable(
        WorkflowExecutionId workflowExecutionId, WebhookRequest webhookRequest) {

        return webhookWorkflowSyncExecutor.validateOnEnable(workflowExecutionId, webhookRequest);
    }

    @SuppressWarnings("unchecked")
    private static JobParametersDTO createJobParameters(
        WorkflowExecutionId workflowExecutionId, String workflowId, Map<String, ?> inputMap,
        Object triggerOutputValue) {

        return new JobParametersDTO(
            workflowId,
            MapUtils.concat(
                (Map<String, Object>) inputMap, Map.of(workflowExecutionId.getTriggerName(), triggerOutputValue)));
    }

    private Map<String, ?> getInputMap(WorkflowExecutionId workflowExecutionId) {
        JobPrincipalAccessor jobPrincipalAccessor =
            jobPrincipalAccessorRegistry.getJobPrincipalAccessor(workflowExecutionId.getType());

        return jobPrincipalAccessor.getInputMap(
            workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());
    }

    private String getWorkflowId(WorkflowExecutionId workflowExecutionId) {
        JobPrincipalAccessor jobPrincipalAccessor =
            jobPrincipalAccessorRegistry.getJobPrincipalAccessor(workflowExecutionId.getType());

        return jobPrincipalAccessor.getWorkflowId(
            workflowExecutionId.getJobPrincipalId(), workflowExecutionId.getWorkflowUuid());
    }

    private void sendEvent(SseEmitter emitter, String name, Object data) {
        try {
            emitter.send(
                SseEmitter.event()
                    .name(name)
                    .data(data instanceof String ? JsonUtils.write(data) : data));
        } catch (Exception exception) {
            if (logger.isTraceEnabled()) {
                logger.trace(exception.getMessage(), exception);
            }
        }
    }
}
