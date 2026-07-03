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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.automation.ai.tool.model.WorkflowInfo;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Marko Kriskovic
 * @author Ivica Cardic
 */
@ExtendWith({
    MockitoExtension.class, ObjectMapperSetupExtension.class
})
class ProjectWorkflowToolsTest {

    private static final String DEFINITION = """
        {"label": "My Flow", "triggers": [], "tasks": []}""";

    private static final String WORKFLOW_DTO_DEFINITION = """
        {"label": "My Flow", "tasks": []}""";

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private UserService userService;

    @Mock
    private WorkspaceFacade workspaceFacade;

    @Mock
    private ObjectProvider<WorkflowArtifactRecorder> recorderProvider;

    @Mock
    private WorkflowArtifactRecorder recorder;

    @Mock
    private ObjectProvider<WorkflowTestConfigurationFacade> testConfigurationFacadeProvider;

    @Mock
    private WorkflowTestConfigurationFacade workflowTestConfigurationFacade;

    @Mock
    private ToolContext toolContext;

    @Test
    void testCreateProjectWorkflowRecordsArtifact() {
        when(recorderProvider.getIfAvailable()).thenReturn(recorder);

        ProjectWorkflow projectWorkflow = buildProjectWorkflow(55L, 7L, "wf-uuid-1");

        when(projectWorkflowFacade.addWorkflow(7L, DEFINITION)).thenReturn(projectWorkflow);

        ProjectWorkflowTools tools = newTools();

        tools.createProjectWorkflow(7L, DEFINITION, toolContext);

        verify(recorder).recordWorkflowArtifact(toolContext, true, "wf-uuid-1", 7L, 55L, "My Flow");
    }

    @Test
    void testUpdateWorkflowRecordsArtifact() {
        when(recorderProvider.getIfAvailable()).thenReturn(recorder);

        ProjectWorkflowDTO dto = buildDto("wf-uuid-2", 88L, 3);

        when(projectWorkflowFacade.getProjectWorkflow("wf-uuid-2")).thenReturn(dto);

        ProjectWorkflow projectWorkflow = buildProjectWorkflow(88L, 9L, "wf-uuid-2");

        when(projectWorkflowService.getProjectWorkflow(88L)).thenReturn(projectWorkflow);

        ProjectWorkflowTools tools = newTools();

        tools.updateWorkflow("wf-uuid-2", DEFINITION, toolContext);

        verify(recorder).recordWorkflowArtifact(toolContext, false, "wf-uuid-2", 9L, 88L, "My Flow");
    }

    @Test
    void testCreateProjectWorkflowSkipsRecordingWhenNoRecorder() {
        when(recorderProvider.getIfAvailable()).thenReturn(null);

        ProjectWorkflow projectWorkflow = buildProjectWorkflow(55L, 7L, "wf-uuid-1");

        when(projectWorkflowFacade.addWorkflow(7L, DEFINITION)).thenReturn(projectWorkflow);

        ProjectWorkflowTools tools = newTools();

        tools.createProjectWorkflow(7L, DEFINITION, toolContext);

        verify(recorder, never()).recordWorkflowArtifact(any(), eq(true), any(), anyLong(), any(), any());
    }

    @Test
    void testCreateProjectWorkflowSucceedsWhenRecorderThrows() {
        when(recorderProvider.getIfAvailable()).thenReturn(recorder);

        ProjectWorkflow projectWorkflow = buildProjectWorkflow(55L, 7L, "wf-uuid-1");

        when(projectWorkflowFacade.addWorkflow(7L, DEFINITION)).thenReturn(projectWorkflow);

        doThrow(new RuntimeException("boom"))
            .when(recorder)
            .recordWorkflowArtifact(any(), eq(true), any(), anyLong(), any(), any());

        ProjectWorkflowTools tools = newTools();

        assertThatCode(() -> tools.createProjectWorkflow(7L, DEFINITION, toolContext))
            .doesNotThrowAnyException();
    }

    @Test
    void testUpdateWorkflowSucceedsWhenProjectIdLookupThrows() {
        when(recorderProvider.getIfAvailable()).thenReturn(recorder);

        ProjectWorkflowDTO dto = buildDto("wf-uuid-2", 88L, "Updated Flow", 3);

        when(projectWorkflowFacade.getProjectWorkflow("wf-uuid-2")).thenReturn(dto);
        when(projectWorkflowService.getProjectWorkflow(88L)).thenThrow(new RuntimeException("lookup boom"));

        ProjectWorkflowTools tools = newTools();

        assertThatCode(() -> tools.updateWorkflow("wf-uuid-2", DEFINITION, toolContext))
            .doesNotThrowAnyException();

        verify(recorder, never()).recordWorkflowArtifact(any(), eq(false), any(), anyLong(), any(), any());
    }

    @Test
    void testSearchWorkflowsScopesToAccessibleProjects() {
        mockAccessibleProjects(7L, 1L, 10L);

        when(projectWorkflowFacade.getProjectWorkflows(10L)).thenReturn(List.of(buildDto("wf-1", 1L, 1)));

        ProjectWorkflowTools tools = newTools();

        List<WorkflowInfo> result = tools.searchWorkflows("flow", null);

        assertThat(result).hasSize(1);

        // The whole-tenant enumeration must never be used.
        verify(projectWorkflowFacade, never()).getProjectWorkflows();
    }

