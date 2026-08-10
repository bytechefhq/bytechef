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
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.JobInputConstants;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import com.bytechef.platform.workflow.task.dispatcher.subflow.PendingSubflowRequest;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowRequestConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinator listener for the agent-tool durable sub-workflow bridge. When an agent job suspends with a
 * {@link PendingSubflowRequest}, starts the requested sub-workflow as a top-level job once the agent is durably
 * {@code STOPPED} (deferred launch — the sub-workflow cannot complete before the agent is resumable).
 *
 * @author Ivica Cardic
 */
public class AgentSubflowLauncher implements ApplicationEventListener {

    private static final Logger log = LoggerFactory.getLogger(AgentSubflowLauncher.class);

    private final ChildJobPrincipalFactory childJobPrincipalFactory;
    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI2")
    public AgentSubflowLauncher(
        ChildJobPrincipalFactory childJobPrincipalFactory, JobService jobService,
        TaskExecutionService taskExecutionService) {

        this.childJobPrincipalFactory = childJobPrincipalFactory;
        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (!(applicationEvent instanceof JobStatusApplicationEvent jobStatusApplicationEvent)) {
            return;
        }

        if (jobStatusApplicationEvent.getStatus() != Job.Status.STOPPED) {
            return;
        }

        long agentJobId = jobStatusApplicationEvent.getJobId();

        Job agentJob = jobService.getJob(agentJobId);

        if (agentJob.getMetadata()
            .containsKey(SubflowRequestConstants.LAUNCHED_SUBFLOW_JOB_ID)) {

            return; // already launched (broker redelivery)
        }

        PendingSubflowRequest request = extractPendingSubflowRequest(agentJobId);

        if (request == null) {
            return; // an ordinary stop -- nothing to do
        }

        Map<String, Object> subflowInputs = new HashMap<>();

        subflowInputs.put(request.inputsName(), request.inputs());
        subflowInputs.put(JobInputConstants.TRIGGER_NAME_INPUT, request.inputsName());

        JobParametersDTO jobParametersDTO = new JobParametersDTO(
            request.workflowId(), subflowInputs, Map.of(SubflowRequestConstants.AGENT_JOB_ID, agentJobId));

        long subflowJobId = childJobPrincipalFactory.createPrincipalLinkedJob(agentJobId, jobParametersDTO);

        // Record the launched sub-workflow id on the agent job for idempotency on broker redelivery. If this update
        // throws (DB blip, @Version optimistic-lock collision), the launcher logs at ERROR -- without that signal
        // the broker would redeliver, the idempotency guard at the top of this method would see no record, and a
        // duplicate sub-workflow would be created (side effects, double approval notifications, etc.). See review
        // finding I3. The launched sub-workflow job is already committed at this point -- on update failure we
        // accept the orphan rather than swallowing the duplicate-launch risk silently.

        Map<String, Object> agentJobMetadata = new HashMap<>(agentJob.getMetadata());

        agentJobMetadata.put(SubflowRequestConstants.LAUNCHED_SUBFLOW_JOB_ID, subflowJobId);

        agentJob.setMetadata(agentJobMetadata);

        try {
            jobService.update(agentJob);
        } catch (RuntimeException exception) {
            log.error(
                "Failed to record launched sub-workflow id {} on agent job {} -- broker redelivery may launch "
                    + "a duplicate sub-workflow job; the orphan sub-workflow must be reconciled manually",
                subflowJobId, agentJobId, exception);

            throw exception;
        }

        if (log.isDebugEnabled()) {
            log.debug("Launched sub-workflow job {} for suspended agent job {}", subflowJobId, agentJobId);
        }
    }

    private PendingSubflowRequest extractPendingSubflowRequest(long agentJobId) {
        Optional<TaskExecution> taskExecution = taskExecutionService.fetchLastJobTaskExecution(agentJobId);

        if (taskExecution.isEmpty()) {
            return null;
        }

        Map<String, ?> metadata = taskExecution.get()
            .getMetadata();

        Suspend suspend = MapUtils.get(metadata, MetadataConstants.SUSPEND, Suspend.class);

        if (suspend == null) {
            return null;
        }

        return MapUtils.get(
            suspend.continueParameters(), SubflowRequestConstants.PENDING_SUBFLOW, PendingSubflowRequest.class);
    }
}
