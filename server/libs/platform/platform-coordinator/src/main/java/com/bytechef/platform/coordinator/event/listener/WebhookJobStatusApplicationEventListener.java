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
import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.listener.ApplicationEventListener;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.util.backoff.ExponentialBackOff;
import org.springframework.web.client.RestTemplate;

/**
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Jun 9, 2017
 */
public class WebhookJobStatusApplicationEventListener implements ApplicationEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebhookJobStatusApplicationEventListener.class);

    private final JobService jobService;
    private final RestTemplate restTemplate = new RestTemplate();

    @SuppressFBWarnings("EI2")
    public WebhookJobStatusApplicationEventListener(JobService jobService) {
        this.jobService = jobService;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof JobStatusApplicationEvent jobStatusApplicationEvent) {
            long jobId = jobStatusApplicationEvent.getJobId();

            Job job = jobService.getJob(jobId);

            for (Job.Webhook webhook : job.getWebhooks()) {
                if (JobStatusApplicationEvent.JOB_STATUS.equals(webhook.type())) {
                    Map<String, Object> webhookEvent = webhook.toMap();

                    webhookEvent.put(WorkflowConstants.EVENT, jobStatusApplicationEvent);

                    RetryTemplate retryTemplate = createRetryTemplate(webhook);

                    try {
                        retryTemplate.execute(() -> {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Calling data table webhook {} -> {}", webhook.url(), webhookEvent);
                            }

                            return restTemplate.postForObject(webhook.url(), webhookEvent, String.class);
                        });
                    } catch (RetryException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    private RetryTemplate createRetryTemplate(Job.Webhook webhook) {
        Job.Retry retry = webhook.retry();

        return new RetryTemplate(
            RetryPolicy.builder()
                .backOff(new ExponentialBackOff(getInitialInterval(retry), getMultiplier(retry)))
                .maxRetries(getMaxAttempts(retry))
                .build());
    }

    private static int getMaxAttempts(Job.Retry retry) {
        return retry.maxAttempts() == null
            ? 5
            : retry.maxAttempts();
    }

    private static double getMultiplier(Job.Retry retry) {
        return retry.multiplier() == null
            ? 2.0
            : retry.multiplier();
    }

//    private static long getMaxInterval(Job.Retry retry) {
//        return (retry.maxInterval() == null
//            ? Duration.of(2, ChronoUnit.SECONDS)
//            : Duration.of(retry.maxInterval(), ChronoUnit.SECONDS))
//                .toMillis();
//    }

    private static long getInitialInterval(Job.Retry retry) {
        return (retry.initialInterval() == null
            ? Duration.of(2, ChronoUnit.SECONDS)
            : Duration.of(retry.initialInterval(), ChronoUnit.SECONDS))
                .toMillis();
    }
}
