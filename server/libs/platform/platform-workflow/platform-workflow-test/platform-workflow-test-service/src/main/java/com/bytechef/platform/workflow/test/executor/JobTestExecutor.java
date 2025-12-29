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

package com.bytechef.platform.workflow.test.executor;

import static com.bytechef.platform.workflow.test.dto.TaskStatusEventDTO.Status.COMPLETED;
import static com.bytechef.platform.workflow.test.dto.TaskStatusEventDTO.Status.STARTED;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.coordinator.event.ErrorEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.TaskExecutionCompleteEvent;
import com.bytechef.atlas.coordinator.event.TaskStartedApplicationEvent;
import com.bytechef.atlas.execution.domain.Context;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.ContextService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionDefinition.WebhookResponse;
import com.bytechef.error.ExecutionError;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.coordinator.job.JobSyncExecutor;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.webhook.executor.constant.WebhookConstants;
import com.bytechef.platform.workflow.execution.dto.JobDTO;
import com.bytechef.platform.workflow.execution.dto.TaskExecutionDTO;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.platform.workflow.test.dto.ExecutionErrorEventDTO;
import com.bytechef.platform.workflow.test.dto.JobStatusEventDTO;
import com.bytechef.platform.workflow.test.dto.TaskStatusEventDTO;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.apache.commons.lang3.Validate;
import tools.jackson.core.type.TypeReference;

/**
 * @author Ivica Cardic
 */
public class JobTestExecutor {

    private final ComponentDefinitionService componentDefinitionService;
    private final ContextService contextService;
    private final Evaluator evaluator;
    private final JobSyncExecutor jobSyncExecutor;
    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI")
    public JobTestExecutor(
        ComponentDefinitionService componentDefinitionService, ContextService contextService, Evaluator evaluator,
        JobSyncExecutor jobSyncExecutor, TaskDispatcherDefinitionService taskDispatcherDefinitionService,
        TaskExecutionService taskExecutionService, TaskFileStorage taskFileStorage) {

        this.componentDefinitionService = componentDefinitionService;
        this.contextService = contextService;
        this.evaluator = evaluator;
        this.jobSyncExecutor = jobSyncExecutor;
        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
    }

    /**
     * Executes a job based on the provided job parameters. This method initiates the job by starting it with the given
     * parameters and then waits for the job's completion to retrieve its final data.
     *
     * @param jobParametersDTO the data transfer object containing the parameters required to configure and execute the
     *                         job
     * @return a {@link JobDTO} instance containing the processed job data, including execution details and outputs
     */
    public JobDTO execute(JobParametersDTO jobParametersDTO) {
        long jobId = start(jobParametersDTO);

        return await(jobId);
    }

    /**
     * Starts a new job based on the given job parameters and returns its unique identifier. This method delegates the
     * job creation and starting process to the job synchronization executor.
     *
     * @param jobParametersDTO the data transfer object containing the parameters required to create and start the job
     * @return the unique identifier of the newly created and started job
     */
    public long start(JobParametersDTO jobParametersDTO) {
        return jobSyncExecutor.startJob(jobParametersDTO);
    }

