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

package com.bytechef.automation.ai.agent.facade;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @author Ivica Cardic
 */
class AiAgentFacadeAuthorizationTest {

    private static final String AGENT_CREATE_BY_WORKSPACE_ID =
        "hasPermission(#workspaceId, 'Workspace', 'AGENT_CREATE')";

    private static final String AGENT_VIEW_BY_WORKSPACE_ID =
        "hasPermission(#workspaceId, 'Workspace', 'AGENT_VIEW')";

    private static final String AGENT_VIEW_BY_WORKSPACE_ID_IN_ENVIRONMENT =
        "hasWorkspaceScopeInEnvironmentId(#workspaceId, 'AGENT_VIEW', #environmentId)";

    private static final String AGENT_DELETE_BY_ID = "hasPermission(#id, 'AiAgent', 'AGENT_DELETE')";

    private static final String AGENT_EDIT_BY_AGENT_ID = "hasPermission(#agentId, 'AiAgent', 'AGENT_EDIT')";

    private static final String AGENT_EDIT_BY_ID = "hasPermission(#id, 'AiAgent', 'AGENT_EDIT')";

    private static final String AGENT_VIEW_BY_AGENT_ID = "hasPermission(#agentId, 'AiAgent', 'AGENT_VIEW')";

    private static final String AGENT_VIEW_BY_ID = "hasPermission(#id, 'AiAgent', 'AGENT_VIEW')";

    private static final String CHANNEL_AGENT_EDIT =
        "hasPermission(#channelId, 'AiAgentChannel', 'AGENT_EDIT')";

    private static final String ELEMENT_AGENT_EDIT =
        "hasPermission(#elementId, 'AiAgentElement', 'AGENT_EDIT')";

    private static final Set<String> UNGATED_METHOD_NAMES = Set.of("getAgentChannelDefinitions");

    @Test
    void testCreateAgentRequiresWorkspaceAgentCreator() {
        assertExpression(AGENT_CREATE_BY_WORKSPACE_ID, "createAgent", String.class, String.class, long.class,
            Long.class);
    }

    @Test
    void testImportAgentRequiresWorkspaceAgentCreator() {
        assertExpression(AGENT_CREATE_BY_WORKSPACE_ID, "importAgent", long.class, String.class, Long.class);
    }

    @Test
    void testCopyProjectAgentsRequiresWorkspaceAgentCreator() {
        assertExpression(AGENT_CREATE_BY_WORKSPACE_ID, "copyProjectAgents", long.class, long.class, long.class);
    }

    @Test
    void testGetAgentsRequiresWorkspaceAgentViewer() {
        assertExpression(AGENT_VIEW_BY_WORKSPACE_ID, "getAgents", long.class);
    }

    @Test
    void testGetAgentDeploymentsRequiresWorkspaceAgentViewer() {
        assertExpression(AGENT_VIEW_BY_WORKSPACE_ID, "getAgentDeployments", long.class);
    }

    @Test
    void testGetWorkspaceChatAgentsRequiresWorkspaceAgentViewer() {
        assertExpression(AGENT_VIEW_BY_WORKSPACE_ID_IN_ENVIRONMENT, "getWorkspaceChatAgents", long.class, long.class);
    }

