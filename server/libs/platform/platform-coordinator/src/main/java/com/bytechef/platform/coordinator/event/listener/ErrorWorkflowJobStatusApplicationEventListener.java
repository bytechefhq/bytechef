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

package com.bytechef.platform.coordinator.event.listener;

import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.domain.ErrorWorkflowDispatch;
import com.bytechef.exception.RateLimitExceededException;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.coordinator.ErrorWorkflowDispatchCounter;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dispatches the configured error workflow when a run ends FAILED.
 * <p>
 * Ordered after the cost and workflow-alert listeners so dispatch never delays alerting. Fail-open throughout: handling
 * an error must never manufacture a second one.
 *
 * @author Ivica Cardic
 */
public class ErrorWorkflowJobStatusApplicationEventListener implements ApplicationEventListener {

    public static final String ERROR_HANDLER_FOR = "errorHandlerFor";

    private static final Logger log = LoggerFactory.getLogger(ErrorWorkflowJobStatusApplicationEventListener.class);

    private final AtomicBoolean unsupportedOperationLogged = new AtomicBoolean(false);
    private final ErrorWorkflowPayloadFactory errorWorkflowPayloadFactory;
    private final ErrorWorkflowResolver errorWorkflowResolver;
    private final JobService jobService;
    private final PrincipalJobFacade principalJobFacade;
    private final PrincipalJobService principalJobService;
    private final TaskExecutionService taskExecutionService;
    private final @Nullable ErrorWorkflowDispatchCounter counter;

    @SuppressFBWarnings("EI2")
    public ErrorWorkflowJobStatusApplicationEventListener(
        ErrorWorkflowPayloadFactory errorWorkflowPayloadFactory, ErrorWorkflowResolver errorWorkflowResolver,
        JobService jobService, PrincipalJobFacade principalJobFacade, PrincipalJobService principalJobService,
        TaskExecutionService taskExecutionService, @Nullable ErrorWorkflowDispatchCounter counter) {

        this.errorWorkflowPayloadFactory = errorWorkflowPayloadFactory;
        this.errorWorkflowResolver = errorWorkflowResolver;
        this.jobService = jobService;
        this.principalJobFacade = principalJobFacade;
        this.principalJobService = principalJobService;
        this.taskExecutionService = taskExecutionService;
        this.counter = counter;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (!(applicationEvent instanceof JobStatusApplicationEvent jobStatusApplicationEvent)) {
            return;
        }

        if (jobStatusApplicationEvent.getStatus() != Job.Status.FAILED) {
            return;
        }

        try {
            dispatch(jobStatusApplicationEvent.getJobId());
        } catch (UnsupportedOperationException exception) {
            record("skipped_unsupported");

            if (unsupportedOperationLogged.compareAndSet(false, true)) {
                log.warn(
                    "Error workflow dispatch is not supported in this deployment topology; "
                        + "the project-workflow service is a remote stub");
            }
        } catch (RateLimitExceededException exception) {
            // The handler job goes through the normal admission gate, so a failure storm that saturates the plan's
            // concurrency/rate/cost limits rejects handler dispatch the same way it would reject any other job
            // submission. That is the gate working as intended, not a dispatch bug -- log it at DEBUG without a
            // stack trace so the storm the gates exist to bound doesn't itself flood the logs.
            record("rejected");

            if (log.isDebugEnabled()) {
                log.debug(
                    "Error workflow dispatch rejected by an admission gate for job {}: {}",
                    jobStatusApplicationEvent.getJobId(), exception.getMessage());
            }
        } catch (Exception exception) {
            record("failed");

            log.warn(
                "Error workflow dispatch failed for job {}", jobStatusApplicationEvent.getJobId(), exception);
        }
    }

    private void dispatch(long jobId) {
        Job job = jobService.getJob(jobId);

        // Recursion cap. Without this one check a persistently broken handler spawns jobs forever. Runs FIRST,
        // before any other check.
        if (job.getMetadata(ERROR_HANDLER_FOR) != null) {
            record("skipped_recursion");

            return;
        }

        // A subflow's child job carries a non-null parentTaskExecutionId. Resolving against a child job's workflow
        // id targets the wrong workflow and would double-dispatch alongside the parent's own FAILED event, so only
        // the top-level failed run may dispatch a handler.
        if (job.getParentTaskExecutionId() != null) {
            record("skipped_subflow_child");

            return;
        }

        Optional<Long> principalId = principalJobService.fetchJobPrincipalId(jobId, PlatformType.AUTOMATION);

        if (principalId.isEmpty()) {
            record("skipped_no_config");

            return;
        }

        Optional<ErrorWorkflowDispatch> reference =
            errorWorkflowResolver.resolve(principalId.get(), job.getWorkflowId());

        if (reference.isEmpty()) {
            record("skipped_no_config");

            return;
        }

        ErrorWorkflowDispatch dispatch = reference.get();

        // The payload is the handler's INPUT. Passing Map.of() here would dispatch a handler that receives
        // nothing, which is the whole point of the feature. The workflow block describes the FAILED run.
        Map<String, Object> payload = errorWorkflowPayloadFactory.build(
            job, fetchFailingTaskExecution(jobId),
            new ErrorWorkflowPayloadFactory.ErrorWorkflowContext(
                dispatch.projectId(), dispatch.failedProjectWorkflowId(), dispatch.failedWorkflowId(),
                dispatch.failedWorkflowLabel(), dispatch.environment()));

        // Every other trigger dispatch in this codebase (see TriggerCompletionHandler) nests its output under the
        // trigger's node name, and editor data pills are emitted node-name-prefixed accordingly. Passing the payload
        // as top-level inputs would leave every pill in a handler built in the editor resolving to null.
        Map<String, Object> inputs = Map.of(dispatch.errorTriggerName(), payload);

        JobParametersDTO jobParametersDTO = new JobParametersDTO(
            dispatch.handlerWorkflowId(), inputs, Map.of(ERROR_HANDLER_FOR, String.valueOf(jobId)));

        principalJobFacade.createJob(jobParametersDTO, principalId.get(), PlatformType.AUTOMATION);

        record("dispatched");
    }

    /**
     * Returns the LEAF task execution that actually failed, never an ancestor. {@code TaskExecutionErrorEventListener}
     * walks a failed task's ancestors and marks each of them FAILED too, but without setting their error -- only the
     * leaf (or an {@code on-error/} dispatcher) carries the real {@link com.bytechef.error.ExecutionError}. Handing the
     * payload factory an ancestor would degrade {@code error.message} to its default placeholder and lose the real
     * cause. Task executions come back ordered by creation date, so the last FAILED one that carries a non-null error
     * is the most recently failed, i.e. the leaf.
     */
    private @Nullable TaskExecution fetchFailingTaskExecution(long jobId) {
        List<TaskExecution> taskExecutions = taskExecutionService.getJobTaskExecutions(jobId);

        TaskExecution failingTaskExecution = null;

        for (TaskExecution taskExecution : taskExecutions) {
            if (taskExecution.getStatus() == TaskExecution.Status.FAILED && taskExecution.getError() != null) {
                failingTaskExecution = taskExecution;
            }
        }

        return failingTaskExecution;
    }

    private void record(String outcome) {
        if (counter != null) {
            counter.record(outcome);
        }
    }
}
