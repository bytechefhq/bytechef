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

package com.bytechef.automation.ai.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.ScopedConnectionGuard;
import com.bytechef.ai.copilot.tool.context.ScopedProjectToolContext;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.automation.ai.tool.model.WorkflowInfo;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.ai.tool.util.AllowedComponentNamesUtils;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class ClusterElementToolsTest {

    private static final long FOREIGN_PROJECT_ID = 9L;
    private static final long OWN_PROJECT_ID = 7L;
    private static final long WORKSPACE_ID = 1L;

    @Mock
    private ComponentDefinitionService componentDefinitionService;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private ScopedConnectionGuard scopedConnectionGuard;

    @Mock
    private ToolContext toolContext;

    @Mock
    private WorkspaceScopeResolver workspaceScopeResolver;

    @Nested
    class ScopedProjectTest {

        @Test
        void testUpdateWorkflowRootPropertiesRejectsAWorkflowOfAnotherProject() {
            when(projectWorkflowService.getWorkflowProjectWorkflow("wf-uuid-1"))
                .thenReturn(new ProjectWorkflow(FOREIGN_PROJECT_ID, 1, "wf-uuid-1"));

            ClusterElementTools clusterElementTools = newTools();

            assertThatCode(
                () -> clusterElementTools.updateWorkflowRootProperties(
                    "wf-uuid-1", "{\"label\": \"label\"}", null, scopedToolContext()))
                        .isInstanceOf(ExecutionException.class)
                        .hasMessageContaining("was not found in this chat's project");

            verify(projectWorkflowFacade, never()).getProjectWorkflow(anyString());
            verify(projectWorkflowFacade, never()).updateWorkflow(anyString(), anyString(), anyInt());
            verify(workspaceScopeResolver, never()).resolveWorkspace(any(), any());
        }

        @Test
        void testUpdateClusterElementTaskRejectsAWorkflowOfAnotherProject() {
            when(projectWorkflowService.getWorkflowProjectWorkflow("wf-uuid-1"))
                .thenReturn(new ProjectWorkflow(FOREIGN_PROJECT_ID, 1, "wf-uuid-1"));

            ClusterElementTools clusterElementTools = newTools();

            assertThatCode(
                () -> clusterElementTools.updateClusterElementTask(
                    "wf-uuid-1", "agent_1", "{}", null, scopedToolContext()))
                        .isInstanceOf(ExecutionException.class)
                        .hasMessageContaining("was not found in this chat's project");

            verify(projectWorkflowFacade, never()).getProjectWorkflow(anyString());
            verify(projectWorkflowFacade, never()).updateWorkflow(anyString(), anyString(), anyInt());
        }

        @Test
        void testUpdateClusterElementTaskUpdatesAWorkflowOfItsOwnProject() {
            when(projectWorkflowService.getWorkflowProjectWorkflow("wf-uuid-1"))
                .thenReturn(new ProjectWorkflow(OWN_PROJECT_ID, 1, "wf-uuid-1"));

            mockProjectWorkflow();

            WorkflowInfo workflowInfo = newTools().updateClusterElementTask(
                "wf-uuid-1", "agent_1", "{}", null, scopedToolContext());

            assertThat(workflowInfo).isNotNull();
            verify(projectWorkflowFacade).updateWorkflow(anyString(), anyString(), anyInt());
            verify(workspaceScopeResolver, never()).resolveWorkspace(any(), any());
        }

        @Test
        void testUpdateClusterElementTaskRejectsAClusterElementOutsideTheAllowList() {
            when(projectWorkflowService.getWorkflowProjectWorkflow("wf-uuid-1"))
                .thenReturn(new ProjectWorkflow(OWN_PROJECT_ID, 1, "wf-uuid-1"));

            mockProjectWorkflow();

            ComponentDefinition openAiComponentDefinition = mock(ComponentDefinition.class);

            when(openAiComponentDefinition.isConnectionRequired()).thenReturn(true);
            when(componentDefinitionService.fetchComponentDefinition("openAi", null))
                .thenReturn(Optional.of(openAiComponentDefinition));

            ClusterElementTools clusterElementTools = newTools();

            ToolContext allowListToolContext = new ToolContext(
                Map.of(
                    ScopedProjectToolContext.TOOL_CONTEXT_SCOPED_PROJECT_ID_KEY, OWN_PROJECT_ID,
                    AllowedComponentNamesUtils.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, Set.of("aiAgent")));

            assertThatCode(
                () -> clusterElementTools.updateClusterElementTask(
                    "wf-uuid-1", "agent_1", "{\"model\": {\"name\": \"model_1\", \"type\": \"openAi/v1/model\"}}",
                    null, allowListToolContext))
                        .isInstanceOf(ExecutionException.class)
                        .hasMessageContaining(
                            "This chat can't use openAi: it isn't one of the integrations available to this user.")
                        .hasMessageNotContaining("Failed to update");

            verify(projectWorkflowFacade, never()).updateWorkflow(anyString(), anyString(), anyInt());
        }

        @Test
        void testUpdateWorkflowRootPropertiesRejectsATaskOutsideTheAllowList() {
            when(projectWorkflowService.getWorkflowProjectWorkflow("wf-uuid-1"))
                .thenReturn(new ProjectWorkflow(OWN_PROJECT_ID, 1, "wf-uuid-1"));

            mockProjectWorkflow();

            ComponentDefinition openAiComponentDefinition = mock(ComponentDefinition.class);

            when(openAiComponentDefinition.isConnectionRequired()).thenReturn(true);
            when(componentDefinitionService.fetchComponentDefinition("openAi", null))
                .thenReturn(Optional.of(openAiComponentDefinition));

            ClusterElementTools clusterElementTools = newTools();

            ToolContext allowListToolContext = new ToolContext(
                Map.of(
                    ScopedProjectToolContext.TOOL_CONTEXT_SCOPED_PROJECT_ID_KEY, OWN_PROJECT_ID,
                    AllowedComponentNamesUtils.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, Set.of("aiAgent")));

            assertThatCode(
                () -> clusterElementTools.updateWorkflowRootProperties(
                    "wf-uuid-1", "{\"tasks\": [{\"name\": \"ask_1\", \"type\": \"openAi/v1/ask\"}]}", null,
                    allowListToolContext))
                        .isInstanceOf(ExecutionException.class)
                        .hasMessageContaining(
                            "This chat can't use openAi: it isn't one of the integrations available to this user.")
                        .hasMessageNotContaining("Failed to update");

            verify(projectWorkflowFacade, never()).updateWorkflow(anyString(), anyString(), anyInt());
        }
    }

    @Nested
    class WorkspaceScopeTest {

        @Test
        void testUpdateWorkflowRootPropertiesRejectsAWorkflowOfAnotherWorkspace() {
            mockWorkflowInWorkspace(2L);

            ClusterElementTools clusterElementTools = newTools();

            assertThatCode(
                () -> clusterElementTools.updateWorkflowRootProperties(
                    "wf-uuid-1", "{\"label\": \"label\"}", null, toolContext))
                        .isInstanceOf(ExecutionException.class)
                        .hasMessageContaining("not found in workspace " + WORKSPACE_ID);

            verify(projectWorkflowFacade, never()).getProjectWorkflow(anyString());
            verify(projectWorkflowFacade, never()).updateWorkflow(anyString(), anyString(), anyInt());
        }

        @Test
        void testUpdateWorkflowRootPropertiesUpdatesAWorkflowOfTheWorkspace() {
            mockWorkflowInWorkspace(WORKSPACE_ID);
            mockProjectWorkflow();

            WorkflowInfo workflowInfo = newTools().updateWorkflowRootProperties(
                "wf-uuid-1", "{\"label\": \"label\"}", null, toolContext);

            assertThat(workflowInfo).isNotNull();
            verify(projectWorkflowFacade).updateWorkflow(anyString(), anyString(), anyInt());
        }

        @Test
        void testUpdateClusterElementTaskRejectsAWorkflowOfAnotherWorkspace() {
            mockWorkflowInWorkspace(2L);

            ClusterElementTools clusterElementTools = newTools();

            assertThatCode(
                () -> clusterElementTools.updateClusterElementTask(
                    "wf-uuid-1", "agent_1", "{}", null, toolContext))
                        .isInstanceOf(ExecutionException.class)
                        .hasMessageContaining("not found in workspace " + WORKSPACE_ID);

            verify(projectWorkflowFacade, never()).getProjectWorkflow(anyString());
            verify(projectWorkflowFacade, never()).updateWorkflow(anyString(), anyString(), anyInt());
        }

        @Test
        void testUpdateClusterElementTaskUpdatesAWorkflowOfTheWorkspace() {
            mockWorkflowInWorkspace(WORKSPACE_ID);
            mockProjectWorkflow();

            WorkflowInfo workflowInfo = newTools().updateClusterElementTask(
                "wf-uuid-1", "agent_1", "{}", null, toolContext);

            assertThat(workflowInfo).isNotNull();
            verify(projectWorkflowFacade).updateWorkflow(anyString(), anyString(), anyInt());
        }
    }

    private void mockProjectWorkflow() {
        Workflow workflow = new Workflow(
            "wf-uuid-1", "{\"tasks\": [{\"name\": \"agent_1\", \"type\": \"aiAgent/v1/chat\"}]}",
            Workflow.Format.JSON);

        when(projectWorkflowFacade.getProjectWorkflow("wf-uuid-1"))
            .thenReturn(new ProjectWorkflowDTO(workflow, new ProjectWorkflow(55L), false));
    }

    private static ToolContext scopedToolContext() {
        return new ToolContext(Map.of(ScopedProjectToolContext.TOOL_CONTEXT_SCOPED_PROJECT_ID_KEY, OWN_PROJECT_ID));
    }

    private void mockWorkflowInWorkspace(long workspaceId) {
        when(workspaceScopeResolver.resolveWorkspace(null, toolContext))
            .thenReturn(new WorkspaceScopeResolver.Resolved(WORKSPACE_ID, 0L));
        when(projectWorkflowService.getWorkflowProjectWorkflow("wf-uuid-1"))
            .thenReturn(new ProjectWorkflow(7L, 1, "wf-uuid-1"));
        when(projectService.fetchProject(7L)).thenReturn(
            Optional.of(
                Project.builder()
                    .id(7L)
                    .name("project")
                    .workspaceId(workspaceId)
                    .build()));
    }

    private ClusterElementTools newTools() {
        return new ClusterElementTools(
            componentDefinitionService, projectWorkflowFacade,
            new ScopedProjectGuard(
                projectService, projectWorkflowService, scopedConnectionGuard, workspaceScopeResolver));
    }
}
