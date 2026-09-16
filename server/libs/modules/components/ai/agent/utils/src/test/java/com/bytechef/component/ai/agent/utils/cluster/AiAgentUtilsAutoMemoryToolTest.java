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

package com.bytechef.component.ai.agent.utils.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ClusterElementContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.ai.auto.memory.AiAutoMemory;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryOwner;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryPrincipalType;
import com.bytechef.platform.ai.auto.memory.AiAutoMemoryService;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.JobContextAware;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.constant.PlatformType;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @author Ivica Cardic
 */
class AiAgentUtilsAutoMemoryToolTest {

    private final AiAutoMemoryService aiAutoMemoryService = mock(AiAutoMemoryService.class);
    private final Parameters connectionParameters = mock(Parameters.class);
    private final Parameters inputParameters = mock(Parameters.class);
    private final ProjectDeploymentService projectDeploymentService = mock(ProjectDeploymentService.class);
    private final ProjectService projectService = mock(ProjectService.class);

    private final ToolCallbackProviderFunction toolCallbackProviderFunction = new AiAgentUtilsAutoMemoryTool(
        aiAutoMemoryService, projectDeploymentService, projectService).clusterElementDefinition.getElement();

    @BeforeEach
    void beforeEach() {
        when(aiAutoMemoryService.isAvailable()).thenReturn(true);
    }

    @Test
    void testApplyWithoutJobContextExplainsThatMemoryIsUnavailable() throws Exception {
        ClusterElementContext clusterElementContext = mock(ClusterElementContext.class);

        ToolCallback[] toolCallbacks = toolCallbackProviderFunction
            .apply(inputParameters, connectionParameters, clusterElementContext)
            .getToolCallbacks();

        assertThat(callMemoryView(toolCallbacks)).contains("Memory is unavailable")
            .contains("does not identify the running workflow")
            .doesNotContain("Deploy the workflow");

        verifyNoInteractions(projectDeploymentService, projectService);
        verify(aiAutoMemoryService, never()).list(any(), any());
    }

    /**
     * An agent running as another agent's tool is handed a job-aware action context — not an {@link ActionContextAware}
     * — over the parent's context. Its memory must still belong to the deployment the parent run executes.
     */
    @Test
    void testApplyFromANestedAgentContextUsesTheParentRunsDeployment() throws Exception {
        ActionContextAware actionContext = givenProjectDeploymentRun();
        ActionContext nestedAgentContext = mock(
            ActionContext.class, withSettings().extraInterfaces(JobContextAware.class));

        when(((JobContextAware) nestedAgentContext).toActionContext("aiAgentUtils", 1, "autoMemoryTool", null))
            .thenReturn(actionContext);

        callMemoryView(
            toolCallbackProviderFunction.apply(inputParameters, connectionParameters, nestedAgentContext)
                .getToolCallbacks());

        verify(aiAutoMemoryService).list(
            new AiAutoMemoryOwner(7L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 11L, Environment.STAGING), null);
    }

    /**
     * Where the application does not store memory — the microservices worker's stub — the tools say so instead of
     * failing on the first call, and the deployment is never looked up: there it is a remote call that could only fail
     * the run.
     */
    @Test
    void testApplyWhereMemoryIsNotStoredExplainsThatMemoryIsUnavailable() throws Exception {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(aiAutoMemoryService.isAvailable()).thenReturn(false);
        when(actionContext.getPlatformType()).thenReturn(PlatformType.AUTOMATION);
        when(actionContext.getJobPrincipalId()).thenReturn(11L);
        when(actionContext.getEnvironmentId()).thenReturn(1L);

        ToolCallback[] toolCallbacks = toolCallbackProviderFunction
            .apply(inputParameters, connectionParameters, actionContext)
            .getToolCallbacks();

        assertThat(callMemoryView(toolCallbacks)).contains("Memory is unavailable")
            .contains("requires the ByteChef server application");

        verify(aiAutoMemoryService, never()).list(any(), any());
        verifyNoInteractions(projectDeploymentService, projectService);
    }

    @Test
    void testApplyForProjectDeploymentExposesMemoryTools() throws Exception {
        ToolCallback[] toolCallbacks = applyForProjectDeployment();

        assertThat(Arrays.stream(toolCallbacks)
            .map(ToolCallback::getToolDefinition)
            .map(ToolDefinition::name))
                .containsExactlyInAnyOrder(
                    "MemoryCreate", "MemoryDelete", "MemoryInsert", "MemoryRename", "MemoryStrReplace", "MemoryView");
    }

    /**
     * The AI Agent sets no tool context, so Spring AI's tool calling manager hands every tool an empty one. Each tool
     * must then reach its own logic — with no arguments that is an index read or an index error — rather than fail in
     * argument binding, which the tools would only report as the generic unexpected failure.
     */
    @Test
    void testEveryMemoryToolRunsWithTheEmptyToolContextOfAnAgentRun() throws Exception {
        for (ToolCallback toolCallback : applyForProjectDeployment()) {
            assertThat(toolCallback.call("{}", new ToolContext(Map.of())))
                .isNotEmpty()
                .doesNotContain("failed and was not applied");
            assertThat(toolCallback.call("{}"))
                .isNotEmpty()
                .doesNotContain("failed and was not applied");
        }
    }

