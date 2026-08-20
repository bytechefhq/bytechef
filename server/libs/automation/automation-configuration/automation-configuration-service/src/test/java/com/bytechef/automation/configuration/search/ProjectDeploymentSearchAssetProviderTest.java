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

package com.bytechef.automation.configuration.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * A deployment is exactly as visible as the project it deploys, in search as everywhere else.
 *
 * @author Ivica Cardic
 */
class ProjectDeploymentSearchAssetProviderTest {

    private static final long HIDDEN_PROJECT_ID = 2L;
    private static final long VISIBLE_PROJECT_ID = 1L;

    @Test
    void testSearchDropsDeploymentsOfProjectsTheFilterHides() {
        ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
        ProjectService projectService = mock(ProjectService.class);

        // Both deployments' projects match the query and both are returned by the project service, so the only thing
        // that can exclude the second one is the visibility filter.
        when(projectDeploymentService.getProjectDeployments()).thenReturn(
            List.of(projectDeployment(10L, VISIBLE_PROJECT_ID), projectDeployment(11L, HIDDEN_PROJECT_ID)));
        when(projectService.getProjects(List.of(VISIBLE_PROJECT_ID, HIDDEN_PROJECT_ID)))
            .thenReturn(List.of(project(VISIBLE_PROJECT_ID), project(HIDDEN_PROJECT_ID)));

        ProjectDeploymentSearchAssetProvider projectDeploymentSearchAssetProvider =
            new ProjectDeploymentSearchAssetProvider(
                projectDeploymentService, projectService,
                new ProjectVisibilityFilter(objectProvider(visibleOnly(VISIBLE_PROJECT_ID))));

        List<ProjectDeploymentSearchResult> projectDeploymentSearchResults =
            projectDeploymentSearchAssetProvider.search("project", 10);

        assertThat(projectDeploymentSearchResults).extracting(ProjectDeploymentSearchResult::id)
            .containsExactly(10L);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<ResourceVisibilityResolver> objectProvider(
        ResourceVisibilityResolver resourceVisibilityResolver) {

        ObjectProvider<ResourceVisibilityResolver> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(resourceVisibilityResolver);

        return objectProvider;
    }

    private static Project project(long id) {
        Project project = new Project();

        project.setId(id);
        project.setName("project-" + id);
        project.setWorkspaceId(9L);
        project.setVisibility(ResourceVisibility.WORKSPACE);

        return project;
    }

    private static ProjectDeployment projectDeployment(long id, long projectId) {
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setId(id);
        projectDeployment.setProjectId(projectId);

        return projectDeployment;
    }

    private static ResourceVisibilityResolver visibleOnly(long visibleId) {
        return (resourceType, workspaceId, candidates) -> candidates.stream()
            .map(VisibilityRecord::id)
            .filter(id -> id == visibleId)
            .collect(Collectors.toSet());
    }
}