    @Test
    void testSearchWorkflowsRejectsInaccessibleProjectId() {
        mockAccessibleProjects(7L, 1L, 10L);

        ProjectWorkflowTools tools = newTools();

        List<WorkflowInfo> result = tools.searchWorkflows("flow", 99L);

        assertThat(result).isEmpty();

        verify(projectWorkflowFacade, never()).getProjectWorkflows(99L);
        verify(projectWorkflowFacade, never()).getProjectWorkflows();
    }

    @Test
    void testSearchWorkflowsReturnsEmptyWhenNoUser() {
        when(userService.fetchCurrentUser()).thenReturn(Optional.empty());

        ProjectWorkflowTools tools = newTools();

        List<WorkflowInfo> result = tools.searchWorkflows("flow", null);

        assertThat(result).isEmpty();

        verify(projectWorkflowFacade, never()).getProjectWorkflows();
        verify(projectWorkflowFacade, never()).getProjectWorkflows(anyLong());
    }

    private void mockAccessibleProjects(long userId, long workspaceId, long projectId) {
        User user = org.mockito.Mockito.mock(User.class);

        when(user.getId()).thenReturn(userId);
        when(userService.fetchCurrentUser()).thenReturn(Optional.of(user));

        Workspace workspace = org.mockito.Mockito.mock(Workspace.class);

        when(workspace.getId()).thenReturn(workspaceId);
        when(workspaceFacade.getUserWorkspaces(userId)).thenReturn(List.of(workspace));
        when(projectService.getWorkspaceProjectIds(workspaceId)).thenReturn(List.of(projectId));
    }

    @Test
    void testSaveWorkflowTestConnectionDelegatesToFacadeWithAssetFileEnvironment() {
        when(testConfigurationFacadeProvider.getIfAvailable()).thenReturn(workflowTestConfigurationFacade);
        when(toolContext.getContext()).thenReturn(Map.of("bytechef.assetFile.environmentId", 2L));

        ProjectWorkflowTools tools = newTools();

        String result = tools.saveWorkflowTestConnection("wf-uuid-1", "sendChannelMessage_1", "slack", 1107L,
            toolContext);

        verify(workflowTestConfigurationFacade)
            .saveWorkflowTestConfigurationConnection("wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, 2L);
        assertThat(result).contains("1107", "sendChannelMessage_1");
    }

    @Test
    void testSaveWorkflowTestConnectionFallsBackToAgentToolEnvironmentKey() {
        when(testConfigurationFacadeProvider.getIfAvailable()).thenReturn(workflowTestConfigurationFacade);
        when(toolContext.getContext()).thenReturn(Map.of("bytechef.agentTool.environmentId", 1L));

        ProjectWorkflowTools tools = newTools();

        tools.saveWorkflowTestConnection("wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, toolContext);

        verify(workflowTestConfigurationFacade)
            .saveWorkflowTestConfigurationConnection("wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, 1L);
    }

    @Test
    void testSaveWorkflowTestConnectionDefaultsEnvironmentToDevelopmentWhenAbsent() {
        when(testConfigurationFacadeProvider.getIfAvailable()).thenReturn(workflowTestConfigurationFacade);
        when(toolContext.getContext()).thenReturn(Map.of());

        ProjectWorkflowTools tools = newTools();

        tools.saveWorkflowTestConnection("wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, toolContext);

        verify(workflowTestConfigurationFacade)
            .saveWorkflowTestConfigurationConnection("wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, 0L);
    }

    @Test
    void testSaveWorkflowTestConnectionThrowsWhenFacadeAbsent() {
        when(testConfigurationFacadeProvider.getIfAvailable()).thenReturn(null);

        ProjectWorkflowTools tools = newTools();

        assertThatCode(
            () -> tools.saveWorkflowTestConnection("wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, toolContext))
                .isInstanceOf(ExecutionException.class);
    }

    private ProjectWorkflowTools newTools() {
        return new ProjectWorkflowTools(
            projectService, projectWorkflowFacade, projectWorkflowService, userService, workspaceFacade,
            recorderProvider, testConfigurationFacadeProvider);
    }

    private static ProjectWorkflow buildProjectWorkflow(long id, long projectId, String workflowId) {
        ProjectWorkflow projectWorkflow = new ProjectWorkflow(projectId, 1, workflowId);

        ReflectionTestUtils.setField(projectWorkflow, "id", id);

        return projectWorkflow;
    }

    private static ProjectWorkflowDTO buildDto(String workflowUuid, long projectWorkflowId, int version) {
        return buildDto(workflowUuid, projectWorkflowId, null, version);
    }

    private static ProjectWorkflowDTO buildDto(
        String workflowUuid, long projectWorkflowId, @SuppressWarnings("unused") String label, int version) {

        Workflow workflow = new Workflow(workflowUuid, WORKFLOW_DTO_DEFINITION, Workflow.Format.JSON);

        workflow.setVersion(version);

        ProjectWorkflow projectWorkflow = new ProjectWorkflow(projectWorkflowId);

        return new ProjectWorkflowDTO(workflow, projectWorkflow, false);
    }
}
