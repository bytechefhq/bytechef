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

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class WorkflowSearchAssetProviderTest {

    @Test
    void testReturnsOnlyWorkflowsOfCallerWorkspaceProjectsUpToLimit() {
        ProjectService projectService = mock(ProjectService.class);
        ProjectWorkflowService projectWorkflowService = mock(ProjectWorkflowService.class);
        WorkflowService workflowService = mock(WorkflowService.class);

        when(projectService.getWorkspaceProjectIds(10L)).thenReturn(List.of(200L));

        List<ProjectWorkflow> projectWorkflows = List.of(
            createProjectWorkflow(1L, 100L, "w1"), createProjectWorkflow(2L, 100L, "w2"),
            createProjectWorkflow(3L, 200L, "w3"), createProjectWorkflow(4L, 200L, "w4"));
        List<Workflow> workflows = List.of(
            createWorkflow("w1", "Order foreign"), createWorkflow("w2", "Order foreign 2"),
            createWorkflow("w3", "Order own"), createWorkflow("w4", "Order own 2"));

        when(projectWorkflowService.getLatestProjectWorkflows()).thenReturn(projectWorkflows);
        when(workflowService.getWorkflows(anyList())).thenReturn(workflows);

        WorkflowSearchAssetProvider workflowSearchAssetProvider = new WorkflowSearchAssetProvider(
            projectService, projectWorkflowService, workflowService);

        List<WorkflowSearchResult> results = workflowSearchAssetProvider.search("order", 1, Set.of(10L));

        assertThat(results).extracting(WorkflowSearchResult::id)
            .containsExactly(3L);
    }

    private static ProjectWorkflow createProjectWorkflow(long id, long projectId, String workflowId) {
        ProjectWorkflow projectWorkflow = mock(ProjectWorkflow.class);

        when(projectWorkflow.getId()).thenReturn(id);
        when(projectWorkflow.getProjectId()).thenReturn(projectId);
        when(projectWorkflow.getWorkflowId()).thenReturn(workflowId);

        return projectWorkflow;
    }

    private static Workflow createWorkflow(String id, String label) {
        Workflow workflow = mock(Workflow.class);

        when(workflow.getId()).thenReturn(id);
        when(workflow.getLabel()).thenReturn(label);

        return workflow;
    }
}
