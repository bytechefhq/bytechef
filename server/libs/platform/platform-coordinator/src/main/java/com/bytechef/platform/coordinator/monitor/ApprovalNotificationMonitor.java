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
import com.bytechef.platform.notification.domain.NotificationEvent;
import com.bytechef.platform.notification.handler.NotificationHandlerContext;
import com.bytechef.platform.notification.handler.NotificationHandlerRegistry;
import com.bytechef.platform.notification.handler.NotificationSenderRegistry;
import com.bytechef.platform.notification.service.NotificationService;
import com.bytechef.platform.workflow.execution.JobSuspends;
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
import java.util.Optional;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * @author Ivica Cardic
 */
public class ApprovalNotificationMonitor {

    static final String APPROVAL_ESCALATED_AT = "approvalEscalatedAt";
    static final String APPROVAL_REMINDER_SENT_AT = "approvalReminderSentAt";

    private static final Logger log = LoggerFactory.getLogger(ApprovalNotificationMonitor.class);

    private final ApprovalNotificationDispatcher approvalNotificationDispatcher;
    private final @Nullable ApprovalTokens approvalTokens;
    private final @Nullable Duration escalationAfter;
    private final JobService jobService;
    private final @Nullable String publicUrl;
    private final @Nullable Duration reminderLeadTime;
    private final TaskExecutionService taskExecutionService;
    private final TenantService tenantService;

    @SuppressFBWarnings("EI")
    public ApprovalNotificationMonitor(
        @Nullable ApprovalTokens approvalTokens, @Nullable Duration escalationAfter, JobService jobService,
        NotificationHandlerRegistry notificationHandlerRegistry,
        NotificationSenderRegistry notificationSenderRegistry, NotificationService notificationService,
        @Nullable String publicUrl, @Nullable Duration reminderLeadTime, TaskExecutionService taskExecutionService,
        TenantService tenantService) {

        this.approvalNotificationDispatcher = new ApprovalNotificationDispatcher(
            notificationHandlerRegistry, notificationSenderRegistry, notificationService);
        this.approvalTokens = approvalTokens;
        this.escalationAfter = positiveOrNull(escalationAfter);
        this.jobService = jobService;
        this.publicUrl = publicUrl;
        this.reminderLeadTime = positiveOrNull(reminderLeadTime);
        this.taskExecutionService = taskExecutionService;
        this.tenantService = tenantService;
    }

    @Scheduled(initialDelayString = "PT6M", fixedDelayString = "PT15M")
    @SchedulerLock(name = "approvalNotificationMonitor", lockAtLeastFor = "PT5M", lockAtMostFor = "PT1H")
    public void notifyPendingApprovals() {
        if (reminderLeadTime == null && escalationAfter == null) {
            return;
        }

        for (String tenantId : tenantService.getTenantIds()) {
            try {
                TenantContext.runWithTenantId(tenantId, this::notifyCurrentTenant);
            } catch (RuntimeException runtimeException) {
                log.warn("Approval-notification sweep failed for tenant {}", tenantId, runtimeException);
            }
        }
    }

    private void notifyCurrentTenant() {
        Instant now = Instant.now();

        List<Job> stoppedJobs;

        try {
            stoppedJobs = jobService.getStaleJobs(Job.Status.STOPPED, now);
        } catch (UnsupportedOperationException unsupportedOperationException) {
            if (log.isDebugEnabled()) {
                log.debug("Stale-job finder unavailable; skipping approval-notification sweep");
            }

            return;
        }

        for (Job job : stoppedJobs) {
            try {
                notifyJob(job, now);
            } catch (RuntimeException runtimeException) {
                log.warn("Could not process the approval notifications for job {}", job.getId(), runtimeException);
            }
        }
    }

    private void notifyJob(Job job, Instant now) {
        Object jobResumeId = job.getMetadata(MetadataConstants.JOB_RESUME_ID);

        if (jobResumeId == null || !JobSuspends.isSuspendedForApproval(job)) {
            return;
        }

        boolean reminderPending = reminderLeadTime != null && job.getMetadata(APPROVAL_REMINDER_SENT_AT) == null;
        boolean escalationPending = escalationAfter != null && job.getMetadata(APPROVAL_ESCALATED_AT) == null;

        if (!reminderPending && !escalationPending) {
            return;
        }

        TaskExecution taskExecution = fetchSuspendedTaskExecution(job);

        if (taskExecution == null) {
            return;
        }

        Instant expiresAt = SuspendUtils.extractSuspendExpiresAt(taskExecution.getMetadata());

        Job currentJob = job;

        if (reminderPending && isReminderDue(expiresAt, now)) {
            currentJob = claimAndSend(
                currentJob, jobResumeId.toString(), APPROVAL_REMINDER_SENT_AT,
                NotificationEvent.Type.JOB_APPROVAL_EXPIRING, expiresAt, now);
        }

        if (currentJob != null && escalationPending && isEscalationDue(taskExecution, expiresAt, now)) {
            claimAndSend(
                currentJob, jobResumeId.toString(), APPROVAL_ESCALATED_AT,
                NotificationEvent.Type.JOB_APPROVAL_ESCALATED, expiresAt, now);
        }
    }