    @Test
    void testEveryWorkspaceKeyedMethodIsGated() {
        List<Method> workspaceKeyedMethods = Arrays.stream(AiAgentFacadeImpl.class.getDeclaredMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .filter(method -> !method.isSynthetic())
            .filter(
                method -> Arrays.stream(method.getParameters())
                    .anyMatch(parameter -> "workspaceId".equals(parameter.getName())))
            .toList();

        assertThat(
            workspaceKeyedMethods.stream()
                .map(Method::getName)
                .sorted()
                .toList())
                    .as(
                        "public workspaceId-keyed methods of AiAgentFacadeImpl (empty here means -parameters is off " +
                            "and this test is checking nothing)")
                    .containsExactly(
                        "copyProjectAgents", "createAgent", "getAgentDeployments", "getAgents",
                        "getWorkspaceChatAgents", "importAgent");

        assertThat(
            workspaceKeyedMethods.stream()
                .filter(method -> {
                    PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

                    return preAuthorize == null || !isWorkspaceScopedExpression(preAuthorize.value());
                })
                .map(Method::getName)
                .sorted()
                .toList())
                    .as(
                        "workspaceId-keyed methods without a workspace-keyed hasPermission or " +
                            "hasWorkspaceScopeInEnvironmentId gate")
                    .isEmpty();
    }

    @Test
    void testDeleteAgentRequiresAgentDelete() {
        assertExpression(AGENT_DELETE_BY_ID, "deleteAgent", long.class);
    }

    @Test
    void testPrepareProjectPublishIsGatedOnTheProject() {
        assertExpression("hasPermission(#projectId, 'Project', 'WORKFLOW_EDIT')", "prepareProjectPublish", long.class);
        assertExpression("hasPermission(#projectId, 'Project', 'WORKFLOW_VIEW')", "isProjectPublishable", long.class);
    }

    @Test
    void testDeleteProjectAgentsIsGatedOnTheProject() {
        assertExpression("hasPermission(#projectId, 'Project', 'PROJECT_DELETE')", "deleteProjectAgents", long.class);
    }

    @Test
    void testGetAgentRequiresAgentView() {
        assertExpression(AGENT_VIEW_BY_ID, "getAgent", long.class);
    }

    @Test
    void testGetAgentVersionsRequiresAgentView() {
        assertExpression(AGENT_VIEW_BY_ID, "getAgentVersions", long.class);
    }

    @Test
    void testExportAgentRequiresAgentView() {
        assertExpression(AGENT_VIEW_BY_ID, "exportAgent", long.class);
        assertExpression(
            "hasPermission(#projectId, 'Project', 'WORKFLOW_VIEW')", "exportProjectAgentVersions", long.class,
            int.class);
    }

    @Test
    void testGetDraftWorkflowIdRequiresAgentView() {
        assertExpression(AGENT_VIEW_BY_AGENT_ID, "getDraftWorkflowId", long.class);
    }

    @Test
    void testUpdateAgentRequiresAgentEdit() {
        assertExpression(AGENT_EDIT_BY_ID, "updateAgent", long.class, String.class, String.class, String.class);
    }

    @Test
    void testUpdateAgentFromExportRequiresAgentEdit() {
        assertExpression(AGENT_EDIT_BY_ID, "updateAgentFromExport", long.class, String.class);
        assertExpression(
            "hasPermission(#projectId, 'Project', 'WORKFLOW_EDIT')", "restoreProjectAgent", long.class, int.class,
            UUID.class);
        assertExpression(
            "hasPermission(#projectId, 'Project', 'WORKFLOW_EDIT')", "restoreProjectAgents", long.class, int.class);
    }

    @Test
    void testUpdateAgentSettingsRequiresAgentEdit() {
        assertExpression(AGENT_EDIT_BY_ID, "updateAgentSettings", long.class, Map.class);
    }

    @Test
    void testAddAgentChannelRequiresAgentEdit() {
        assertExpression(AGENT_EDIT_BY_AGENT_ID, "addAgentChannel", long.class, String.class, Map.class, Long.class);
    }

    @Test
    void testAddAgentElementRequiresAgentEdit() {
        assertExpression(
            AGENT_EDIT_BY_AGENT_ID, "addAgentElement", long.class, String.class, Long.class, Map.class, Long.class);
    }

    @Test
    void testUpdateAgentChannelRequiresAgentEditOnTheChannelsOwnWorkspace() {
        assertExpression(CHANNEL_AGENT_EDIT, "updateAgentChannel", long.class, Map.class, Long.class);
    }

    @Test
    void testDeleteAgentChannelRequiresAgentEditOnTheChannelsOwnWorkspace() {
        assertExpression(CHANNEL_AGENT_EDIT, "deleteAgentChannel", long.class);
    }

    @Test
    void testUpdateAgentElementRequiresAgentEditOnTheElementsOwnWorkspace() {
        assertExpression(ELEMENT_AGENT_EDIT, "updateAgentElement", long.class, Map.class, Long.class);
    }

    @Test
    void testDeleteAgentElementRequiresAgentEditOnTheElementsOwnWorkspace() {
        assertExpression(ELEMENT_AGENT_EDIT, "deleteAgentElement", long.class);
    }

    @Test
    void testGetAgentChannelDefinitionsStaysAuthenticatedOnly() {
        assertExpression("isAuthenticated()", "getAgentChannelDefinitions");
    }

    @Test
    void testEveryFacadeMethodIsGated() {
        List<Method> facadeMethods = Arrays.stream(AiAgentFacade.class.getDeclaredMethods())
            .filter(method -> !method.isSynthetic())
            .map(method -> findMethod(method.getName(), method.getParameterTypes()))
            .toList();

        assertThat(facadeMethods)
            .as("methods of AiAgentFacade (empty here means the discovery filter is broken, not that all is well)")
            .hasSize(27);

        assertThat(
            facadeMethods.stream()
                .filter(method -> !UNGATED_METHOD_NAMES.contains(method.getName()))
                .filter(method -> {
                    PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

                    return preAuthorize == null || !isResourceGatedExpression(preAuthorize.value());
                })
                .map(Method::getName)
                .sorted()
                .toList())
                    .as("AiAgentFacade methods without a hasPermission or hasWorkspaceScopeInEnvironmentId gate")
                    .isEmpty();
    }

    private static void assertExpression(String expression, String methodName, Class<?>... parameterTypes) {
        Method method = findMethod(methodName, parameterTypes);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on %s", methodName)
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(expression);
    }

    private static Method findMethod(String methodName, Class<?>... parameterTypes) {
        try {
            return AiAgentFacadeImpl.class.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError("method " + methodName + " not found", exception);
        }
    }

    private static boolean isWorkspaceScopedExpression(String expression) {
        return expression.startsWith("hasPermission(#workspaceId, 'Workspace', ")
            || expression.startsWith("hasWorkspaceScopeInEnvironmentId(#workspaceId, ");
    }

    private static boolean isResourceGatedExpression(String expression) {
        return expression.startsWith("hasPermission(") || expression.startsWith("hasWorkspaceScopeInEnvironmentId(");
    }
}
