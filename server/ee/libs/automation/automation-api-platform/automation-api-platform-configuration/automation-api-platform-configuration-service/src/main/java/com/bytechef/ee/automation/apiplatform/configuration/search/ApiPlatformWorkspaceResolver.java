/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.search;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Resolves API-collection project-deployment ids to their owning workspace (deployment &rarr; project &rarr;
 * {@code project.workspace_id}) so the API-collection/endpoint search providers can stamp {@code workspaceId} on their
 * results and the search aggregator can drop results from workspaces the caller cannot access.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
class ApiPlatformWorkspaceResolver {

    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    ApiPlatformWorkspaceResolver(
        ProjectDeploymentService projectDeploymentService, ProjectService projectService) {

        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
    }

    /**
     * Maps each given project-deployment id to its owning workspace id (or {@code null} when it cannot be resolved).
     */
    Map<Long, Long> getWorkspaceIdsByProjectDeploymentId(List<Long> projectDeploymentIds) {
        if (projectDeploymentIds.isEmpty()) {
            return Map.of();
        }

        List<ProjectDeployment> projectDeployments = projectDeploymentService.getProjectDeployments(
            projectDeploymentIds);

        List<Long> projectIds = projectDeployments.stream()
            .map(ProjectDeployment::getProjectId)
            .distinct()
            .toList();

        Map<Long, Long> projectIdToWorkspaceId = new HashMap<>();

        for (Project project : projectService.getProjects(projectIds)) {
            projectIdToWorkspaceId.put(project.getId(), project.getWorkspaceId());
        }

        Map<Long, Long> workspaceIdByProjectDeploymentId = new HashMap<>();

        for (ProjectDeployment projectDeployment : projectDeployments) {
            workspaceIdByProjectDeploymentId.put(
                projectDeployment.getId(), projectIdToWorkspaceId.get(projectDeployment.getProjectId()));
        }

        return workspaceIdByProjectDeploymentId;
    }
}