    /**
     * The generic tool module cannot reference the entity, so its MemoryCreate description repeats the limits.
     */
    @Test
    void testMemoryCreateDescriptionStatesTheEntityLimits() throws Exception {
        ToolCallback memoryCreate = Arrays.stream(applyForProjectDeployment())
            .filter(toolCallback -> "MemoryCreate".equals(toolCallback.getToolDefinition()
                .name()))
            .findFirst()
            .orElseThrow();

        ToolDefinition toolDefinition = memoryCreate.getToolDefinition();

        assertThat(toolDefinition.description()).contains(
            "title at most " + AiAutoMemory.MAX_TITLE_LENGTH,
            "description at most " + AiAutoMemory.MAX_DESCRIPTION_LENGTH,
            String.format(Locale.ROOT, "body at most %,d", AiAutoMemory.MAX_CONTENT_LENGTH));
    }

    @Test
    void testProjectDeploymentMemoryIsOwnedByTheDeploymentInItsProjectWorkspace() throws Exception {
        callMemoryView(applyForProjectDeployment());

        verify(aiAutoMemoryService).list(
            new AiAutoMemoryOwner(7L, AiAutoMemoryPrincipalType.PROJECT_DEPLOYMENT, 11L, Environment.STAGING), null);
    }

    @Test
    void testEmbeddedMemoryIsOwnedByTheIntegrationInstanceInTheDefaultWorkspace() throws Exception {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getPlatformType()).thenReturn(PlatformType.EMBEDDED);
        when(actionContext.getJobPrincipalId()).thenReturn(21L);
        when(actionContext.getEnvironmentId()).thenReturn(2L);

        callMemoryView(
            toolCallbackProviderFunction.apply(inputParameters, connectionParameters, actionContext)
                .getToolCallbacks());

