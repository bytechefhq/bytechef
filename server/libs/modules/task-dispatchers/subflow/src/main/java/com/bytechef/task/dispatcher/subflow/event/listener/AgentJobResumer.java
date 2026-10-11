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

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.task.dispatcher.subflow.SubflowRequestConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * @author Ivica Cardic
 */
class AgentJobResumer {

    private static final Logger log = LoggerFactory.getLogger(AgentJobResumer.class);

    private final JobFacade jobFacade;
    private final JobService jobService;

    @SuppressFBWarnings("EI2")
    AgentJobResumer(JobFacade jobFacade, JobService jobService) {
        this.jobFacade = jobFacade;
        this.jobService = jobService;
    }

    boolean resume(long agentJobId, String agentJobResumeId, SubflowOutcome subflowOutcome) {
        Job agentJob = jobService.getJob(agentJobId);

        if (agentJob.getStatus() != Job.Status.STOPPED) {
            log.warn(
                "Agent job {} is {} (expected STOPPED); not resuming it for suspend {}", agentJobId,
                agentJob.getStatus(), agentJobResumeId);

            return false;
        }

        Map<String, ?> agentJobMetadata = agentJob.getMetadata();

        if (!Objects.equals(agentJobResumeId, agentJobMetadata.get(MetadataConstants.JOB_RESUME_ID)) ||
            Objects.equals(agentJobResumeId, agentJobMetadata.get(MetadataConstants.CONSUMED_JOB_RESUME_ID))) {

            log.warn(
                "Agent job {} no longer waits on suspend {}; not resuming it", agentJobId, agentJobResumeId);

            return false;
        }

        Long taskExecutionResumeId = MapUtils.getLong(agentJobMetadata, MetadataConstants.TASK_EXECUTION_RESUME_ID);

        if (taskExecutionResumeId == null) {
            log.error(
                "Agent job {} has no {}; it cannot be resumed after suspend {}", agentJobId,
                MetadataConstants.TASK_EXECUTION_RESUME_ID, agentJobResumeId);

            return false;
        }

        Map<String, Object> updatedMetadata = new HashMap<>(agentJobMetadata);

        updatedMetadata.put(MetadataConstants.CONSUMED_JOB_RESUME_ID, agentJobResumeId);
        updatedMetadata.remove(SubflowRequestConstants.LAUNCHED_SUBFLOW_RESUME_ID);

        agentJob.setMetadata(updatedMetadata);

        try {
            jobService.update(agentJob);
        } catch (OptimisticLockingFailureException exception) {
            log.warn(
                "Lost the resume of agent job {} for suspend {} to a concurrent update", agentJobId, agentJobResumeId,
                exception);

            return false;
        } catch (RuntimeException exception) {
            log.error("Failed to claim the resume of agent job {} for suspend {}", agentJobId, agentJobResumeId,
                exception);

            return false;
        }

        try {
            jobFacade.resumeJob(agentJobId, taskExecutionResumeId, subflowOutcome.toResumeData());
        } catch (RuntimeException exception) {
            log.error(
                "Claimed the resume of agent job {} for suspend {} but failed to dispatch it; the job stays stopped",
                agentJobId, agentJobResumeId, exception);

            return false;
        }

        return true;
    }
}
