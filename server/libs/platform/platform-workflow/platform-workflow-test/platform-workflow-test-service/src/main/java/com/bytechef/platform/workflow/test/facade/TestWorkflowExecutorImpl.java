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

package com.bytechef.platform.workflow.test.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.commons.util.RandomUtils;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.WorkflowTestConfiguration;
import com.bytechef.platform.configuration.domain.WorkflowTestConfigurationConnection;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.dto.WorkflowNodeOutputDTO;
import com.bytechef.platform.configuration.facade.WorkflowNodeOutputFacade;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.job.sync.SseStreamBridge;
import com.bytechef.platform.job.sync.executor.JobSyncExecutor;
import com.bytechef.platform.webhook.executor.constant.WebhookConstants;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution;
import com.bytechef.platform.workflow.execution.domain.TriggerExecution.Status;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.platform.workflow.execution.dto.TriggerExecutionDTO;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.platform.workflow.test.dto.WorkflowTestExecutionDTO;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.util.TenantCacheKeyUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;

/**
 * Implementation of TestWorkflowExecutor for executing workflow tests.
 *
 * @author Ivica Cardic
 */
public class TestWorkflowExecutorImpl implements TestWorkflowExecutor {

    private static final Logger logger = LoggerFactory.getLogger(TestWorkflowExecutorImpl.class);

    private final ComponentDefinitionService componentDefinitionService;
    private final ContextService contextService;
    private final Evaluator evaluator;
    private final JobSyncExecutor jobSyncExecutor;
    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;
    private final WorkflowNodeOutputFacade workflowNodeOutputFacade;
    private final WorkflowService workflowService;
    private final WorkflowTestConfigurationService workflowTestConfigurationService;

    @SuppressFBWarnings("EI")
    public TestWorkflowExecutorImpl(
        ComponentDefinitionService componentDefinitionService, ContextService contextService, Evaluator evaluator,
        JobSyncExecutor jobSyncExecutor, TaskDispatcherDefinitionService taskDispatcherDefinitionService,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage, WorkflowService workflowService,
        WorkflowNodeOutputFacade workflowNodeOutputFacade,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        this.componentDefinitionService = componentDefinitionService;
        this.contextService = contextService;
        this.evaluator = evaluator;
        this.jobSyncExecutor = jobSyncExecutor;
        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
        this.workflowService = workflowService;
        this.workflowNodeOutputFacade = workflowNodeOutputFacade;
        this.workflowTestConfigurationService = workflowTestConfigurationService;
    }

    @Override
    public void executeAsync(
        String workflowId, Map<String, Object> inputs, long environmentId, Consumer<String> afterStartCallback,
        Function<String, SseStreamBridge> sseStreamBridgeFactory,
        BiConsumer<String, CompletableFuture<WorkflowTestExecutionDTO>> afterFutureCallback,
        Consumer<String> whenCompleteCallback) {

        WorkflowTestParameters workflowTestParameters = getWorkflowTestParameters(workflowId, inputs, environmentId);

        long jobId = jobSyncExecutor.startJob(workflowTestParameters.jobParametersDTO());

        String key = TenantCacheKeyUtils.getKey(jobId);

        afterStartCallback.accept(key);

        SseStreamBridge sseStreamBridge = sseStreamBridgeFactory.apply(key);

        List<AutoCloseable> handles = registerListeners(jobId, sseStreamBridge);

        String currentTenantId = TenantContext.getCurrentTenantId();

        CompletableFuture<WorkflowTestExecutionDTO> future = CompletableFuture.supplyAsync(
            () -> TenantContext.callWithTenantId(currentTenantId, () -> {
                sseStreamBridge.onEvent(Map.of("event", "start", "payload", Map.of("jobId", String.valueOf(jobId))));

                JobDTO jobDTO = await(jobId);

                return new WorkflowTestExecutionDTO(jobDTO, null);
            }));

        afterFutureCallback.accept(key, future);

        future.whenComplete((result, throwable) -> {
            try {
                if (throwable != null) {
                    if (throwable instanceof CancellationException) {
                        stop(jobId);
                    }

                    sseStreamBridge.onEvent(
                        Map.of(
                            "event", "error",
                            "payload", Objects.toString(throwable.getMessage(), "An error occurred")));
                } else {
                    sseStreamBridge.onEvent(Map.of("event", "result", "payload", result));
                }
            } finally {
                whenCompleteCallback.accept(key);

                unregisterListeners(handles);
            }
        });
    }

