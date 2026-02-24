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

    @SuppressFBWarnings("EI")
    public PrincipalJobFacadeImpl(
        PrincipalJobService principalJobService, JobFacade jobFacade, JobService jobService,
        WorkflowService workflowService) {

        this.principalJobService = principalJobService;
        this.jobFacade = jobFacade;
        this.jobService = jobService;
        this.workflowService = workflowService;
    }

    @Override
    public long createChildJob(long parentJobId, JobParametersDTO jobParametersDTO) {
        long childJobId = jobFacade.createJob(jobParametersDTO);

        boolean principalAssociated = false;

        for (PlatformType platformType : PlatformType.values()) {
            Optional<Long> principalId = principalJobService.fetchJobPrincipalId(parentJobId, platformType);

            if (principalId.isPresent()) {
                principalJobService.create(childJobId, principalId.get(), platformType);

                principalAssociated = true;

                break;
            }
        }

        if (!principalAssociated) {
            log.warn(
                "No principal found for parent job {} -- child job {} will have no principal association",
                parentJobId, childJobId);
        }

        return childJobId;
    }

    @Override
    // TODO @Transactional
    public long createJob(JobParametersDTO jobParametersDTO, long jobPrincipalId, PlatformType type) {
        long jobId = jobFacade.createJob(jobParametersDTO);

        principalJobService.create(jobId, jobPrincipalId, type);

        return jobId;
    }

    @Override
    @Transactional
    public Job createSyncJob(JobParametersDTO jobParametersDTO, long jobPrincipalId, PlatformType type) {
        Job job = jobService.create(jobParametersDTO, workflowService.getWorkflow(jobParametersDTO.getWorkflowId()));

        principalJobService.create(Validate.notNull(job.getId(), "id"), jobPrincipalId, type);

        return job;
    }
}
