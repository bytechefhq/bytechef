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
import com.bytechef.platform.constant.JobInputConstants;
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
        } catch (Exception exception) {
            record("failed");

            log.warn(
                "Error workflow dispatch failed for job {}", jobStatusApplicationEvent.getJobId(), exception);
        }
    }

    private void dispatch(long jobId) {
        Job job = jobService.getJob(jobId);

        if (job.getMetadata(ERROR_HANDLER_FOR) != null) {
            record("skipped_recursion");

            return;
        }

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

        Map<String, Object> payload = errorWorkflowPayloadFactory.build(
            job, fetchFailingTaskExecution(jobId),
            new ErrorWorkflowPayloadFactory.ErrorWorkflowContext(
                dispatch.projectId(), dispatch.failedProjectWorkflowId(), dispatch.failedWorkflowId(),
                dispatch.failedWorkflowLabel(), dispatch.environment()));

        Map<String, Object> inputs = Map.of(
            dispatch.errorTriggerName(), payload,
            JobInputConstants.TRIGGER_NAME_INPUT, dispatch.errorTriggerName());

        JobParametersDTO jobParametersDTO = new JobParametersDTO(
            dispatch.handlerWorkflowId(), inputs, Map.of(ERROR_HANDLER_FOR, String.valueOf(jobId)));

        principalJobFacade.createJob(jobParametersDTO, principalId.get(), PlatformType.AUTOMATION);

        record("dispatched");
    }

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
