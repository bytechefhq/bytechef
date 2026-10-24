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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.bytechef.ai.copilot.tool.ScopedConnectionGuard;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ai.copilot.tool.context.ScopedProjectToolContext;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.automation.ai.tool.model.ProjectWorkflowInfo;
import com.bytechef.automation.ai.tool.model.WorkflowInfo;
import com.bytechef.automation.ai.tool.model.WorkflowSaveInfo;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectFacade;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.ai.tool.util.AllowedComponentNamesUtils;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.facade.ComponentDefinitionFacade;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.workflow.validator.WorkflowValidatorFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    private static final long WORKSPACE_ID = 1L;

    @Mock
    private ComponentDefinitionFacade componentDefinitionFacade;

    @Mock
    private ProjectFacade projectFacade;

    @Mock
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private ScopedConnectionGuard scopedConnectionGuard;

    @Mock
    private ToolContext toolContext;

    @Mock
    private WorkspaceConnectionFacade workspaceConnectionFacade;

    @Mock
    private WorkspaceFacade workspaceFacade;

    @Nested
    class ValidateBeforeSaveTest {

        @Mock
        private WorkflowValidatorFacade workflowValidatorFacade;

        @Test
        void testCreateProjectWorkflowSavesNothingWhenTheDefinitionHasErrors() {
            when(workflowValidatorFacade.validateWorkflow(eq(DEFINITION), any(), anyLong())).thenReturn(
                new WorkflowValidatorFacade.WorkflowValidationResult(
                    List.of("[slack_1] Field 'channel' is required"), List.of()));

            ProjectWorkflowTools tools = newTools(null, workflowValidatorFacade);

            assertThatCode(() -> tools.createProjectWorkflow(7L, DEFINITION, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("Nothing was saved")
                .hasMessageContaining("[slack_1] Field 'channel' is required");

            verify(projectWorkflowFacade, never()).addWorkflow(anyLong(), anyString());
            verify(projectWorkflowFacade, never()).updateWorkflow(anyString(), anyString(), anyInt());
        }

        @Test
        void testUpdateWorkflowSavesNothingWhenTheDefinitionHasErrors() {
            when(projectWorkflowFacade.getProjectWorkflow("wf-uuid-2")).thenReturn(buildDto("wf-uuid-2", 88L, 3));
            when(workflowValidatorFacade.validateWorkflow(eq(DEFINITION), any(), anyLong())).thenReturn(
                new WorkflowValidatorFacade.WorkflowValidationResult(
                    List.of("[slack_1] Field 'channel' is required"), List.of()));

            ProjectWorkflowTools tools = newTools(null, workflowValidatorFacade);

            assertThatCode(() -> tools.updateWorkflow("wf-uuid-2", DEFINITION, 3, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("Nothing was saved")
                .hasMessageContaining("[slack_1] Field 'channel' is required");

            verify(projectWorkflowFacade, never()).updateWorkflow(anyString(), anyString(), anyInt());
        }

        @Test
        void testUpdateWorkflowSavesADefinitionWhoseOnlyErrorIsAMissingConnection() {
            String missingConnection = "Missing connection for slack";

            when(projectWorkflowFacade.getProjectWorkflow("wf-uuid-2")).thenReturn(buildDto("wf-uuid-2", 88L, 3));
            when(projectWorkflowFacade.updateWorkflow("wf-uuid-2", DEFINITION, 3))
                .thenReturn(buildDto("wf-uuid-2", 88L, 4));
            when(workflowValidatorFacade.validateWorkflow(eq(DEFINITION), eq("wf-uuid-2"), anyLong())).thenReturn(
                new WorkflowValidatorFacade.WorkflowValidationResult(
                    List.of("[slack_1] " + missingConnection), List.of(),
                    List.of(
                        new WorkflowValidatorFacade.NodeValidationIssue(
                            "slack_1", null, WorkflowValidatorFacade.WorkflowIssueKind.MISSING_CONNECTION,
                            WorkflowValidatorFacade.WorkflowIssueSeverity.ERROR, missingConnection))));

            WorkflowSaveInfo workflowSaveInfo = newTools(null, workflowValidatorFacade)
                .updateWorkflow("wf-uuid-2", DEFINITION, 3, toolContext);

            assertThat(workflowSaveInfo.version()).isEqualTo(4);
        }

        @Test
        void testUpdateWorkflowSavesAValidDefinitionAndReturnsItsWarnings() {
            when(projectWorkflowFacade.getProjectWorkflow("wf-uuid-2")).thenReturn(buildDto("wf-uuid-2", 88L, 3));
            when(projectWorkflowFacade.updateWorkflow("wf-uuid-2", DEFINITION, 3))
                .thenReturn(buildDto("wf-uuid-2", 88L, 4));
            when(workflowValidatorFacade.validateWorkflow(eq(DEFINITION), any(), anyLong())).thenReturn(
                new WorkflowValidatorFacade.WorkflowValidationResult(
                    List.of(), List.of("[slack_1] Property 'text' is empty")));

            WorkflowSaveInfo workflowSaveInfo = newTools(null, workflowValidatorFacade)
                .updateWorkflow("wf-uuid-2", DEFINITION, 3, toolContext);

            assertThat(workflowSaveInfo.version()).isEqualTo(4);
            assertThat(workflowSaveInfo.warnings()).containsExactly("[slack_1] Property 'text' is empty");
        }
    }

    @Nested
    class PersistedWorkflowCaptureTest {

        @Test
        void testCreateProjectWorkflowCapturesPersistedIds() {
            List<Map<String, Object>> captures = Collections.synchronizedList(new ArrayList<>());

            when(toolContext.getContext())
                .thenReturn(Map.of("bytechef.workflowEditor.persistedWorkflows", captures));

            ProjectWorkflow projectWorkflow = buildProjectWorkflow(55L, 7L, "wf-uuid-1");

            when(projectWorkflowFacade.addWorkflow(7L, DEFINITION)).thenReturn(projectWorkflow);

            ProjectWorkflowTools tools = newTools();

            tools.createProjectWorkflow(7L, DEFINITION, toolContext);

            assertThat(captures).hasSize(1);
            assertThat(captures.get(0))
                .containsEntry("created", true)
                .containsEntry("workflowId", "wf-uuid-1")
                .containsEntry("projectId", 7L)
                .containsEntry("projectWorkflowId", 55L)
                .containsEntry("name", "My Flow");
        }

        @Test
        void testUpdateWorkflowCapturesPersistedIds() {
            List<Map<String, Object>> captures = Collections.synchronizedList(new ArrayList<>());

            when(toolContext.getContext())
                .thenReturn(Map.of("bytechef.workflowEditor.persistedWorkflows", captures));

            ProjectWorkflowDTO dto = buildDto("wf-uuid-2", 88L, 3);

            when(projectWorkflowFacade.getProjectWorkflow("wf-uuid-2")).thenReturn(dto);

            when(projectWorkflowFacade.updateWorkflow("wf-uuid-2", DEFINITION, 3))
                .thenReturn(buildDto("wf-uuid-2", 88L, 4));
            when(projectWorkflowFacade.getWorkflowProjectId("wf-uuid-2")).thenReturn(9L);

            ProjectWorkflowTools tools = newTools();

            tools.updateWorkflow("wf-uuid-2", DEFINITION, 3, toolContext);

            assertThat(captures).hasSize(1);
            assertThat(captures.get(0))
                .containsEntry("created", false)
                .containsEntry("workflowId", "wf-uuid-2")
                .containsEntry("projectId", 9L)
                .containsEntry("projectWorkflowId", 88L);
        }

        @Test
        void testUpdateWorkflowSucceedsWhenProjectIdLookupThrows() {
            ProjectWorkflowDTO dto = buildDto("wf-uuid-2", 88L, 3);

            when(projectWorkflowFacade.getProjectWorkflow("wf-uuid-2")).thenReturn(dto);
            when(projectWorkflowFacade.updateWorkflow("wf-uuid-2", DEFINITION, 3))
                .thenReturn(buildDto("wf-uuid-2", 88L, 4));
            when(projectWorkflowFacade.getWorkflowProjectId("wf-uuid-2"))
                .thenThrow(new RuntimeException("lookup boom"));

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.updateWorkflow("wf-uuid-2", DEFINITION, 3, toolContext))
                .doesNotThrowAnyException();
        }

        @Test
        void testCreateProjectWorkflowFillsTheEmptyWorkflowTheBuildRequestNamed() {
            List<Map<String, Object>> captures = Collections.synchronizedList(new ArrayList<>());

            when(toolContext.getContext()).thenReturn(Map.of(
                "bytechef.workflowEditor.persistedWorkflows", captures,
                "bytechef.workflowEditor.targetWorkflowId", "wf-target"));
            when(projectWorkflowFacade.getWorkflowProjectId("wf-target")).thenReturn(7L);
            when(projectWorkflowFacade.getProjectWorkflow("wf-target")).thenReturn(buildDto("wf-target", 66L, 2));
            when(projectWorkflowFacade.updateWorkflow("wf-target", DEFINITION, 2))
                .thenReturn(buildDto("wf-target", 66L, 3));

            ProjectWorkflowInfo projectWorkflowInfo = newTools().createProjectWorkflow(7L, DEFINITION, toolContext);

            assertThat(projectWorkflowInfo.workflowId()).isEqualTo("wf-target");
            assertThat(captures).hasSize(1);
            assertThat(captures.get(0))
                .containsEntry("created", false)
                .containsEntry("workflowId", "wf-target");

            verify(projectWorkflowFacade, never()).addWorkflow(anyLong(), anyString());
        }

        @Test
        void testCreateProjectWorkflowLeavesANamedWorkflowThatAlreadyHasTasks() {
            Workflow workflow = new Workflow(
                "wf-target", """
                    {"label": "Built", "tasks": [{"name": "logger_1", "type": "logger/v1/info", "parameters": {}}]}""",
                Workflow.Format.JSON);

            when(toolContext.getContext()).thenReturn(Map.of("bytechef.workflowEditor.targetWorkflowId", "wf-target"));
            when(projectWorkflowFacade.getWorkflowProjectId("wf-target")).thenReturn(7L);
            when(projectWorkflowFacade.getProjectWorkflow("wf-target"))
                .thenReturn(new ProjectWorkflowDTO(workflow, new ProjectWorkflow(66L), false));
            when(projectWorkflowFacade.addWorkflow(7L, DEFINITION)).thenReturn(buildProjectWorkflow(55L, 7L, "wf-new"));

            ProjectWorkflowInfo projectWorkflowInfo = newTools().createProjectWorkflow(7L, DEFINITION, toolContext);

            assertThat(projectWorkflowInfo.workflowId()).isEqualTo("wf-new");

            verify(projectWorkflowFacade, never()).updateWorkflow(anyString(), anyString(), anyInt());
        }

        @Test
        void testCreateProjectWorkflowIgnoresANamedWorkflowOfAnotherProject() {
            when(toolContext.getContext()).thenReturn(Map.of("bytechef.workflowEditor.targetWorkflowId", "wf-target"));
            when(projectWorkflowFacade.getWorkflowProjectId("wf-target")).thenReturn(8L);
            when(projectWorkflowFacade.addWorkflow(7L, DEFINITION)).thenReturn(buildProjectWorkflow(55L, 7L, "wf-new"));

            ProjectWorkflowInfo projectWorkflowInfo = newTools().createProjectWorkflow(7L, DEFINITION, toolContext);

            assertThat(projectWorkflowInfo.workflowId()).isEqualTo("wf-new");

            verify(projectWorkflowFacade, never()).getProjectWorkflow("wf-target");
        }
    }

    @Nested
    class WorkflowArtifactRecorderTest {

        @Test
        void testCreateProjectWorkflowRecordsCreatedArtifact() {
            WorkflowArtifactRecorder workflowArtifactRecorder = mock(WorkflowArtifactRecorder.class);

            when(projectWorkflowFacade.addWorkflow(5L, DEFINITION))
                .thenReturn(buildProjectWorkflow(11L, 5L, "wf-1"));

            newTools(workflowArtifactRecorder).createProjectWorkflow(5L, DEFINITION, toolContext);

            verify(workflowArtifactRecorder).recordWorkflowArtifact(toolContext, true, "wf-1", 5L, 11L, "My Flow");
        }

        @Test
        void testUpdateWorkflowRecordsUpdatedArtifact() {
            WorkflowArtifactRecorder workflowArtifactRecorder = mock(WorkflowArtifactRecorder.class);

            stubUpdate();

            newTools(workflowArtifactRecorder).updateWorkflow("wf-1", DEFINITION, 3, toolContext);

            verify(workflowArtifactRecorder).recordWorkflowArtifact(toolContext, false, "wf-1", 5L, 11L, "My Flow");
        }

        @Test
        void testRecorderFailureDoesNotFailUpdateWorkflow() {
            WorkflowArtifactRecorder workflowArtifactRecorder = mock(WorkflowArtifactRecorder.class);

            doThrow(new IllegalStateException("db down")).when(workflowArtifactRecorder)
                .recordWorkflowArtifact(any(), anyBoolean(), any(), anyLong(), any(), any());

            stubUpdate();

            WorkflowSaveInfo workflowSaveInfo = newTools(workflowArtifactRecorder)
                .updateWorkflow("wf-1", DEFINITION, 3, toolContext);

            assertThat(workflowSaveInfo).isNotNull();

            verify(workflowArtifactRecorder).recordWorkflowArtifact(toolContext, false, "wf-1", 5L, 11L, "My Flow");
        }

        @Test
        void testRecorderFailureDoesNotFailCreateProjectWorkflow() {
            WorkflowArtifactRecorder workflowArtifactRecorder = mock(WorkflowArtifactRecorder.class);

            doThrow(new IllegalStateException("db down")).when(workflowArtifactRecorder)
                .recordWorkflowArtifact(any(), anyBoolean(), any(), anyLong(), any(), any());

            when(projectWorkflowFacade.addWorkflow(5L, DEFINITION))
                .thenReturn(buildProjectWorkflow(11L, 5L, "wf-1"));

            ProjectWorkflowInfo projectWorkflowInfo = newTools(workflowArtifactRecorder)
                .createProjectWorkflow(5L, DEFINITION, toolContext);

            assertThat(projectWorkflowInfo).isNotNull();

            verify(workflowArtifactRecorder).recordWorkflowArtifact(toolContext, true, "wf-1", 5L, 11L, "My Flow");
        }

        @Test
        void testNoRecorderBeanIsIgnored() {
            stubUpdate();

            ProjectWorkflowTools tools = newTools(null);

            assertThatCode(() -> tools.updateWorkflow("wf-1", DEFINITION, 3, toolContext))
                .doesNotThrowAnyException();
        }

        private void stubUpdate() {
            when(projectWorkflowFacade.getProjectWorkflow("wf-1")).thenReturn(buildDto("wf-1", 11L, 3));
            when(projectWorkflowFacade.updateWorkflow("wf-1", DEFINITION, 3))
                .thenReturn(buildDto("wf-1", 11L, 4));
            when(projectWorkflowFacade.getWorkflowProjectId("wf-1")).thenReturn(5L);
        }
    }

    @Nested
    class UpdateWorkflowTest {

        @Test
        void testUpdateWorkflowPassesTheVersionTheAgentReadAndReturnsTheNewVersion() {
            when(projectWorkflowFacade.getProjectWorkflow("wf-uuid-2")).thenReturn(buildDto("wf-uuid-2", 88L, 3));
            when(projectWorkflowFacade.updateWorkflow("wf-uuid-2", DEFINITION, 3))
                .thenReturn(buildDto("wf-uuid-2", 88L, 4));

            WorkflowSaveInfo workflowSaveInfo = newTools().updateWorkflow("wf-uuid-2", DEFINITION, 3, toolContext);

            assertThat(workflowSaveInfo.version()).isEqualTo(4);
        }

        @Test
        void testUpdateWorkflowRejectsAStaleVersion() {
            when(projectWorkflowFacade.getProjectWorkflow("wf-uuid-2")).thenReturn(buildDto("wf-uuid-2", 88L, 5));

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.updateWorkflow("wf-uuid-2", DEFINITION, 3, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("getWorkflow");

            verify(projectWorkflowFacade, never()).updateWorkflow(any(), any(), anyInt());
        }
    }

    @Nested
    class ScopedProjectTest {

        private static final long OWN_PROJECT_ID = 1114L;
        private static final String OWN_WORKFLOW_ID = "own-workflow";
        private static final long VENDOR_PROJECT_ID = 7L;
        private static final String VENDOR_WORKFLOW_ID = "vendor-workflow";

        @Test
        void testScopedRunCannotUpdateAWorkflowOfAnotherProject() {
            stubWorkflowProject(VENDOR_WORKFLOW_ID, VENDOR_PROJECT_ID);

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.updateWorkflow(VENDOR_WORKFLOW_ID, DEFINITION, 1, scopedToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("was not found in this chat's project");

            verify(projectWorkflowFacade, never()).getProjectWorkflow(VENDOR_WORKFLOW_ID);
            verify(projectWorkflowFacade, never()).updateWorkflow(any(), any(), anyInt());
        }

        @Test
        void testScopedRunCanUpdateAWorkflowOfItsOwnProject() {
            stubWorkflowProject(OWN_WORKFLOW_ID, OWN_PROJECT_ID);

            when(projectWorkflowFacade.getProjectWorkflow(OWN_WORKFLOW_ID))
                .thenReturn(buildDto(OWN_WORKFLOW_ID, 1L, 1));
            when(projectWorkflowFacade.updateWorkflow(OWN_WORKFLOW_ID, DEFINITION, 1))
                .thenReturn(buildDto(OWN_WORKFLOW_ID, 1L, 2));

            WorkflowSaveInfo workflowSaveInfo =
                newTools().updateWorkflow(OWN_WORKFLOW_ID, DEFINITION, 1, scopedToolContext());

            assertThat(workflowSaveInfo.version()).isEqualTo(2);
        }

        @Test
        void testScopedRunCannotReadOrDeleteAWorkflowOfAnotherProject() {
            stubWorkflowProject(VENDOR_WORKFLOW_ID, VENDOR_PROJECT_ID);

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.getWorkflow(VENDOR_WORKFLOW_ID, scopedToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("was not found in this chat's project");
            assertThatCode(() -> tools.deleteWorkflow(VENDOR_WORKFLOW_ID, scopedToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("was not found in this chat's project");
            assertThatCode(
                () -> tools.saveWorkflowTestConnection(VENDOR_WORKFLOW_ID, "slack_1", "slack", 1L, scopedToolContext()))
                    .isInstanceOf(ExecutionException.class)
                    .hasMessageContaining("was not found in this chat's project");

            verify(projectWorkflowFacade, never()).getProjectWorkflow(VENDOR_WORKFLOW_ID);
            verify(projectWorkflowFacade, never()).deleteWorkflow(any());
        }

        @Test
        void testScopedRunCanReadListAndDeleteInItsOwnProject() {
            stubWorkflowProject(OWN_WORKFLOW_ID, OWN_PROJECT_ID);

            when(projectWorkflowFacade.getProjectWorkflow(OWN_WORKFLOW_ID))
                .thenReturn(buildDto(OWN_WORKFLOW_ID, 1L, 1));
            when(projectWorkflowFacade.getProjectWorkflows(OWN_PROJECT_ID))
                .thenReturn(List.of(buildDto(OWN_WORKFLOW_ID, 1L, 1)));

            ProjectWorkflowTools tools = newTools();

            assertThat(tools.getWorkflow(OWN_WORKFLOW_ID, scopedToolContext())
                .version()).isEqualTo(1);
            assertThat(tools.listWorkflows(OWN_PROJECT_ID, null, scopedToolContext())).hasSize(1);

            tools.deleteWorkflow(OWN_WORKFLOW_ID, scopedToolContext());

            verify(projectWorkflowFacade).deleteWorkflow(any());
        }

        @Test
        void testScopedRunTreatsAnUnknownWorkflowAsNotFound() {
            when(projectWorkflowService.getWorkflowProjectWorkflow("missing"))
                .thenThrow(new IllegalArgumentException("not found"));

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.updateWorkflow("missing", DEFINITION, 1, scopedToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("was not found in this chat's project");
        }

        @Test
        void testScopedRunCannotListOrCreateInAnotherProject() {
            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.listWorkflows(VENDOR_PROJECT_ID, null, scopedToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessage(ScopedProjectGuard.OUT_OF_SCOPE_MESSAGE);
            assertThatCode(() -> tools.createProjectWorkflow(VENDOR_PROJECT_ID, DEFINITION, scopedToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessage(ScopedProjectGuard.OUT_OF_SCOPE_MESSAGE);

            verify(projectWorkflowFacade, never()).getProjectWorkflows(anyLong());
            verify(projectWorkflowFacade, never()).addWorkflow(anyLong(), any());
        }

        @Test
        void testScopedRunSearchesOnlyItsOwnProject() {
            when(projectWorkflowFacade.getProjectWorkflows(OWN_PROJECT_ID)).thenReturn(List.of());

            newTools().searchWorkflows("standup", null, WORKSPACE_ID, scopedToolContext());

            verify(projectWorkflowFacade).getProjectWorkflows(OWN_PROJECT_ID);
            verify(projectFacade, never()).getWorkspaceProjectWorkflows(anyLong());
        }

        @Test
        void testScopedRunCannotSearchAnotherProject() {
            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.searchWorkflows("standup", VENDOR_PROJECT_ID, null, scopedToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessage(ScopedProjectGuard.OUT_OF_SCOPE_MESSAGE);
        }

        @Test
        void testScopedRunCannotBindAConnectionTheUserIsNotEntitledTo() {
            stubWorkflowProject(OWN_WORKFLOW_ID, OWN_PROJECT_ID);

            ToolContext toolContext = scopedToolContext();

            when(scopedConnectionGuard.fetchDenialMessage(null, 900L, toolContext))
                .thenReturn(ScopedConnectionGuard.NOT_ENTITLED_MESSAGE);

            ProjectWorkflowTools tools = newTools();

            assertThatCode(
                () -> tools.saveWorkflowTestConnection(OWN_WORKFLOW_ID, "slack_1", "slack", 900L, toolContext))
                    .isInstanceOf(ExecutionException.class)
                    .hasMessage(ScopedConnectionGuard.NOT_ENTITLED_MESSAGE);

            verify(projectWorkflowFacade, never()).saveWorkflowTestConnection(any(), any(), any(), anyLong(),
                anyLong());
            verify(workspaceConnectionFacade, never()).getConnections(anyLong(), any(), any(), any(), any());
        }

        @Test
        void testScopedRunBindsAConnectionTheUserIsEntitledToWithoutTheWorkspaceCheck() {
            stubWorkflowProject(OWN_WORKFLOW_ID, OWN_PROJECT_ID);

            ToolContext toolContext = scopedToolContext();

            newTools().saveWorkflowTestConnection(OWN_WORKFLOW_ID, "slack_1", "slack", 42L, toolContext);

            verify(scopedConnectionGuard).fetchDenialMessage(null, 42L, toolContext);
            verify(projectWorkflowFacade).saveWorkflowTestConnection(OWN_WORKFLOW_ID, "slack_1", "slack", 42L, 0L);
            verify(workspaceConnectionFacade, never()).getConnections(anyLong(), any(), any(), any(), any());
        }

        @Test
        void testUnscopedRunIsNotRestrictedByTheGuard() {
            when(projectWorkflowFacade.getProjectWorkflow(VENDOR_WORKFLOW_ID))
                .thenReturn(buildDto(VENDOR_WORKFLOW_ID, 501L, 1));
            when(projectWorkflowFacade.updateWorkflow(VENDOR_WORKFLOW_ID, DEFINITION, 1))
                .thenReturn(buildDto(VENDOR_WORKFLOW_ID, 501L, 2));

            newTools().updateWorkflow(VENDOR_WORKFLOW_ID, DEFINITION, 1, new ToolContext(Map.of()));

            verify(projectWorkflowFacade).updateWorkflow(VENDOR_WORKFLOW_ID, DEFINITION, 1);
            verify(projectWorkflowService, never()).getWorkflowProjectWorkflow(any());
        }

        private ToolContext scopedToolContext() {
            Map<String, Object> context = new HashMap<>(
                AgentToolInvocationContext.builder()
                    .environmentId(0L)
                    .tenantId("000001")
                    .authentication(new UsernamePasswordAuthenticationToken("1234567890", null, List.of()))
                    .build()
                    .toToolContext());

            context.put(ScopedProjectToolContext.TOOL_CONTEXT_SCOPED_PROJECT_ID_KEY, OWN_PROJECT_ID);

            return new ToolContext(context);
        }

        private void stubWorkflowProject(String workflowId, long projectId) {
            when(projectWorkflowService.getWorkflowProjectWorkflow(workflowId))
                .thenReturn(buildProjectWorkflow(1L, projectId, workflowId));
        }
    }

    @Nested
    class AllowedComponentNamesTest {

        private static final long OWN_PROJECT_ID = 1114L;
        private static final String OWN_WORKFLOW_ID = "own-workflow";

        @Test
        void testUpdateWorkflowRejectsATaskOfAComponentOutsideTheAllowList() {
            stubOwnWorkflowProject();
            stubComponentDefinitions(Map.of("salesforce", true));

            String definition = """
                {
                    "label": "Flow",
                    "triggers": [{"name": "trigger_1", "type": "manual/v1/manual"}],
                    "tasks": [
                        {"name": "slack_1", "type": "slack/v1/sendMessage", "parameters": {}},
                        {"name": "salesforce_1", "type": "salesforce/v1/createRecord", "parameters": {}}
                    ]
                }""";

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.updateWorkflow(OWN_WORKFLOW_ID, definition, 1, allowListToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessage("This chat can't use salesforce: it isn't one of the integrations available to this user.");

            verify(projectWorkflowFacade, never()).updateWorkflow(any(), any(), anyInt());
        }

        @Test
        void testUpdateWorkflowRejectsATaskNestedInATaskDispatcher() {
            stubOwnWorkflowProject();
            stubComponentDefinitions(Map.of("salesforce", true));

            String definition = """
                {
                    "label": "Flow",
                    "tasks": [
                        {
                            "name": "condition_1",
                            "type": "condition/v1",
                            "parameters": {
                                "caseTrue": [
                                    {
                                        "name": "loop_1",
                                        "type": "loop/v1",
                                        "parameters": {
                                            "iteratee": [
                                                {"name": "salesforce_1", "type": "salesforce/v1/createRecord"}
                                            ]
                                        }
                                    }
                                ],
                                "caseFalse": []
                            }
                        }
                    ]
                }""";

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.updateWorkflow(OWN_WORKFLOW_ID, definition, 1, allowListToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("This chat can't use salesforce");

            verify(projectWorkflowFacade, never()).updateWorkflow(any(), any(), anyInt());
        }

        @Test
        void testUpdateWorkflowRejectsAClusterElementOutsideTheAllowList() {
            stubOwnWorkflowProject();
            stubComponentDefinitions(Map.of("aiAgent", false, "openAi", true, "hubspot", true));

            String definition = """
                {
                    "label": "Flow",
                    "tasks": [
                        {
                            "name": "aiAgent_1",
                            "type": "aiAgent/v1/chat",
                            "clusterElements": {
                                "model": {"name": "openAi_1", "type": "openAi/v1/model"},
                                "tools": [{"name": "hubspot_1", "type": "hubspot/v1/createContact"}]
                            }
                        }
                    ]
                }""";

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.updateWorkflow(OWN_WORKFLOW_ID, definition, 1, allowListToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessage(
                    "This chat can't use openAi, hubspot: they aren't among the integrations available to this user.");

            verify(projectWorkflowFacade, never()).updateWorkflow(any(), any(), anyInt());
        }

        @Test
        void testUpdateWorkflowAcceptsTaskDispatchersAndConnectionFreeComponents() {
            stubOwnWorkflowProject();
            stubComponentDefinitions(Map.of("logger", false));

            String definition = """
                {
                    "label": "Flow",
                    "tasks": [
                        {
                            "name": "condition_1",
                            "type": "condition/v1",
                            "parameters": {
                                "caseTrue": [
                                    {"name": "slack_1", "type": "slack/v1/sendMessage"},
                                    {"name": "logger_1", "type": "logger/v1/info"}
                                ]
                            }
                        },
                        {"name": "loop_1", "type": "loop/v1", "parameters": {"iteratee": []}}
                    ]
                }""";

            when(projectWorkflowFacade.getProjectWorkflow(OWN_WORKFLOW_ID))
                .thenReturn(buildDto(OWN_WORKFLOW_ID, 1L, 1));
            when(projectWorkflowFacade.updateWorkflow(OWN_WORKFLOW_ID, definition, 1))
                .thenReturn(buildDto(OWN_WORKFLOW_ID, 1L, 2));

            newTools().updateWorkflow(OWN_WORKFLOW_ID, definition, 1, allowListToolContext());

            verify(projectWorkflowFacade).updateWorkflow(OWN_WORKFLOW_ID, definition, 1);
        }

        @Test
        void testUpdateWorkflowWithoutTheAllowListIsUnaffected() {
            stubOwnWorkflowProject();

            String definition = """
                {"label": "Flow", "tasks": [{"name": "salesforce_1", "type": "salesforce/v1/createRecord"}]}""";

            when(projectWorkflowFacade.getProjectWorkflow(OWN_WORKFLOW_ID))
                .thenReturn(buildDto(OWN_WORKFLOW_ID, 1L, 1));
            when(projectWorkflowFacade.updateWorkflow(OWN_WORKFLOW_ID, definition, 1))
                .thenReturn(buildDto(OWN_WORKFLOW_ID, 1L, 2));

            newTools().updateWorkflow(OWN_WORKFLOW_ID, definition, 1, scopedToolContext(null));

            verify(projectWorkflowFacade).updateWorkflow(OWN_WORKFLOW_ID, definition, 1);
            verifyNoInteractions(componentDefinitionFacade);
        }

        @Test
        void testCreateProjectWorkflowRejectsAComponentOutsideTheAllowList() {
            stubComponentDefinitions(Map.of("salesforce", true));

            String definition = """
                {"label": "Flow", "tasks": [{"name": "salesforce_1", "type": "salesforce/v1/createRecord"}]}""";

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.createProjectWorkflow(OWN_PROJECT_ID, definition, allowListToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("This chat can't use salesforce");

            verify(projectWorkflowFacade, never()).addWorkflow(anyLong(), anyString());
        }

        private ToolContext allowListToolContext() {
            return scopedToolContext(Set.of("slack"));
        }

        private ToolContext scopedToolContext(Set<String> allowedComponentNames) {
            Map<String, Object> context = new HashMap<>(
                AgentToolInvocationContext.builder()
                    .environmentId(0L)
                    .tenantId("000001")
                    .authentication(new UsernamePasswordAuthenticationToken("1234567890", null, List.of()))
                    .build()
                    .toToolContext());

            context.put(ScopedProjectToolContext.TOOL_CONTEXT_SCOPED_PROJECT_ID_KEY, OWN_PROJECT_ID);

            if (allowedComponentNames != null) {
                context.put(
                    AllowedComponentNamesUtils.TOOL_CONTEXT_ALLOWED_COMPONENT_NAMES_KEY, allowedComponentNames);
            }

            return new ToolContext(context);
        }

        private void stubComponentDefinitions(Map<String, Boolean> connectionRequiredByComponentName) {
            Map<String, ComponentDefinition> componentDefinitions = new HashMap<>();

            connectionRequiredByComponentName.forEach((componentName, connectionRequired) -> {
                ComponentDefinition componentDefinition = mock(
                    ComponentDefinition.class, withSettings().strictness(Strictness.LENIENT));

                when(componentDefinition.isConnectionRequired()).thenReturn(connectionRequired);

                componentDefinitions.put(componentName, componentDefinition);
            });

            when(componentDefinitionFacade.fetchComponentDefinition(anyString(), isNull()))
                .thenAnswer(invocation -> Optional.ofNullable(componentDefinitions.get(invocation.getArgument(0))));
        }

        private void stubOwnWorkflowProject() {
            when(projectWorkflowService.getWorkflowProjectWorkflow(OWN_WORKFLOW_ID))
                .thenReturn(buildProjectWorkflow(1L, OWN_PROJECT_ID, OWN_WORKFLOW_ID));
        }
    }

    @Nested
    class ListWorkflowsTest {

        @Test
        void testListWorkflowsWithoutAQueryListsEveryWorkflowOfTheProject() {
            when(projectWorkflowFacade.getProjectWorkflows(10L)).thenReturn(
                List.of(buildDto("wf-1", "Send welcome email", null), buildDto("wf-2", "Sync contacts", null)));

            List<WorkflowInfo> result = newTools().listWorkflows(10L, null, toolContext);

            assertThat(result).extracting(WorkflowInfo::name)
                .containsExactly("Send welcome email", "Sync contacts");
        }

        @Test
        void testListWorkflowsWithAQueryReturnsOnlyWorkflowsMatchingNameOrDescription() {
            when(projectWorkflowFacade.getProjectWorkflows(10L)).thenReturn(
                List.of(
                    buildDto("wf-1", "Send welcome email", null), buildDto("wf-2", "Sync contacts", null),
                    buildDto("wf-3", "Nightly job", "Emails the daily digest")));

            List<WorkflowInfo> result = newTools().listWorkflows(10L, " EMAIL ", toolContext);

            assertThat(result).extracting(WorkflowInfo::name)
                .containsExactly("Send welcome email", "Nightly job");
        }

        @Test
        void testListWorkflowsWithABlankQueryListsEveryWorkflowOfTheProject() {
            when(projectWorkflowFacade.getProjectWorkflows(10L)).thenReturn(
                List.of(buildDto("wf-1", "Send welcome email", null), buildDto("wf-2", "Sync contacts", null)));

            assertThat(newTools().listWorkflows(10L, "  ", toolContext)).hasSize(2);
        }
    }

    @Nested
    class SearchWorkflowsTest {

        @Test
        void testSearchWorkflowsSearchesTheContextWorkspaceWhenNoProjectIsGiven() {
            when(toolContext.getContext())
                .thenReturn(Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID));
            when(projectFacade.getWorkspaceProjectWorkflows(WORKSPACE_ID)).thenReturn(List.of(buildDto("wf-1", 1L, 1)));

            List<WorkflowInfo> result = newTools().searchWorkflows("flow", null, null, toolContext);

            assertThat(result).hasSize(1);
        }

        @Test
        void testSearchWorkflowsSearchesTheGivenProject() {
            when(projectWorkflowFacade.getProjectWorkflows(10L)).thenReturn(List.of(buildDto("wf-1", 1L, 1)));

            List<WorkflowInfo> result = newTools().searchWorkflows("flow", 10L, null, toolContext);

            assertThat(result).hasSize(1);

            verify(projectFacade, never()).getWorkspaceProjectWorkflows(anyLong());
        }

        @Test
        void testSearchWorkflowsSearchesAGivenProjectTheCallerCanView() {
            PermissionService permissionService = mock(PermissionService.class);

            when(permissionService.hasWorkspaceScopeForProject(10L, ScopedProjectGuard.WORKFLOW_VIEW))
                .thenReturn(true);
            when(projectWorkflowFacade.getProjectWorkflows(10L)).thenReturn(List.of(buildDto("wf-1", 1L, 1)));

            List<WorkflowInfo> result =
                newToolsCheckedBy(permissionService).searchWorkflows("flow", 10L, null, toolContext);

            assertThat(result).hasSize(1);
        }

        @Test
        void testSearchWorkflowsRejectsAGivenProjectTheCallerCannotView() {
            PermissionService permissionService = mock(PermissionService.class);

            when(permissionService.hasWorkspaceScopeForProject(10L, ScopedProjectGuard.WORKFLOW_VIEW))
                .thenReturn(false);

            ProjectWorkflowTools tools = newToolsCheckedBy(permissionService);

            assertThatCode(() -> tools.searchWorkflows("flow", 10L, null, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("Project 10 was not found or you do not have access to it.");

            verify(projectWorkflowFacade, never()).getProjectWorkflows(anyLong());
            verify(projectFacade, never()).getWorkspaceProjectWorkflows(anyLong());
        }

        @Test
        void testSearchWorkflowsRequiresAProjectOrAWorkspace() {
            when(toolContext.getContext()).thenReturn(Map.of());

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.searchWorkflows("flow", null, null, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("listWorkspaces");

            verify(projectFacade, never()).getWorkspaceProjectWorkflows(anyLong());
        }

        @Test
        void testSearchWorkflowsRejectsAWorkspaceTheCallerCannotAccess() {
            when(toolContext.getContext()).thenReturn(Map.of());
            when(workspaceFacade.getMyWorkspaces()).thenReturn(List.of(new Workspace(WORKSPACE_ID, "Workspace")));

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.searchWorkflows("flow", null, 99L, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("Workspace 99 is not accessible to the caller.");

            verify(projectFacade, never()).getWorkspaceProjectWorkflows(anyLong());
        }

        @Test
        void testSearchWorkflowsRejectsAWorkspaceOtherThanTheChatWorkspace() {
            when(toolContext.getContext())
                .thenReturn(Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, WORKSPACE_ID));

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.searchWorkflows("flow", null, 99L, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("this conversation is scoped to");

            verify(projectFacade, never()).getWorkspaceProjectWorkflows(anyLong());
        }
    }

    @Nested
    class SaveWorkflowTestConnectionTest {

        @Test
        void testSaveWorkflowTestConnectionDelegatesToFacadeWithAutomationToolEnvironment() {
            mockWorkflowWorkspace("wf-uuid-1", 7L, WORKSPACE_ID);

            when(toolContext.getContext())
                .thenReturn(Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 2L));

            mockConnections(WORKSPACE_ID, 2L, 1107L);

            ProjectWorkflowTools tools = newTools();

            String result = tools.saveWorkflowTestConnection(
                "wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, toolContext);

            verify(projectWorkflowFacade)
                .saveWorkflowTestConnection("wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, 2L);
            assertThat(result).contains("1107", "sendChannelMessage_1");
        }

        @Test
        void testSaveWorkflowTestConnectionFallsBackToAgentToolEnvironmentKey() {
            mockWorkflowWorkspace("wf-uuid-1", 7L, WORKSPACE_ID);

            when(toolContext.getContext()).thenReturn(Map.of("bytechef.agentTool.environmentId", 1L));

            mockConnections(WORKSPACE_ID, 1L, 1107L);

            ProjectWorkflowTools tools = newTools();

            tools.saveWorkflowTestConnection("wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, toolContext);

            verify(projectWorkflowFacade)
                .saveWorkflowTestConnection("wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, 1L);
        }

        @Test
        void testSaveWorkflowTestConnectionFailsWhenNoEnvironmentIsResolvable() {
            when(toolContext.getContext()).thenReturn(Map.of());

            ProjectWorkflowTools tools = newTools();

            assertThatCode(
                () -> tools.saveWorkflowTestConnection(
                    "wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, toolContext))
                        .isInstanceOf(ExecutionException.class);

            verify(projectWorkflowFacade, never())
                .saveWorkflowTestConnection(any(), any(), any(), anyLong(), anyLong());
        }

        @Test
        void testSaveWorkflowTestConnectionRejectsAConnectionOutsideTheWorkflowWorkspace() {
            mockWorkflowWorkspace("wf-uuid-1", 7L, WORKSPACE_ID);

            when(toolContext.getContext())
                .thenReturn(Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 2L));

            mockConnections(WORKSPACE_ID, 2L, 5L);

            ProjectWorkflowTools tools = newTools();

            assertThatCode(
                () -> tools.saveWorkflowTestConnection(
                    "wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, toolContext))
                        .isInstanceOf(ExecutionException.class)
                        .hasMessageContaining("Connection 1107 not found");

            verify(projectWorkflowFacade, never())
                .saveWorkflowTestConnection(any(), any(), any(), anyLong(), anyLong());
        }
    }

    private void mockConnections(long workspaceId, long environmentId, long connectionId) {
        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .id(connectionId)
            .build();

        when(workspaceConnectionFacade.getConnections(workspaceId, null, null, environmentId, null))
            .thenReturn(List.of(connectionDTO));
    }

    private void mockWorkflowWorkspace(String workflowId, long projectId, long workspaceId) {
        when(projectWorkflowFacade.getWorkflowProjectId(workflowId)).thenReturn(projectId);
        when(projectFacade.getProject(projectId)).thenReturn(
            ProjectDTO.builder()
                .id(projectId)
                .workspaceId(workspaceId)
                .build());
    }

    private ProjectWorkflowTools newTools() {
        return newTools(null);
    }

    @SuppressWarnings("unchecked")
    private ProjectWorkflowTools newToolsCheckedBy(PermissionService permissionService) {
        return new ProjectWorkflowTools(
            componentDefinitionFacade, projectFacade, projectWorkflowFacade,
            new ScopedProjectGuard(permissionService, projectWorkflowService, scopedConnectionGuard),
            mock(ObjectProvider.class), mock(ObjectProvider.class), workspaceConnectionFacade, workspaceFacade);
    }

    private ProjectWorkflowTools newTools(WorkflowArtifactRecorder workflowArtifactRecorder) {
        return newTools(workflowArtifactRecorder, null);
    }

    @SuppressWarnings("unchecked")
    private ProjectWorkflowTools newTools(
        WorkflowArtifactRecorder workflowArtifactRecorder, WorkflowValidatorFacade workflowValidatorFacade) {

        ObjectProvider<WorkflowArtifactRecorder> workflowArtifactRecorderProvider = mock(ObjectProvider.class);

        if (workflowArtifactRecorder != null) {
            when(workflowArtifactRecorderProvider.getIfAvailable()).thenReturn(workflowArtifactRecorder);
        }

        ObjectProvider<WorkflowValidatorFacade> workflowValidatorFacadeProvider = mock(ObjectProvider.class);

        if (workflowValidatorFacade != null) {
            when(workflowValidatorFacadeProvider.getIfAvailable()).thenReturn(workflowValidatorFacade);
        }

        return new ProjectWorkflowTools(
            componentDefinitionFacade, projectFacade, projectWorkflowFacade,
            new ScopedProjectGuard(new GrantingPermissionService(), projectWorkflowService, scopedConnectionGuard),
            workflowArtifactRecorderProvider,
            workflowValidatorFacadeProvider, workspaceConnectionFacade, workspaceFacade);
    }

    private static ProjectWorkflow buildProjectWorkflow(long id, long projectId, String workflowId) {
        ProjectWorkflow projectWorkflow = new ProjectWorkflow(projectId, 1, workflowId);

        ReflectionTestUtils.setField(projectWorkflow, "id", id);

        return projectWorkflow;
    }

    private static ProjectWorkflowDTO buildDto(String workflowUuid, String label, @Nullable String description) {
        Map<String, Object> definition = new HashMap<>();

        definition.put("label", label);
        definition.put("tasks", List.of());

        if (description != null) {
            definition.put("description", description);
        }

        Workflow workflow = new Workflow(workflowUuid, JsonUtils.write(definition), Workflow.Format.JSON);

        return new ProjectWorkflowDTO(workflow, new ProjectWorkflow(1L), false);
    }

    private static ProjectWorkflowDTO buildDto(String workflowUuid, long projectWorkflowId, int version) {
        Workflow workflow = new Workflow(workflowUuid, WORKFLOW_DTO_DEFINITION, Workflow.Format.JSON);

        workflow.setVersion(version);

        ProjectWorkflow projectWorkflow = new ProjectWorkflow(projectWorkflowId);

        return new ProjectWorkflowDTO(workflow, projectWorkflow, false);
    }
}
