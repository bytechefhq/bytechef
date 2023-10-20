
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
import com.bytechef.atlas.event.JobStatusWorkflowEvent;
import com.bytechef.atlas.event.WorkflowEvent;
import com.bytechef.atlas.service.JobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        if (JobStatusWorkflowEvent.JOB_STATUS.equals(workflowEvent.getType())) {
            handleEvent((JobStatusWorkflowEvent) workflowEvent);
        }
    }

    private void handleEvent(JobStatusWorkflowEvent workflowEvent) {
        Long jobId = workflowEvent.getJobId();

        Job job = jobService.getJob(jobId);

        if (job == null) {
            logger.warn("Unknown job: {}", jobId);

            return;
        }

        for (Job.Webhook webhook : job.getWebhooks()) {
            if (JobStatusWorkflowEvent.JOB_STATUS.equals(webhook.type())) {

                Map<String, Object> webhookEvent = webhook.toMap();

                webhookEvent.put(WorkflowConstants.EVENT, workflowEvent);

                RetryTemplate retryTemplate = createRetryTemplate(webhook);

                retryTemplate.execute(context -> {
                    if (context.getRetryCount() == 0) {
                        logger.debug(
                            "Calling webhook {} -> {}",
                            webhook.url(),
                            webhookEvent);
                    } else {
                        logger.debug(
                            "[Retry: {}] Calling webhook {} -> {}",
                            context.getRetryCount(),
                            webhook.url(),
                            webhookEvent);
                    }

                    return restTemplate.postForObject(webhook.url(), webhookEvent, String.class);
                });
            }
        }
    }

    private RetryTemplate createRetryTemplate(Job.Webhook webhook) {
        Job.Retry retry = webhook.retry();

        RetryTemplate retryTemplate = new RetryTemplate();

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();

        backOffPolicy.setInitialInterval(
            (retry.initialInterval() == null
                ? Duration.of(2, ChronoUnit.SECONDS)
                : Duration.of(retry.initialInterval(), ChronoUnit.SECONDS))
                    .toMillis());
        backOffPolicy.setMaxInterval(
            (retry.maxInterval() == null
                ? Duration.of(2, ChronoUnit.SECONDS)
                : Duration.of(retry.maxInterval(), ChronoUnit.SECONDS))
                    .toMillis());
        backOffPolicy.setMultiplier(
            retry.multiplier() == null
                ? 2.0
                : retry.multiplier());

        retryTemplate.setBackOffPolicy(backOffPolicy);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();

        retryPolicy.setMaxAttempts(
            retry.maxAttempts() == null
                ? 5
                : retry.maxAttempts());

        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }
}
