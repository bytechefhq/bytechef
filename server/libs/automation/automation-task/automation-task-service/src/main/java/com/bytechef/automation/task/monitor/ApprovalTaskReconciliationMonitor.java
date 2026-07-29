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
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Reconciles OPEN and IN_PROGRESS {@link ApprovalTask} rows against the state of their backing run.
 * {@code ApprovalTaskCompletionListener} marks a row COMPLETED when the resume commits in the same JVM, but nothing
 * closed rows whose run expired (failed by the coordinator's approval-expiry sweep), failed outright, or was purged by
 * retention — those stayed OPEN forever with a dead form link. This periodic per-tenant sweep marks such rows
 * {@link ApprovalTask.Status#EXPIRED}, and marks rows COMPLETED when the run finished without the in-JVM resume event
 * being observed (a resume served by another node in a distributed deployment).
 *
 * <p>
 * Runs shortly after the coordinator's expiry sweep cadence so expired runs are usually already FAILED when this
 * monitor sees them. Disable with {@code bytechef.automation.approval-task.reconciliation.enabled=false}.
 * </p>
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(
    name = "bytechef.automation.approval-task.reconciliation.enabled", havingValue = "true", matchIfMissing = true)
public class ApprovalTaskReconciliationMonitor {

    private static final Logger log = LoggerFactory.getLogger(ApprovalTaskReconciliationMonitor.class);

    private final ApprovalTaskService approvalTaskService;
    private final JobService jobService;
    private final TenantService tenantService;

    @SuppressFBWarnings("EI")
    public ApprovalTaskReconciliationMonitor(
        ApprovalTaskService approvalTaskService, JobService jobService, TenantService tenantService) {

        this.approvalTaskService = approvalTaskService;
        this.jobService = jobService;
        this.tenantService = tenantService;
    }

    @Scheduled(initialDelayString = "PT7M", fixedDelayString = "PT15M")
    public void reconcileApprovalTasks() {
        for (String tenantId : tenantService.getTenantIds()) {
            try {
                TenantContext.runWithTenantId(tenantId, this::reconcileCurrentTenant);
            } catch (Exception exception) {
                log.warn(
                    "Approval-task reconciliation failed for tenant {}: {}", tenantId, exception.getMessage());
            }
        }
    }

    private void reconcileCurrentTenant() {
        for (ApprovalTask approvalTask : approvalTaskService.getUnresolvedApprovalTasks()) {
            ApprovalTask.Status resolvedStatus = resolveStatus(approvalTask);

            if (resolvedStatus == null) {
                continue;
            }

            approvalTask.setStatus(resolvedStatus);

            approvalTaskService.update(approvalTask);

            if (log.isDebugEnabled()) {
                log.debug(
                    "Marked approval task id={} as {} after reconciling its backing run", approvalTask.getId(),
                    resolvedStatus);
            }
        }
    }

    /**
     * Resolves the status an unresolved approval task should transition to, or {@code null} when the backing run is
     * still legitimately pending (or the row carries no parseable resume id).
     */
    private ApprovalTask.@Nullable Status resolveStatus(ApprovalTask approvalTask) {
        String jobResumeIdString = approvalTask.getJobResumeId();

        if (jobResumeIdString == null) {
            return null;
        }

        long jobId;

        try {
            JobResumeId jobResumeId = JobResumeId.parse(jobResumeIdString);

            jobId = jobResumeId.getJobId();
        } catch (Exception exception) {
            return null;
        }

        Optional<Job> jobOptional;

        try {
            jobOptional = jobService.fetchJob(jobId);
        } catch (Exception exception) {
            // A transient failure fetching the run (DB hiccup, or a REST/network blip in the distributed
            // deployment) is NOT proof the run is gone — skip this row and retry on the next sweep rather than
            // permanently marking a still-resumable approval EXPIRED.
            if (log.isDebugEnabled()) {
                log.debug(
                    "Could not fetch job {} while reconciling approval task {}; skipping this cycle",
                    jobId, approvalTask.getId(), exception);
            }

            return null;
        }

        if (jobOptional.isEmpty()) {
            // The run is genuinely gone (purged by retention) — the approval can never be resolved.
            return ApprovalTask.Status.EXPIRED;
        }

        return switch (jobOptional.get()
            .getStatus()) {
            // Resolved on another node — the in-JVM resume event never reached the completion listener.
            case COMPLETED -> ApprovalTask.Status.COMPLETED;
            // Failed by the approval-expiry sweep or a hard error — unresolvable either way.
            case FAILED, CANCELLED -> ApprovalTask.Status.EXPIRED;
            default -> null;
        };
    }
}
