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
import com.bytechef.automation.task.security.ApprovalTaskJobResolver;
import com.bytechef.automation.task.service.ApprovalTaskService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ApprovalTaskFacadeImpl implements ApprovalTaskFacade {

    private final ApprovalTaskJobResolver approvalTaskJobResolver;
    private final ApprovalTaskService approvalTaskService;
    private final PrincipalJobService principalJobService;
    private final ProjectDeploymentService projectDeploymentService;

    @SuppressFBWarnings("EI")
    public ApprovalTaskFacadeImpl(
        ApprovalTaskJobResolver approvalTaskJobResolver, ApprovalTaskService approvalTaskService,
        PrincipalJobService principalJobService, ProjectDeploymentService projectDeploymentService) {

        this.approvalTaskJobResolver = approvalTaskJobResolver;
        this.approvalTaskService = approvalTaskService;
        this.principalJobService = principalJobService;
        this.projectDeploymentService = projectDeploymentService;
    }

    @Override
    @PreAuthorize("isTenantAdmin() or " +
        "hasPermission(@approvalTaskJobResolver.getJobId(#approvalTask.jobResumeId), 'Job', 'DEPLOYMENT_EDIT')")
    public ApprovalTask createApprovalTask(ApprovalTask approvalTask) {
        if (approvalTask.getJobResumeId() != null) {
            approvalTask.setEnvironment(getEnvironment(approvalTask.getJobResumeId()));
        }

        return approvalTaskService.create(approvalTask);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'ApprovalTask', 'DEPLOYMENT_EDIT')")
    public void deleteApprovalTask(long id) {
        approvalTaskService.delete(id);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'ApprovalTask', 'DEPLOYMENT_VIEW') or isResourceOwner(#id, 'ApprovalTask')")
    @Transactional(readOnly = true)
    public ApprovalTask getApprovalTask(long id) {
        return approvalTaskService.getApprovalTask(id);
    }

    @Override
    @PostFilter("isTenantAdmin() or (filterObject.assigneeId != null and isCurrentUser(filterObject.assigneeId))")
    @Transactional(readOnly = true)
    public List<ApprovalTask> getApprovalTasks(@Nullable Integer environmentId) {
        return approvalTaskService.getApprovalTasks(environmentId);
    }

    @Override
    @PostFilter("hasPermission(filterObject.id, 'ApprovalTask', 'DEPLOYMENT_VIEW') or " +
        "isResourceOwner(filterObject.id, 'ApprovalTask')")
    @Transactional(readOnly = true)
    public List<ApprovalTask> getApprovalTasks(List<Long> ids) {
        return approvalTaskService.getApprovalTasks(ids);
    }

    @Override
    @PreAuthorize("hasPermission(#approvalTask.id, 'ApprovalTask', 'DEPLOYMENT_EDIT') or " +
        "isResourceOwner(#approvalTask.id, 'ApprovalTask')")
    public ApprovalTask updateApprovalTask(ApprovalTask approvalTask) {
        return approvalTaskService.update(approvalTask);
    }

    private Environment getEnvironment(String jobResumeIdString) {
        JobResumeId jobResumeId = approvalTaskJobResolver.fetchJobResumeId(jobResumeIdString)
            .orElseThrow(() -> new IllegalArgumentException("Invalid or expired job resume id"));

        long projectDeploymentId = principalJobService.getJobPrincipalId(
            jobResumeId.getJobId(), PlatformType.AUTOMATION);

        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(projectDeploymentId);

        return projectDeployment.getEnvironment();
    }
}
