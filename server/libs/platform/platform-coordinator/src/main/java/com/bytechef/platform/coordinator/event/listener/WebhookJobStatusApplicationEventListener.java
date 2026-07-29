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
import com.bytechef.platform.notification.delivery.WebhookNotificationClient;
import com.bytechef.platform.notification.delivery.WebhookNotificationClient.WebhookRetry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Posts a job's registered {@code JOB_STATUS} callback webhooks on every status transition. Delivery mechanics (HTTP
 * client, exponential-backoff retry) are delegated to the shared {@link WebhookNotificationClient} — the central
 * transport for all outbound webhooks; only the per-job payload shaping and the {@link Job.Retry} schedule mapping live
 * here.
 *
 * @author Arik Cohen
 * @author Ivica Cardic
 * @since Jun 9, 2017
 */
public class WebhookJobStatusApplicationEventListener implements ApplicationEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebhookJobStatusApplicationEventListener.class);

    private final JobService jobService;
    private final WebhookNotificationClient webhookNotificationClient;

    @SuppressFBWarnings("EI2")
    public WebhookJobStatusApplicationEventListener(
        JobService jobService, WebhookNotificationClient webhookNotificationClient) {

        this.jobService = jobService;
        this.webhookNotificationClient = webhookNotificationClient;
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

                    if (log.isDebugEnabled()) {
                        log.debug("Calling job status webhook {} -> {}", webhook.url(), webhookEvent);
                    }

                    webhookNotificationClient.deliverEvent(webhook.url(), webhookEvent, toWebhookRetry(webhook));
                }
            }
        }
    }

    private static WebhookRetry toWebhookRetry(Job.Webhook webhook) {
        Job.Retry retry = webhook.retry();

        return new WebhookRetry(getMaxAttempts(retry), getInitialInterval(retry), getMultiplier(retry));
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

    private static Duration getInitialInterval(Job.Retry retry) {
        return retry.initialInterval() == null
            ? Duration.of(2, ChronoUnit.SECONDS)
            : Duration.of(retry.initialInterval(), ChronoUnit.SECONDS);
    }
}
