/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.atlas.coordinator.event;

import com.bytechef.atlas.Constants;
import com.bytechef.atlas.event.EventListener;
import com.bytechef.atlas.event.Events;
import com.bytechef.atlas.event.WorkflowEvent;
import com.bytechef.atlas.job.JobStatus;
import com.bytechef.atlas.job.domain.Job;
import com.bytechef.atlas.service.context.ContextService;
import com.bytechef.atlas.service.job.JobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class DeleteContextEventListener implements EventListener {

    private static final Logger logger = LoggerFactory.getLogger(DeleteContextEventListener.class);

    private final ContextService contextService;
    private final JobService jobService;

    public DeleteContextEventListener(ContextService contextService, JobService jobService) {
        this.contextService = contextService;
        this.jobService = jobService;
    }

    @Override
    public void onApplicationEvent(WorkflowEvent workflowEvent) {
        if (Events.JOB_STATUS.equals(workflowEvent.getType())) {
            handleEvent(workflowEvent);
        }
    }

    private void handleEvent(WorkflowEvent workflowEvent) {
        String jobId = workflowEvent.getRequiredString(Constants.JOB_ID);

        Job job = jobService.getJob(jobId);

        if (job == null) {
            logger.warn("Unknown job: {}", jobId);

            return;
        }

        if (JobStatus.COMPLETED.equals(job.getStatus()) || JobStatus.FAILED.equals(job.getStatus())) {
            contextService.deleteJobContext(jobId);
        }
    }
}
