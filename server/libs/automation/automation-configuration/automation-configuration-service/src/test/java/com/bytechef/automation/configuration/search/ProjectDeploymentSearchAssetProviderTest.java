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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.ownership.ProjectOwner;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectOwnershipService;
import com.bytechef.automation.configuration.service.ProjectService;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ProjectDeploymentSearchAssetProviderTest {

    @Test
    void testSearchHidesDeploymentsOfProjectsOwnedByAnyEmbeddedOwner() {
        ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
        ProjectOwnershipService projectOwnershipService = mock(ProjectOwnershipService.class);
        ProjectService projectService = mock(ProjectService.class);

        List<ProjectDeployment> projectDeployments = List.of(
            createDeployment(10L, 1L), createDeployment(11L, 2L), createDeployment(12L, 3L));
        List<Project> projects = List.of(
            createProject(1L, "Sales catalog"), createProject(2L, "Sales user"), createProject(3L, "Sales"));

        when(projectDeploymentService.getProjectDeployments()).thenReturn(projectDeployments);
        when(projectService.getProjects(any())).thenReturn(projects);
        when(projectOwnershipService.getOwnedProjectIds(any(), eq(EnumSet.allOf(ProjectOwner.class))))
            .thenReturn(Set.of(1L, 2L));

        ProjectDeploymentSearchAssetProvider projectDeploymentSearchAssetProvider =
            new ProjectDeploymentSearchAssetProvider(
                projectDeploymentService, projectOwnershipService, projectService);

        List<ProjectDeploymentSearchResult> results = projectDeploymentSearchAssetProvider.search(
            "sales", 10, Set.of(5L));

        assertThat(results).extracting(ProjectDeploymentSearchResult::id)
            .containsExactly(12L);
    }

    private static ProjectDeployment createDeployment(long id, long projectId) {
        ProjectDeployment projectDeployment = mock(ProjectDeployment.class);

        when(projectDeployment.getId()).thenReturn(id);
        when(projectDeployment.getProjectId()).thenReturn(projectId);

        return projectDeployment;
    }

    private static Project createProject(long id, String name) {
        Project project = mock(Project.class);

        when(project.getId()).thenReturn(id);
        when(project.getName()).thenReturn(name);
        when(project.getWorkspaceId()).thenReturn(5L);

        return project;
    }
}
