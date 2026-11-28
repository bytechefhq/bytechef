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

package com.bytechef.automation.configuration.tool.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.mcp.server.spi.McpServerToolCallbackContributor;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.tool.DeploymentToolCallbacksFactory;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class DeploymentMcpContributorConfigurationTest {

    private final DeploymentMcpContributorConfiguration configuration =
        new DeploymentMcpContributorConfiguration();
    private final WorkspaceFacade workspaceFacade = mock(WorkspaceFacade.class);

    @Test
    void contributesTheSevenFlatDeploymentToolsWhenFactoryPresent() {
        DeploymentToolCallbacksFactory deploymentToolCallbacksFactory = new DeploymentToolCallbacksFactory(
            mock(ProjectDeploymentFacade.class), mock(ProjectWorkflowService.class));

        McpServerToolCallbackContributor contributor = configuration.deploymentFlatCrudMcpContributor(
            presentFactory(deploymentToolCallbacksFactory), workspaceFacade);

        assertThat(contributor.getToolCallbacks())
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrder(
                "listProjectDeployments", "createProjectDeployment", "updateProjectDeployment",
                "deleteProjectDeployment", "rollbackProjectDeployment", "toggleProjectDeployment", "promoteWorkflow");
    }

    @Test
    void deploymentContributorSkipsWhenFactoryAbsent() {
        McpServerToolCallbackContributor contributor = configuration.deploymentFlatCrudMcpContributor(
            absentFactory(), workspaceFacade);

        assertThat(contributor.getToolCallbacks()).isEmpty();
    }

    @Test
    void onlyListProjectDeploymentsAcceptsWorkspaceId() {
        DeploymentToolCallbacksFactory deploymentToolCallbacksFactory = new DeploymentToolCallbacksFactory(
            mock(ProjectDeploymentFacade.class), mock(ProjectWorkflowService.class));

        McpServerToolCallbackContributor contributor = configuration.deploymentFlatCrudMcpContributor(
            presentFactory(deploymentToolCallbacksFactory), workspaceFacade);

        List<ToolCallback> toolCallbacks = contributor.getToolCallbacks();

        ToolCallback listToolCallback = toolCallbacks.stream()
            .filter(toolCallback -> "listProjectDeployments".equals(
                toolCallback.getToolDefinition()
                    .name()))
            .findFirst()
            .orElseThrow();

        assertThat(listToolCallback.getToolDefinition()
            .inputSchema()).contains("workspaceId");

        List<ToolCallback> otherToolCallbacks = toolCallbacks.stream()
            .filter(toolCallback -> !"listProjectDeployments".equals(
                toolCallback.getToolDefinition()
                    .name()))
            .toList();

        assertThat(otherToolCallbacks)
            .allSatisfy(toolCallback -> assertThat(toolCallback.getToolDefinition()
                .inputSchema()).doesNotContain("workspaceId"));
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<DeploymentToolCallbacksFactory> presentFactory(
        DeploymentToolCallbacksFactory deploymentToolCallbacksFactory) {

        ObjectProvider<DeploymentToolCallbacksFactory> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(deploymentToolCallbacksFactory);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<DeploymentToolCallbacksFactory> absentFactory() {
        return mock(ObjectProvider.class);
    }
}
