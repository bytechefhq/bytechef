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
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final ApplicationEventPublisher applicationEventPublisher;
    private final JobFacade jobFacade;
    private final JobService jobService;

    @SuppressFBWarnings("EI")
    public JobResumeFacadeImpl(
        ApplicationEventPublisher applicationEventPublisher, JobFacade jobFacade, JobService jobService) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.jobFacade = jobFacade;
        this.jobService = jobService;
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

        JobResumeId jobResumeId;

        try {
            jobResumeId = JobResumeId.parse(id);
        } catch (IllegalArgumentException illegalArgumentException) {
            log.warn("Invalid resume id: {}", id.replaceAll("[\\r\\n]", ""));

            return JobResumeOutcome.INVALID_ID;
        }

        return TenantContext.callWithTenantId(jobResumeId.getTenantId(), () -> {
            Job job = jobService.getJob(jobResumeId.getJobId());

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

            if (streaming && !MapUtils.getBoolean(job.getMetadata(), MetadataConstants.STREAMING_RESUME, false)) {
                log.warn("Job {} was not suspended for a streaming resume", jobResumeId.getJobId());

                return JobResumeOutcome.STREAMING_NOT_ALLOWED;
            }

            Map<String, Object> jobMetadata = new HashMap<>(job.getMetadata());

            jobMetadata.put(MetadataConstants.CONSUMED_JOB_RESUME_ID, storedJobResumeIdString);

            job.setMetadata(jobMetadata);

            jobService.update(job);

            jobIdConsumer.accept(jobResumeId.getJobId());

            jobFacade.resumeJob(
                jobResumeId.getJobId(), MapUtils.getLong(job.getMetadata(), MetadataConstants.TASK_EXECUTION_RESUME_ID),
                data);

            applicationEventPublisher.publishEvent(new JobResumedEvent(id));

            return JobResumeOutcome.OK;
        });
    }
}
