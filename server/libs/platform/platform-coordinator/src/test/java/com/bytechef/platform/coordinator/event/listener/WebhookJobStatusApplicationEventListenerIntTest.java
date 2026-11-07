/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.coordinator.event.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.TaskStartedApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.Job.Status;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.coordinator.config.PlatformCoordinatorIntTestConfiguration;
import com.bytechef.platform.notification.delivery.WebhookDelivery;
import com.bytechef.platform.notification.delivery.WebhookDeliveryService;
import com.bytechef.platform.notification.delivery.WebhookPayload;
import com.bytechef.platform.notification.delivery.WebhookRetry;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PlatformCoordinatorIntTestConfiguration.class)
@Timeout(30)
class WebhookJobStatusApplicationEventListenerIntTest {

    private static final Job.Retry RETRY = new Job.Retry(3, 60, 7, 4);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobService jobService;

    private final WebhookDeliveryService webhookDeliveryService = mock(WebhookDeliveryService.class);

    private WebhookJobStatusApplicationEventListener listener;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("DELETE FROM task_execution");
        jdbcTemplate.update("DELETE FROM job");

        listener = new WebhookJobStatusApplicationEventListener(jobService, webhookDeliveryService);

        when(webhookDeliveryService.submit(any())).thenReturn(new CompletableFuture<>());
    }

    @Test
    void testSubmitsOneDeliveryPerMatchingWebhookOfTheStoredJob() {
        long jobId = saveJob(
            new Job.Webhook(JobStatusApplicationEvent.JOB_STATUS, "https://example.com/status", RETRY),
            new Job.Webhook(TaskStartedApplicationEvent.TASK_STARTED, "https://example.com/task", RETRY));

        listener.onApplicationEvent(new JobStatusApplicationEvent(jobId, Status.COMPLETED));

        ArgumentCaptor<WebhookDelivery> webhookDeliveryArgumentCaptor = ArgumentCaptor.forClass(WebhookDelivery.class);

        verify(webhookDeliveryService).submit(webhookDeliveryArgumentCaptor.capture());

        WebhookDelivery webhookDelivery = webhookDeliveryArgumentCaptor.getValue();

        assertThat(webhookDelivery.source()).isEqualTo("job_status");
        assertThat(webhookDelivery.description()).isEqualTo("job " + jobId + " status webhook");
        assertThat(webhookDelivery.url()).isEqualTo("https://example.com/status");
        assertThat(webhookDelivery.eventType()).isNull();
        assertThat(webhookDelivery.secret()).isNull();
        assertThat(webhookDelivery.validateUrl()).isTrue();
        assertThat(webhookDelivery.retry()).isEqualTo(new WebhookRetry(7, Duration.ofSeconds(3), 4.0));
        assertThat(webhookDelivery.payloads()).hasSize(1);

        List<WebhookPayload> payloads = webhookDelivery.payloads();

        WebhookPayload payload = payloads.getFirst();

        assertThat(payload.label()).isEqualTo(String.valueOf(jobId));
        assertThat(payload.json()).contains("\"event\"")
            .contains("\"jobId\":" + jobId);
    }

    @Test
    void testReturnsWithoutWaitingForTheDelivery() {
        long jobId =
            saveJob(new Job.Webhook(JobStatusApplicationEvent.JOB_STATUS, "https://example.com/status", RETRY));

        long startNanos = System.nanoTime();

        listener.onApplicationEvent(new JobStatusApplicationEvent(jobId, Status.COMPLETED));

        assertThat(Duration.ofNanos(System.nanoTime() - startNanos)).isLessThan(Duration.ofSeconds(1));

        verify(webhookDeliveryService).submit(any());
    }

    @Test
    void testIgnoresOtherEvents() {
        long jobId =
            saveJob(new Job.Webhook(JobStatusApplicationEvent.JOB_STATUS, "https://example.com/status", RETRY));

        listener.onApplicationEvent(new TaskStartedApplicationEvent(jobId, 7L));

        verify(webhookDeliveryService, never()).submit(any());
    }

    private long saveJob(Job.Webhook... webhooks) {
        Job job = new Job();

        job.setLabel("Test Workflow");
        job.setStatus(Status.STARTED);
        job.setWebhooks(List.of(webhooks));
        job.setWorkflowId("demo:1234");

        job = jobRepository.save(job);

        return Objects.requireNonNull(job.getId());
    }
}
