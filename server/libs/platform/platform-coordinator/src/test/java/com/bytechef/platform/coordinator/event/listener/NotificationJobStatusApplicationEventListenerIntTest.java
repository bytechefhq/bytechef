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

import com.bytechef.atlas.coordinator.event.JobStatusApplicationEvent;
import com.bytechef.atlas.coordinator.event.TaskStartedApplicationEvent;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.coordinator.config.PlatformCoordinatorIntTestConfiguration;
import com.bytechef.platform.coordinator.metrics.JobExecutionCounter;
import com.bytechef.platform.coordinator.notification.JobStatusEmailNotificationHandler;
import com.bytechef.platform.coordinator.notification.JobStatusSlackNotificationHandler;
import com.bytechef.platform.coordinator.notification.JobStatusWebhookNotificationHandler;
import com.bytechef.platform.coordinator.notification.NotificationHandlerContexts;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.handler.EmailNotificationHandler;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import com.bytechef.platform.notification.handler.NotificationHandlerRegistry;
import com.bytechef.platform.notification.handler.NotificationSender;
import com.bytechef.platform.notification.handler.NotificationSenderRegistry;
import com.bytechef.platform.notification.handler.NotificationTestResult;
import com.bytechef.platform.notification.handler.SlackNotificationHandler;
import com.bytechef.platform.notification.repository.NotificationEventRepository;
import com.bytechef.platform.notification.repository.NotificationRepository;
import com.bytechef.platform.notification.service.NotificationServiceImpl;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PlatformCoordinatorIntTestConfiguration.class)
class NotificationJobStatusApplicationEventListenerIntTest {

    private static final String FAILING_NOTIFICATION_NAME = "failing";
    private static final String JOB_EXECUTION_COUNTER = "bytechef_job_execution";
    private static final String JOB_LABEL = "Daily sync";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobService jobService;

    @Autowired
    private JobStatusEmailNotificationHandler jobStatusEmailNotificationHandler;

    @Autowired
    private JobStatusSlackNotificationHandler jobStatusSlackNotificationHandler;

    @Autowired
    private JobStatusWebhookNotificationHandler jobStatusWebhookNotificationHandler;

    @Autowired
    private NotificationEventRepository notificationEventRepository;

    @Autowired
    private NotificationHandlerContexts notificationHandlerContexts;

    @Autowired
    private NotificationRepository notificationRepository;

    private final List<String> attemptedNotificationNames = new CopyOnWriteArrayList<>();
    private final List<Delivery> deliveries = new CopyOnWriteArrayList<>();
    private SimpleMeterRegistry meterRegistry;
    private NotificationHandlerRegistry notificationHandlerRegistry;
    private NotificationSenderRegistry notificationSenderRegistry;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("DELETE FROM notification_notification_event");
        jdbcTemplate.update("DELETE FROM notification");
        jdbcTemplate.update("DELETE FROM task_execution");
        jdbcTemplate.update("DELETE FROM job");

        attemptedNotificationNames.clear();
        deliveries.clear();

        meterRegistry = new SimpleMeterRegistry();

        notificationHandlerRegistry = new NotificationHandlerRegistry(
            List.of(
                jobStatusEmailNotificationHandler, jobStatusSlackNotificationHandler,
                jobStatusWebhookNotificationHandler));

