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

package com.bytechef.automation.task.monitor;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.service.ApprovalTaskService;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(
    name = "bytechef.automation.approval-task.reconciliation.enabled", havingValue = "true", matchIfMissing = true)
public class ApprovalTaskReconciliationMonitor {

    private static final Logger log = LoggerFactory.getLogger(ApprovalTaskReconciliationMonitor.class);

    private final ApprovalTaskService approvalTaskService;
    private final ApprovalTokens approvalTokens;
    private final JobService jobService;
    private final TenantService tenantService;

    @SuppressFBWarnings("EI")
    public ApprovalTaskReconciliationMonitor(
        ApprovalTaskService approvalTaskService, ApprovalTokens approvalTokens, JobService jobService,
        TenantService tenantService) {

        this.approvalTaskService = approvalTaskService;
        this.approvalTokens = approvalTokens;
        this.jobService = jobService;
        this.tenantService = tenantService;
    }

    @Scheduled(initialDelayString = "PT7M", fixedDelayString = "PT15M")
    public void reconcileApprovalTasks() {
        for (String tenantId : tenantService.getTenantIds()) {
            try {
                TenantContext.runWithTenantId(tenantId, this::reconcileCurrentTenant);
            } catch (RuntimeException runtimeException) {
                log.warn("Approval-task reconciliation failed for tenant {}", tenantId, runtimeException);
            }
        }
    }

    private void reconcileCurrentTenant() {
        for (ApprovalTask approvalTask : approvalTaskService.getUnresolvedApprovalTasks()) {
            try {
                reconcile(approvalTask);
            } catch (RuntimeException runtimeException) {
                log.warn("Could not reconcile approval task {}", approvalTask.getId(), runtimeException);
            }
        }
    }

    private void reconcile(ApprovalTask approvalTask) {
        ApprovalTask.Status resolvedStatus = resolveStatus(approvalTask);

        if (resolvedStatus == null) {
            return;
        }

        if (resolvedStatus == ApprovalTask.Status.COMPLETED) {
            approvalTask.complete();
        } else {
            approvalTask.expire();
        }

        approvalTaskService.resolve(approvalTask);

        if (log.isDebugEnabled()) {
            log.debug(
                "Marked approval task id={} as {} after reconciling its backing run", approvalTask.getId(),
                resolvedStatus);
        }
    }

    private ApprovalTask.@Nullable Status resolveStatus(ApprovalTask approvalTask) {
        String jobResumeIdString = approvalTask.getJobResumeId();

        if (jobResumeIdString == null) {
            return null;
        }

        Optional<JobResumeId> jobResumeIdOptional = approvalTokens.resolveJobResumeId(jobResumeIdString);

        if (jobResumeIdOptional.isEmpty()) {
            Instant dueDate = approvalTask.getDueDate();

            if (dueDate != null && dueDate.isBefore(Instant.now())) {
                return ApprovalTask.Status.EXPIRED;
            }

            log.warn("Approval task {} carries an invalid or expired resume token", approvalTask.getId());

            return null;
        }

        JobResumeId jobResumeId = jobResumeIdOptional.get();

        Optional<Job> jobOptional;

        try {
            jobOptional = jobService.fetchJob(jobResumeId.getJobId());
        } catch (RuntimeException runtimeException) {
            log.warn(
                "Could not fetch job {} while reconciling approval task {}; skipping this cycle",
                jobResumeId.getJobId(), approvalTask.getId(), runtimeException);

            return null;
        }

        if (jobOptional.isEmpty()) {
            return ApprovalTask.Status.EXPIRED;
        }

        Job job = jobOptional.get();

        return switch (job.getStatus()) {
            case COMPLETED -> ApprovalTask.Status.COMPLETED;
            case FAILED, CANCELLED -> ApprovalTask.Status.EXPIRED;
            case STOPPED -> resolveStoppedStatus(job, jobResumeId);
            default -> null;
        };
    }

    private static ApprovalTask.@Nullable Status resolveStoppedStatus(Job job, JobResumeId jobResumeId) {
        Object currentJobResumeId = job.getMetadata(MetadataConstants.JOB_RESUME_ID);

        if (currentJobResumeId == null) {
            return ApprovalTask.Status.EXPIRED;
        }

        if (jobResumeId.matches(currentJobResumeId.toString())) {
            return null;
        }

        return ApprovalTask.Status.COMPLETED;
    }
}
