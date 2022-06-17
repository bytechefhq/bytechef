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

import com.bytechef.atlas.Accessor;
import com.bytechef.atlas.Constants;
import com.bytechef.atlas.MapObject;
import com.bytechef.atlas.event.Events;
import com.bytechef.atlas.event.WorkflowEvent;
import com.bytechef.atlas.job.domain.Job;
import com.bytechef.atlas.job.service.JobService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestTemplate;

/**
 * @author Arik Cohen
 * @since Jun 9, 2017
 */
public class JobStatusWebhookEventListener implements EventListener {

    private static final Logger logger = LoggerFactory.getLogger(JobStatusWebhookEventListener.class);

    private final JobService jobService;
    private final RestTemplate rest = new RestTemplate();

    public JobStatusWebhookEventListener(JobService jobService) {
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
            if (Events.JOB_STATUS.equals(webhook.getRequiredString(Constants.TYPE))) {
                MapObject webhookEvent = new MapObject(webhook.asMap());
                webhookEvent.put(Constants.EVENT, aEvent.asMap());
                RetryTemplate retryTemplate = createRetryTemplate(webhook);
                retryTemplate.execute(context -> {
                    if (context.getRetryCount() == 0) {
                        logger.debug(
                                "Calling webhook {} -> {}", webhook.getRequiredString(Constants.URL), webhookEvent);
                    } else {
                        logger.debug(
                                "[Retry: {}] Calling webhook {} -> {}",
                                context.getRetryCount(),
                                webhook.getRequiredString(Constants.URL),
                                webhookEvent);
                    }
                    return rest.postForObject(webhook.getRequiredString(Constants.URL), webhookEvent, String.class);
                });
            }
        }
    }

    private RetryTemplate createRetryTemplate(Accessor aWebhook) {
        MapObject retryParams = aWebhook.get("retry", MapObject.class, new MapObject());
        RetryTemplate retryTemplate = new RetryTemplate();
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(
                retryParams.getDuration("initialInterval", "2s").toMillis());
        backOffPolicy.setMaxInterval(
                retryParams.getDuration("maxInterval", "30s").toMillis());
        backOffPolicy.setMultiplier(retryParams.getDouble("multiplier", 2.0));
        retryTemplate.setBackOffPolicy(backOffPolicy);
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(retryParams.getInteger("maxAttempts", 5));
        retryTemplate.setRetryPolicy(retryPolicy);
        return retryTemplate;
    }

    @Override
    public void onApplicationEvent(WorkflowEvent aEvent) {
        if (aEvent.getType().equals(Events.JOB_STATUS)) {
            handleEvent(aEvent);
        }
    }
}
