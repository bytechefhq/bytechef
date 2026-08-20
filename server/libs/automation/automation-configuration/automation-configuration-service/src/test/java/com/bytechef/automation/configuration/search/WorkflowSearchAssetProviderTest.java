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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.security.ProjectVisibilityFilter;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver.VisibilityRecord;
import com.bytechef.platform.security.domain.ResourceVisibility;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * A workflow is exactly as visible as the project that owns it, in search as everywhere else.
 *
 * @author Ivica Cardic
 */
class WorkflowSearchAssetProviderTest {

    private static final long HIDDEN_PROJECT_ID = 2L;
    private static final long VISIBLE_PROJECT_ID = 1L;

    @Test
    void testSearchDropsWorkflowsOfProjectsTheFilterHides() {
        ProjectService projectService = mock(ProjectService.class);
        ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
        WorkflowService workflowService = mock(WorkflowService.class);

        // Built ahead of the stubbings below: the helper stubs its own mock, and Mockito rejects that inside an
        // unfinished when(...).
        List<Workflow> workflows = List.of(workflow("workflow-1"), workflow("workflow-2"));

        // Both workflows match the query and both projects are returned by the project service, so the only thing
        // that can exclude the second one is the visibility filter.
        when(projectWorkflowService.getLatestProjectWorkflows()).thenReturn(
            List.of(
                new ProjectWorkflow(VISIBLE_PROJECT_ID, 1, "workflow-1"),
                new ProjectWorkflow(HIDDEN_PROJECT_ID, 1, "workflow-2")));
        when(workflowService.getWorkflows(List.of("workflow-1", "workflow-2"))).thenReturn(workflows);
        when(projectService.getProjects(List.of(VISIBLE_PROJECT_ID, HIDDEN_PROJECT_ID)))
            .thenReturn(List.of(project(VISIBLE_PROJECT_ID), project(HIDDEN_PROJECT_ID)));

        WorkflowSearchAssetProvider workflowSearchAssetProvider = new WorkflowSearchAssetProvider(
            projectService, new ProjectVisibilityFilter(objectProvider(visibleOnly(VISIBLE_PROJECT_ID))),
            projectWorkflowService, workflowService);

        List<WorkflowSearchResult> workflowSearchResults = workflowSearchAssetProvider.search("order", 10);

        assertThat(workflowSearchResults).extracting(WorkflowSearchResult::projectId)
            .containsExactly(VISIBLE_PROJECT_ID);
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

    private static ResourceVisibilityResolver visibleOnly(long visibleId) {
        return (resourceType, workspaceId, candidates) -> candidates.stream()
            .map(VisibilityRecord::id)
            .filter(id -> id == visibleId)
            .collect(Collectors.toSet());
    }

    private static Workflow workflow(String id) {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getId()).thenReturn(id);
        when(workflow.getLabel()).thenReturn("Order sync " + id);

        return workflow;
    }
}
