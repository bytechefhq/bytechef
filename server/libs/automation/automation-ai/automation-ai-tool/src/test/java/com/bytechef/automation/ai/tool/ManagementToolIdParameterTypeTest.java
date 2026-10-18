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

import static com.bytechef.automation.ai.tool.DeleteProjectDeploymentToolCallbackTest.toolContext;
import static com.bytechef.automation.ai.tool.DeleteProjectDeploymentToolCallbackTest.workspaceFacade;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bytechef.automation.ai.mcp.facade.McpProjectFacade;
import com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.automation.ai.tool.knowledgebase.AddKnowledgeBaseDocumentToolCallback;
import com.bytechef.automation.ai.tool.knowledgebase.DeleteKnowledgeBaseDocumentToolCallback;
import com.bytechef.automation.ai.tool.knowledgebase.QueryKnowledgeBaseToolCallback;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseFacade;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ManagementToolIdParameterTypeTest {

    private static final JsonMapper JSON_MAPPER = new JsonMapper();

    @ParameterizedTest(name = "{0}")
    @MethodSource("numericIdParameters")
    void testNumericIdParametersAreAdvertisedAsIntegers(ToolCallback toolCallback, List<String> idPropertyNames) {
        ToolDefinition toolDefinition = toolCallback.getToolDefinition();

        JsonNode propertiesNode = JSON_MAPPER.readTree(toolDefinition.inputSchema())
            .get("properties");

        for (String idPropertyName : idPropertyNames) {
            JsonNode typeNode = propertiesNode.get(idPropertyName)
                .get("type");

            assertThat(typeNode.asString())
                .as("%s.%s", toolDefinition.name(), idPropertyName)
                .isEqualTo("integer");
        }
    }

    @Test
    void testListProjectDeploymentsAdvertisesProjectIdLikeTheWorkspaceIdItIsWrappedWith() {
        WorkspaceScopedFlatToolCallback workspaceScopedFlatToolCallback = new WorkspaceScopedFlatToolCallback(
            new ListProjectDeploymentsToolCallback(mock(ProjectDeploymentFacade.class)), mock(WorkspaceFacade.class));

        ToolDefinition toolDefinition = workspaceScopedFlatToolCallback.getToolDefinition();

        JsonNode propertiesNode = JSON_MAPPER.readTree(toolDefinition.inputSchema())
            .get("properties");

        assertThat(propertiesNode.get("projectId")
            .get("type")
            .asString()).isEqualTo("integer");
        assertThat(propertiesNode.get("workspaceId")
            .get("type")
            .asString()).isEqualTo("integer");
    }

    @Test
    void testListProjectDeploymentsAcceptsAnIntegerProjectId() {
        ProjectDeploymentFacade projectDeploymentFacade = mock(ProjectDeploymentFacade.class);

        ListProjectDeploymentsToolCallback listProjectDeploymentsToolCallback =
            new ListProjectDeploymentsToolCallback(projectDeploymentFacade);

        String result = listProjectDeploymentsToolCallback.call(
            "{\"projectId\":7}",
            new ToolContext(Map.of(AutomationToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY, 99L)));

        assertThat(result).doesNotContain("error");

        verify(projectDeploymentFacade).getWorkspaceProjectDeployments(99L, null, 7L, null, false);
    }

    @Test
    void testToggleProjectDeploymentAcceptsAnIntegerAndAStringProjectDeploymentId() {
        ProjectDeploymentFacade projectDeploymentFacade = workspaceFacade(7L);

        ToggleProjectDeploymentToolCallback toggleProjectDeploymentToolCallback =
            new ToggleProjectDeploymentToolCallback(projectDeploymentFacade);

        String integerResult = toggleProjectDeploymentToolCallback.call(
            "{\"projectDeploymentId\":7,\"enabled\":true}", toolContext());
        String stringResult = toggleProjectDeploymentToolCallback.call(
            "{\"projectDeploymentId\":\"7\",\"enabled\":false}", toolContext());

        assertThat(integerResult).doesNotContain("error");
        assertThat(stringResult).doesNotContain("error");

        verify(projectDeploymentFacade).enableProjectDeployment(7L, true);
        verify(projectDeploymentFacade).enableProjectDeployment(7L, false);
    }

    static Stream<Arguments> numericIdParameters() {
        ProjectDeploymentFacade projectDeploymentFacade = mock(ProjectDeploymentFacade.class);
        McpProjectFacade mcpProjectFacade = mock(McpProjectFacade.class);
        KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade = mock(KnowledgeBaseDocumentFacade.class);
        WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade = mock(WorkspaceKnowledgeBaseFacade.class);

        return Stream.of(
            Arguments.of(new ListProjectDeploymentsToolCallback(projectDeploymentFacade), List.of("projectId")),
            Arguments.of(new CreateProjectDeploymentToolCallback(projectDeploymentFacade), List.of("projectId")),
            Arguments.of(new PromoteWorkflowToolCallback(projectDeploymentFacade), List.of("projectId")),
            Arguments.of(
                new DeleteProjectDeploymentToolCallback(projectDeploymentFacade), List.of("projectDeploymentId")),
            Arguments.of(
                new RollbackProjectDeploymentToolCallback(projectDeploymentFacade), List.of("projectDeploymentId")),
            Arguments.of(
                new ToggleProjectDeploymentToolCallback(projectDeploymentFacade), List.of("projectDeploymentId")),
            Arguments.of(
                new UpdateProjectDeploymentToolCallback(projectDeploymentFacade), List.of("projectDeploymentId")),
            Arguments.of(new CreateMcpProjectToolCallback(mcpProjectFacade), List.of("mcpServerId", "projectId")),
            Arguments.of(
                new CloneMcpProjectToolCallback(mcpProjectFacade), List.of("mcpProjectId", "targetMcpServerId")),
            Arguments.of(
                new UpdateMcpServerToolCallback(mock(WorkspaceMcpServerFacade.class)), List.of("mcpServerId")),
            Arguments.of(
                new AddKnowledgeBaseDocumentToolCallback(knowledgeBaseDocumentFacade, workspaceKnowledgeBaseFacade),
                List.of("knowledgeBaseId")),
            Arguments.of(
                new DeleteKnowledgeBaseDocumentToolCallback(knowledgeBaseDocumentFacade, workspaceKnowledgeBaseFacade),
                List.of("knowledgeBaseId", "documentId")),
            Arguments.of(
                new QueryKnowledgeBaseToolCallback(
                    workspaceKnowledgeBaseFacade, mock(KnowledgeBaseFacade.class), knowledgeBaseDocumentFacade),
                List.of("knowledgeBaseId")));
    }
}
