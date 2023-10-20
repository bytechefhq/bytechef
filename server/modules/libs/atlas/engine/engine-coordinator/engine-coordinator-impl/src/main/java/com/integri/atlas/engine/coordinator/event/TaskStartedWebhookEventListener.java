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

package com.integri.atlas.engine.coordinator.event;

import com.integri.atlas.engine.Accessor;
import com.integri.atlas.engine.Constants;
import com.integri.atlas.engine.MapObject;
import com.integri.atlas.engine.event.Events;
import com.integri.atlas.engine.event.WorkflowEvent;
import com.integri.atlas.engine.job.Job;
import com.integri.atlas.engine.job.service.JobService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Arik Cohen
 * @since Jun 9, 2017
 */
public class TaskStartedWebhookEventListener implements EventListener {

    private final JobService jobService;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RestTemplate rest = new RestTemplate();

    public TaskStartedWebhookEventListener(JobService jobService) {
        this.jobService = jobService;
    }

    private void handleEvent(WorkflowEvent aEvent) {
        String jobId = aEvent.getRequiredString(Constants.JOB_ID);
        Job job = jobService.getJob(jobId);
        if (job == null) {
            logger.warn("Unknown job: {}", jobId);
            return;
        }
        List<Accessor> webhooks = job.getWebhooks();
        for (Accessor webhook : webhooks) {
            if (Events.TASK_STARTED.equals(webhook.getRequiredString(Constants.TYPE))) {
                MapObject webhookEvent = new MapObject(webhook.asMap());
                webhookEvent.put(Constants.EVENT, aEvent.asMap());
                rest.postForObject(webhook.getRequiredString(Constants.URL), webhookEvent, String.class);
            }
        }
    }

    @Override
    public void onApplicationEvent(WorkflowEvent aEvent) {
        if (aEvent.getType().equals(Events.TASK_STARTED)) {
            handleEvent(aEvent);
        }
    }
}
