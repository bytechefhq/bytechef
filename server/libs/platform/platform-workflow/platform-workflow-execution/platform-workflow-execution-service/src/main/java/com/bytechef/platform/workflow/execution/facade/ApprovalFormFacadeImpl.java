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
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
    private final TaskStateService taskStateService;

    @SuppressFBWarnings("EI")
    public ApprovalFormFacadeImpl(
        JobService jobService, TaskExecutionService taskExecutionService, TaskStateService taskStateService) {

        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
        this.taskStateService = taskStateService;
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

            Map<String, ?> jobMetadata = job.getMetadata();

            if (!jobResumeId.matches((String) jobMetadata.get(MetadataConstants.JOB_RESUME_ID))) {
                throw new IllegalStateException(
                    "Approval form is no longer available; the resume id of job " + jobResumeId.getJobId() +
                        " does not match");
            }

            TaskExecution taskExecution = taskExecutionService.getTaskExecution(
                MapUtils.getLong(jobMetadata, MetadataConstants.TASK_EXECUTION_RESUME_ID));

            Map<String, Object> result = new HashMap<>(getFormParameters(jobResumeId, taskExecution));

            Map<String, ?> taskExecutionMetadata = taskExecution.getMetadata();

            Object environmentId = taskExecutionMetadata.get(ENVIRONMENT_ID_METADATA_KEY);

            if (environmentId != null) {
                result.put(ENVIRONMENT_ID_METADATA_KEY, environmentId);
            }

            return result;
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> getFormParameters(JobResumeId jobResumeId, TaskExecution taskExecution) {
        Optional<Suspend> suspendOptional = taskStateService.fetchValue(jobResumeId);

        if (suspendOptional.isPresent()) {
            Suspend suspend = suspendOptional.get();

            if (suspend.continueParameters()
                .get(MetadataConstants.APPROVAL_FORM_PARAMETERS) instanceof Map<?, ?> formParameters) {

                return (Map<String, ?>) formParameters;
            }
        }

        return taskExecution.getParameters();
    }
}
