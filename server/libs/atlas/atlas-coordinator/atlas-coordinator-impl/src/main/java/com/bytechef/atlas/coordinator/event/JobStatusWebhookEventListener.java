
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

import com.bytechef.atlas.constants.WorkflowConstants;
import com.bytechef.atlas.domain.Job;
import com.bytechef.atlas.event.JobStatusWorkflowEvent;
import com.bytechef.atlas.event.WorkflowEvent;
import com.bytechef.atlas.service.JobService;
import com.bytechef.commons.utils.MapUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Jun 9, 2017
 */
public class JobStatusWebhookEventListener implements EventListener {

    private static final Logger logger = LoggerFactory.getLogger(JobStatusWebhookEventListener.class);

    private final JobService jobService;
    private final RestTemplate restTemplate = new RestTemplate();

    @SuppressFBWarnings("EI2")
    public JobStatusWebhookEventListener(JobService jobService) {
        this.jobService = jobService;
    }

    @Override
    public void onApplicationEvent(WorkflowEvent workflowEvent) {
        if (workflowEvent.getType()
            .equals(JobStatusWorkflowEvent.JOB_STATUS)) {
            handleEvent((JobStatusWorkflowEvent) workflowEvent);
        }
    }

    private void handleEvent(JobStatusWorkflowEvent workflowEvent) {
        String jobId = workflowEvent.getJobId();
        Job job = jobService.getJob(jobId);

        if (job == null) {
            logger.warn("Unknown job: {}", jobId);
            return;
        }

        for (Map<String, Object> webhook : job.getWebhooks()) {
            if (JobStatusWorkflowEvent.JOB_STATUS.equals(MapUtils.getRequiredString(webhook, WorkflowConstants.TYPE))) {
                Map<String, Object> webhookEvent = new HashMap<>(webhook);

                webhookEvent.put(WorkflowConstants.EVENT, workflowEvent);

                RetryTemplate retryTemplate = createRetryTemplate(webhook);

                retryTemplate.execute(context -> {
                    if (context.getRetryCount() == 0) {
                        logger.debug(
                            "Calling webhook {} -> {}",
                            MapUtils.getRequiredString(webhook, WorkflowConstants.URL),
                            webhookEvent);
                    } else {
                        logger.debug(
                            "[Retry: {}] Calling webhook {} -> {}",
                            context.getRetryCount(),
                            MapUtils.getRequiredString(webhook, WorkflowConstants.URL),
                            webhookEvent);
                    }

                    return restTemplate.postForObject(
                        MapUtils.getRequiredString(webhook, WorkflowConstants.URL), webhookEvent, String.class);
                });
            }
        }
    }

    private RetryTemplate createRetryTemplate(Map<String, Object> webhook) {
        Map<String, Object> retryParams = MapUtils.get(webhook, "retry", new ParameterizedTypeReference<>() {},
            Collections.emptyMap());

        RetryTemplate retryTemplate = new RetryTemplate();

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();

        backOffPolicy.setInitialInterval(
            MapUtils.getDuration(retryParams, "initialInterval", Duration.of(2, ChronoUnit.SECONDS))
                .toMillis());
        backOffPolicy.setMaxInterval(
            MapUtils.getDuration(retryParams, "maxInterval", Duration.of(30, ChronoUnit.SECONDS))
                .toMillis());
        backOffPolicy.setMultiplier(MapUtils.getDouble(retryParams, "multiplier", 2.0));
        retryTemplate.setBackOffPolicy(backOffPolicy);
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(MapUtils.getInteger(retryParams, "maxAttempts", 5));
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }
}
