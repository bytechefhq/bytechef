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

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.definition.SuspendUtils;
import com.bytechef.platform.notification.domain.Notification;
import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.handler.NotificationHandler;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import com.bytechef.platform.notification.handler.NotificationHandlerRegistry;
import com.bytechef.platform.notification.handler.NotificationSender;
import com.bytechef.platform.notification.handler.NotificationSenderRegistry;
import com.bytechef.platform.notification.service.NotificationService;
import com.bytechef.platform.workflow.execution.token.ApprovalFormUrls;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Reminds subscribed notification targets about runs paused on an approval that is about to expire. A suspended run
 * sits STOPPED until someone resolves it; once the suspend expiry passes, {@link ApprovalExpiryMonitor} fails the run.
 * This sweep fires a {@code JOB_APPROVAL_EXPIRING} notification through the central notification registry while there
 * is still time to act — delivery targets are the admin-configured {@code Notification} rows, the same registry the
 * job-status notifications use. Reminders are per-run idempotent: the send is recorded in the job metadata under
 * {@code approvalReminderSentAt}, so each paused run is reminded at most once.
 *
 * <p>
 * The lead time defaults to 24 hours before expiry and is configurable via
 * {@code bytechef.workflow.execution.approval-reminder.lead-time}; disable the sweep entirely with
 * {@code bytechef.workflow.execution.approval-reminder.enabled=false}. Approvals whose total lifetime is shorter than
 * the lead time are reminded on the first sweep after they pause. In the distributed EE deployment the
 * stale-STOPPED-jobs finder is served over REST by the execution app (see {@code RemoteJobServiceClient#getStaleJobs}),
 * so the sweep runs there too; the {@code UnsupportedOperationException} catch below is a defensive fallback only.
 * </p>
 *
 * @author Ivica Cardic
 */
public class ApprovalReminderMonitor {

    private static final String APPROVAL_REMINDER_SENT_AT = "approvalReminderSentAt";

    private static final Logger log = LoggerFactory.getLogger(ApprovalReminderMonitor.class);

    private final @Nullable ApprovalTokens approvalTokens;
    private final JobService jobService;
    private final Duration leadTime;
    private final NotificationHandlerRegistry notificationHandlerRegistry;
    private final NotificationSenderRegistry notificationSenderRegistry;
    private final NotificationService notificationService;
    private final @Nullable String publicUrl;
    private final TaskExecutionService taskExecutionService;
    private final TenantService tenantService;

    @SuppressFBWarnings("EI")
    public ApprovalReminderMonitor(
        @Nullable ApprovalTokens approvalTokens, JobService jobService, Duration leadTime,
        NotificationHandlerRegistry notificationHandlerRegistry,
        NotificationSenderRegistry notificationSenderRegistry, NotificationService notificationService,
        @Nullable String publicUrl, TaskExecutionService taskExecutionService, TenantService tenantService) {

        this.approvalTokens = approvalTokens;
        this.jobService = jobService;
        this.leadTime = leadTime;
        this.notificationHandlerRegistry = notificationHandlerRegistry;
        this.notificationSenderRegistry = notificationSenderRegistry;
        this.notificationService = notificationService;
        this.publicUrl = publicUrl;
        this.taskExecutionService = taskExecutionService;
        this.tenantService = tenantService;
    }

    @Scheduled(initialDelayString = "PT6M", fixedDelayString = "PT15M")
    public void remindExpiringApprovals() {
        for (String tenantId : tenantService.getTenantIds()) {
            try {
                TenantContext.runWithTenantId(tenantId, this::remindCurrentTenant);
            } catch (UnsupportedOperationException unsupportedOperationException) {
                if (log.isDebugEnabled()) {
                    log.debug(
                        "Stale-job finder unavailable for tenant {}; skipping approval-reminder sweep", tenantId);
                }
            } catch (Exception exception) {
                log.warn("Approval-reminder sweep failed for tenant {}: {}", tenantId, exception.getMessage());
            }
        }
    }

    private void remindCurrentTenant() {
        Instant now = Instant.now();

        for (Job job : jobService.getStaleJobs(Job.Status.STOPPED, now)) {
            Object jobResumeId = job.getMetadata(MetadataConstants.JOB_RESUME_ID);

            if (jobResumeId == null || job.getMetadata(APPROVAL_REMINDER_SENT_AT) != null) {
                continue;
            }

            Instant expiresAt = readSuspendExpiry(job);

            if (expiresAt == null || !expiresAt.isAfter(now) || expiresAt.isAfter(now.plus(leadTime))) {
                continue;
            }

            // Claim the reminder BEFORE sending so two coordinator replicas cannot both send it: the first to commit
            // the APPROVAL_REMINDER_SENT_AT marker wins under the job's @Version, and the loser's stale-version update
            // throws and skips. Writing the marker first degrades a send failure to a missed reminder rather than a
            // duplicate — acceptable because the expiry sweep is the real safety net.
            try {
                markReminderSent(job, now);
            } catch (OptimisticLockingFailureException optimisticLockingFailureException) {
                continue;
            }

            try {
                sendReminder(job, jobResumeId.toString(), expiresAt);
            } catch (Exception exception) {
                log.warn(
                    "Could not send approval reminder for job {}: {}", job.getId(), exception.getMessage());
            }
        }
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private void sendReminder(Job job, String jobResumeId, Instant expiresAt) {
        NotificationEvent.Type eventType = NotificationEvent.Type.JOB_APPROVAL_EXPIRING;

        String formUrl = ApprovalFormUrls.buildFormUrl(publicUrl, jobResumeId, approvalTokens)
            .orElse(null);

        NotificationHandlerContext notificationHandlerContext = new NotificationHandlerContext.Builder()
            .approvalExpiresAt(expiresAt.toString())
            .approvalFormUrl(formUrl)
            .eventType(eventType)
            .jobId(job.getId())
            .jobName(job.getLabel())
            .build();

        List<Notification> notifications = notificationService.getNotifications(eventType);

        for (Notification notification : notifications) {
            NotificationSender notificationSender = notificationSenderRegistry.getNotificationSender(
                notification.getType());

            NotificationHandler notificationHandler = notificationHandlerRegistry.getNotificationHandler(
                eventType, notification.getType());

            if (notificationSender == null || notificationHandler == null) {
                log.warn(
                    "No {} found for notification {} and event type {}; skipping",
                    notificationSender == null ? "sender" : "handler", notification.getId(), eventType);

                continue;
            }

            notificationSender.send(notification, notificationHandler, notificationHandlerContext);
        }
    }

    private void markReminderSent(Job job, Instant sentAt) {
        Map<String, Object> metadata = new HashMap<>(job.getMetadata());

        metadata.put(APPROVAL_REMINDER_SENT_AT, sentAt.toString());

        job.setMetadata(metadata);

        jobService.update(job);
    }

    private @Nullable Instant readSuspendExpiry(Job job) {
        Long taskExecutionResumeId = MapUtils.getLong(job.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID);

        if (taskExecutionResumeId == null) {
            return null;
        }

        try {
            TaskExecution taskExecution = taskExecutionService.getTaskExecution(taskExecutionResumeId);

            return SuspendUtils.extractSuspendExpiresAt(taskExecution.getMetadata());
        } catch (Exception exception) {
            return null;
        }
    }
}
