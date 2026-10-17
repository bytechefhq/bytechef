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

package com.bytechef.platform.coordinator.monitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.repository.JobRepository;
import com.bytechef.atlas.execution.repository.TaskExecutionRepository;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.coordinator.config.PlatformCoordinatorIntTestConfiguration;
import com.bytechef.platform.coordinator.config.RecordingWebhookNotificationSender;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import com.bytechef.platform.notification.handler.NotificationHandlerRegistry;
import com.bytechef.platform.notification.handler.NotificationSenderRegistry;
import com.bytechef.platform.notification.repository.NotificationEventRepository;
import com.bytechef.platform.notification.repository.NotificationRepository;
import com.bytechef.platform.notification.service.NotificationService;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.tenant.service.TenantService;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = PlatformCoordinatorIntTestConfiguration.class)
class ApprovalNotificationMonitorIntTest {

    private static final Duration ESCALATION_AFTER = Duration.ofHours(4);
    private static final Duration REMINDER_LEAD_TIME = Duration.ofHours(24);
    private static final String PUBLIC_URL = "http://localhost:5173";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JobRepository jobRepository;

    @MockitoSpyBean
    private JobService jobService;

    @Autowired
    private NotificationEventRepository notificationEventRepository;

    @Autowired
    private NotificationHandlerRegistry notificationHandlerRegistry;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationSenderRegistry notificationSenderRegistry;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RecordingWebhookNotificationSender recordingWebhookNotificationSender;

    @Autowired
    private TaskExecutionRepository taskExecutionRepository;

    @Autowired
    private TaskExecutionService taskExecutionService;

    @Autowired
    private TenantService tenantService;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("DELETE FROM notification_notification_event");
        jdbcTemplate.update("DELETE FROM notification");
        jdbcTemplate.update("DELETE FROM task_execution");
        jdbcTemplate.update("DELETE FROM job");

