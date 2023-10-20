
/*
 * Copyright 2016-2018 the original author or authors.
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
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.bytechef.atlas.coordinator.event;

import com.bytechef.atlas.constant.WorkflowConstants;
import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.event.TaskStartedWorkflowEvent;
import com.bytechef.atlas.event.WorkflowEvent;
import com.bytechef.atlas.service.JobService;
import com.bytechef.commons.util.MapValueUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Jun 9, 2017
 */
public class TaskStartedWebhookEventListener implements EventListener {

    private final JobService jobService;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RestTemplate rest = new RestTemplate();

    @SuppressFBWarnings("EI2")
    public TaskStartedWebhookEventListener(JobService jobService) {
        this.jobService = jobService;
    }

    @Override
    public void onApplicationEvent(WorkflowEvent workflowEvent) {
        if (workflowEvent.getType()
            .equals(TaskStartedWorkflowEvent.TASK_STARTED)) {
            handleEvent((TaskStartedWorkflowEvent) workflowEvent);
        }
    }

    private void handleEvent(TaskStartedWorkflowEvent workflowEvent) {
        Long jobId = workflowEvent.getJobId();

        Job job = jobService.getJob(jobId);

        if (job == null) {
            logger.warn("Unknown job: {}", jobId);

            return;
        }

        for (Map<String, Object> webhook : job.getWebhooks()) {
            if (TaskStartedWorkflowEvent.TASK_STARTED.equals(
                MapValueUtils.getRequiredString(webhook, WorkflowConstants.TYPE))) {
                Map<String, Object> webhookEvent = new HashMap<>(webhook);

                webhookEvent.put(WorkflowConstants.EVENT, workflowEvent);

                rest.postForObject(
                    MapValueUtils.getRequiredString(webhook, WorkflowConstants.URL), webhookEvent, String.class);
            }
        }
    }
}
