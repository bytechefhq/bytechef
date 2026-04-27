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

package com.bytechef.automation.task.facade;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.service.ApprovalTaskService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ApprovalTaskFacadeImpl implements ApprovalTaskFacade {

    private final ApprovalTaskService approvalTaskService;
    private final PrincipalJobService principalJobService;
    private final ProjectDeploymentService projectDeploymentService;

    @SuppressFBWarnings("EI")
    public ApprovalTaskFacadeImpl(
        ApprovalTaskService approvalTaskService, PrincipalJobService principalJobService,
        ProjectDeploymentService projectDeploymentService) {

        this.approvalTaskService = approvalTaskService;
        this.principalJobService = principalJobService;
        this.projectDeploymentService = projectDeploymentService;
    }

    @Override
    public ApprovalTask createApprovalTask(ApprovalTask approvalTask) {
        approvalTask.setEnvironment(getEnvironment(approvalTask.getJobResumeId()));

        return approvalTaskService.create(approvalTask);
    }

    private Environment getEnvironment(String jobResumeIdString) {
        Assert.notNull(jobResumeIdString, "'jobResumeId' must not be null");

        JobResumeId jobResumeId = JobResumeId.parse(jobResumeIdString);

        long projectDeploymentId = principalJobService.getJobPrincipalId(
            jobResumeId.getJobId(), PlatformType.AUTOMATION);

        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(projectDeploymentId);

        return projectDeployment.getEnvironment();
    }
}