    @Override
    public WorkflowTestExecutionDTO executeSync(String workflowId, Map<String, Object> inputs, long environmentId) {
        WorkflowTestParameters workflowTestParameters = getWorkflowTestParameters(workflowId, inputs, environmentId);

        return new WorkflowTestExecutionDTO(
            execute(workflowTestParameters.jobParametersDTO()), workflowTestParameters.triggerExecutionDTO());
    }

    @Override
    public void stop(long jobId) {
        jobSyncExecutor.stopJob(jobId);
    }

    private JobDTO await(long jobId) {
        Job job = jobSyncExecutor.awaitJob(jobId, false);

        try {
            return new JobDTO(
                job, getOutputs(job),
                CollectionUtils.map(
                    taskExecutionService.getJobTaskExecutions(Validate.notNull(job.getId(), "id")),
                    taskExecution -> {
                        Map<String, ?> context = taskFileStorage.readContextValue(
                            contextService.peek(
                                Validate.notNull(taskExecution.getId(), "id"), Context.Classname.TASK_EXECUTION));

                        WorkflowTask workflowTask = taskExecution.getWorkflowTask();
                        DefinitionResult definitionResult = getDefinition(taskExecution);

                        Object output = taskExecution.getOutput() == null
                            ? null
                            : taskFileStorage.readTaskExecutionOutput(taskExecution.getOutput());

                        return new TaskExecutionDTO(
                            taskExecution, definitionResult.title(), definitionResult.icon(),
                            workflowTask.evaluateParameters(context, evaluator), output);
                    }));
        } finally {
            jobSyncExecutor.deleteJob(Validate.notNull(job.getId(), "id"));
        }
    }

    private JobDTO execute(JobParametersDTO jobParametersDTO) {
        long jobId = jobSyncExecutor.startJob(jobParametersDTO);

        return await(jobId);
    }

    private DefinitionResult getDefinition(TaskExecution taskExecution) {
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(taskExecution.getType());

        if (componentDefinitionService.hasComponentDefinition(
            workflowNodeType.name(), workflowNodeType.version())) {

            ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
                workflowNodeType.name(), workflowNodeType.version());

            return new DefinitionResult(componentDefinition.getTitle(), componentDefinition.getIcon());
        }

        TaskDispatcherDefinition taskDispatcherDefinition = taskDispatcherDefinitionService.getTaskDispatcherDefinition(
            workflowNodeType.name(), workflowNodeType.version());

        return new DefinitionResult(taskDispatcherDefinition.getTitle(), taskDispatcherDefinition.getIcon());
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> getOutputs(Job job) {
        Map<String, ?> outputs = null;

        if (job.getOutputs() != null) {
            outputs = taskFileStorage.readJobOutputs(job.getOutputs());

            if (outputs.containsKey(WebhookConstants.WEBHOOK_RESPONSE)) {
                ActionDefinition.WebhookResponse webhookResponse = MapUtils.getRequired(
                    outputs, WebhookConstants.WEBHOOK_RESPONSE, new TypeReference<>() {});

                outputs = (Map<String, ?>) webhookResponse.getBody();
            } else {
                outputs = taskFileStorage.readContextValue(job.getOutputs());
            }
        }

        return outputs;
    }

