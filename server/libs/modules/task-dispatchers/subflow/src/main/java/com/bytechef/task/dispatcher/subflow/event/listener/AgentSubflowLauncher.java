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
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.ConvertUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.constant.JobInputConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import com.bytechef.platform.workflow.task.dispatcher.subflow.PendingSubflowRequest;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowRequestConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * @author Ivica Cardic
 */
public class AgentSubflowLauncher implements ApplicationEventListener {

    public static final String LOST_REQUEST_ERROR =
        "Error: the sub-workflow request was lost before the sub-workflow could start.";

    private static final Logger log = LoggerFactory.getLogger(AgentSubflowLauncher.class);

    private final AgentJobResumer agentJobResumer;
    private final ChildJobPrincipalFactory childJobPrincipalFactory;
    private final JobService jobService;
    private final int maxDepth;
    private final TaskStateService taskStateService;

    @SuppressFBWarnings("EI2")
    public AgentSubflowLauncher(
        ChildJobPrincipalFactory childJobPrincipalFactory, JobFacade jobFacade, JobService jobService, int maxDepth,
        TaskStateService taskStateService) {

        Validate.isTrue(maxDepth >= 1, "maxDepth must be at least 1, was %d", maxDepth);

        this.agentJobResumer = new AgentJobResumer(jobFacade, jobService);
        this.childJobPrincipalFactory = childJobPrincipalFactory;
        this.jobService = jobService;
        this.maxDepth = maxDepth;
        this.taskStateService = taskStateService;
    }

    public static String tooDeepError(int maxDepth) {
        return "Error: the chain of agent-to-agent calls exceeded the maximum nesting depth of " + maxDepth
            + "; the target agent may call back into an agent already running in this chain.";
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (!(applicationEvent instanceof JobStatusApplicationEvent jobStatusApplicationEvent) ||
            jobStatusApplicationEvent.getStatus() != Job.Status.STOPPED || !jobStatusApplicationEvent.isSuspended()) {

            return;
        }

        long agentJobId = jobStatusApplicationEvent.getJobId();

        try {
            launchSubflow(agentJobId);
        } catch (RuntimeException exception) {
            log.error("Failed to handle the suspend of agent job {}", agentJobId, exception);
        }
    }

    private void launchSubflow(long agentJobId) {
        Job agentJob = jobService.getJob(agentJobId);

        String agentJobResumeId = MapUtils.getString(agentJob.getMetadata(), MetadataConstants.JOB_RESUME_ID);

        if (agentJobResumeId == null ||
            Objects.equals(
                agentJobResumeId, agentJob.getMetadata(SubflowRequestConstants.LAUNCHED_SUBFLOW_RESUME_ID))) {

            return;
        }

        PendingSubflowRequest pendingSubflowRequest;

        try {
            pendingSubflowRequest = fetchPendingSubflowRequest(agentJobResumeId);
        } catch (RuntimeException exception) {
            log.error(
                "Failed to read the sub-workflow request of agent job {} for suspend {}", agentJobId, agentJobResumeId,
                exception);

            resumeWithFailure(agentJobId, agentJobResumeId, LOST_REQUEST_ERROR);

            return;
        }

        if (pendingSubflowRequest == null) {
            return;
        }

        int depth = MapUtils.getInteger(agentJob.getMetadata(), SubflowRequestConstants.SUBFLOW_DEPTH, 0) + 1;

        if (depth > maxDepth) {
            log.warn(
                "Refusing to launch a sub-workflow for agent job {}: depth {} would exceed the maximum of {}",
                agentJobId, depth, maxDepth);

            resumeWithFailure(agentJobId, agentJobResumeId, tooDeepError(maxDepth));

            return;
        }

        if (!claimLaunch(agentJob, agentJobResumeId)) {
            return;
        }

        try {
            long subflowJobId = childJobPrincipalFactory.createPrincipalLinkedJob(
                agentJobId, createJobParameters(pendingSubflowRequest, agentJobId, agentJobResumeId, depth));

            if (log.isDebugEnabled()) {
                log.debug("Launched sub-workflow job {} for suspended agent job {}", subflowJobId, agentJobId);
            }
        } catch (RuntimeException exception) {
            log.error(
                "Failed to launch sub-workflow '{}' for agent job {}", pendingSubflowRequest.workflowId(), agentJobId,
                exception);

            resumeWithFailure(
                agentJobId, agentJobResumeId,
                "Error: could not start sub-workflow '%s': %s".formatted(
                    pendingSubflowRequest.workflowId(), exception.getMessage()));
        }
    }

    private boolean claimLaunch(Job agentJob, String agentJobResumeId) {
        Map<String, Object> agentJobMetadata = new HashMap<>(agentJob.getMetadata());

        agentJobMetadata.put(SubflowRequestConstants.LAUNCHED_SUBFLOW_RESUME_ID, agentJobResumeId);

        agentJob.setMetadata(agentJobMetadata);

        try {
            jobService.update(agentJob);
        } catch (OptimisticLockingFailureException exception) {
            log.warn(
                "Lost the sub-workflow launch for agent job {} and suspend {} to a concurrent update",
                agentJob.getId(), agentJobResumeId, exception);

            return false;
        } catch (RuntimeException exception) {
            log.error(
                "Failed to claim the sub-workflow launch for agent job {} and suspend {}", agentJob.getId(),
                agentJobResumeId, exception);

            return false;
        }

        return true;
    }

    private void resumeWithFailure(long agentJobId, String agentJobResumeId, String error) {
        if (!agentJobResumer.resume(agentJobId, agentJobResumeId, AgentJobResumer.failed(error))) {
            log.error(
                "Could not hand the failure '{}' back to agent job {} for suspend {}", error, agentJobId,
                agentJobResumeId);
        }
    }

    private static JobParametersDTO createJobParameters(
        PendingSubflowRequest pendingSubflowRequest, long agentJobId, String agentJobResumeId, int depth) {

        Map<String, Object> subflowInputs = new HashMap<>();

        subflowInputs.put(pendingSubflowRequest.inputsName(), pendingSubflowRequest.inputs());
        subflowInputs.put(JobInputConstants.TRIGGER_NAME_INPUT, pendingSubflowRequest.inputsName());

        return new JobParametersDTO(
            pendingSubflowRequest.workflowId(), subflowInputs,
            Map.of(
                SubflowRequestConstants.AGENT_JOB_ID, agentJobId, SubflowRequestConstants.AGENT_JOB_RESUME_ID,
                agentJobResumeId, SubflowRequestConstants.SUBFLOW_DEPTH, depth));
    }

    private @Nullable PendingSubflowRequest fetchPendingSubflowRequest(String agentJobResumeId) {
        Optional<Object> suspendValue = taskStateService.fetchValue(JobResumeId.parse(agentJobResumeId));

        if (suspendValue.isEmpty()) {
            return null;
        }

        Suspend suspend = ConvertUtils.convertValue(suspendValue.get(), Suspend.class);

        return MapUtils.get(
            suspend.continueParameters(), SubflowRequestConstants.PENDING_SUBFLOW, PendingSubflowRequest.class);
    }
}
