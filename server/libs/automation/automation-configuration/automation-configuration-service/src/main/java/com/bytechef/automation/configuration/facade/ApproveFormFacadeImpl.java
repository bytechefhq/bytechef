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

package com.bytechef.automation.configuration.facade;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional(readOnly = true)
public class ApproveFormFacadeImpl implements ApproveFormFacade {

    private final JobService jobService;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI")
    public ApproveFormFacadeImpl(JobService jobService, TaskExecutionService taskExecutionService) {
        this.jobService = jobService;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public Map<String, ?> getApproveForm(long jobId) {
        Job job = jobService.getJob(jobId);

        if (job.getStatus() != Job.Status.STOPPED) {
            throw new IllegalStateException(
                "Approval form is no longer available; job " + jobId + " is " + job.getStatus());
        }

        TaskExecution taskExecution = taskExecutionService.fetchLastJobTaskExecution(job.getId())
            .orElseThrow(() -> new IllegalStateException("No task execution found for job " + jobId));

        return taskExecution.getParameters();
    }
}