    /**
     * Awaits the completion of a job identified by its unique job ID, processes the job's data, and returns a data
     * transfer object (DTO) representation of the job. The method ensures that after processing the job, its resources
     * are cleaned up.
     *
     * @param jobId the unique identifier of the job to be awaited and processed
     * @return a {@link JobDTO} object containing the processed job data, including task executions and outputs
     */
    public JobDTO await(long jobId) {
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

    /**
     * Stops the execution of a running job identified by its unique job ID. This method delegates the stop signal to
     * the job synchronization executor to halt the specified job in the in-memory pipeline.
     *
     * @param jobId the unique identifier of the job to be stopped
     */
    public void stop(long jobId) {
        jobSyncExecutor.stopJob(jobId);
    }

    /**
     * Adds an error listener for a specific job identified by the given job ID. When an error occurs during the job's
     * execution, the listener will receive an {@link ExecutionErrorEventDTO} containing the job ID and the
     * corresponding error message. The method returns an {@link AutoCloseable} instance that allows the caller to
     * unregister the listener when no longer needed.
     *
     * @param jobId    the unique identifier of the job for which the error listener is to be added
     * @param listener a {@link Consumer} that processes {@link ExecutionErrorEventDTO} when an error occurs
     * @return an {@link AutoCloseable} instance that can be used to remove the added listener
     */
    public AutoCloseable addErrorListener(long jobId, Consumer<ExecutionErrorEventDTO> listener) {
        return jobSyncExecutor.addErrorListener(jobId, (ErrorEvent err) -> {
            ExecutionError executionError = err.getError();

            String message = executionError != null ? executionError.getMessage() : "Error";

            listener.accept(new ExecutionErrorEventDTO(jobId, message));
        });
    }

    /**
     * Adds a listener that will be triggered when the status of a job with the specified job ID changes. The listener
     * receives a {@link JobStatusEventDTO} containing the job's ID, its new status, and the timestamp of the change.
     * This method returns an {@link AutoCloseable} instance, allowing the caller to unregister the listener when it is
     * no longer needed.
     *
     * @param jobId    the unique identifier of the job whose status changes the listener will monitor
     * @param listener a {@link Consumer} that processes {@link JobStatusEventDTO} when a status change occurs
     * @return an {@link AutoCloseable} instance that can be used to remove the added listener
     */
    public AutoCloseable addJobStatusListener(long jobId, Consumer<JobStatusEventDTO> listener) {
        return jobSyncExecutor.addJobStatusListener(jobId, (JobStatusApplicationEvent event) -> {
            Job.Status status = event.getStatus();

            listener.accept(new JobStatusEventDTO(jobId, status.name(), event.getCreateDate()));
        });
    }

    /**
     * Adds a listener that will be triggered when a task associated with the specified job ID is marked as completed.
     * The listener receives a {@link TaskStatusEventDTO} containing details about the completed task, such as its ID,
     * status, name, type, and execution time range (start and end timestamps). The method returns an
     * {@link AutoCloseable} instance that can be used to remove the added listener, ensuring proper resource cleanup
     * when the listener is no longer needed.
     *
     * @param jobId    the unique identifier of the job whose task completion events are to be listened to
     * @param listener a {@link Consumer} that processes {@link TaskStatusEventDTO} objects for corresponding task
     *                 completion events
     * @return an {@link AutoCloseable} instance that can be used to unregister the added listener
     */
    public AutoCloseable addTaskExecutionCompleteListener(long jobId, Consumer<TaskStatusEventDTO> listener) {
        return jobSyncExecutor.addTaskExecutionCompleteListener(jobId, (TaskExecutionCompleteEvent event) -> {
            TaskExecution taskExecution = event.getTaskExecution();

            listener.accept(
                new TaskStatusEventDTO(
                    jobId, Objects.requireNonNull(taskExecution.getId()), COMPLETED, taskExecution.getName(),
                    taskExecution.getType(), taskExecution.getStartDate(), taskExecution.getEndDate()));
        });
    }

    /**
     * Adds a listener that will be triggered when a task associated with the specified job ID is started. The listener
     * receives a {@link TaskStatusEventDTO} containing details about the started task, such as its ID, status, and
     * timestamp. The method returns an {@link AutoCloseable} instance that can be used to remove the added listener.
     *
     * @param jobId    the unique identifier of the job whose task started events are to be listened to
     * @param listener a {@link Consumer} that processes {@link TaskStatusEventDTO} objects for task started events
     * @return an {@link AutoCloseable} instance that can be used to unregister the added listener
     */
    public AutoCloseable addTaskStartedListener(long jobId, Consumer<TaskStatusEventDTO> listener) {
        return jobSyncExecutor.addTaskStartedListener(jobId, (TaskStartedApplicationEvent e) -> listener.accept(
            new TaskStatusEventDTO(jobId, e.getTaskExecutionId(), STARTED, null, null, e.getCreateDate(), null)));
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
                WebhookResponse webhookResponse = MapUtils.getRequired(
                    outputs, WebhookConstants.WEBHOOK_RESPONSE, new TypeReference<>() {});

                outputs = (Map<String, ?>) webhookResponse.getBody();
            } else {
                outputs = taskFileStorage.readContextValue(job.getOutputs());
            }
        }

        return outputs;
    }

    record DefinitionResult(String title, String icon) {
    }
}
