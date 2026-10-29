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

package com.bytechef.platform.workflow.execution;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.error.ExecutionError;
import com.bytechef.platform.component.constant.MetadataConstants;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public final class JobSuspends {

    private JobSuspends() {
    }

    /**
     * Tells whether a completed task execution asked to suspend its job.
     */
    public static boolean isSuspending(TaskExecution taskExecution) {
        Map<String, ?> metadata = taskExecution.getMetadata();

        return metadata.containsKey(MetadataConstants.JOB_RESUME_ID);
    }

    /**
     * Tells whether a job is stopped on a suspend that waits for an approval decision.
     */
    public static boolean isAwaitingApproval(Job job) {
        return job.getStatus() == Job.Status.STOPPED && job.getMetadata(MetadataConstants.JOB_RESUME_ID) != null &&
            isSuspendedForApproval(job);
    }

    /**
     * Tells whether the job's latest suspend waits for an approval decision, whatever the job's status is now.
     */
    public static boolean isSuspendedForApproval(Job job) {
        return Boolean.TRUE.equals(job.getMetadata(MetadataConstants.APPROVAL_RESUME));
    }

    /**
     * Tells whether a job is suspended because an agent tool asked for a sub-workflow run and has not restarted yet,
     * including while a claimed resume is still on its way. The sub-workflow bridge resumes such a job, so callers
     * waiting for its result keep waiting instead of treating the suspend as the end of the run.
     */
    public static boolean isAwaitingSubflow(Job job) {
        return job.getStatus() == Job.Status.STOPPED && job.getMetadata(MetadataConstants.JOB_RESUME_ID) != null &&
            isSuspendedForSubflow(job);
    }

    /**
     * Tells whether the job's latest suspend was requested by an agent tool to run a sub-workflow, whatever the job's
     * status is now, so a suspend event handled after the job already resumed is still recognized.
     */
    public static boolean isSuspendedForSubflow(Job job) {
        return Boolean.TRUE.equals(job.getMetadata(MetadataConstants.SUBFLOW_RESUME));
    }

    /**
     * The error a run gets when its pending approval expires.
     */
    public static String approvalExpiredReason(Instant expiresAt) {
        return "The pending approval expired at %s without being resolved".formatted(expiresAt);
    }

    /**
     * Fails a job whose pending approval expired: spends its resume id and records that it expired, so a later resume
     * of that approval is answered as gone and an interactivity reply can say it expired.
     */
    public static void failExpiredApproval(Job job, Instant expiresAt, Instant now) {
        String reason = approvalExpiredReason(expiresAt);

        closeApproval(job, MetadataConstants.EXPIRED_JOB_RESUME_ID);

        job.setEndDate(now);
        job.setError(new ExecutionError(reason, List.of()));
        job.setStatus(Job.Status.FAILED);
    }

    /**
     * Spends the job's resume id and records that the approval was cancelled rather than decided.
     */
    public static void closeCancelledApproval(Job job) {
        closeApproval(job, MetadataConstants.CANCELLED_JOB_RESUME_ID);
    }

    /**
     * Tells whether the job's current approval was closed by its expiry.
     */
    public static boolean isApprovalExpired(Job job) {
        Object jobResumeId = job.getMetadata(MetadataConstants.JOB_RESUME_ID);

        return jobResumeId != null &&
            Objects.equals(jobResumeId, job.getMetadata(MetadataConstants.EXPIRED_JOB_RESUME_ID));
    }

    private static void closeApproval(Job job, String closureMetadataKey) {
        Map<String, Object> jobMetadata = new HashMap<>(job.getMetadata());

        Object jobResumeId = job.getMetadata(MetadataConstants.JOB_RESUME_ID);

        jobMetadata.put(MetadataConstants.CONSUMED_JOB_RESUME_ID, jobResumeId);
        jobMetadata.put(closureMetadataKey, jobResumeId);

        Object taskExecutionResumeId = job.getMetadata(MetadataConstants.TASK_EXECUTION_RESUME_ID);

        if (taskExecutionResumeId != null) {
            jobMetadata.put(MetadataConstants.LAST_RESUMED_TASK_EXECUTION_ID, taskExecutionResumeId);
        }

        job.setMetadata(jobMetadata);
    }
}
