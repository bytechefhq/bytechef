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

package com.bytechef.automation.task.security;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.task.domain.ApprovalTask;
import com.bytechef.automation.task.repository.ApprovalTaskRepository;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.JobResumeId;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import java.util.OptionalLong;
import org.springframework.stereotype.Component;

/**
 * Maps an approval task id to the workspace of the project whose deployment ran the job the task was raised for
 * (approval task &rarr; job resume id &rarr; job &rarr; principal {@link ProjectDeployment} &rarr; {@link Project}),
 * and to its assignee as the owning user. A task raised for no job, or whose job cannot be traced to a project, has no
 * workspace.
 *
 * @author Ivica Cardic
 */
@Component
public class ApprovalTaskOwnershipResolver implements ResourceOwnershipResolver {

    private final ApprovalTaskRepository approvalTaskRepository;
    private final PrincipalJobService principalJobService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    public ApprovalTaskOwnershipResolver(
        ApprovalTaskRepository approvalTaskRepository, PrincipalJobService principalJobService,
        ProjectDeploymentService projectDeploymentService, ProjectService projectService) {

        this.approvalTaskRepository = approvalTaskRepository;
        this.principalJobService = principalJobService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
    }

    @Override
    public String resourceType() {
        return "ApprovalTask";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        Optional<ApprovalTask> approvalTaskOptional = approvalTaskRepository.findById(id);

        if (approvalTaskOptional.isEmpty()) {
            return ResourceOwner.unknown();
        }

        ApprovalTask approvalTask = approvalTaskOptional.get();

        OptionalLong workspaceId = fetchJobId(approvalTask.getJobResumeId())
            .flatMap(jobId -> principalJobService.fetchJobPrincipalId(jobId, PlatformType.AUTOMATION))
            .flatMap(projectDeploymentService::fetchProjectDeployment)
            .map(ProjectDeployment::getProjectId)
            .flatMap(projectService::fetchProject)
            .map(Project::getWorkspaceId)
            .map(OptionalLong::of)
            .orElseGet(OptionalLong::empty);

        Long assigneeId = approvalTask.getAssigneeId();

        OptionalLong ownerUserId = assigneeId == null ? OptionalLong.empty() : OptionalLong.of(assigneeId);

        return ResourceOwner.of(workspaceId, ownerUserId);
    }

    private static Optional<Long> fetchJobId(String jobResumeId) {
        if (jobResumeId == null) {
            return Optional.empty();
        }

        try {
            JobResumeId parsedJobResumeId = JobResumeId.parse(jobResumeId);

            return Optional.of(parsedJobResumeId.getJobId());
        } catch (IllegalArgumentException illegalArgumentException) {
            return Optional.empty();
        }
    }
}
