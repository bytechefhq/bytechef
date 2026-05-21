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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinator listener for the agent-tool durable sub-workflow bridge. When a sub-workflow job launched by
 * {@link AgentSubflowLauncher} reaches a terminal state, resumes the suspended agent job with the sub-workflow result
 * (or an LLM-readable error).
 *
 * @author Ivica Cardic
 */
public class AgentSubflowResumeListener implements ApplicationEventListener {

    private static final Logger log = LoggerFactory.getLogger(AgentSubflowResumeListener.class);

    private final JobFacade jobFacade;
    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;
    private final TaskFileStorage taskFileStorage;

    @SuppressFBWarnings("EI2")
    public AgentSubflowResumeListener(
        JobFacade jobFacade, JobService jobService, TaskExecutionService taskExecutionService,
        TaskFileStorage taskFileStorage) {

        this.jobFacade = jobFacade;
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

        if (status != Job.Status.COMPLETED && status != Job.Status.FAILED) {
            return;
        }

        Job subflowJob = jobService.getJob(jobStatusApplicationEvent.getJobId());

        Object agentJobIdObject = subflowJob.getMetadata()
            .get(SubflowRequestConstants.AGENT_JOB_ID);

        if (agentJobIdObject == null) {
            return; // not an agent-initiated sub-workflow
        }

        long agentJobId = ((Number) agentJobIdObject).longValue();

        Job agentJob = jobService.getJob(agentJobId);

        if (agentJob.getStatus() != Job.Status.STOPPED) {
            log.warn(
                "Agent job {} is {} (expected STOPPED) when sub-workflow job {} terminated; skipping resume",
                agentJobId, agentJob.getStatus(), subflowJob.getId());

            return;
        }

        Map<String, ?> resumeData = status == Job.Status.COMPLETED
            ? buildCompletedResumeData(subflowJob)
            : buildFailedResumeData(subflowJob);

        jobFacade.resumeJob(
            agentJobId, MapUtils.getLong(agentJob.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID),
            resumeData);

        if (log.isDebugEnabled()) {
            log.debug("Resumed agent job {} after sub-workflow job {} {}", agentJobId, subflowJob.getId(), status);
        }
    }

    private Map<String, ?> buildCompletedResumeData(Job subflowJob) {
        Optional<Object> callableOutput;

        try {
            callableOutput = getCallableResponseOutput(subflowJob);
        } catch (RuntimeException exception) {
            // CALLABLE_RESPONSE metadata was present but extraction failed (e.g., schema drift, IO failure).
            // Do NOT silently fall back to job outputs -- the agent would see the wrong payload and the LLM
            // would reason on garbage. This is the silent-corruption failure mode #5055 was filed against;
            // surface it as an LLM-readable error instead.

            log.error(
                "Failed to extract callable response output from sub-workflow job {} -- "
                    + "agent will be resumed with an error payload to avoid silent data corruption",
                subflowJob.getId(), exception);

            return Map.of(
                "error", "Sub-workflow '%s' completed but its result could not be read: %s"
                    .formatted(describeSubflow(subflowJob), exception.getMessage()));
        }

        Object output = callableOutput.orElseGet(() -> subflowJob.getOutputs() == null
            ? Map.of()
            : taskFileStorage.readJobOutputs(subflowJob.getOutputs()));

        if (output == null) {
            return Map.of();
        }

        if (output instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, ?> typed = (Map<String, ?>) map;

            return typed;
        }

        return Map.of("result", output);
    }

    private static Map<String, ?> buildFailedResumeData(Job subflowJob) {
        ExecutionError error = subflowJob.getError();
        String reason = error != null && error.getMessage() != null ? error.getMessage() : "unknown error";

        return Map.of(
            "error", "Sub-workflow '%s' failed: %s".formatted(describeSubflow(subflowJob), reason));
    }

    /**
     * Returns a human-readable identifier for the sub-workflow. {@link Job#getWorkflowId()} is a base64-encoded id that
     * is not LLM-actionable; preferring it when no better label is available is still better than nothing.
     */
    private static String describeSubflow(Job subflowJob) {
        return subflowJob.getWorkflowId() != null ? subflowJob.getWorkflowId() : "unknown";
    }

    /**
     * Returns the unwrapped {@code CallableResponse.output()} when the sub-workflow's last task execution carries a
     * {@code CALLABLE_RESPONSE} metadata marker, or {@code Optional.empty()} when the marker is absent. Differs from
     * the previous implementation in NOT catching extraction failures -- when the marker is present, extraction failure
     * is a real defect and must propagate to the caller, which surfaces it to the LLM as an error tool-result instead
     * of silently substituting raw job outputs.
     */
    private Optional<Object> getCallableResponseOutput(Job job) {
        Optional<TaskExecution> lastTaskExecution = taskExecutionService.fetchLastJobTaskExecution(
            Objects.requireNonNull(job.getId()));

        if (lastTaskExecution.isEmpty()) {
            return Optional.empty(); // no task executions (e.g., trivial workflow) -- legitimate absence
        }

        TaskExecution taskExecution = lastTaskExecution.get();

        if (!taskExecution.getMetadata()
            .containsKey(MetadataConstants.CALLABLE_RESPONSE)) {

            return Optional.empty(); // no callable response marker -- legitimate absence, fall through to job outputs
        }

        CallableResponse callableResponse = ConvertUtils.convertValue(
            taskFileStorage.readTaskExecutionOutput(taskExecution.getOutput()), CallableResponse.class);

        return Optional.ofNullable(callableResponse.output());
    }
}
