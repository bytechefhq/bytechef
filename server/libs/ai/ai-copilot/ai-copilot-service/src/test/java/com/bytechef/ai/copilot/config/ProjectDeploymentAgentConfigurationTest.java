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

package com.bytechef.ai.copilot.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.agui.core.exception.AGUIException;
import com.bytechef.ai.copilot.agent.SliceSpringAIAgent;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.automation.ai.tool.DeploymentToolCallbacksFactory;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Ivica Cardic
 */
final class ProjectDeploymentAgentConfigurationTest {

    private final ProjectDeploymentAgentConfiguration configuration = newConfiguration();

    private final SecurityContextRehydrator securityContextRehydrator = mock(SecurityContextRehydrator.class);

    private final DeploymentToolCallbacksFactory deploymentToolCallbacksFactory =
        new DeploymentToolCallbacksFactory(mock(ProjectDeploymentFacade.class));

    @Test
    void testAskAgentUsesReadToolsAndBuildAgentUsesWriteTools() throws AGUIException {
        SliceSpringAIAgent askAgent = configuration.projectDeploymentAskSpringAIAgent(
            mock(ChatMemory.class), mock(ChatModel.class), deploymentToolCallbacksFactory,
            securityContextRehydrator, emptyProvider());

        SliceSpringAIAgent buildAgent = configuration.projectDeploymentBuildSpringAIAgent(
            mock(ChatMemory.class), mock(ChatModel.class), deploymentToolCallbacksFactory,
            securityContextRehydrator, emptyProvider());

        assertThat(askAgent.getAgentId()).isEqualTo("project_deployment_ask");
        assertThat(buildAgent.getAgentId()).isEqualTo("project_deployment_build");

        List<String> buildToolNames = toolNames(
            configuration.buildToolCallbacks(securityContextRehydrator, deploymentToolCallbacksFactory));

        assertThat(buildToolNames).contains(
            "listProjectDeployments", "createProjectDeployment", "updateProjectDeployment",
            "deleteProjectDeployment", "rollbackProjectDeployment", "toggleProjectDeployment", "promoteWorkflow");
    }

    @Test
    void testAskAgentToolsAreReadOnly() {
        List<String> askToolNames = toolNames(
            configuration.askToolCallbacks(securityContextRehydrator, deploymentToolCallbacksFactory));

        assertThat(askToolNames).containsExactly("listProjectDeployments");
        assertThat(askToolNames).doesNotContain(
            "createProjectDeployment", "updateProjectDeployment", "deleteProjectDeployment",
            "rollbackProjectDeployment", "toggleProjectDeployment", "promoteWorkflow");
    }

    private static ProjectDeploymentAgentConfiguration newConfiguration() {
        ProjectDeploymentAgentConfiguration configuration = new ProjectDeploymentAgentConfiguration();

        setResourceField(configuration, "promptProjectDeploymentAskResource", "prompt_project_deployment_ask.txt");
        setResourceField(
            configuration, "promptProjectDeploymentBuildResource", "prompt_project_deployment_build.txt");

        return configuration;
    }

    private static void setResourceField(
        ProjectDeploymentAgentConfiguration configuration, String fieldName, String classpathResource) {

        try {
            Field field = ProjectDeploymentAgentConfiguration.class.getDeclaredField(fieldName);

            field.setAccessible(true);
            field.set(configuration, new ClassPathResource(classpathResource));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static List<String> toolNames(List<ToolCallback> toolCallbacks) {
        return toolCallbacks.stream()
            .map(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .toList();
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyProvider() {
        return mock(ObjectProvider.class);
    }
}
