/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class ApiCollectionOwnershipResolverTest {

    private static final long API_COLLECTION_ID = 5L;
    private static final long PROJECT_DEPLOYMENT_ID = 11L;
    private static final long PROJECT_ID = 13L;
    private static final long WORKSPACE_ID = 42L;

    private final ApiCollectionService apiCollectionService = mock(ApiCollectionService.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectService projectService = mock(ProjectService.class);

    private final ApiCollectionOwnershipResolver apiCollectionOwnershipResolver = new ApiCollectionOwnershipResolver(
        apiCollectionService, projectDeploymentService, projectService);

    @BeforeEach
    void setUp() {
        ApiCollection apiCollection = new ApiCollection();

        apiCollection.setProjectDeploymentId(PROJECT_DEPLOYMENT_ID);

        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setProjectId(PROJECT_ID);

        when(apiCollectionService.fetchApiCollection(API_COLLECTION_ID)).thenReturn(Optional.of(apiCollection));
        when(projectDeploymentService.fetchProjectDeployment(PROJECT_DEPLOYMENT_ID))
            .thenReturn(Optional.of(projectDeployment));
    }

    @Test
    void testOwnerIsTheWorkspaceOfTheBackingDeploymentsProject() {
        givenProjectInWorkspace(WORKSPACE_ID);

        assertThat(apiCollectionOwnershipResolver.resolveOwner(API_COLLECTION_ID)
            .workspaceId()).hasValue(WORKSPACE_ID);
    }

    @Test
    void testAProjectWithNoWorkspaceHasNoOwner() {
        givenProjectInWorkspace(null);

        ResourceOwner resourceOwner = apiCollectionOwnershipResolver.resolveOwner(API_COLLECTION_ID);

        assertThat(resourceOwner.workspaceId()).isEmpty();
    }

    private void givenProjectInWorkspace(Long workspaceId) {
        Project project = new Project();

        project.setWorkspaceId(workspaceId);

        when(projectService.fetchProject(PROJECT_ID)).thenReturn(Optional.of(project));
    }
}