        verify(aiAutoMemoryService).list(
            new AiAutoMemoryOwner(
                Workspace.DEFAULT_WORKSPACE_ID, AiAutoMemoryPrincipalType.INTEGRATION_INSTANCE, 21L,
                Environment.PRODUCTION),
            null);
        verifyNoInteractions(projectDeploymentService, projectService);
    }

    /**
     * An editor test run has no job principal. The agent still gets the memory tools, so it can tell the user why it
     * cannot remember rather than having no memory tools and no explanation.
     */
    @Test
    void testApplyWithoutJobPrincipalExposesToolsThatExplainMemoryIsUnavailable() throws Exception {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getPlatformType()).thenReturn(PlatformType.AUTOMATION);
        when(actionContext.getJobPrincipalId()).thenReturn(null);
        when(actionContext.getEnvironmentId()).thenReturn(1L);

        ToolCallback[] toolCallbacks = toolCallbackProviderFunction
            .apply(inputParameters, connectionParameters, actionContext)
            .getToolCallbacks();

        assertThat(toolCallbacks).hasSize(6);
        assertThat(callMemoryView(toolCallbacks)).contains("Error: Memory is unavailable")
            .contains("editor test run");

        verifyNoInteractions(aiAutoMemoryService, projectDeploymentService, projectService);
    }

    @Test
    void testApplyWithUnknownEnvironmentExplainsThatMemoryIsUnavailable() throws Exception {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getPlatformType()).thenReturn(PlatformType.AUTOMATION);
        when(actionContext.getJobPrincipalId()).thenReturn(11L);
        when(actionContext.getEnvironmentId()).thenReturn(null);

        ToolCallback[] toolCallbacks = toolCallbackProviderFunction
            .apply(inputParameters, connectionParameters, actionContext)
            .getToolCallbacks();

        assertThat(callMemoryView(toolCallbacks)).contains("Memory is unavailable")
            .contains("environment is unknown");

        verifyNoInteractions(aiAutoMemoryService, projectDeploymentService, projectService);
    }

    /**
     * Environment ids are the {@code Environment} ordinals, so an id outside them names no environment memory could be
     * scoped to.
     */
    @Test
    void testApplyWithAnOutOfRangeEnvironmentExplainsThatMemoryIsUnavailable() throws Exception {
        for (long environmentId : new long[] {
            -1L, Environment.values().length
        }) {
            ActionContextAware actionContext = mock(ActionContextAware.class);

            when(actionContext.getPlatformType()).thenReturn(PlatformType.AUTOMATION);
            when(actionContext.getJobPrincipalId()).thenReturn(11L);
            when(actionContext.getEnvironmentId()).thenReturn(environmentId);

            ToolCallback[] toolCallbacks = toolCallbackProviderFunction
                .apply(inputParameters, connectionParameters, actionContext)
                .getToolCallbacks();

            assertThat(callMemoryView(toolCallbacks)).contains("Memory is unavailable")
                .contains("environment is unknown");
        }

        verifyNoInteractions(aiAutoMemoryService, projectDeploymentService, projectService);
    }

    @Test
    void testApplyWithoutAPlatformTypeExplainsThatMemoryIsUnavailable() throws Exception {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getPlatformType()).thenReturn(null);
        when(actionContext.getJobPrincipalId()).thenReturn(11L);
        when(actionContext.getEnvironmentId()).thenReturn(1L);

        ToolCallback[] toolCallbacks = toolCallbackProviderFunction
            .apply(inputParameters, connectionParameters, actionContext)
            .getToolCallbacks();

        assertThat(callMemoryView(toolCallbacks)).contains("Memory is unavailable")
            .contains("does not support memory");

        verify(aiAutoMemoryService, never()).list(any(), any());
        verifyNoInteractions(projectDeploymentService, projectService);
    }

    /**
     * The deployment can outlive its project for a moment while a project is being deleted; that is the same "not
     * found" as a missing deployment, not an outage.
     */
    @Test
    void testApplyForADeploymentWhoseProjectCannotBeFoundExplainsThatMemoryIsUnavailable() throws Exception {
        ActionContextAware actionContext = mock(ActionContextAware.class);
        ProjectDeployment projectDeployment = new ProjectDeployment();

        projectDeployment.setProjectId(3L);

        when(actionContext.getPlatformType()).thenReturn(PlatformType.AUTOMATION);
        when(actionContext.getJobPrincipalId()).thenReturn(11L);
        when(actionContext.getEnvironmentId()).thenReturn(1L);
        when(projectDeploymentService.getProjectDeployment(11L)).thenReturn(projectDeployment);
        when(projectService.getProject(3L)).thenThrow(new NoSuchElementException());

        ToolCallback[] toolCallbacks = toolCallbackProviderFunction
            .apply(inputParameters, connectionParameters, actionContext)
            .getToolCallbacks();

        assertThat(callMemoryView(toolCallbacks)).contains("Memory is unavailable")
            .contains("deployment could not be found");

        verify(aiAutoMemoryService, never()).list(any(), any());
    }

    @Test
    void testApplyForADeploymentThatCannotBeFoundExplainsThatMemoryIsUnavailable() throws Exception {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getPlatformType()).thenReturn(PlatformType.AUTOMATION);
        when(actionContext.getJobPrincipalId()).thenReturn(11L);
        when(actionContext.getEnvironmentId()).thenReturn(1L);
        when(projectDeploymentService.getProjectDeployment(11L)).thenThrow(new NoSuchElementException());

        ToolCallback[] toolCallbacks = toolCallbackProviderFunction
            .apply(inputParameters, connectionParameters, actionContext)
            .getToolCallbacks();

        assertThat(callMemoryView(toolCallbacks)).contains("Memory is unavailable")
            .contains("deployment could not be found");

        verify(aiAutoMemoryService, never()).list(any(), any());
    }

    /**
     * Only a missing deployment means memory is unavailable. Any other lookup failure is an outage, which must fail the
     * tool setup visibly rather than tell the model its deployment does not exist.
     */
    @Test
    void testApplyWhenTheDeploymentLookupFailsPropagatesTheFailure() {
        ActionContextAware actionContext = mock(ActionContextAware.class);

        when(actionContext.getPlatformType()).thenReturn(PlatformType.AUTOMATION);
        when(actionContext.getJobPrincipalId()).thenReturn(11L);
        when(actionContext.getEnvironmentId()).thenReturn(1L);
        when(projectDeploymentService.getProjectDeployment(11L))
            .thenThrow(new IllegalStateException("Connection pool exhausted"));

        assertThatThrownBy(
            () -> toolCallbackProviderFunction.apply(inputParameters, connectionParameters, actionContext))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Connection pool exhausted");

        verify(aiAutoMemoryService, never()).list(any(), any());
    }

    private ToolCallback[] applyForProjectDeployment() throws Exception {
        return toolCallbackProviderFunction.apply(inputParameters, connectionParameters, givenProjectDeploymentRun())
            .getToolCallbacks();
    }

    private ActionContextAware givenProjectDeploymentRun() {
        ActionContextAware actionContext = mock(ActionContextAware.class);
        Project project = new Project();
        ProjectDeployment projectDeployment = new ProjectDeployment();

        project.setWorkspaceId(7L);
        projectDeployment.setProjectId(3L);

        when(actionContext.getPlatformType()).thenReturn(PlatformType.AUTOMATION);
        when(actionContext.getJobPrincipalId()).thenReturn(11L);
        when(actionContext.getEnvironmentId()).thenReturn(1L);
        when(projectDeploymentService.getProjectDeployment(11L)).thenReturn(projectDeployment);
        when(projectService.getProject(3L)).thenReturn(project);

        return actionContext;
    }

    private static String callMemoryView(ToolCallback[] toolCallbacks) {
        ToolCallback memoryView = Arrays.stream(toolCallbacks)
            .filter(toolCallback -> "MemoryView".equals(toolCallback.getToolDefinition()
                .name()))
            .findFirst()
            .orElseThrow();

        return memoryView.call("{\"path\": \"MEMORY.md\"}", new ToolContext(Map.of()));
    }
}
