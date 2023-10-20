/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import com.integri.atlas.engine.core.Accessor;
import com.integri.atlas.engine.core.DSL;
import com.integri.atlas.engine.core.MapObject;
import com.integri.atlas.engine.coordinator.job.Job;
import com.integri.atlas.engine.coordinator.job.JobRepository;
import java.util.List;

import com.integri.atlas.engine.core.event.Events;
import com.integri.atlas.engine.core.event.WorkflowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Arik Cohen
 * @since Jun 9, 2017
 */
public class TaskStartedWebhookEventListener implements EventListener {

    private final JobRepository jobRepository;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final RestTemplate rest = new RestTemplate();

    public TaskStartedWebhookEventListener(JobRepository aJobRepository) {
        jobRepository = aJobRepository;
    }

    private void handleEvent(WorkflowEvent aEvent) {
        String jobId = aEvent.getRequiredString(DSL.JOB_ID);
        Job job = jobRepository.getById(jobId);
        if (job == null) {
            logger.warn("Unknown job: {}", jobId);
            return;
        }
        List<Accessor> webhooks = job.getWebhooks();
        for (Accessor webhook : webhooks) {
            if (Events.TASK_STARTED.equals(webhook.getRequiredString(DSL.TYPE))) {
                MapObject webhookEvent = new MapObject(webhook.asMap());
                webhookEvent.put(DSL.EVENT, aEvent.asMap());
                rest.postForObject(webhook.getRequiredString(DSL.URL), webhookEvent, String.class);
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
