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

package com.bytechef.task.dispatcher.subflow.event.listener;

import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionDefinition.CallableResponse;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowRequestConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class AgentSubflowResumeListener implements ApplicationEventListener {

    private static final Logger log = LoggerFactory.getLogger(AgentSubflowResumeListener.class);

    private final AgentJobResumer agentJobResumer;
    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI2")
    public AgentSubflowResumeListener(
        JobFacade jobFacade, JobService jobService, TaskExecutionService taskExecutionService,
        TaskFileStorage taskFileStorage) {

        this.agentJobResumer = new AgentJobResumer(jobFacade, jobService);
        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
        this.taskFileStorage = taskFileStorage;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (!(applicationEvent instanceof JobStatusApplicationEvent jobStatusApplicationEvent)) {
            return;
        }

        Job.Status status = jobStatusApplicationEvent.getStatus();

        if (status != Job.Status.COMPLETED && status != Job.Status.FAILED && status != Job.Status.STOPPED) {
            return;
        }

        if (status == Job.Status.STOPPED && jobStatusApplicationEvent.isSuspended()) {
            return;
        }

        Job subflowJob = jobService.getJob(jobStatusApplicationEvent.getJobId());

        Long agentJobId = MapUtils.getLong(subflowJob.getMetadata(), SubflowRequestConstants.AGENT_JOB_ID);
        String agentJobResumeId = MapUtils.getString(
            subflowJob.getMetadata(), SubflowRequestConstants.AGENT_JOB_RESUME_ID);

        if (agentJobId == null || agentJobResumeId == null) {
            return;
        }

        Map<String, Object> resumeData = switch (status) {
            case COMPLETED -> buildCompletedResumeData(subflowJob);
            case FAILED -> AgentJobResumer.failed(
                "Sub-workflow '%s' failed: %s".formatted(describeSubflow(subflowJob), getErrorMessage(subflowJob)));
            default -> AgentJobResumer.failed(
                "Sub-workflow '%s' was stopped before it finished".formatted(describeSubflow(subflowJob)));
        };

        if (agentJobResumer.resume(agentJobId, agentJobResumeId, resumeData) && log.isDebugEnabled()) {
            log.debug("Resumed agent job {} after sub-workflow job {} {}", agentJobId, subflowJob.getId(), status);
        }
    }

    private Map<String, Object> buildCompletedResumeData(Job subflowJob) {
        try {
            Optional<Object> callableOutput = getCallableResponseOutput(subflowJob);

            if (callableOutput.isPresent()) {
                return AgentJobResumer.completed(callableOutput.get());
            }

            return AgentJobResumer.completed(
                subflowJob.getOutputs() == null ? null : taskFileStorage.readJobOutputs(subflowJob.getOutputs()));
        } catch (RuntimeException exception) {
            log.error("Failed to read the outputs of sub-workflow job {}", subflowJob.getId(), exception);

            return AgentJobResumer.failed(
                "Sub-workflow '%s' completed but its result could not be read: %s".formatted(
                    describeSubflow(subflowJob), exception.getMessage()));
        }
    }

    private String getErrorMessage(Job subflowJob) {
        List<TaskExecution> taskExecutions;

        try {
            taskExecutions = taskExecutionService.getJobTaskExecutions(Objects.requireNonNull(subflowJob.getId()));
        } catch (RuntimeException exception) {
            log.error("Failed to read the task executions of sub-workflow job {}", subflowJob.getId(), exception);

            taskExecutions = List.of();
        }

        for (int index = taskExecutions.size() - 1; index >= 0; index--) {
            TaskExecution taskExecution = taskExecutions.get(index);

            ExecutionError executionError = taskExecution.getError();

            if (taskExecution.getStatus() == TaskExecution.Status.FAILED && executionError != null &&
                executionError.getMessage() != null) {

                return executionError.getMessage();
            }
        }

        ExecutionError jobError = subflowJob.getError();

        return jobError != null && jobError.getMessage() != null ? jobError.getMessage() : "unknown error";
    }

    private static String describeSubflow(Job subflowJob) {
        String workflowId = subflowJob.getWorkflowId();

        return workflowId == null ? "unknown" : workflowId;
    }

    private Optional<Object> getCallableResponseOutput(Job job) {
        Optional<TaskExecution> lastTaskExecution = taskExecutionService.fetchLastJobTaskExecution(
            Objects.requireNonNull(job.getId()));

        if (lastTaskExecution.isEmpty()) {
            return Optional.empty();
        }

        TaskExecution taskExecution = lastTaskExecution.get();

        if (!taskExecution.getMetadata()
            .containsKey(MetadataConstants.CALLABLE_RESPONSE)) {

            return Optional.empty();
        }

        CallableResponse callableResponse = ConvertUtils.convertValue(
            taskFileStorage.readTaskExecutionOutput(taskExecution.getOutput()), CallableResponse.class);

        return Optional.ofNullable(callableResponse.output());
    }
}
