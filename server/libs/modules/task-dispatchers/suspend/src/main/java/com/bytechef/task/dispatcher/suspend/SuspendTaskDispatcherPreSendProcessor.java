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

package com.bytechef.task.dispatcher.suspend;

import com.bytechef.atlas.coordinator.task.dispatcher.TaskDispatcherPreSendProcessor;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.component.definition.ActionDefinition.Suspend;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.TaskStateService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class SuspendTaskDispatcherPreSendProcessor implements TaskDispatcherPreSendProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SuspendTaskDispatcherPreSendProcessor.class);

    private final JobService jobService;
    private final TaskStateService taskStateService;

    @SuppressFBWarnings("EI")
    public SuspendTaskDispatcherPreSendProcessor(JobService jobService, TaskStateService taskStateService) {
        this.jobService = jobService;
        this.taskStateService = taskStateService;
    }

    @Override
    public boolean canProcess(TaskExecution taskExecution) {
        Job job = jobService.getJob(Validate.notNull(taskExecution.getJobId(), "jobId"));

        return job.getMetadata(MetadataConstants.JOB_RESUME_ID) != null;
    }

    @Override
    public TaskExecution process(TaskExecution taskExecution) {
        Job job = jobService.getJob(Validate.notNull(taskExecution.getJobId(), "jobId"));

        String jobResumeIdString = (String) job.getMetadata(MetadataConstants.JOB_RESUME_ID);

        JobResumeId jobResumeId = JobResumeId.parse(jobResumeIdString);

        Optional<Suspend> suspendOptional = taskStateService.fetchValue(jobResumeId);

        if (suspendOptional.isEmpty()) {
            logger.warn("No suspend state found for jobResumeId={}", jobResumeId);
        }

        suspendOptional.ifPresent(suspend -> taskExecution.putMetadata(MetadataConstants.SUSPEND, suspend));

        Map<String, Object> jobMetadata = new HashMap<>(job.getMetadata());

        jobMetadata.remove(MetadataConstants.JOB_RESUME_ID);

        job.setMetadata(jobMetadata);

        jobService.update(job);

        taskStateService.delete(jobResumeId);

        return taskExecution;
    }
}
