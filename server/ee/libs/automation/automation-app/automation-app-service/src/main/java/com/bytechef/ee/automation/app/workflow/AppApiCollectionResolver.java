/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.workflow;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Resolves an app's API collection binding to the collection it serves now. A binding resolves only while the
 * collection exists, its project deployment exists, is enabled and is in the given environment, and the deployment's
 * project belongs to the given workspace. Bindable collections are those of the workspace and environment whose
 * deployment exists, disabled ones included, so an admin can bind a collection before enabling it.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AppApiCollectionResolver {

    private final ApiCollectionService apiCollectionService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    public AppApiCollectionResolver(
        ApiCollectionService apiCollectionService, ProjectDeploymentService projectDeploymentService,
        ProjectService projectService) {

        this.apiCollectionService = apiCollectionService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
    }

    public List<ApiCollection> getBindableApiCollections(long workspaceId, Environment environment) {
        List<ApiCollection> bindableApiCollections = new ArrayList<>();

        for (ApiCollection apiCollection : apiCollectionService.getApiCollections(
            workspaceId, environment, null, null)) {

            if (fetchDeployment(workspaceId, environment, apiCollection).isPresent()) {
                bindableApiCollections.add(apiCollection);
            }
        }

        return bindableApiCollections;
    }

    public Optional<ApiCollection> resolve(long workspaceId, Environment environment, long apiCollectionId) {
        Optional<ApiCollection> apiCollection = apiCollectionService.fetchApiCollection(apiCollectionId);

        if (apiCollection.isEmpty()) {
            return Optional.empty();
        }

        boolean enabled = fetchDeployment(workspaceId, environment, apiCollection.get())
            .map(ProjectDeployment::isEnabled)
            .orElse(false);

        return enabled ? apiCollection : Optional.empty();
    }

    private Optional<ProjectDeployment> fetchDeployment(
        long workspaceId, Environment environment, ApiCollection apiCollection) {

        Long projectDeploymentId = apiCollection.getProjectDeploymentId();

        if (projectDeploymentId == null) {
            return Optional.empty();
        }

        return projectDeploymentService.fetchProjectDeployment(projectDeploymentId)
            .filter(projectDeployment -> projectDeployment.getEnvironment() == environment)
            .filter(projectDeployment -> isProjectInWorkspace(workspaceId, projectDeployment));
    }

    private boolean isProjectInWorkspace(long workspaceId, ProjectDeployment projectDeployment) {
        return projectService.fetchProject(projectDeployment.getProjectId())
            .map(Project::getWorkspaceId)
            .filter(projectWorkspaceId -> Objects.equals(projectWorkspaceId, workspaceId))
            .isPresent();
    }
}
