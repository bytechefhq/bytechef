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

package com.bytechef.platform.coordinator.event.listener;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ErrorWorkflowDispatch;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public class ErrorWorkflowResolver {

    private static final String ERROR_TRIGGER_COMPONENT_NAME = "workflow";
    private static final String ERROR_TRIGGER_OPERATION_NAME = "newWorkflowError";

    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI2")
    public ErrorWorkflowResolver(
        ProjectDeploymentService projectDeploymentService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService, WorkflowService workflowService) {

        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
    }

    public Optional<ErrorWorkflowDispatch> resolve(long jobPrincipalId, String failedWorkflowId) {
        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(jobPrincipalId);

        long projectId = projectDeployment.getProjectId();

        ProjectWorkflow failingProjectWorkflow = projectWorkflowService.getWorkflowProjectWorkflow(failedWorkflowId);

        ProjectWorkflow configuredProjectWorkflow = getConfiguredProjectWorkflow(projectId, failingProjectWorkflow);

        if (configuredProjectWorkflow.isErrorWorkflowDisabled()) {
            return Optional.empty();
        }

        Long targetId = configuredProjectWorkflow.getErrorProjectWorkflowId();

        if (targetId == null) {
            Project project = projectService.getProject(projectId);

            targetId = project.getErrorProjectWorkflowId();
        }

        if (targetId == null || targetId.equals(configuredProjectWorkflow.getId()) ||
            targetId.equals(failingProjectWorkflow.getId())) {

            return Optional.empty();
        }

        ProjectWorkflow target = projectWorkflowService.getProjectWorkflow(targetId);

        String targetUuid = target.getUuidAsString();

        if (targetUuid != null && targetUuid.equals(failingProjectWorkflow.getUuidAsString())) {
            return Optional.empty();
        }

        String handlerWorkflowId = getHandlerWorkflowId(jobPrincipalId, target);

        Workflow handlerWorkflow = workflowService.getWorkflow(handlerWorkflowId);

        Optional<String> errorTriggerName = WorkflowTrigger.of(handlerWorkflow)
            .stream()
            .filter(ErrorWorkflowResolver::isErrorTrigger)
            .map(WorkflowTrigger::getName)
            .findFirst();

        if (errorTriggerName.isEmpty()) {
            return Optional.empty();
        }

        Workflow failedWorkflow = workflowService.getWorkflow(failedWorkflowId);

        Environment environment = projectDeployment.getEnvironment();

        return Optional.of(
            new ErrorWorkflowDispatch(
                handlerWorkflowId, projectId, failingProjectWorkflow.getId(), failedWorkflowId,
                failedWorkflow.getLabel(), environment == null ? null : environment.name(),
                errorTriggerName.get()));
    }

    private ProjectWorkflow getConfiguredProjectWorkflow(long projectId, ProjectWorkflow failingProjectWorkflow) {
        String uuid = failingProjectWorkflow.getUuidAsString();

        if (uuid == null) {
            return failingProjectWorkflow;
        }

        return projectWorkflowService.getLastProjectWorkflow(projectId, uuid);
    }

    private String getHandlerWorkflowId(long projectDeploymentId, ProjectWorkflow target) {
        String uuid = target.getUuidAsString();

        if (uuid == null) {
            return target.getWorkflowId();
        }

        return projectWorkflowService.fetchProjectWorkflowWorkflowId(projectDeploymentId, uuid)
            .orElse(target.getWorkflowId());
    }

    private static boolean isErrorTrigger(WorkflowTrigger trigger) {
        WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(trigger.getType());

        return Objects.equals(workflowNodeType.name(), ERROR_TRIGGER_COMPONENT_NAME) &&
            Objects.equals(workflowNodeType.operation(), ERROR_TRIGGER_OPERATION_NAME);
    }
}