    private boolean isReminderDue(@Nullable Instant expiresAt, Instant now) {
        if (reminderLeadTime == null || expiresAt == null || !expiresAt.isAfter(now)) {
            return false;
        }

        Instant remindFrom = expiresAt.minus(reminderLeadTime);

        return !remindFrom.isAfter(now);
    }

    private boolean isEscalationDue(TaskExecution taskExecution, @Nullable Instant expiresAt, Instant now) {
        if (escalationAfter == null || (expiresAt != null && !expiresAt.isAfter(now))) {
            return false;
        }

        Instant raisedAt = SuspendUtils.extractApprovalRaisedAt(taskExecution.getMetadata());

        if (raisedAt == null) {
            raisedAt = taskExecution.getStartDate();
        }

        if (raisedAt == null) {
            return false;
        }

        Instant escalateAt = raisedAt.plus(escalationAfter);

        return !escalateAt.isAfter(now);
    }

    private @Nullable Job claimAndSend(
        Job job, String jobResumeId, String markerKey, NotificationEvent.Type eventType,
        @Nullable Instant expiresAt, Instant now) {

        Job claimedJob;

        try {
            claimedJob = putMarker(job, markerKey, now.toString());
        } catch (OptimisticLockingFailureException optimisticLockingFailureException) {
            return null;
        }

        ApprovalNotificationDispatcher.Result result;

        try {
            result = send(claimedJob, jobResumeId, eventType, expiresAt);
        } catch (RuntimeException runtimeException) {
            log.warn("Could not send the {} notification for job {}", eventType, job.getId(), runtimeException);

            result = ApprovalNotificationDispatcher.Result.FAILED;
        }

        if (result == ApprovalNotificationDispatcher.Result.DELIVERED) {
            return claimedJob;
        }

        return releaseClaim(claimedJob, markerKey);
    }

    private ApprovalNotificationDispatcher.Result send(
        Job job, String jobResumeId, NotificationEvent.Type eventType, @Nullable Instant expiresAt) {

        Optional<String> formUrl = ApprovalFormUrls.buildFormUrl(publicUrl, jobResumeId, approvalTokens, expiresAt);

        NotificationHandlerContext notificationHandlerContext = new NotificationHandlerContext.Builder()
            .approvalExpiresAt(expiresAt == null ? null : expiresAt.toString())
            .approvalFormUrl(formUrl.orElse(null))
            .eventType(eventType)
            .jobId(job.getId())
            .jobName(job.getLabel())
            .build();

        return approvalNotificationDispatcher.dispatch(eventType, notificationHandlerContext);
    }

    private @Nullable Job releaseClaim(Job claimedJob, String markerKey) {
        try {
            return putMarker(claimedJob, markerKey, null);
        } catch (RuntimeException runtimeException) {
            log.warn(
                "Could not release the {} claim on job {}; it will not be retried", markerKey, claimedJob.getId(),
                runtimeException);

            return null;
        }
    }

    private Job putMarker(Job job, String markerKey, @Nullable String value) {
        Map<String, Object> metadata = new HashMap<>(job.getMetadata());

        if (value == null) {
            metadata.remove(markerKey);
        } else {
            metadata.put(markerKey, value);
        }

        job.setMetadata(metadata);

        return jobService.update(job);
    }

    private @Nullable TaskExecution fetchSuspendedTaskExecution(Job job) {
        Long taskExecutionResumeId = MapUtils.getLong(job.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID);

        if (taskExecutionResumeId == null) {
            return null;
        }

        try {
            return taskExecutionService.getTaskExecution(taskExecutionResumeId);
        } catch (RuntimeException runtimeException) {
            log.warn(
                "Could not read the suspended task execution {} of job {}; skipping its notifications this sweep",
                taskExecutionResumeId, job.getId(), runtimeException);

            return null;
        }
    }

    private static @Nullable Duration positiveOrNull(@Nullable Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return null;
        }

        return duration;
    }
}
