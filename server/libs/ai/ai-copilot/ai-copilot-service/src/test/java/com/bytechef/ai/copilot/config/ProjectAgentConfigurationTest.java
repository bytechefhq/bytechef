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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.agui.core.exception.AGUIException;
import com.bytechef.ai.copilot.agent.ProjectSpringAIAgent;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.automation.ai.tool.ProjectTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ReadProjectTools;
import com.bytechef.automation.ai.tool.ReadProjectWorkflowTools;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Ivica Cardic
 */
final class ProjectAgentConfigurationTest {

    private final ProjectAgentConfiguration configuration = newConfiguration();

    private final SecurityContextRehydrator securityContextRehydrator = mock(SecurityContextRehydrator.class);

    @Test
    void testAskAgentUsesReadToolsAndBuildAgentUsesWriteTools() throws AGUIException {
        ProjectSpringAIAgent askAgent = configuration.projectAskSpringAIAgent(
            mock(ChatMemory.class), mock(ChatModel.class), new ReadProjectTools(mock(ProjectTools.class)),
            new ReadProjectWorkflowTools(mock(ProjectWorkflowTools.class)), securityContextRehydrator,
            emptyProvider());

        ProjectSpringAIAgent buildAgent = configuration.projectBuildSpringAIAgent(
            mock(ChatMemory.class), mock(ChatModel.class), mock(ProjectTools.class), mock(ProjectWorkflowTools.class),
            securityContextRehydrator, present(mock(ChatClient.class)), present(() -> mock(ChatClient.class)),
            emptyProvider());

        assertThat(askAgent.getAgentId()).isEqualTo("project_ask");
        assertThat(buildAgent.getAgentId()).isEqualTo("project_build");

        List<String> askToolNames = toolNames(
            configuration.askToolCallbacks(
                securityContextRehydrator, new ReadProjectTools(mock(ProjectTools.class)),
                new ReadProjectWorkflowTools(mock(ProjectWorkflowTools.class))));

        assertThat(askToolNames).contains("listProjects")
            .doesNotContain("createProject");

        List<String> buildToolNames = toolNames(
            configuration.buildToolCallbacks(
                securityContextRehydrator, mock(ProjectTools.class), mock(ProjectWorkflowTools.class),
                present(mock(ChatClient.class)), present(() -> mock(ChatClient.class))));

        assertThat(buildToolNames).contains("createProject", "project_workflow_agent");
    }

    /**
     * {@code buildToolCallbacks} is exercised directly with empty {@link ObjectProvider}s here purely to verify the
     * defensive {@code ifAvailable} skip mechanics. This is NOT a supported runtime configuration: since
     * {@code ProjectAgentConfiguration} is now gated on {@code bytechef.ai.copilot.enabled} alone (matching
     * {@code CopilotConfiguration}, which declares the {@code workflowEditorBuildSubAgentChatClient} and
     * {@code converterBuildSubAgentChatClientSupplier} beans), the delegates are always present whenever this
     * configuration's beans are registered.
     */
    @Test
    void testBuildToolCallbacksSkipsDelegatesWhenProvidersEmpty() {
        List<String> buildToolNames = toolNames(
            configuration.buildToolCallbacks(
                securityContextRehydrator, mock(ProjectTools.class), mock(ProjectWorkflowTools.class),
                emptyProvider(), emptyProvider()));

        assertThat(buildToolNames).contains("createProject")
            .doesNotContain("project_workflow_agent", "converter_agent");
    }

    private static ProjectAgentConfiguration newConfiguration() {
        ProjectAgentConfiguration configuration = new ProjectAgentConfiguration();

        setResourceField(configuration, "promptProjectAskResource", "prompt_project_ask.txt");
        setResourceField(configuration, "promptProjectBuildResource", "prompt_project_build.txt");

        return configuration;
    }

    private static void setResourceField(
        ProjectAgentConfiguration configuration, String fieldName, String classpathResource) {

        try {
            Field field = ProjectAgentConfiguration.class.getDeclaredField(fieldName);

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
    private static <T> ObjectProvider<T> present(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);

        doAnswer(invocation -> {
            Consumer<T> consumer = invocation.getArgument(0);

            consumer.accept(value);

            return null;
        }).when(provider)
            .ifAvailable(any());

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> emptyProvider() {
        return mock(ObjectProvider.class);
    }
}
