/*
 * Copyright 2016-2020 the original author or authors.
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
 * Modifications copyright (C) 2025 ByteChef
 */

package com.bytechef.platform.coordinator.event.listener;

import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.coordinator.event.ApplicationEvent;
import com.bytechef.atlas.coordinator.event.TaskStartedApplicationEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Jun 9, 2017
 */
public class WebhookTaskStartedApplicationEventListener implements ApplicationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebhookTaskStartedApplicationEventListener.class);

    private final JobService jobService;

    private final RestTemplate rest = new RestTemplate();

    @SuppressFBWarnings("EI2")
    public WebhookTaskStartedApplicationEventListener(JobService jobService) {
        this.jobService = jobService;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof TaskStartedApplicationEvent taskStartedApplicationEvent) {
            Long jobId = taskStartedApplicationEvent.getJobId();

            Job job = jobService.getJob(jobId);

            if (job == null) {
                logger.warn("Unknown job: {}", jobId);

                return;
            }

            for (Job.Webhook webhook : job.getWebhooks()) {
                if (TaskStartedApplicationEvent.TASK_STARTED.equals(webhook.type())) {
                    Map<String, Object> webhookEvent = webhook.toMap();

                    webhookEvent.put(WorkflowConstants.EVENT, taskStartedApplicationEvent);

                    rest.postForObject(webhook.url(), webhookEvent, String.class);

                    if (logger.isDebugEnabled()) {
                        logger.debug("Webhook url={}, type='{}' called", webhook.url(), webhook.type());
                    }
                }
            }
        }
    }
}
