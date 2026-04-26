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

package com.bytechef.platform.workflow.execution.facade;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional(readOnly = true)
public class ApprovalFormFacadeImpl implements ApprovalFormFacade {

    private static final String ENVIRONMENT_ID_METADATA_KEY = "environmentId";

    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI")
    public ApprovalFormFacadeImpl(JobService jobService, TaskExecutionService taskExecutionService) {
        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public Map<String, ?> getApprovalForm(String id) {
        JobResumeId jobResumeId = JobResumeId.parse(id);

        return TenantContext.callWithTenantId(jobResumeId.getTenantId(), () -> {
            Job job = jobService.getJob(jobResumeId.getJobId());

            if (job.getStatus() != Job.Status.STOPPED) {
                throw new IllegalStateException(
                    "Approval form is no longer available; job " + jobResumeId.getJobId() + " is " + job.getStatus());
            }

            TaskExecution taskExecution = taskExecutionService.fetchLastJobTaskExecution(job.getId())
                .orElseThrow(
                    () -> new IllegalStateException("No task execution found for job " + jobResumeId.getJobId()));

            Map<String, Object> result = new HashMap<>(taskExecution.getParameters());

            Object environmentId = taskExecution.getMetadata()
                .get(ENVIRONMENT_ID_METADATA_KEY);

            if (environmentId != null) {
                result.put(ENVIRONMENT_ID_METADATA_KEY, environmentId);
            }

            return result;
        });
    }
}
