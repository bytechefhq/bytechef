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

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.service.LicenceJobUsageService;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
public class PrincipalJobFacadeImpl implements PrincipalJobFacade {

    private static final Logger log = LoggerFactory.getLogger(PrincipalJobFacadeImpl.class);

    private final PrincipalJobService principalJobService;
    private final JobFacade jobFacade;
    private final JobService jobService;
    private final WorkflowService workflowService;
    private final LicenceJobUsageService licenceJobUsageService;

    @SuppressFBWarnings("EI")
    public PrincipalJobFacadeImpl(
        PrincipalJobService principalJobService, JobFacade jobFacade, JobService jobService,
        WorkflowService workflowService, LicenceJobUsageService licenceJobUsageService) {

        this.principalJobService = principalJobService;
        this.jobFacade = jobFacade;
        this.jobService = jobService;
        this.workflowService = workflowService;
        this.licenceJobUsageService = licenceJobUsageService;
    }

    @Override
    public long createChildJob(long parentJobId, JobParametersDTO jobParametersDTO, PlatformType platformType) {
        long childJobId = jobFacade.createJob(jobParametersDTO);

        Optional<Long> principalId = principalJobService.fetchJobPrincipalId(parentJobId, platformType);

        if (principalId.isPresent()) {
            principalJobService.create(childJobId, principalId.get(), platformType);
        } else {
            log.warn(
                "No principal found for parent job {} -- child job {} will have no principal association",
                parentJobId, childJobId);
        }

        return childJobId;
    }

    @Override
    // TODO @Transactional
    public long createJob(JobParametersDTO jobParametersDTO, long jobPrincipalId, PlatformType type) {
        licenceJobUsageService.consumeOrThrow();

        long jobId = jobFacade.createJob(jobParametersDTO);

        principalJobService.create(jobId, jobPrincipalId, type);

        return jobId;
    }

    @Override
    @Transactional
    public long createPrincipalLinkedJob(
        long referenceJobId, JobParametersDTO jobParametersDTO, PlatformType platformType) {

        // The contract of this method is "linked to the same principal instance as referenceJobId." If we cannot
        // establish the linkage, refusing to create the job is strictly safer than silently creating an orphan
        // (no tenant attribution, no billing, invisible to workspace-scoped lookups). The caller is the agent-tool
        // sub-workflow bridge; failing here surfaces as a coordinator-level error rather than a silent multi-tenant
        // leak. See review finding C3.

        long principalId = principalJobService.fetchJobPrincipalId(referenceJobId, platformType)
            .orElseThrow(() -> new IllegalStateException(
                "Cannot create principal-linked job: no principal found for reference job %d (type=%s)"
                    .formatted(referenceJobId, platformType)));

        long jobId = jobFacade.createJob(jobParametersDTO);

        principalJobService.create(jobId, principalId, platformType);

        return jobId;
    }

    @Override
    @Transactional
    public Job createJobWithoutDispatch(JobParametersDTO jobParametersDTO, long jobPrincipalId, PlatformType type) {
        licenceJobUsageService.consumeOrThrow();

        Job job = jobService.create(jobParametersDTO, workflowService.getWorkflow(jobParametersDTO.getWorkflowId()));

        principalJobService.create(Validate.notNull(job.getId(), "id"), jobPrincipalId, type);

        return job;
    }
}
