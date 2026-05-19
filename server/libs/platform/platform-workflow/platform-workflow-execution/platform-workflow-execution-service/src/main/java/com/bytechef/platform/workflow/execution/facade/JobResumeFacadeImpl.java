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
import com.bytechef.platform.component.constant.MetadataConstants;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.event.JobResumedEvent;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
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
    @SuppressFBWarnings("CRLF_INJECTION_LOGS")
    public JobResumeOutcome resumeJob(String id, Map<String, Object> data) {
        JobResumeId jobResumeId;

        try {
            jobResumeId = JobResumeId.parse(id);
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

            jobFacade.resumeJob(jobResumeId.getJobId(), data);

            applicationEventPublisher.publishEvent(new JobResumedEvent(id));

            return JobResumeOutcome.OK;
        });
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
