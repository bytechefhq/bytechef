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
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.event.JobResumedEvent;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongConsumer;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Ivica Cardic
 */
@Service
public class JobResumeFacadeImpl implements JobResumeFacade {

    private static final Logger log = LoggerFactory.getLogger(JobResumeFacadeImpl.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectProvider<ApprovalTokens> approvalTokensProvider;
    private final JobFacade jobFacade;
    private final JobService jobService;
    private final TransactionOperations transactionOperations;

    @Autowired
    @SuppressFBWarnings("EI")
    public JobResumeFacadeImpl(
        ApplicationEventPublisher applicationEventPublisher, ObjectProvider<ApprovalTokens> approvalTokensProvider,
        JobFacade jobFacade, JobService jobService, PlatformTransactionManager platformTransactionManager) {

        this(
            applicationEventPublisher, approvalTokensProvider, jobFacade, jobService,
            new TransactionTemplate(platformTransactionManager));
    }

    @SuppressFBWarnings("EI")
    public JobResumeFacadeImpl(
        ApplicationEventPublisher applicationEventPublisher, ObjectProvider<ApprovalTokens> approvalTokensProvider,
        JobFacade jobFacade, JobService jobService, TransactionOperations transactionOperations) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.approvalTokensProvider = approvalTokensProvider;
        this.jobFacade = jobFacade;
        this.jobService = jobService;
        this.transactionOperations = transactionOperations;
    }

    @Override
    public JobResumeOutcome resumeExpiredJob(String id) {
        JobResumeId jobResumeId = parseJobResumeId(id);

        if (jobResumeId == null) {
            return JobResumeOutcome.INVALID_ID;
        }

        return callInTenantTransaction(jobResumeId, transactionStatus -> {
            Job job = jobService.getJob(jobResumeId.getJobId());

            JobResumeOutcome rejectedOutcome = getRejectedOutcome(job, jobResumeId);

            if (rejectedOutcome == JobResumeOutcome.NOT_YET_SUSPENDED) {
                return rejectedOutcome;
            }

            if (rejectedOutcome != null) {
                log.info(
                    "Ignoring the expired suspend of job {}; the job no longer waits on it ({})",
                    jobResumeId.getJobId(), rejectedOutcome);

                return rejectedOutcome;
            }

            consumeJobResumeId(job);

            Long taskExecutionResumeId = MapUtils.getLong(
                job.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID);

            if (taskExecutionResumeId == null) {
                jobFacade.resumeJob(jobResumeId.getJobId());
            } else {
                jobFacade.resumeJob(jobResumeId.getJobId(), taskExecutionResumeId, null);
            }

            applicationEventPublisher.publishEvent(new JobResumedEvent(id));

            return JobResumeOutcome.OK;
        });
    }

    @Override
    public JobResumeOutcome resumeJob(String id, Map<String, Object> data) {
        return resumeJob(id, data, false, jobId -> {});
    }

    @Override
    public JobResumeOutcome resumeJobStreaming(
        String id, Map<String, Object> data, LongConsumer jobIdConsumer) {

        return resumeJob(id, data, true, jobIdConsumer);
    }

    @SuppressFBWarnings("CRLF_INJECTION_LOGS")
    private JobResumeOutcome resumeJob(
        String id, Map<String, Object> data, boolean streaming, LongConsumer jobIdConsumer) {

        Optional<String> innerTokenOptional = resolveInnerToken(id);

        if (innerTokenOptional.isEmpty()) {
            log.warn("Invalid resume token: {}", id.replaceAll("[\\r\\n]", ""));

            return JobResumeOutcome.INVALID_ID;
        }

        JobResumeId jobResumeId = parseJobResumeId(innerTokenOptional.get());

        if (jobResumeId == null) {
            return JobResumeOutcome.INVALID_ID;
        }

        return callInTenantTransaction(jobResumeId, transactionStatus -> {
            Job job = jobService.getJob(jobResumeId.getJobId());

            JobResumeOutcome rejectedOutcome = getRejectedOutcome(job, jobResumeId);

            if (rejectedOutcome != null) {
                return rejectedOutcome;
            }

            if (streaming && !MapUtils.getBoolean(job.getMetadata(), MetadataConstants.STREAMING_RESUME, false)) {
                log.warn("Job {} was not suspended for a streaming resume", jobResumeId.getJobId());

                return JobResumeOutcome.STREAMING_NOT_ALLOWED;
            }

            consumeJobResumeId(job);

            jobIdConsumer.accept(jobResumeId.getJobId());

            jobFacade.resumeJob(
                jobResumeId.getJobId(), MapUtils.getLong(job.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID),
                data);

            applicationEventPublisher.publishEvent(new JobResumedEvent(id));

            return JobResumeOutcome.OK;
        });
    }

    private <T> T callInTenantTransaction(JobResumeId jobResumeId, TransactionCallback<T> transactionCallback) {
        return TenantContext.callWithTenantId(
            jobResumeId.getTenantId(),
            () -> Objects.requireNonNull(transactionOperations.execute(transactionCallback)));
    }

    private Optional<String> resolveInnerToken(String id) {
        ApprovalTokens approvalTokens = approvalTokensProvider.getIfAvailable();

        if (approvalTokens == null) {
            return Optional.of(id);
        }

        return approvalTokens.resolveInnerToken(id);
    }

    private void consumeJobResumeId(Job job) {
        Map<String, Object> jobMetadata = new HashMap<>(job.getMetadata());

        jobMetadata.put(MetadataConstants.CONSUMED_JOB_RESUME_ID, job.getMetadata(MetadataConstants.JOB_RESUME_ID));

        job.setMetadata(jobMetadata);

        jobService.update(job);
    }

    private static @Nullable JobResumeOutcome getRejectedOutcome(Job job, JobResumeId jobResumeId) {
        String consumedJobResumeIdString = (String) job.getMetadata(MetadataConstants.CONSUMED_JOB_RESUME_ID);

        if (jobResumeId.matches(consumedJobResumeIdString)) {
            log.debug("Job {} was already resumed with this resume id", jobResumeId.getJobId());

            return JobResumeOutcome.GONE;
        }

        Job.Status status = job.getStatus();

        if (status == Job.Status.CREATED || status == Job.Status.STARTED) {
            log.debug("Job {} has not been suspended yet; status is {}", jobResumeId.getJobId(), status);

            return JobResumeOutcome.NOT_YET_SUSPENDED;
        }

        if (status == Job.Status.FAILED) {
            log.warn("Cannot resume job {}; it failed", jobResumeId.getJobId());

            return JobResumeOutcome.JOB_FAILED;
        }

        if (status != Job.Status.STOPPED) {
            log.warn("Cannot resume job {}; status is {}", jobResumeId.getJobId(), status);

            return JobResumeOutcome.GONE;
        }

        String storedJobResumeIdString = (String) job.getMetadata(MetadataConstants.JOB_RESUME_ID);

        if (!jobResumeId.matches(storedJobResumeIdString)) {
            log.warn("Resume token does not match stored value for job {}", jobResumeId.getJobId());

            return JobResumeOutcome.INVALID_ID;
        }

        return null;
    }

    @SuppressFBWarnings("CRLF_INJECTION_LOGS")
    private static @Nullable JobResumeId parseJobResumeId(String id) {
        try {
            return JobResumeId.parse(id);
        } catch (IllegalArgumentException illegalArgumentException) {
            log.warn("Invalid resume id: {}", id.replaceAll("[\\r\\n]", ""));

            return null;
        }
    }
}