        recordingWebhookNotificationSender.reset();
    }

    @Test
    void testExpiringApprovalSendsReminderOnceAndMarksJob() {
        Instant expiresAt = hoursFromNow(4);

        long jobId = saveApprovalJob(expiresAt, hoursFromNow(-1), null);

        saveNotification("ok", Notification.Type.WEBHOOK, NotificationEvent.Type.JOB_APPROVAL_EXPIRING);

        ApprovalNotificationMonitor approvalNotificationMonitor = monitor(REMINDER_LEAD_TIME, null);

        approvalNotificationMonitor.notifyPendingApprovals();
        approvalNotificationMonitor.notifyPendingApprovals();

        List<NotificationHandlerContext> notificationHandlerContexts =
            recordingWebhookNotificationSender.getDeliveredNotificationHandlerContexts();

        assertThat(notificationHandlerContexts).hasSize(1);

        NotificationHandlerContext notificationHandlerContext = notificationHandlerContexts.getFirst();

        Job job = getJob(jobId);

        assertThat(notificationHandlerContext.getEventType()).isEqualTo(NotificationEvent.Type.JOB_APPROVAL_EXPIRING);
        assertThat(notificationHandlerContext.getJobId()).isEqualTo(jobId);
        assertThat(notificationHandlerContext.getApprovalExpiresAt()).isEqualTo(expiresAt.toString());
        assertThat(notificationHandlerContext.getApprovalFormUrl())
            .isEqualTo(PUBLIC_URL + "/resume/" + job.getMetadata(MetadataConstants.JOB_RESUME_ID));
        assertThat(job.getMetadata(ApprovalNotificationMonitor.APPROVAL_REMINDER_SENT_AT)).isNotNull();
    }

    @Test
    void testReminderIsNotSentWhenAnotherReplicaAlreadyClaimedIt() {
        long jobId = saveApprovalJob(hoursFromNow(4), hoursFromNow(-1), null);

        saveNotification("ok", Notification.Type.WEBHOOK, NotificationEvent.Type.JOB_APPROVAL_EXPIRING);

        touchJobAfterTheSweepReadsIt(jobId);

        monitor(REMINDER_LEAD_TIME, null).notifyPendingApprovals();

        assertThat(recordingWebhookNotificationSender.getAttemptedNotificationNames()).isEmpty();
        assertThat(getMarker(jobId, ApprovalNotificationMonitor.APPROVAL_REMINDER_SENT_AT)).isNull();
    }

    @Test
    void testAlreadyRemindedJobIsSkipped() {
        long jobId = saveApprovalJob(hoursFromNow(4), hoursFromNow(-1), null);

        putMarker(jobId, ApprovalNotificationMonitor.APPROVAL_REMINDER_SENT_AT);

        saveNotification("ok", Notification.Type.WEBHOOK, NotificationEvent.Type.JOB_APPROVAL_EXPIRING);

        monitor(REMINDER_LEAD_TIME, null).notifyPendingApprovals();

        assertThat(recordingWebhookNotificationSender.getAttemptedNotificationNames()).isEmpty();
    }

    @Test
    void testSuspendThatIsNotAnApprovalIsSkipped() {
        long jobId = saveApprovalJob(hoursFromNow(4), hoursFromNow(-6), null);

        removeMetadata(jobId, MetadataConstants.APPROVAL_RESUME);

        saveNotification("ok", Notification.Type.WEBHOOK, NotificationEvent.Type.JOB_APPROVAL_EXPIRING);
        saveNotification("ok", Notification.Type.WEBHOOK, NotificationEvent.Type.JOB_APPROVAL_ESCALATED);

        monitor(REMINDER_LEAD_TIME, ESCALATION_AFTER).notifyPendingApprovals();

        assertThat(recordingWebhookNotificationSender.getAttemptedNotificationNames()).isEmpty();
    }

    @Test
    void testApprovalOutsideLeadTimeIsSkipped() {
        long jobId = saveApprovalJob(hoursFromNow(48), hoursFromNow(-1), null);

        saveNotification("ok", Notification.Type.WEBHOOK, NotificationEvent.Type.JOB_APPROVAL_EXPIRING);

        monitor(REMINDER_LEAD_TIME, null).notifyPendingApprovals();

        assertThat(recordingWebhookNotificationSender.getAttemptedNotificationNames()).isEmpty();
        assertThat(getMarker(jobId, ApprovalNotificationMonitor.APPROVAL_REMINDER_SENT_AT)).isNull();
    }

    @Test
    void testExpiredApprovalIsNeitherRemindedNorEscalated() {
        saveApprovalJob(hoursFromNow(-1), hoursFromNow(-10), null);

        saveNotification("ok", Notification.Type.WEBHOOK, NotificationEvent.Type.JOB_APPROVAL_EXPIRING);
        saveNotification("ok", Notification.Type.WEBHOOK, NotificationEvent.Type.JOB_APPROVAL_ESCALATED);

        monitor(REMINDER_LEAD_TIME, ESCALATION_AFTER).notifyPendingApprovals();

        assertThat(recordingWebhookNotificationSender.getAttemptedNotificationNames()).isEmpty();
    }

    @Test
    void testNoConfiguredNotificationClearsTheReminderMarkerForTheNextSweep() {
        long jobId = saveApprovalJob(hoursFromNow(4), hoursFromNow(-1), null);

        monitor(REMINDER_LEAD_TIME, null).notifyPendingApprovals();

        assertThat(getMarker(jobId, ApprovalNotificationMonitor.APPROVAL_REMINDER_SENT_AT)).isNull();
    }

    @Test
    void testTotalSendFailureClearsTheReminderMarkerForTheNextSweep() {
        long jobId = saveApprovalJob(hoursFromNow(4), hoursFromNow(-1), null);

        saveNotification(
            RecordingWebhookNotificationSender.FAILING_NOTIFICATION_NAME, Notification.Type.WEBHOOK,
            NotificationEvent.Type.JOB_APPROVAL_EXPIRING);

        monitor(REMINDER_LEAD_TIME, null).notifyPendingApprovals();

        assertThat(recordingWebhookNotificationSender.getAttemptedNotificationNames())
            .containsExactly(RecordingWebhookNotificationSender.FAILING_NOTIFICATION_NAME);
        assertThat(getMarker(jobId, ApprovalNotificationMonitor.APPROVAL_REMINDER_SENT_AT)).isNull();
    }

    @Test
    void testOneFailingReminderNotificationDoesNotSkipTheOthers() {
        long jobId = saveApprovalJob(hoursFromNow(4), hoursFromNow(-1), null);

        saveNotification(
            RecordingWebhookNotificationSender.FAILING_NOTIFICATION_NAME, Notification.Type.WEBHOOK,
            NotificationEvent.Type.JOB_APPROVAL_EXPIRING);
        saveNotification("ok", Notification.Type.WEBHOOK, NotificationEvent.Type.JOB_APPROVAL_EXPIRING);

        monitor(REMINDER_LEAD_TIME, null).notifyPendingApprovals();

        assertThat(recordingWebhookNotificationSender.getAttemptedNotificationNames())
            .containsExactlyInAnyOrder(RecordingWebhookNotificationSender.FAILING_NOTIFICATION_NAME, "ok");
        assertThat(recordingWebhookNotificationSender.getDeliveredNotificationHandlerContexts()).hasSize(1);
        assertThat(getMarker(jobId, ApprovalNotificationMonitor.APPROVAL_REMINDER_SENT_AT)).isNotNull();
    }

    @Test
    void testSweepContinuesPastAJobWhoseClaimFailsUnexpectedly() {
        long firstJobId = saveApprovalJob(hoursFromNow(4), hoursFromNow(-1), null);
        long secondJobId = saveApprovalJob(hoursFromNow(4), hoursFromNow(-1), null);

        saveNotification("ok", Notification.Type.WEBHOOK, NotificationEvent.Type.JOB_APPROVAL_EXPIRING);

        doThrow(new IllegalStateException("database unavailable"))
            .doCallRealMethod()
            .when(jobService)
            .update(any());

        monitor(REMINDER_LEAD_TIME, null).notifyPendingApprovals();

        List<NotificationHandlerContext> notificationHandlerContexts =
            recordingWebhookNotificationSender.getDeliveredNotificationHandlerContexts();

        assertThat(notificationHandlerContexts).hasSize(1);

        NotificationHandlerContext notificationHandlerContext = notificationHandlerContexts.getFirst();

        assertThat(notificationHandlerContext.getJobId()).isIn(firstJobId, secondJobId);
    }

    @Test
    void testStalledApprovalSendsEscalationOnceAndMarksJob() {
        Instant expiresAt = hoursFromNow(48);

        long jobId = saveApprovalJob(expiresAt, hoursFromNow(-5), null);

        saveNotification("ok", Notification.Type.WEBHOOK, NotificationEvent.Type.JOB_APPROVAL_ESCALATED);

        ApprovalNotificationMonitor approvalNotificationMonitor = monitor(null, ESCALATION_AFTER);

        approvalNotificationMonitor.notifyPendingApprovals();
        approvalNotificationMonitor.notifyPendingApprovals();

        List<NotificationHandlerContext> notificationHandlerContexts =
            recordingWebhookNotificationSender.getDeliveredNotificationHandlerContexts();

        assertThat(notificationHandlerContexts).hasSize(1);

        NotificationHandlerContext notificationHandlerContext = notificationHandlerContexts.getFirst();

        assertThat(notificationHandlerContext.getEventType())
            .isEqualTo(NotificationEvent.Type.JOB_APPROVAL_ESCALATED);
        assertThat(notificationHandlerContext.getApprovalExpiresAt()).isEqualTo(expiresAt.toString());
        assertThat(getMarker(jobId, ApprovalNotificationMonitor.APPROVAL_ESCALATED_AT)).isNotNull();
    }

    @Test
    void testApprovalWithinEscalationWindowIsSkipped() {
        long jobId = saveApprovalJob(hoursFromNow(48), hoursFromNow(-1), null);

        saveNotification("ok", Notification.Type.WEBHOOK, NotificationEvent.Type.JOB_APPROVAL_ESCALATED);

        monitor(null, ESCALATION_AFTER).notifyPendingApprovals();

        assertThat(recordingWebhookNotificationSender.getAttemptedNotificationNames()).isEmpty();
        assertThat(getMarker(jobId, ApprovalNotificationMonitor.APPROVAL_ESCALATED_AT)).isNull();
    }

    @Test
    void testEscalationWindowStartsWhenTheApprovalWasRaised() {
        long jobId = saveApprovalJob(hoursFromNow(10), hoursFromNow(-5), hoursFromNow(-1));

        saveNotification("ok", Notification.Type.WEBHOOK, NotificationEvent.Type.JOB_APPROVAL_ESCALATED);

        monitor(null, ESCALATION_AFTER).notifyPendingApprovals();

        assertThat(recordingWebhookNotificationSender.getAttemptedNotificationNames()).isEmpty();
        assertThat(getMarker(jobId, ApprovalNotificationMonitor.APPROVAL_ESCALATED_AT)).isNull();
    }

    @Test
    void testAlreadyEscalatedJobIsSkipped() {
        long jobId = saveApprovalJob(hoursFromNow(48), hoursFromNow(-5), null);

        putMarker(jobId, ApprovalNotificationMonitor.APPROVAL_ESCALATED_AT);

        saveNotification("ok", Notification.Type.WEBHOOK, NotificationEvent.Type.JOB_APPROVAL_ESCALATED);

        monitor(null, ESCALATION_AFTER).notifyPendingApprovals();

        assertThat(recordingWebhookNotificationSender.getAttemptedNotificationNames()).isEmpty();
    }

    @Test
    void testNoEscalationAfterConfiguredIsNoOp() {
        saveApprovalJob(hoursFromNow(48), hoursFromNow(-5), null);

        saveNotification("ok", Notification.Type.WEBHOOK, NotificationEvent.Type.JOB_APPROVAL_ESCALATED);

        monitor(null, null).notifyPendingApprovals();
        monitor(null, Duration.ZERO).notifyPendingApprovals();

        assertThat(recordingWebhookNotificationSender.getAttemptedNotificationNames()).isEmpty();
    }

    @Test
    void testEscalationIsNotSentWhenAnotherReplicaAlreadyClaimedIt() {
        long jobId = saveApprovalJob(hoursFromNow(48), hoursFromNow(-5), null);

        saveNotification("ok", Notification.Type.WEBHOOK, NotificationEvent.Type.JOB_APPROVAL_ESCALATED);

        touchJobAfterTheSweepReadsIt(jobId);

        monitor(null, ESCALATION_AFTER).notifyPendingApprovals();

        assertThat(recordingWebhookNotificationSender.getAttemptedNotificationNames()).isEmpty();
        assertThat(getMarker(jobId, ApprovalNotificationMonitor.APPROVAL_ESCALATED_AT)).isNull();
    }

    @Test
    void testTotalEscalationFailureClearsTheMarkerForTheNextSweep() {
        long jobId = saveApprovalJob(hoursFromNow(48), hoursFromNow(-5), null);

        saveNotification(
            RecordingWebhookNotificationSender.FAILING_NOTIFICATION_NAME, Notification.Type.WEBHOOK,
            NotificationEvent.Type.JOB_APPROVAL_ESCALATED);

        monitor(null, ESCALATION_AFTER).notifyPendingApprovals();

        assertThat(recordingWebhookNotificationSender.getAttemptedNotificationNames()).hasSize(1);
        assertThat(getMarker(jobId, ApprovalNotificationMonitor.APPROVAL_ESCALATED_AT)).isNull();
    }

    @Test
    void testNoConfiguredEscalationNotificationClearsTheMarkerForTheNextSweep() {
        long jobId = saveApprovalJob(hoursFromNow(48), hoursFromNow(-5), null);

        monitor(null, ESCALATION_AFTER).notifyPendingApprovals();

        assertThat(getMarker(jobId, ApprovalNotificationMonitor.APPROVAL_ESCALATED_AT)).isNull();
    }

    @Test
    void testReminderAndEscalationDueInTheSameSweepAreBothSent() {
        long jobId = saveApprovalJob(hoursFromNow(4), hoursFromNow(-5), null);

        saveNotification("ok", Notification.Type.WEBHOOK, NotificationEvent.Type.JOB_APPROVAL_EXPIRING);
        saveNotification("ok", Notification.Type.WEBHOOK, NotificationEvent.Type.JOB_APPROVAL_ESCALATED);

        monitor(REMINDER_LEAD_TIME, ESCALATION_AFTER).notifyPendingApprovals();

        assertThat(recordingWebhookNotificationSender.getDeliveredNotificationHandlerContexts())
            .extracting(NotificationHandlerContext::getEventType)
            .containsExactly(NotificationEvent.Type.JOB_APPROVAL_EXPIRING,
                NotificationEvent.Type.JOB_APPROVAL_ESCALATED);
        assertThat(getMarker(jobId, ApprovalNotificationMonitor.APPROVAL_REMINDER_SENT_AT)).isNotNull();
        assertThat(getMarker(jobId, ApprovalNotificationMonitor.APPROVAL_ESCALATED_AT)).isNotNull();
    }

    @Test
    void testMissingSenderCountsAsAFailedAttempt() {
        long jobId = saveApprovalJob(hoursFromNow(4), hoursFromNow(-1), null);

        saveNotification("email", Notification.Type.EMAIL, NotificationEvent.Type.JOB_APPROVAL_EXPIRING);

        monitor(REMINDER_LEAD_TIME, null).notifyPendingApprovals();

        assertThat(getMarker(jobId, ApprovalNotificationMonitor.APPROVAL_REMINDER_SENT_AT)).isNull();
    }

    @Test
    void testMissingHandlerCountsAsAFailedAttempt() {
        long jobId = saveApprovalJob(hoursFromNow(4), hoursFromNow(-1), null);

        saveNotification("slack", Notification.Type.SLACK, NotificationEvent.Type.JOB_APPROVAL_EXPIRING);

        monitor(REMINDER_LEAD_TIME, null).notifyPendingApprovals();

        assertThat(getMarker(jobId, ApprovalNotificationMonitor.APPROVAL_REMINDER_SENT_AT)).isNull();
    }

    @Test
    void testMissingSenderAlongsideADeliveredNotificationCountsAsDelivered() {
        long jobId = saveApprovalJob(hoursFromNow(4), hoursFromNow(-1), null);

        saveNotification("email", Notification.Type.EMAIL, NotificationEvent.Type.JOB_APPROVAL_EXPIRING);
        saveNotification("ok", Notification.Type.WEBHOOK, NotificationEvent.Type.JOB_APPROVAL_EXPIRING);

        monitor(REMINDER_LEAD_TIME, null).notifyPendingApprovals();

        assertThat(recordingWebhookNotificationSender.getDeliveredNotificationHandlerContexts()).hasSize(1);
        assertThat(getMarker(jobId, ApprovalNotificationMonitor.APPROVAL_REMINDER_SENT_AT)).isNotNull();
    }

    private ApprovalNotificationMonitor monitor(
        @Nullable Duration reminderLeadTime, @Nullable Duration escalationAfter) {

        return new ApprovalNotificationMonitor(
            null, escalationAfter, jobService, notificationHandlerRegistry, notificationSenderRegistry,
            notificationService, PUBLIC_URL, reminderLeadTime, taskExecutionService, tenantService);
    }

    private long saveApprovalJob(Instant expiresAt, Instant startedAt, @Nullable Instant raisedAt) {
        Job job = new Job();

        job.setLabel("Test Workflow");
        job.setStatus(Job.Status.STOPPED);
        job.setWorkflowId("demo:1234");

        job = jobRepository.save(job);

        long jobId = Objects.requireNonNull(job.getId());

        Map<String, Object> suspend = new HashMap<>();

        suspend.put("expiresAt", expiresAt.toString());

        if (raisedAt != null) {
            suspend.put("continueParameters", Map.of(MetadataConstants.APPROVAL_RAISED_AT, raisedAt.toString()));
        }

        TaskExecution taskExecution = TaskExecution.builder()
            .jobId(jobId)
            .taskNumber(1)
            .workflowTask(new WorkflowTask(Map.of("name", "approval1", "type", "approval/v1/approve")))
            .build();

        taskExecution.setStartDate(startedAt);
        taskExecution.putMetadata(MetadataConstants.SUSPEND, suspend);

        taskExecution = taskExecutionRepository.save(taskExecution);

        JobResumeId jobResumeId = JobResumeId.of(jobId);

        job.setMetadata(
            Map.of(
                MetadataConstants.APPROVAL_RESUME, true, MetadataConstants.JOB_RESUME_ID, jobResumeId.toString(),
                MetadataConstants.TASK_EXECUTION_RESUME_ID, Objects.requireNonNull(taskExecution.getId())));

        jobRepository.save(job);

        return jobId;
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

    private void touchJobAfterTheSweepReadsIt(long jobId) {
        doAnswer(invocation -> {
            Object staleJobs = invocation.callRealMethod();

            putMarker(jobId, "touchedByAnotherReplica");

            return staleJobs;
        }).when(jobService)
            .getStaleJobs(any(), any());
    }

    private Job getJob(long jobId) {
        return OptionalUtils.get(jobRepository.findById(jobId));
    }

    private @Nullable Object getMarker(long jobId, String markerKey) {
        Job job = getJob(jobId);

        return job.getMetadata(markerKey);
    }

    private void putMarker(long jobId, String markerKey) {
        Job job = getJob(jobId);

        Map<String, Object> metadata = new HashMap<>(job.getMetadata());

        Instant now = Instant.now();

        metadata.put(markerKey, now.toString());

        job.setMetadata(metadata);

        jobRepository.save(job);
    }

    private void removeMetadata(long jobId, String key) {
        Job job = getJob(jobId);

        Map<String, Object> metadata = new HashMap<>(job.getMetadata());

        metadata.remove(key);

        job.setMetadata(metadata);

        jobRepository.save(job);
    }

    private static Instant hoursFromNow(long hours) {
        Instant now = Instant.now();

        return now.plus(Duration.ofHours(hours));
    }
}
