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
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Resolves API collections to their owning workspace (project deployment &rarr; project &rarr;
 * {@code project.workspace_id}) so the API collection and endpoint search providers can stamp the workspace on their
 * results.
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
    ApiPlatformWorkspaceResolver(ProjectDeploymentService projectDeploymentService, ProjectService projectService) {
        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
    }

    /**
     * Maps each given API collection's project deployment id to its owning workspace id; unresolvable ids are absent.
     */
    Map<Long, Long> getWorkspaceIdsByProjectDeploymentId(List<ApiCollection> apiCollections) {
        List<Long> projectDeploymentIds = apiCollections.stream()
            .map(ApiCollection::getProjectDeploymentId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        if (projectDeploymentIds.isEmpty()) {
            return Map.of();
        }

        List<ProjectDeployment> projectDeployments = projectDeploymentService.getProjectDeployments(
            projectDeploymentIds);

        List<Long> projectIds = projectDeployments.stream()
            .map(ProjectDeployment::getProjectId)
            .distinct()
            .toList();

        Map<Long, Long> workspaceIdByProjectId = new HashMap<>();

        for (Project project : projectService.getProjects(projectIds)) {
            workspaceIdByProjectId.put(project.getId(), project.getWorkspaceId());
        }

        Map<Long, Long> workspaceIdByProjectDeploymentId = new HashMap<>();

        for (ProjectDeployment projectDeployment : projectDeployments) {
            Long workspaceId = workspaceIdByProjectId.get(projectDeployment.getProjectId());

            if (workspaceId != null) {
                workspaceIdByProjectDeploymentId.put(projectDeployment.getId(), workspaceId);
            }
        }

        return workspaceIdByProjectDeploymentId;
    }
}
