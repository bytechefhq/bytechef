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
import com.bytechef.automation.configuration.service.ProjectService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ProjectSearchAssetProviderTest {

    @Test
    void testReturnsOnlyCallerWorkspaceProjectsUpToLimit() {
        ProjectService projectService = mock(ProjectService.class);

        List<Project> projects = List.of(
            createProject(1L, "Sales foreign", 99L), createProject(2L, "Sales foreign 2", 99L),
            createProject(3L, "Sales own", 10L), createProject(4L, "Sales own 2", 10L),
            createProject(5L, "Sales own 3", 10L));

        when(projectService.getProjects(false, null, null, null, null, null)).thenReturn(projects);

        ProjectSearchAssetProvider projectSearchAssetProvider = new ProjectSearchAssetProvider(projectService);

        List<ProjectSearchResult> results = projectSearchAssetProvider.search("sales", 2, Set.of(10L));

        assertThat(results).extracting(ProjectSearchResult::id)
            .containsExactly(3L, 4L);
    }

    private static Project createProject(long id, String name, long workspaceId) {
        Project project = mock(Project.class);

        when(project.getId()).thenReturn(id);
        when(project.getName()).thenReturn(name);
        when(project.getWorkspaceId()).thenReturn(workspaceId);

        return project;
    }
}
