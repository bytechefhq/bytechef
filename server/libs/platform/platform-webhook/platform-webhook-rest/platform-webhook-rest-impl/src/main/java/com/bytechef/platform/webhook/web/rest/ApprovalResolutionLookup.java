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

package com.bytechef.platform.webhook.web.rest;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tells a decided approval apart from an expired or otherwise closed one, for a resume that came back {@code GONE}.
 *
 * @author Ivica Cardic
 */
public class ApprovalResolutionLookup {

    private static final Logger log = LoggerFactory.getLogger(ApprovalResolutionLookup.class);

    private final JobService jobService;
    private final @Nullable ApprovalTokens approvalTokens;

    @SuppressFBWarnings("EI")
    public ApprovalResolutionLookup(JobService jobService, @Nullable ApprovalTokens approvalTokens) {
        this.jobService = jobService;
        this.approvalTokens = approvalTokens;
    }

    /**
     * Describes why an approval whose resume came back {@code GONE} can no longer be decided.
     */
    public Resolution describe(String resumeId) {
        JobResumeId jobResumeId = parseJobResumeId(resumeId);

        if (jobResumeId == null) {
            return Resolution.UNAVAILABLE;
        }

        try {
            Optional<Job> jobOptional = TenantContext.callWithTenantId(
                jobResumeId.getTenantId(), () -> jobService.fetchJob(jobResumeId.getJobId()));

            return jobOptional
                .map(job -> describe(job, jobResumeId))
                .orElse(Resolution.UNAVAILABLE);
        } catch (RuntimeException exception) {
            log.warn("Could not read job {} to describe a spent approval", jobResumeId.getJobId(), exception);

            return Resolution.UNAVAILABLE;
        }
    }

    private static Resolution describe(Job job, JobResumeId jobResumeId) {
        if (jobResumeId.matches((String) job.getMetadata(MetadataConstants.EXPIRED_JOB_RESUME_ID))) {
            return Resolution.EXPIRED;
        }

        if (jobResumeId.matches((String) job.getMetadata(MetadataConstants.CANCELLED_JOB_RESUME_ID))) {
            return Resolution.UNAVAILABLE;
        }

        if (jobResumeId.matches((String) job.getMetadata(MetadataConstants.CONSUMED_JOB_RESUME_ID))) {
            return Resolution.RESOLVED;
        }

        return switch (job.getStatus()) {
            case CREATED, STARTED, COMPLETED -> Resolution.RESOLVED;
            case STOPPED -> jobResumeId.matches((String) job.getMetadata(MetadataConstants.JOB_RESUME_ID))
                ? Resolution.UNAVAILABLE : Resolution.RESOLVED;
            case FAILED, CANCELLED -> Resolution.UNAVAILABLE;
        };
    }

    private @Nullable JobResumeId parseJobResumeId(String resumeId) {
        String innerToken = approvalTokens == null
            ? resumeId
            : approvalTokens.resolveInnerToken(resumeId)
                .orElse(null);

        if (innerToken == null) {
            return null;
        }

        try {
            return JobResumeId.parse(innerToken);
        } catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }

    public enum Resolution {

        EXPIRED, RESOLVED, UNAVAILABLE
    }
}
