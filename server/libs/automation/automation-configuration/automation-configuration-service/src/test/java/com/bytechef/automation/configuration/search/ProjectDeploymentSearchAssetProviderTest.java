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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.SystemProjects;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ProjectDeploymentSearchAssetProviderTest {

    @Test
    void testDeploymentSearchSkipsConnectedUserDeployments() {
        ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
        ProjectService projectService = mock(ProjectService.class);

        List<ProjectDeployment> deployments = List.of(
            createDeployment(1L, 100L), createDeployment(2L, 200L), createDeployment(3L, 300L),
            createDeployment(4L, 400L));

        when(projectDeploymentService.getProjectDeployments()).thenReturn(deployments);

        List<Project> projects = List.of(
            createProject(100L, "Orders"),
            createProject(200L, SystemProjects.EMBEDDED_CONNECTED_USER_NAME_PREFIX + "Orders ext-1"),
            createProject(300L, SystemProjects.EMBEDDED_AUTOMATION_NAME_PREFIX + "Orders"),
            createProject(400L, "Orders again"));

        when(projectService.getProjects(anyList())).thenReturn(projects);

        ProjectDeploymentSearchAssetProvider provider = new ProjectDeploymentSearchAssetProvider(
            projectDeploymentService, projectService);

        List<ProjectDeploymentSearchResult> results = provider.search("orders", 10, Set.of(10L));

        assertThat(results).extracting(ProjectDeploymentSearchResult::id)
            .containsExactly(1L, 4L);
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
        when(project.getWorkspaceId()).thenReturn(10L);

        return project;
    }
}
