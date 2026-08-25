/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.variable;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.platform.variable.domain.VariableScope;
import com.bytechef.ee.platform.variable.provider.VariableScopeProvider;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Maps automation job principals (project deployments) and project workflows to their workspace's variable scope. A
 * project without a workspace yields no scope, hence no variables. Embedded's automation-bridge catalog projects carry
 * {@code Workspace.DEFAULT_WORKSPACE_ID} and therefore read the default workspace's variables.
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
public class ProjectVariableScopeProvider implements VariableScopeProvider {

    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;
    private final ProjectWorkflowService projectWorkflowService;

    @SuppressFBWarnings("EI")
    public ProjectVariableScopeProvider(
        ProjectDeploymentService projectDeploymentService, ProjectService projectService,
        ProjectWorkflowService projectWorkflowService) {

        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
        this.projectWorkflowService = projectWorkflowService;
    }

    @Override
    public PlatformType getType() {
        return PlatformType.AUTOMATION;
    }

    @Override
    public Optional<VariableScope> getVariableScope(long jobPrincipalId) {
        ProjectDeployment projectDeployment = projectDeploymentService.getProjectDeployment(jobPrincipalId);

        return workspaceScope(projectDeployment.getProjectId());
    }

    @Override
    public Optional<VariableScope> getVariableScopeByWorkflowId(String workflowId) {
        return projectWorkflowService.fetchWorkflowProjectWorkflow(workflowId)
            .flatMap(projectWorkflow -> workspaceScope(projectWorkflow.getProjectId()));
    }

    private Optional<VariableScope> workspaceScope(long projectId) {
        Project project = projectService.getProject(projectId);

        Long workspaceId = project.getWorkspaceId();

        if (workspaceId == null) {
            return Optional.empty();
        }

        return Optional.of(VariableScope.workspace(workspaceId));
    }
}
