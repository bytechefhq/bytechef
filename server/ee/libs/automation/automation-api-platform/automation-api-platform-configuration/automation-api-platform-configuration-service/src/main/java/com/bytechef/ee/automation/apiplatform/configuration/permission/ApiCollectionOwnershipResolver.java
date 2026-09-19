/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.permission;

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

/**
 * Maps an API-collection id to its owning workspace, resolved through the synthetic {@code __API_COLLECTION__}
 * {@link ProjectDeployment} every collection is backed by: collection &rarr; {@code projectDeploymentId} &rarr;
 * deployment &rarr; project &rarr; workspace. The same hop {@code ProjectDeploymentOwnershipResolver} makes, one link
 * further back.
 *
 * <p>
 * Resolving through the backing deployment rather than inventing a parallel ownership path is what lets
 * {@link ApiCollectionEnvironmentResolver} answer the environment question from the same row, and it keeps API
 * collections consistent with the deployment they are: a collection cannot belong to a workspace its deployment does
 * not.
 *
 * <p>
 * Fails closed at every hop, as the SPI requires: a missing collection, a missing deployment, a deployment with no
 * project, or a project with no workspace all yield {@link ResourceOwner#unknown()} rather than throwing. Uses
 * {@code fetchApiCollection} rather than {@code getApiCollection} for exactly that reason -- that service documents the
 * distinction for authorization callers.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class ApiCollectionOwnershipResolver implements ResourceOwnershipResolver {

    private final ApiCollectionService apiCollectionService;
    private final ProjectDeploymentService projectDeploymentService;
    private final ProjectService projectService;

    @SuppressFBWarnings("EI")
    public ApiCollectionOwnershipResolver(
        ApiCollectionService apiCollectionService, ProjectDeploymentService projectDeploymentService,
        ProjectService projectService) {

        this.apiCollectionService = apiCollectionService;
        this.projectDeploymentService = projectDeploymentService;
        this.projectService = projectService;
    }

    @Override
    public String resourceType() {
        return "ApiCollection";
    }

    @Override
    public ResourceOwner resolveOwner(long id) {
        return apiCollectionService.fetchApiCollection(id)
            .map(ApiCollection::getProjectDeploymentId)
            .flatMap(projectDeploymentService::fetchProjectDeployment)
            .map(ProjectDeployment::getProjectId)
            .flatMap(projectService::fetchProject)
            .map(project -> ResourceOwner.ofWorkspace(project.getWorkspaceId()))
            .orElseGet(ResourceOwner::unknown);
    }
}