        notificationSenderRegistry = new NotificationSenderRegistry(
            List.of(new RecordingEmailNotificationSender(), new RecordingSlackNotificationSender()));
    }

    @Test
    void testOtherApplicationEventsAreIgnored() {
        long jobId = saveJob(Job.Status.STARTED);

        saveNotification("email", Notification.Type.EMAIL, NotificationEvent.Type.JOB_STARTED);

        listener(true).onApplicationEvent(new TaskStartedApplicationEvent(jobId, 7L));

        assertThat(attemptedNotificationNames).isEmpty();
        assertThat(meterRegistry.getMeters()).isEmpty();
    }

    @Test
    void testJobStatusEventIsCountedByTheJobExecutionCounter() {
        long jobId = saveJob(Job.Status.COMPLETED);

        listener(true).onApplicationEvent(new JobStatusApplicationEvent(jobId, Job.Status.COMPLETED));

        double count = meterRegistry.get(JOB_EXECUTION_COUNTER)
            .tag("job_name", JOB_LABEL)
            .tag("job_status", "completed")
            .counter()
            .count();

        assertThat(count).isEqualTo(1.0);
        assertThat(attemptedNotificationNames).isEmpty();
    }

    @Test
    void testJobStatusEventIsHandledWithoutAJobExecutionCounter() {
        long jobId = saveJob(Job.Status.FAILED);

        saveNotification("email", Notification.Type.EMAIL, NotificationEvent.Type.JOB_FAILED);

        listener(false).onApplicationEvent(new JobStatusApplicationEvent(jobId, Job.Status.FAILED));

        assertThat(deliveries)
            .extracting(Delivery::notificationName)
            .containsExactly("email");
        assertThat(meterRegistry.getMeters()).isEmpty();
    }

    @Test
    void testNotificationSubscribedToAnotherStatusIsNotSent() {
        long jobId = saveJob(Job.Status.COMPLETED);

        saveNotification("failedOnly", Notification.Type.EMAIL, NotificationEvent.Type.JOB_FAILED);

        listener(true).onApplicationEvent(new JobStatusApplicationEvent(jobId, Job.Status.COMPLETED));

        assertThat(attemptedNotificationNames).isEmpty();
    }

    @Test
    void testEverySubscribedNotificationIsSentWithTheEventTypeOfTheJobStatus() {
        long jobId = saveJob(Job.Status.FAILED);

        saveNotification("email", Notification.Type.EMAIL, NotificationEvent.Type.JOB_FAILED);
        saveNotification("slack", Notification.Type.SLACK, NotificationEvent.Type.JOB_FAILED);

        listener(true).onApplicationEvent(new JobStatusApplicationEvent(jobId, Job.Status.FAILED));

        assertThat(deliveries)
            .extracting(Delivery::notificationName)
            .containsExactlyInAnyOrder("email", "slack");

        Delivery emailDelivery = getDelivery("email");
        Delivery slackDelivery = getDelivery("slack");

        NotificationHandlerContext notificationHandlerContext = emailDelivery.notificationHandlerContext();

        assertThat(notificationHandlerContext).isSameAs(slackDelivery.notificationHandlerContext());
        assertThat(notificationHandlerContext.getEventType()).isEqualTo(NotificationEvent.Type.JOB_FAILED);
        assertThat(notificationHandlerContext.getJobId()).isEqualTo(jobId);
        assertThat(notificationHandlerContext.getJobName()).isEqualTo(JOB_LABEL);
        assertThat(emailDelivery.message())
            .isEqualTo("Job Execution Status - Job %s (ID: %d) failed".formatted(JOB_LABEL, jobId));
        assertThat(slackDelivery.message())
            .startsWith(":rotating_light: ")
            .contains("Job %s (ID: %d) execution failed.".formatted(JOB_LABEL, jobId));
    }

    @Test
    void testSuspendedJobStatusEventIsSentAsAJobStoppedNotification() {
        long jobId = saveJob(Job.Status.STOPPED);

        saveNotification("email", Notification.Type.EMAIL, NotificationEvent.Type.JOB_STOPPED);

        listener(true).onApplicationEvent(JobStatusApplicationEvent.suspended(jobId));

        Delivery delivery = getDelivery("email");

        NotificationHandlerContext notificationHandlerContext = delivery.notificationHandlerContext();

        assertThat(notificationHandlerContext.getEventType()).isEqualTo(NotificationEvent.Type.JOB_STOPPED);
        assertThat(delivery.message())
            .isEqualTo("Job Execution Status - Job %s (ID: %d) stopped".formatted(JOB_LABEL, jobId));
    }

    @Test
    void testPaymentRequiredJobStatusEventIsSentAsAJobPaymentRequiredNotification() {
        long jobId = saveJob(Job.Status.PAYMENT_REQUIRED);

        saveNotification("email", Notification.Type.EMAIL, NotificationEvent.Type.JOB_PAYMENT_REQUIRED);

        listener(true).onApplicationEvent(new JobStatusApplicationEvent(jobId, Job.Status.PAYMENT_REQUIRED));

        Delivery delivery = getDelivery("email");

        NotificationHandlerContext notificationHandlerContext = delivery.notificationHandlerContext();

        assertThat(notificationHandlerContext.getEventType()).isEqualTo(NotificationEvent.Type.JOB_PAYMENT_REQUIRED);
        assertThat(delivery.message())
            .isEqualTo("Job Execution Status - Job %s (ID: %d) payment required".formatted(JOB_LABEL, jobId));
    }

    @Test
    void testNotificationWithoutASenderIsSkippedAndTheOthersAreSent() {
        long jobId = saveJob(Job.Status.COMPLETED);

        saveNotification("webhook", Notification.Type.WEBHOOK, NotificationEvent.Type.JOB_COMPLETED);
        saveNotification("email", Notification.Type.EMAIL, NotificationEvent.Type.JOB_COMPLETED);

        listener(true).onApplicationEvent(new JobStatusApplicationEvent(jobId, Job.Status.COMPLETED));

        assertThat(deliveries)
            .extracting(Delivery::notificationName)
            .containsExactly("email");
    }

    @Test
    void testNotificationWithoutAHandlerIsSkippedAndTheOthersAreSent() {
        long jobId = saveJob(Job.Status.PAYMENT_REQUIRED);

        saveNotification("slack", Notification.Type.SLACK, NotificationEvent.Type.JOB_PAYMENT_REQUIRED);
        saveNotification("email", Notification.Type.EMAIL, NotificationEvent.Type.JOB_PAYMENT_REQUIRED);

        listener(true).onApplicationEvent(new JobStatusApplicationEvent(jobId, Job.Status.PAYMENT_REQUIRED));

        assertThat(attemptedNotificationNames).containsExactly("email");
        assertThat(deliveries)
            .extracting(Delivery::notificationName)
            .containsExactly("email");
    }

    @Test
    void testSenderFailureDoesNotStopTheRemainingNotifications() {
        long jobId = saveJob(Job.Status.FAILED);

        saveNotification(FAILING_NOTIFICATION_NAME, Notification.Type.EMAIL, NotificationEvent.Type.JOB_FAILED);
        saveNotification("slack", Notification.Type.SLACK, NotificationEvent.Type.JOB_FAILED);

        listener(true).onApplicationEvent(new JobStatusApplicationEvent(jobId, Job.Status.FAILED));

        assertThat(attemptedNotificationNames).containsExactlyInAnyOrder(FAILING_NOTIFICATION_NAME, "slack");
        assertThat(deliveries)
            .extracting(Delivery::notificationName)
            .containsExactly("slack");
    }

    private Delivery getDelivery(String notificationName) {
        return deliveries.stream()
            .filter(delivery -> notificationName.equals(delivery.notificationName()))
            .findFirst()
            .orElseThrow();
    }

    private NotificationJobStatusApplicationEventListener listener(boolean withJobExecutionCounter) {
        JobExecutionCounter jobExecutionCounter = null;

        if (withJobExecutionCounter) {
            jobExecutionCounter = new JobExecutionCounter(meterRegistry);
        }

        return new NotificationJobStatusApplicationEventListener(
            jobExecutionCounter, jobService, notificationHandlerContexts, notificationHandlerRegistry,
            notificationSenderRegistry, new NotificationServiceImpl(notificationRepository));
    }

    private long saveJob(Job.Status status) {
        Job job = new Job();

        job.setLabel(JOB_LABEL);
        job.setStatus(status);
        job.setWorkflowId("demo:1234");

        job = jobRepository.save(job);

        return Objects.requireNonNull(job.getId());
    }

    private void saveNotification(String name, Notification.Type type, NotificationEvent.Type eventType) {
        List<NotificationEvent> notificationEvents = notificationEventRepository.findAll();

        NotificationEvent notificationEvent = notificationEvents.stream()
            .filter(curNotificationEvent -> curNotificationEvent.getType() == eventType)
            .findFirst()
            .orElseThrow();

        Notification notification = new Notification();

        notification.setName(name);
        notification.setNotificationEventIds(List.of(notificationEvent.getId()));
        notification.setSettings(Map.of());
        notification.setType(type);

        notificationRepository.save(notification);
    }

    private void recordSend(
        Notification notification, @Nullable String message, NotificationHandlerContext notificationHandlerContext) {

        String name = notification.getName();

        attemptedNotificationNames.add(name);

        if (FAILING_NOTIFICATION_NAME.equals(name)) {
            throw new IllegalStateException("SMTP down");
        }

        deliveries.add(new Delivery(name, message, notificationHandlerContext));
    }

    private record Delivery(
        String notificationName, @Nullable String message, NotificationHandlerContext notificationHandlerContext) {
    }

    private final class RecordingEmailNotificationSender implements NotificationSender<EmailNotificationHandler> {

        @Override
        public Notification.Type getType() {
            return Notification.Type.EMAIL;
        }

        @Override
        public CompletableFuture<Void> send(
            Notification notification, EmailNotificationHandler notificationHandler,
            NotificationHandlerContext notificationHandlerContext) {

            recordSend(notification, notificationHandler.getSubject(notificationHandlerContext),
                notificationHandlerContext);

            return CompletableFuture.completedFuture(null);
        }

        @Override
        public NotificationTestResult sendTest(
            Notification notification, EmailNotificationHandler notificationHandler,
            NotificationHandlerContext notificationHandlerContext) {

            return NotificationTestResult.sent(false);
        }
    }

    private final class RecordingSlackNotificationSender implements NotificationSender<SlackNotificationHandler> {

        @Override
        public Notification.Type getType() {
            return Notification.Type.SLACK;
        }

        @Override
        public CompletableFuture<Void> send(
            Notification notification, SlackNotificationHandler notificationHandler,
            NotificationHandlerContext notificationHandlerContext) {

            recordSend(notification, notificationHandler.getText(notificationHandlerContext),
                notificationHandlerContext);

            return CompletableFuture.completedFuture(null);
        }

        @Override
        public NotificationTestResult sendTest(
            Notification notification, SlackNotificationHandler notificationHandler,
            NotificationHandlerContext notificationHandlerContext) {

            return NotificationTestResult.sent(false);
        }
    }
}