    /**
     * Gets the workflow test parameters including inputs and trigger execution data.
     *
     * <p>
     * <b>Security Note:</b> The PREDICTABLE_RANDOM suppression is a <b>false positive</b>. This method uses
     * {@link com.bytechef.commons.util.RandomUtils#nextLong()} which is backed by {@link java.security.SecureRandom},
     * not {@link java.util.Random}. The static analysis tool incorrectly flags this because it cannot trace through the
     * utility class. The random values generated are used only for generating temporary test execution IDs which have
     * no security implications.
     */
    @SuppressFBWarnings("PREDICTABLE_RANDOM")
    @SuppressWarnings("unchecked")
    private WorkflowTestParameters getWorkflowTestParameters(
        String workflowId, Map<String, Object> inputs, long environmentId) {

        Optional<WorkflowTestConfiguration> workflowTestConfigurationOptional =
            workflowTestConfigurationService.fetchWorkflowTestConfiguration(workflowId, environmentId);

        Map<String, Map<String, Long>> connectionIdsMap = new HashMap<>();

        List<WorkflowTestConfigurationConnection> workflowTestConfigurationConnections =
            workflowTestConfigurationOptional
                .map(WorkflowTestConfiguration::getConnections)
                .orElse(List.of());

        for (WorkflowTestConfigurationConnection connection : workflowTestConfigurationConnections) {
            Map<String, Long> connectionIdMap = connectionIdsMap.computeIfAbsent(
                connection.getWorkflowNodeName(), key -> new HashMap<>());

            connectionIdMap.put(connection.getWorkflowConnectionKey(), connection.getConnectionId());
        }

        Workflow workflow = workflowService.getWorkflow(workflowId);

        TriggerExecutionDTO triggerExecutionDTO = null;

        List<WorkflowTrigger> workflowTriggers = WorkflowTrigger.of(workflow);

        if (workflowTriggers.isEmpty()) {
            Map<String, ?> workflowTestConfigurationInputs = workflowTestConfigurationOptional
                .map(WorkflowTestConfiguration::getInputs)
                .orElse(Map.of());

            inputs = MapUtils.concat(inputs, (Map<String, Object>) workflowTestConfigurationInputs);
        } else {
            WorkflowTrigger workflowTrigger = workflowTriggers.getFirst();

            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            WorkflowNodeOutputDTO workflowNodeOutputDTO = workflowNodeOutputFacade.getWorkflowNodeOutput(
                workflowId, workflowTrigger.getName(), environmentId);

            TriggerExecution triggerExecution = TriggerExecution.builder()
                .id(-RandomUtils.nextLong())
                .startDate(Instant.now())
                .endDate(Instant.now())
                .status(Status.COMPLETED)
                .workflowTrigger(workflowTrigger)
                .build();

            ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
                workflowNodeType.name(), workflowNodeType.version());

            Map<String, ?> workflowTestConfigurationInputs = workflowTestConfigurationOptional
                .map(WorkflowTestConfiguration::getInputs)
                .orElse(Map.of());

            Object sampleOutput = workflowNodeOutputDTO.getSampleOutput();

            if (sampleOutput == null) {
                sampleOutput = Map.of();
            }

            triggerExecutionDTO = new TriggerExecutionDTO(
                triggerExecution, componentDefinition.getTitle(), componentDefinition.getIcon(),
                workflowTestConfigurationInputs, sampleOutput);

            if (inputs.isEmpty()) {
                WorkflowNodeType triggerNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

                TriggerDefinition triggerDefinition = componentDefinition.getTriggers()
                    .stream()
                    .filter(curTriggerDefinition -> Objects.equals(
                        curTriggerDefinition.getName(), triggerNodeType.operation()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Trigger definition not found"));

                if (!triggerDefinition.isBatch() && sampleOutput instanceof List<?> list) {
                    sampleOutput = list.getFirst();
                }

                inputs = MapUtils.concat(
                    (Map<String, Object>) workflowTestConfigurationInputs,
                    Map.of(workflowTrigger.getName(), sampleOutput));
            }
        }

        return new WorkflowTestParameters(
            new JobParametersDTO(workflowId, inputs, Map.of(MetadataConstants.CONNECTION_IDS, connectionIdsMap)),
            triggerExecutionDTO);
    }

    private List<AutoCloseable> registerListeners(long jobId, SseStreamBridge sseStreamBridge) {
        List<AutoCloseable> handles = new ArrayList<>();

        handles.add(jobSyncExecutor.addJobStatusListener(
            jobId, (event) -> sseStreamBridge.onEvent(Map.of("event", "job", "payload", event))));

        handles.add(
            jobSyncExecutor.addTaskStartedListener(
                jobId, (event) -> sseStreamBridge.onEvent(Map.of("event", "task", "payload", event))));

        handles.add(
            jobSyncExecutor.addTaskExecutionCompleteListener(
                jobId, (event) -> sseStreamBridge.onEvent(Map.of("event", "task", "payload", event))));

        handles.add(
            jobSyncExecutor.addErrorListener(jobId, (event) -> {
                if (logger.isDebugEnabled()) {
                    logger.debug("Received error event for job {}: {}", jobId, event);
                }
            }));

        handles.add(jobSyncExecutor.addSseStreamBridge(jobId, sseStreamBridge));

        return handles;
    }

    private void unregisterListeners(List<AutoCloseable> handles) {
        if (handles != null) {
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
    }

    private record DefinitionResult(String title, String icon) {
    }

    private record WorkflowTestParameters(
        JobParametersDTO jobParametersDTO, TriggerExecutionDTO triggerExecutionDTO) {
    }
}
