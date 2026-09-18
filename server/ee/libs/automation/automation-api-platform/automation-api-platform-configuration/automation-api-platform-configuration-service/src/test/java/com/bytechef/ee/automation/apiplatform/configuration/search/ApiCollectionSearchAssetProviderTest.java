/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiCollection;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class ApiCollectionSearchAssetProviderTest {

    @Mock
    private ApiCollectionService apiCollectionService;

    @Mock
    private ProjectDeploymentService projectDeploymentService;

    @Mock
    private ProjectService projectService;

    private ApiPlatformWorkspaceResolver apiPlatformWorkspaceResolver;

    @BeforeEach
    void beforeEach() {
        apiPlatformWorkspaceResolver = new ApiPlatformWorkspaceResolver(projectDeploymentService, projectService);
    }

    @Test
    void testSearchStampsCollectionWithItsProjectWorkspace() {
        when(apiCollectionService.getApiCollections(null, null, null, null)).thenReturn(
            List.of(createApiCollection(1L, "orders api", 100L)));
        when(projectDeploymentService.getProjectDeployments(List.of(100L))).thenReturn(
            List.of(createProjectDeployment(100L, 200L)));
        when(projectService.getProjects(List.of(200L))).thenReturn(List.of(createProject(200L, 7L)));

        ApiCollectionSearchAssetProvider apiCollectionSearchAssetProvider = new ApiCollectionSearchAssetProvider(
            apiCollectionService, apiPlatformWorkspaceResolver);

        List<ApiCollectionSearchResult> apiCollectionSearchResults = apiCollectionSearchAssetProvider.search(
            "ORDERS", 10);

        assertThat(apiCollectionSearchResults).containsExactly(
            new ApiCollectionSearchResult(1L, "orders api", null, 7L));
    }

    private static ApiCollection createApiCollection(long id, String name, long projectDeploymentId) {
        ApiCollection apiCollection = new ApiCollection();

        apiCollection.setId(id);
        apiCollection.setName(name);
        apiCollection.setProjectDeploymentId(projectDeploymentId);

        return apiCollection;
    }

    private static Project createProject(long id, long workspaceId) {
        Project project = new Project();

        project.setId(id);
        project.setWorkspaceId(workspaceId);

        return project;
    }

    private static ProjectDeployment createProjectDeployment(long id, long projectId) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(id);
        projectDeployment.setProjectId(projectId);

        return projectDeployment;
    }
}
