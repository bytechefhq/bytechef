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

package com.bytechef.platform.workflow.execution.facade;

import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.domain.TaskExecution;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.component.definition.SuspendUtils;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.event.JobResumedEvent;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class JobResumeFacadeImpl implements JobResumeFacade {

    private static final Logger log = LoggerFactory.getLogger(JobResumeFacadeImpl.class);

    private static final String APPROVAL_RESOLUTION_METRIC_NAME = "bytechef_approval_resolution";

    // Reserved key in the approval outcome (see the approval component's output schema); present on approval
    // resumes, absent on ask-user-question resumes — only approval resolutions are counted.
    private static final String APPROVED = "approved";

    // Reserved, server-controlled key carrying the verified resolver identity into the approval outcome. Stripped
    // from inbound payloads so an anonymous form submission cannot spoof an identity; only a caller-supplied,
    // out-of-band-verified value is written back.
    private static final String APPROVED_BY = "approvedBy";

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ApprovalTokens approvalTokens;
    private final JobFacade jobFacade;
    private final JobService jobService;
    private final ObjectProvider<MeterRegistry> meterRegistryObjectProvider;
    private final TaskExecutionService taskExecutionService;

    @SuppressFBWarnings("EI")
    public JobResumeFacadeImpl(
        ApplicationEventPublisher applicationEventPublisher, ApprovalTokens approvalTokens, JobFacade jobFacade,
        JobService jobService, ObjectProvider<MeterRegistry> meterRegistryObjectProvider,
        TaskExecutionService taskExecutionService) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.approvalTokens = approvalTokens;
        this.jobFacade = jobFacade;
        this.jobService = jobService;
        this.meterRegistryObjectProvider = meterRegistryObjectProvider;
        this.taskExecutionService = taskExecutionService;
    }

    @Override
    public JobResumeOutcome resumeJob(String id, Map<String, Object> data) {
        return doResume(id, data, null, jobId -> {});
    }

    @Override
    public JobResumeOutcome resumeJob(String id, Map<String, Object> data, String approvedBy) {
        return doResume(id, data, approvedBy, jobId -> {});
    }

    @Override
    public JobResumeOutcome resumeJobStreaming(String id, Map<String, Object> data, LongConsumer jobIdConsumer) {
        return doResume(id, data, null, jobIdConsumer);
    }

    @SuppressFBWarnings("CRLF_INJECTION_LOGS")
    private JobResumeOutcome doResume(
        String id, Map<String, Object> data, String approvedBy, LongConsumer jobIdConsumer) {

        Map<String, Object> resumeData = normalizeApprovedBy(data, approvedBy);

        String innerToken = approvalTokens.resolveInnerToken(id)
            .orElse(null);

        if (innerToken == null) {
            log.warn("Rejected resume token: {}", id.replaceAll("[\\r\\n]", ""));

            return JobResumeOutcome.INVALID_ID;
        }

        JobResumeId jobResumeId;

        try {
            jobResumeId = JobResumeId.parse(innerToken);
        } catch (IllegalArgumentException illegalArgumentException) {
            log.warn("Invalid resume id: {}", id.replaceAll("[\\r\\n]", ""));

            return JobResumeOutcome.INVALID_ID;
        }

        return TenantContext.callWithTenantId(jobResumeId.getTenantId(), () -> {
            Job job = jobService.getJob(jobResumeId.getJobId());

            if (job.getStatus() != Job.Status.STOPPED) {
                log.warn("Cannot resume job {}; status is {}", jobResumeId.getJobId(), job.getStatus());

                return JobResumeOutcome.GONE;
            }

            String storedJobResumeIdString = (String) job.getMetadata(MetadataConstants.JOB_RESUME_ID);

            if (storedJobResumeIdString == null || !tokenMatches(storedJobResumeIdString, jobResumeId)) {
                log.warn("Resume token does not match stored value for job {}", jobResumeId.getJobId());

                return JobResumeOutcome.INVALID_ID;
            }

            if (isSuspendExpired(job)) {
                log.warn("Rejected resume for job {}: the pending approval expired", jobResumeId.getJobId());

                incrementApprovalExpiredCounter("resume");

                return JobResumeOutcome.GONE;
            }

            // Atomic single-winner claim. The STOPPED check above is advisory: two concurrent resolutions of the same
            // run (e.g. a chat-card Approve and an email-form Reject) both pass it, and the actual resume conflict is
            // only settled later by the coordinator's optimistic lock — telling the loser OK and double-counting the
            // resolution. Claim the run here by transitioning STOPPED -> STARTED under the job's @Version: only the
            // first caller commits; the loser is told GONE. The same version guard rejects resurrecting a run the
            // expiry sweep concurrently flipped to FAILED (that update bumps the version too), so a resume can never
            // flip an already-terminal run back to STARTED. tryClaimResume swallows the optimistic-lock failure
            // internally so this @Transactional method is not marked rollback-only when the claim is lost.
            if (!jobService.tryClaimResume(job)) {
                log.warn(
                    "Lost the resume claim for job {} to a concurrent resolution or the expiry sweep",
                    jobResumeId.getJobId());

                return JobResumeOutcome.GONE;
            }

            jobIdConsumer.accept(jobResumeId.getJobId());

            jobFacade.resumeJob(
                jobResumeId.getJobId(), MapUtils.getLong(job.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID),
                resumeData);

            applicationEventPublisher.publishEvent(new JobResumedEvent(innerToken));

            incrementApprovalResolutionCounter(resumeData);

            return JobResumeOutcome.OK;
        });
    }

    /**
     * Produces the resume payload actually dispatched to the job: a mutable copy of {@code data} with the reserved
     * {@code approvedBy} key removed (so an anonymous form submission cannot spoof an identity) and re-added only when
     * a non-blank, server-verified {@code approvedBy} was supplied by the caller.
     */
    private static Map<String, Object> normalizeApprovedBy(Map<String, Object> data, String approvedBy) {
        Map<String, Object> resumeData = data == null ? new HashMap<>() : new HashMap<>(data);

        resumeData.remove(APPROVED_BY);

        if (approvedBy != null && !approvedBy.isBlank()) {
            resumeData.put(APPROVED_BY, approvedBy);
        }

        return resumeData;
    }

    private void incrementApprovalResolutionCounter(Map<String, Object> data) {
        if (!data.containsKey(APPROVED)) {
            return;
        }

        MeterRegistry meterRegistry = meterRegistryObjectProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        meterRegistry.counter(APPROVAL_RESOLUTION_METRIC_NAME, APPROVED, String.valueOf(data.get(APPROVED)))
            .increment();
    }

    /**
     * Whether the job's pending suspend has expired. The suspend (with its {@code expiresAt}) lives on the suspended
     * task execution's metadata; the job metadata points at that task via {@code taskExecutionResumeId}. Fail-open: a
     * missing pointer, unloadable task, or absent expiry means "not expired" — expiry is an added guard, never a reason
     * to break a legacy resume.
     */
    private boolean isSuspendExpired(Job job) {
        Long taskExecutionResumeId = MapUtils.getLong(job.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID);

        if (taskExecutionResumeId == null) {
            return false;
        }

        try {
            TaskExecution taskExecution = taskExecutionService.getTaskExecution(taskExecutionResumeId);

            Instant expiresAt = SuspendUtils.extractSuspendExpiresAt(taskExecution.getMetadata());

            return expiresAt != null && expiresAt.isBefore(Instant.now());
        } catch (Exception exception) {
            return false;
        }
    }

    private void incrementApprovalExpiredCounter(String source) {
        MeterRegistry meterRegistry = meterRegistryObjectProvider.getIfAvailable();

        if (meterRegistry == null) {
            return;
        }

        meterRegistry.counter("bytechef_approval_expired", "source", source)
            .increment();
    }

    private static boolean tokenMatches(String storedJobResumeIdString, JobResumeId suppliedJobResumeId) {
        JobResumeId storedJobResumeId;

        try {
            storedJobResumeId = JobResumeId.parse(storedJobResumeIdString);
        } catch (IllegalArgumentException illegalArgumentException) {
            return false;
        }

        if (storedJobResumeId.getJobId() != suppliedJobResumeId.getJobId()) {
            return false;
        }

        if (!storedJobResumeId.getTenantId()
            .equals(suppliedJobResumeId.getTenantId())) {

            return false;
        }

        byte[] storedUuidBytes = storedJobResumeId.getUuidAsString()
            .getBytes(StandardCharsets.UTF_8);
        byte[] suppliedUuidBytes = suppliedJobResumeId.getUuidAsString()
            .getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(storedUuidBytes, suppliedUuidBytes);
    }
}
