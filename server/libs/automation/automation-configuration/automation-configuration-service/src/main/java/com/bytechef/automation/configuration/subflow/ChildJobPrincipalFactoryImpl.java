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

package com.bytechef.automation.configuration.subflow;

import com.bytechef.atlas.execution.dto.JobParametersDTO;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.task.dispatcher.subflow.ChildJobPrincipalFactory;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Links a sub-workflow job to the project deployment that contains the called workflow: the caller's own deployment
 * when it contains it, otherwise the deployment of the called workflow's project in the caller's environment.
 *
 * @author Ivica Cardic
 */
@Component
class ChildJobPrincipalFactoryImpl implements ChildJobPrincipalFactory {

    private final PrincipalJobFacade principalJobFacade;
    private final PrincipalJobService principalJobService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectDeploymentWorkflowService projectDeploymentWorkflowService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;

    ChildJobPrincipalFactoryImpl(
        PrincipalJobFacade principalJobFacade, PrincipalJobService principalJobService,
        ProjectDeploymentService projectDeploymentService,
        ProjectDeploymentWorkflowService projectDeploymentWorkflowService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService) {

        this.principalJobFacade = principalJobFacade;
        this.principalJobService = principalJobService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectDeploymentWorkflowService = projectDeploymentWorkflowService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
    }

    @Override
    public long createChildJob(long parentJobId, JobParametersDTO jobParametersDTO) {
        Optional<Long> parentProjectDeploymentIdOptional = principalJobService.fetchJobPrincipalId(
            parentJobId, PlatformType.AUTOMATION);

        if (parentProjectDeploymentIdOptional.isEmpty()) {
            return principalJobFacade.createChildJob(parentJobId, jobParametersDTO, PlatformType.AUTOMATION);
        }

        long projectDeploymentId = resolveProjectDeploymentId(
            parentProjectDeploymentIdOptional.get(), jobParametersDTO.getWorkflowId());

        return principalJobFacade.createJob(jobParametersDTO, projectDeploymentId, PlatformType.AUTOMATION);
    }

    @Override
    public long createPrincipalLinkedJob(long referenceJobId, JobParametersDTO jobParametersDTO) {
        long referenceProjectDeploymentId = principalJobService.fetchJobPrincipalId(
            referenceJobId, PlatformType.AUTOMATION)
            .orElseThrow(() -> new IllegalStateException(
                "Cannot create principal-linked job: no principal found for reference job %d".formatted(
                    referenceJobId)));

        long projectDeploymentId = resolveProjectDeploymentId(
            referenceProjectDeploymentId, jobParametersDTO.getWorkflowId());

        return principalJobFacade.createPrincipalLinkedJob(
            jobParametersDTO, projectDeploymentId, PlatformType.AUTOMATION);
    }

    private long resolveProjectDeploymentId(long callerProjectDeploymentId, String workflowId) {
        if (projectDeploymentWorkflowService.fetchProjectDeploymentWorkflow(callerProjectDeploymentId, workflowId)
            .isPresent()) {

            return callerProjectDeploymentId;
        }

        ProjectDeployment callerProjectDeployment = projectDeploymentService.getProjectDeployment(
            callerProjectDeploymentId);
        ProjectWorkflow projectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(workflowId);

        Project callerProject = projectService.getProject(
            Objects.requireNonNull(callerProjectDeployment.getProjectId()));
        Project project = projectService.getProject(projectWorkflow.getProjectId());

        if (!Objects.equals(callerProject.getWorkspaceId(), project.getWorkspaceId())) {
            throw new IllegalStateException(
                "Workflow '%s' belongs to project '%s' in another workspace and cannot be called from project '%s'"
                    .formatted(workflowId, project.getName(), callerProject.getName()));
        }

        Environment environment = callerProjectDeployment.getEnvironment();

        return projectDeploymentService.fetchProjectDeployment(projectWorkflow.getProjectId(), environment)
            .filter(ProjectDeployment::isEnabled)
            .filter(projectDeployment -> projectDeploymentWorkflowService
                .fetchProjectDeploymentWorkflow(Objects.requireNonNull(projectDeployment.getId()), workflowId)
                .isPresent())
            .map(ProjectDeployment::getId)
            .orElseThrow(() -> new IllegalStateException(
                ("Workflow '%s' of project '%s' cannot be called: the project has no enabled deployment of this " +
                    "workflow in the %s environment").formatted(workflowId, project.getName(), environment)));
    }
}
