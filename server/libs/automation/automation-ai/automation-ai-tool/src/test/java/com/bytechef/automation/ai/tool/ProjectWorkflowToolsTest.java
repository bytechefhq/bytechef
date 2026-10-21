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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.tool.ScopedConnectionGuard;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ai.copilot.tool.context.ScopedProjectToolContext;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.automation.ai.tool.model.WorkflowInfo;
import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.dto.ProjectWorkflowDTO;
import com.bytechef.automation.configuration.facade.ProjectWorkflowFacade;
import com.bytechef.automation.configuration.ownership.ProjectOwner;
import com.bytechef.automation.configuration.service.ProjectOwnershipService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.ai.tool.util.AllowedComponentNamesUtils;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    private static final long FOREIGN_WORKSPACE_ID = 2L;
    private static final String NOT_IN_PROJECT_MESSAGE = "was not found in this chat's project";
    private static final long WORKSPACE_ID = 1L;

    @Mock
    private ComponentDefinitionService componentDefinitionService;

    @Mock
    private ProjectOwnershipService projectOwnershipService;

    @Mock
    private ProjectService projectService;

    @Mock
    private ProjectWorkflowFacade projectWorkflowFacade;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @Mock
    private ObjectProvider<WorkflowTestConfigurationFacade> testConfigurationFacadeProvider;

    @Mock
    private WorkflowTestConfigurationFacade workflowTestConfigurationFacade;

    @Mock
    private ToolContext toolContext;

    @Mock
    private WorkspaceScopeResolver workspaceScopeResolver;

    @Mock
    private ScopedConnectionGuard scopedConnectionGuard;

    @Nested
    class PersistedWorkflowCaptureTest {

        @Test
        void testCreateProjectWorkflowCapturesPersistedIds() {
            List<Map<String, Object>> captures = Collections.synchronizedList(new ArrayList<>());

            when(toolContext.getContext())
                .thenReturn(Map.of("bytechef.workflowEditor.persistedWorkflows", captures));

            resolveWorkspace(WORKSPACE_ID);
            mockProject(7L, WORKSPACE_ID);

            ProjectWorkflow projectWorkflow = buildProjectWorkflow(55L, 7L, "wf-uuid-1");

            when(projectWorkflowFacade.addWorkflow(7L, DEFINITION)).thenReturn(projectWorkflow);

            ProjectWorkflowTools tools = newTools();

            tools.createProjectWorkflow(7L, DEFINITION, null, toolContext);

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

            resolveWorkspace(WORKSPACE_ID);
            mockWorkflow("wf-uuid-2", 9L, WORKSPACE_ID);

            ProjectWorkflowDTO dto = buildDto("wf-uuid-2", 88L, 3);

            when(projectWorkflowFacade.getProjectWorkflow("wf-uuid-2")).thenReturn(dto);

            ProjectWorkflow projectWorkflow = buildProjectWorkflow(88L, 9L, "wf-uuid-2");

            when(projectWorkflowService.getProjectWorkflow(88L)).thenReturn(projectWorkflow);

            ProjectWorkflowTools tools = newTools();

            tools.updateWorkflow("wf-uuid-2", DEFINITION, null, toolContext);

            assertThat(captures).hasSize(1);
            assertThat(captures.get(0))
                .containsEntry("created", false)
                .containsEntry("workflowId", "wf-uuid-2")
                .containsEntry("projectId", 9L)
                .containsEntry("projectWorkflowId", 88L);
        }

        @Test
        void testUpdateWorkflowSucceedsWhenProjectIdLookupThrows() {
            resolveWorkspace(WORKSPACE_ID);
            mockWorkflow("wf-uuid-2", 9L, WORKSPACE_ID);

            ProjectWorkflowDTO dto = buildDto("wf-uuid-2", 88L, 3);

            when(projectWorkflowFacade.getProjectWorkflow("wf-uuid-2")).thenReturn(dto);
            when(projectWorkflowService.getProjectWorkflow(88L)).thenThrow(new RuntimeException("lookup boom"));

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.updateWorkflow("wf-uuid-2", DEFINITION, null, toolContext))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    class SearchWorkflowsTest {

        @Test
        void testSearchWorkflowsScopesToTheResolvedWorkspace() {
            resolveWorkspace(WORKSPACE_ID);

            when(projectService.getWorkspaceProjectIds(WORKSPACE_ID, EnumSet.of(ProjectOwner.EMBEDDED_CONNECTED_USER)))
                .thenReturn(List.of(10L));
            when(projectWorkflowFacade.getProjectWorkflows(10L)).thenReturn(List.of(buildDto("wf-1", 1L, 1)));

            ProjectWorkflowTools tools = newTools();

            List<WorkflowInfo> result = tools.searchWorkflows("flow", null, null, toolContext);

            assertThat(result).hasSize(1);

            verify(projectWorkflowFacade, never()).getProjectWorkflows();
        }

        @Test
        void testSearchWorkflowsRejectsAProjectOutsideTheWorkspace() {
            resolveWorkspace(WORKSPACE_ID);

            when(projectService.getWorkspaceProjectIds(WORKSPACE_ID, EnumSet.of(ProjectOwner.EMBEDDED_CONNECTED_USER)))
                .thenReturn(List.of(10L));

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.searchWorkflows("flow", 99L, null, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("99");

            verify(projectWorkflowFacade, never()).getProjectWorkflows(99L);
            verify(projectWorkflowFacade, never()).getProjectWorkflows();
        }

        @Test
        void testSearchWorkflowsFailsClosedWhenTheWorkspaceIsRejected() {
            when(workspaceScopeResolver.resolveWorkspace(null, toolContext))
                .thenReturn(new WorkspaceScopeResolver.Rejected("{\"error\":\"workspace_required\"}"));

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.searchWorkflows("flow", null, null, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("workspace_required");

            verify(projectService, never()).getWorkspaceProjectIds(anyLong(), anySet());
            verify(projectWorkflowFacade, never()).getProjectWorkflows(anyLong());
        }
    }

    @Nested
    class SaveWorkflowTestConnectionTest {

        @Test
        void testSaveWorkflowTestConnectionDelegatesToFacadeWithAutomationToolEnvironment() {
            resolveWorkspace(WORKSPACE_ID);
            mockWorkflow("wf-uuid-1", 7L, WORKSPACE_ID);

            when(testConfigurationFacadeProvider.getIfAvailable()).thenReturn(workflowTestConfigurationFacade);
            when(toolContext.getContext())
                .thenReturn(Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_ENVIRONMENT_ID_KEY, 2L));

            ProjectWorkflowTools tools = newTools();

            String result = tools.saveWorkflowTestConnection(
                "wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, null, toolContext);

            verify(workflowTestConfigurationFacade)
                .saveWorkflowTestConfigurationConnection("wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, 2L);
            assertThat(result).contains("1107", "sendChannelMessage_1");
        }

        @Test
        void testSaveWorkflowTestConnectionFallsBackToAgentToolEnvironmentKey() {
            resolveWorkspace(WORKSPACE_ID);
            mockWorkflow("wf-uuid-1", 7L, WORKSPACE_ID);

            when(testConfigurationFacadeProvider.getIfAvailable()).thenReturn(workflowTestConfigurationFacade);
            when(toolContext.getContext()).thenReturn(Map.of("bytechef.agentTool.environmentId", 1L));

            ProjectWorkflowTools tools = newTools();

            tools.saveWorkflowTestConnection("wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, null, toolContext);

            verify(workflowTestConfigurationFacade)
                .saveWorkflowTestConfigurationConnection("wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, 1L);
        }

        @Test
        void testSaveWorkflowTestConnectionFailsWhenNoEnvironmentIsResolvable() {
            resolveWorkspace(WORKSPACE_ID);
            mockWorkflow("wf-uuid-1", 7L, WORKSPACE_ID);

            when(testConfigurationFacadeProvider.getIfAvailable()).thenReturn(workflowTestConfigurationFacade);
            when(toolContext.getContext()).thenReturn(Map.of());

            ProjectWorkflowTools tools = newTools();

            assertThatCode(
                () -> tools.saveWorkflowTestConnection(
                    "wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, null, toolContext))
                        .isInstanceOf(ExecutionException.class);

            verify(workflowTestConfigurationFacade, never())
                .saveWorkflowTestConfigurationConnection(any(), any(), any(), anyLong(), anyLong());
        }

        @Test
        void testSaveWorkflowTestConnectionThrowsWhenFacadeAbsent() {
            resolveWorkspace(WORKSPACE_ID);
            mockWorkflow("wf-uuid-1", 7L, WORKSPACE_ID);

            when(testConfigurationFacadeProvider.getIfAvailable()).thenReturn(null);

            ProjectWorkflowTools tools = newTools();

            assertThatCode(
                () -> tools.saveWorkflowTestConnection(
                    "wf-uuid-1", "sendChannelMessage_1", "slack", 1107L, null, toolContext))
                        .isInstanceOf(ExecutionException.class);
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

            assertThatCode(() -> tools.updateWorkflow(VENDOR_WORKFLOW_ID, DEFINITION, null, scopedToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining(NOT_IN_PROJECT_MESSAGE);

            verify(projectWorkflowFacade, never()).getProjectWorkflow(VENDOR_WORKFLOW_ID);
            verify(projectWorkflowFacade, never()).updateWorkflow(any(), any(), anyInt());
            verify(workspaceScopeResolver, never()).resolveWorkspace(any(), any());
        }

        @Test
        void testScopedRunCanUpdateAWorkflowOfItsOwnProjectWithoutResolvingAWorkspace() {
            stubWorkflowProject(OWN_WORKFLOW_ID, OWN_PROJECT_ID);

            when(projectWorkflowFacade.getProjectWorkflow(OWN_WORKFLOW_ID))
                .thenReturn(buildDto(OWN_WORKFLOW_ID, 1L, 1));

            newTools().updateWorkflow(OWN_WORKFLOW_ID, DEFINITION, null, scopedToolContext());

            verify(projectWorkflowFacade).updateWorkflow(OWN_WORKFLOW_ID, DEFINITION, 1);
            verify(workspaceScopeResolver, never()).resolveWorkspace(any(), any());
        }

        @Test
        void testScopedRunCannotReadOrDeleteAWorkflowOfAnotherProject() {
            stubWorkflowProject(VENDOR_WORKFLOW_ID, VENDOR_PROJECT_ID);

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.getWorkflow(VENDOR_WORKFLOW_ID, null, scopedToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining(NOT_IN_PROJECT_MESSAGE);
            assertThatCode(() -> tools.deleteWorkflow(VENDOR_WORKFLOW_ID, null, scopedToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining(NOT_IN_PROJECT_MESSAGE);
            assertThatCode(
                () -> tools.saveWorkflowTestConnection(
                    VENDOR_WORKFLOW_ID, "slack_1", "slack", 1L, null, scopedToolContext()))
                        .isInstanceOf(ExecutionException.class)
                        .hasMessageContaining(NOT_IN_PROJECT_MESSAGE);

            verify(projectWorkflowFacade, never()).getProjectWorkflow(VENDOR_WORKFLOW_ID);
            verify(projectWorkflowFacade, never()).deleteWorkflow(any());
        }

        @Test
        void testScopedRunCannotListOrCreateInAnotherProject() {
            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.listWorkflows(VENDOR_PROJECT_ID, null, scopedToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessage(ScopedProjectGuard.OUT_OF_SCOPE_MESSAGE);
            assertThatCode(() -> tools.createProjectWorkflow(VENDOR_PROJECT_ID, DEFINITION, null, scopedToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessage(ScopedProjectGuard.OUT_OF_SCOPE_MESSAGE);

            verify(projectWorkflowFacade, never()).getProjectWorkflows(anyLong());
            verify(projectWorkflowFacade, never()).addWorkflow(anyLong(), any());
        }

        @Test
        void testScopedRunCannotBindAConnectionTheUserIsNotEntitledTo() {
            stubWorkflowProject(OWN_WORKFLOW_ID, OWN_PROJECT_ID);

            ToolContext toolContext = scopedToolContext();

            when(scopedConnectionGuard.fetchDenialMessage(null, 900L, toolContext))
                .thenReturn(ScopedConnectionGuard.NOT_ENTITLED_MESSAGE);

            ProjectWorkflowTools tools = newTools();

            assertThatCode(
                () -> tools.saveWorkflowTestConnection(OWN_WORKFLOW_ID, "slack_1", "slack", 900L, null, toolContext))
                    .isInstanceOf(ExecutionException.class)
                    .hasMessage(ScopedConnectionGuard.NOT_ENTITLED_MESSAGE);

            verify(workflowTestConfigurationFacade, never()).saveWorkflowTestConfigurationConnection(
                any(), any(), any(), anyLong(), anyLong());
        }

        @Test
        void testScopedRunSearchesOnlyItsOwnProject() {
            when(projectWorkflowFacade.getProjectWorkflows(OWN_PROJECT_ID)).thenReturn(List.of());

            newTools().searchWorkflows("standup", null, 99L, scopedToolContext());

            verify(projectWorkflowFacade).getProjectWorkflows(OWN_PROJECT_ID);
            verify(workspaceScopeResolver, never()).resolveWorkspace(any(), any());
        }

        @Test
        void testScopedRunCannotSearchAnotherProject() {
            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.searchWorkflows("standup", VENDOR_PROJECT_ID, null, scopedToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("not accessible");

            verify(projectWorkflowFacade, never()).getProjectWorkflows(VENDOR_PROJECT_ID);
        }

        @Test
        void testScopedRunReportsAnUnknownWorkflowAsNotFoundInItsProject() {
            when(projectWorkflowService.getWorkflowProjectWorkflow("missing-workflow"))
                .thenThrow(new IllegalArgumentException("ProjectWorkflow not found"));

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.getWorkflow("missing-workflow", null, scopedToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining(NOT_IN_PROJECT_MESSAGE);

            verify(projectWorkflowFacade, never()).getProjectWorkflow("missing-workflow");
        }

        @Test
        void testRunThatSkipsAuthorizationWithoutAScopedProjectIsRejected() {
            ToolContext toolContext = new ToolContext(
                AgentToolInvocationContext.builder()
                    .authentication(new UsernamePasswordAuthenticationToken("1234567890", null, List.of()))
                    .skipAutomationAuthorization(true)
                    .build()
                    .toToolContext());

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.updateWorkflow(OWN_WORKFLOW_ID, DEFINITION, null, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessage(ScopedProjectGuard.UNSCOPED_MESSAGE);
            assertThatCode(() -> tools.searchWorkflows("standup", null, null, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessage(ScopedProjectGuard.UNSCOPED_MESSAGE);

            verify(projectWorkflowFacade, never()).updateWorkflow(any(), any(), anyInt());
            verify(workspaceScopeResolver, never()).resolveWorkspace(any(), any());
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

            assertThatCode(() -> tools.updateWorkflow(OWN_WORKFLOW_ID, definition, null, allowListToolContext()))
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

            assertThatCode(() -> tools.updateWorkflow(OWN_WORKFLOW_ID, definition, null, allowListToolContext()))
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

            assertThatCode(() -> tools.updateWorkflow(OWN_WORKFLOW_ID, definition, null, allowListToolContext()))
                .isInstanceOf(ExecutionException.class)
                .hasMessage(
                    "This chat can't use openAi, hubspot: they aren't among the integrations available to this user.");

            verify(projectWorkflowFacade, never()).updateWorkflow(any(), any(), anyInt());
        }

        @Test
        void testUpdateWorkflowAcceptsTaskDispatchersAndConnectionFreeComponents() {
            stubOwnWorkflowProject();
            stubComponentDefinitions(Map.of("logger", false));

            when(projectWorkflowFacade.getProjectWorkflow(OWN_WORKFLOW_ID))
                .thenReturn(buildDto(OWN_WORKFLOW_ID, 1L, 1));

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

            newTools().updateWorkflow(OWN_WORKFLOW_ID, definition, null, allowListToolContext());

            verify(projectWorkflowFacade).updateWorkflow(OWN_WORKFLOW_ID, definition, 1);
        }

        @Test
        void testUpdateWorkflowWithoutTheAllowListIsUnaffected() {
            stubOwnWorkflowProject();

            when(projectWorkflowFacade.getProjectWorkflow(OWN_WORKFLOW_ID))
                .thenReturn(buildDto(OWN_WORKFLOW_ID, 1L, 1));

            String definition = """
                {"label": "Flow", "tasks": [{"name": "salesforce_1", "type": "salesforce/v1/createRecord"}]}""";

            newTools().updateWorkflow(OWN_WORKFLOW_ID, definition, null, scopedToolContext(null));

            verify(projectWorkflowFacade).updateWorkflow(OWN_WORKFLOW_ID, definition, 1);
            verifyNoInteractions(componentDefinitionService);
        }

        @Test
        void testCreateProjectWorkflowRejectsAComponentOutsideTheAllowList() {
            stubComponentDefinitions(Map.of("salesforce", true));

            String definition = """
                {"label": "Flow", "tasks": [{"name": "salesforce_1", "type": "salesforce/v1/createRecord"}]}""";

            ProjectWorkflowTools tools = newTools();

            assertThatCode(
                () -> tools.createProjectWorkflow(OWN_PROJECT_ID, definition, null, allowListToolContext()))
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
            when(componentDefinitionService.fetchComponentDefinition(anyString(), isNull()))
                .thenAnswer(invocation -> {
                    Boolean connectionRequired = connectionRequiredByComponentName.get(invocation.getArgument(0));

                    if (connectionRequired == null) {
                        return Optional.empty();
                    }

                    ComponentDefinition componentDefinition = mock(ComponentDefinition.class);

                    when(componentDefinition.isConnectionRequired()).thenReturn(connectionRequired);

                    return Optional.of(componentDefinition);
                });
        }

        private void stubOwnWorkflowProject() {
            when(projectWorkflowService.getWorkflowProjectWorkflow(OWN_WORKFLOW_ID))
                .thenReturn(buildProjectWorkflow(1L, OWN_PROJECT_ID, OWN_WORKFLOW_ID));
        }
    }

    @Nested
    class WorkspaceScopeTest {

        @Test
        void testGetWorkflowReadsAWorkflowOfTheWorkspace() {
            resolveWorkspace(WORKSPACE_ID);
            mockWorkflow("wf-uuid-1", 7L, WORKSPACE_ID);

            when(projectWorkflowFacade.getProjectWorkflow("wf-uuid-1")).thenReturn(buildDto("wf-uuid-1", 55L, 1));

            WorkflowInfo workflowInfo = newTools().getWorkflow("wf-uuid-1", null, toolContext);

            assertThat(workflowInfo.id()).isEqualTo("wf-uuid-1");
        }

        @Test
        void testGetWorkflowRejectsAWorkflowOfAnotherWorkspace() {
            resolveWorkspace(WORKSPACE_ID);
            mockWorkflow("wf-uuid-1", 7L, FOREIGN_WORKSPACE_ID);

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.getWorkflow("wf-uuid-1", null, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("not found in workspace " + WORKSPACE_ID);

            verify(projectWorkflowFacade, never()).getProjectWorkflow(anyString());
        }

        @Test
        void testGetWorkflowReadsAWorkflowOfAnEmbeddedCatalogProject() {
            resolveWorkspace(WORKSPACE_ID);
            mockWorkflow("wf-uuid-1", 7L, WORKSPACE_ID);

            givenOwnedProject(7L, ProjectOwner.EMBEDDED_CATALOG);
            when(projectWorkflowFacade.getProjectWorkflow("wf-uuid-1")).thenReturn(buildDto("wf-uuid-1", 55L, 1));

            WorkflowInfo workflowInfo = newTools().getWorkflow("wf-uuid-1", null, toolContext);

            assertThat(workflowInfo.id()).isEqualTo("wf-uuid-1");
        }

        @Test
        void testGetWorkflowRejectsAWorkflowOfAConnectedUserProject() {
            resolveWorkspace(WORKSPACE_ID);
            mockWorkflow("wf-uuid-1", 7L, WORKSPACE_ID);

            givenOwnedProject(7L, ProjectOwner.EMBEDDED_CONNECTED_USER);

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.getWorkflow("wf-uuid-1", null, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("not found in workspace " + WORKSPACE_ID);

            verify(projectWorkflowFacade, never()).getProjectWorkflow(anyString());
        }

        @Test
        void testGetWorkflowRejectsAnUnknownWorkflow() {
            resolveWorkspace(WORKSPACE_ID);

            when(projectWorkflowService.getWorkflowProjectWorkflow("missing"))
                .thenThrow(new IllegalArgumentException("ProjectWorkflow not found"));

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.getWorkflow("missing", null, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("not found in workspace " + WORKSPACE_ID);

            verify(projectWorkflowFacade, never()).getProjectWorkflow(anyString());
        }

        @Test
        void testGetWorkflowFailsClosedWhenTheWorkspaceIsRejected() {
            when(workspaceScopeResolver.resolveWorkspace(FOREIGN_WORKSPACE_ID, toolContext))
                .thenReturn(new WorkspaceScopeResolver.Rejected("{\"error\":\"not accessible\"}"));

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.getWorkflow("wf-uuid-1", FOREIGN_WORKSPACE_ID, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("not accessible");

            verify(projectWorkflowService, never()).getWorkflowProjectWorkflow(anyString());
            verify(projectWorkflowFacade, never()).getProjectWorkflow(anyString());
        }

        @Test
        void testListWorkflowsRejectsAProjectOfAnotherWorkspace() {
            resolveWorkspace(WORKSPACE_ID);
            mockProject(7L, FOREIGN_WORKSPACE_ID);

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.listWorkflows(7L, null, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("Project 7 not found in workspace " + WORKSPACE_ID);

            verify(projectWorkflowFacade, never()).getProjectWorkflows(anyLong());
        }

        @Test
        void testListWorkflowsListsAProjectOfTheWorkspace() {
            resolveWorkspace(WORKSPACE_ID);
            mockProject(7L, WORKSPACE_ID);

            when(projectWorkflowFacade.getProjectWorkflows(7L)).thenReturn(List.of(buildDto("wf-1", 1L, 1)));

            List<WorkflowInfo> workflowInfos = newTools().listWorkflows(7L, null, toolContext);

            assertThat(workflowInfos).hasSize(1);
        }

        @Test
        void testListWorkflowsListsAnEmbeddedCatalogProject() {
            resolveWorkspace(WORKSPACE_ID);
            mockProject(7L, WORKSPACE_ID);
            givenOwnedProject(7L, ProjectOwner.EMBEDDED_CATALOG);

            when(projectWorkflowFacade.getProjectWorkflows(7L)).thenReturn(List.of(buildDto("wf-1", 1L, 1)));

            List<WorkflowInfo> workflowInfos = newTools().listWorkflows(7L, null, toolContext);

            assertThat(workflowInfos).hasSize(1);
        }

        @Test
        void testListWorkflowsRejectsAConnectedUserProject() {
            resolveWorkspace(WORKSPACE_ID);
            mockProject(7L, WORKSPACE_ID);
            givenOwnedProject(7L, ProjectOwner.EMBEDDED_CONNECTED_USER);

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.listWorkflows(7L, null, toolContext))
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("Project 7 not found in workspace " + WORKSPACE_ID);

            verify(projectWorkflowFacade, never()).getProjectWorkflows(anyLong());
        }

        @Test
        void testCreateProjectWorkflowCreatesAWorkflowInAnEmbeddedCatalogProject() {
            resolveWorkspace(WORKSPACE_ID);
            mockProject(7L, WORKSPACE_ID);
            givenOwnedProject(7L, ProjectOwner.EMBEDDED_CATALOG);

            when(projectWorkflowFacade.addWorkflow(7L, DEFINITION))
                .thenReturn(buildProjectWorkflow(55L, 7L, "wf-uuid-1"));

            newTools().createProjectWorkflow(7L, DEFINITION, null, toolContext);

            verify(projectWorkflowFacade).addWorkflow(7L, DEFINITION);
        }

        @Test
        void testUpdateWorkflowUpdatesAWorkflowOfAnEmbeddedCatalogProject() {
            resolveWorkspace(WORKSPACE_ID);
            mockWorkflow("wf-uuid-1", 7L, WORKSPACE_ID);
            givenOwnedProject(7L, ProjectOwner.EMBEDDED_CATALOG);

            when(projectWorkflowFacade.getProjectWorkflow("wf-uuid-1")).thenReturn(buildDto("wf-uuid-1", 55L, 3));

            newTools().updateWorkflow("wf-uuid-1", DEFINITION, null, toolContext);

            verify(projectWorkflowFacade).updateWorkflow("wf-uuid-1", DEFINITION, 3);
        }

        @Test
        void testCreateProjectWorkflowRejectsAProjectOfAnotherWorkspace() {
            resolveWorkspace(WORKSPACE_ID);
            mockProject(7L, FOREIGN_WORKSPACE_ID);

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.createProjectWorkflow(7L, DEFINITION, null, toolContext))
                .isInstanceOf(ExecutionException.class);

            verify(projectWorkflowFacade, never()).addWorkflow(anyLong(), anyString());
        }

        @Test
        void testDeleteWorkflowRejectsAWorkflowOfAnotherWorkspace() {
            resolveWorkspace(WORKSPACE_ID);
            mockWorkflow("wf-uuid-1", 7L, FOREIGN_WORKSPACE_ID);

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.deleteWorkflow("wf-uuid-1", null, toolContext))
                .isInstanceOf(ExecutionException.class);

            verify(projectWorkflowFacade, never()).deleteWorkflow(anyString());
        }

        @Test
        void testUpdateWorkflowRejectsAWorkflowOfAnotherWorkspace() {
            resolveWorkspace(WORKSPACE_ID);
            mockWorkflow("wf-uuid-1", 7L, FOREIGN_WORKSPACE_ID);

            ProjectWorkflowTools tools = newTools();

            assertThatCode(() -> tools.updateWorkflow("wf-uuid-1", DEFINITION, null, toolContext))
                .isInstanceOf(ExecutionException.class);

            verify(projectWorkflowFacade, never()).updateWorkflow(anyString(), anyString(), anyInt());
        }
    }

    private void mockProject(long projectId, long workspaceId) {
        Project project = Project.builder()
            .id(projectId)
            .name("project-" + projectId)
            .workspaceId(workspaceId)
            .build();

        when(projectService.fetchProject(projectId)).thenReturn(Optional.of(project));
    }

    private void givenOwnedProject(long projectId, ProjectOwner projectOwner) {
        when(projectOwnershipService.getOwnedProjectIds(any(), any()))
            .thenAnswer(invocation -> {
                Collection<Long> projectIds = invocation.getArgument(0);
                Set<ProjectOwner> projectOwners = invocation.getArgument(1);

                return projectIds.contains(projectId) && projectOwners.contains(projectOwner)
                    ? Set.of(projectId) : Set.of();
            });
    }

    private void mockWorkflow(String workflowId, long projectId, long workspaceId) {
        when(projectWorkflowService.getWorkflowProjectWorkflow(workflowId))
            .thenReturn(buildProjectWorkflow(100L, projectId, workflowId));

        mockProject(projectId, workspaceId);
    }

    private void resolveWorkspace(long workspaceId) {
        when(workspaceScopeResolver.resolveWorkspace(null, toolContext))
            .thenReturn(new WorkspaceScopeResolver.Resolved(workspaceId, 0L));
    }

    private ProjectWorkflowTools newTools() {
        return new ProjectWorkflowTools(
            componentDefinitionService, projectWorkflowFacade, projectWorkflowService,
            new ScopedProjectGuard(
                projectOwnershipService, projectService, projectWorkflowService, scopedConnectionGuard,
                workspaceScopeResolver),
            testConfigurationFacadeProvider);
    }

    private static ProjectWorkflow buildProjectWorkflow(long id, long projectId, String workflowId) {
        ProjectWorkflow projectWorkflow = new ProjectWorkflow(projectId, 1, workflowId);

        ReflectionTestUtils.setField(projectWorkflow, "id", id);

        return projectWorkflow;
    }

    private static ProjectWorkflowDTO buildDto(String workflowUuid, long projectWorkflowId, int version) {
        Workflow workflow = new Workflow(workflowUuid, WORKFLOW_DTO_DEFINITION, Workflow.Format.JSON);

        workflow.setVersion(version);

        ProjectWorkflow projectWorkflow = new ProjectWorkflow(projectWorkflowId);

        return new ProjectWorkflowDTO(workflow, projectWorkflow, false);
    }
}
