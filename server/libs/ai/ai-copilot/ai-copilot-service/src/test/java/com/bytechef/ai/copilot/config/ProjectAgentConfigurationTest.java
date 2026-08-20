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
import static org.mockito.Mockito.when;

import com.agui.core.exception.AGUIException;
import com.bytechef.ai.copilot.agent.ProjectSpringAIAgent;
import com.bytechef.ai.copilot.tool.ClusterElementAgentToolCallback;
import com.bytechef.ai.copilot.tool.ConverterAgentToolCallback;
import com.bytechef.ai.copilot.tool.ProjectWorkflowAgentToolCallback;
import com.bytechef.ai.copilot.tool.SecurityContextRehydrator;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolContributor;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolScope;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.automation.ai.tool.ProjectTools;
import com.bytechef.automation.ai.tool.ProjectWorkflowTools;
import com.bytechef.automation.ai.tool.ReadProjectTools;
import com.bytechef.automation.ai.tool.ReadProjectWorkflowTools;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
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
        IntelligentToolCatalog catalog = catalogOf(projectWorkflowDefinition(), converterDefinition());

        ProjectSpringAIAgent askAgent = configuration.projectAskSpringAIAgent(
            mock(ChatMemory.class), mock(ChatModel.class), new ReadProjectTools(mock(ProjectTools.class)),
            new ReadProjectWorkflowTools(mock(ProjectWorkflowTools.class)), securityContextRehydrator,
            emptyProvider());

        ProjectSpringAIAgent buildAgent = configuration.projectBuildSpringAIAgent(
            mock(ChatMemory.class), mock(ChatModel.class), mock(ProjectTools.class), mock(ProjectWorkflowTools.class),
            securityContextRehydrator, catalog, emptyProvider());

        assertThat(askAgent.getAgentId()).isEqualTo("project_ask");
        assertThat(buildAgent.getAgentId()).isEqualTo("project_build");

        List<String> askToolNames = toolNames(
            configuration.askToolCallbacks(
                securityContextRehydrator, new ReadProjectTools(mock(ProjectTools.class)),
                new ReadProjectWorkflowTools(mock(ProjectWorkflowTools.class))));

        assertThat(askToolNames).contains("listProjects")
            .doesNotContain("createProject", "buildWorkflow", "importWorkflow");

        List<String> buildToolNames = toolNames(
            configuration.buildToolCallbacks(
                securityContextRehydrator, mock(ProjectTools.class), mock(ProjectWorkflowTools.class), catalog));

        assertThat(buildToolNames).contains("createProject", "buildWorkflow", "importWorkflow");
    }

    /**
     * {@code buildWorkflow} and {@code importWorkflow} are the only two definitions
     * {@code CopilotIntelligentToolContributorConfiguration} scopes to {@link IntelligentToolScope#PROJECT} — every
     * other intelligent delegate (e.g. {@code configureClusterElement}, which ships with an empty
     * {@code panelScopes()}) must not reach this panel's build tool list.
     */
    @Test
    void testBuildToolCallbacksOmitsDefinitionsNotScopedToProjectPanel() {
        List<String> buildToolNames = toolNames(
            configuration.buildToolCallbacks(
                securityContextRehydrator, mock(ProjectTools.class), mock(ProjectWorkflowTools.class),
                catalogOf(clusterElementDefinition())));

        assertThat(buildToolNames).contains("createProject")
            .doesNotContain("configureClusterElement");
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
    private static <T> ObjectProvider<T> emptyProvider() {
        return mock(ObjectProvider.class);
    }

    private static IntelligentToolDefinition projectWorkflowDefinition() {
        ChatClient chatClient = mock(ChatClient.class);

        return new FakeIntelligentToolDefinition(
            "buildWorkflow", Set.of(IntelligentToolScope.PROJECT),
            Map.of(IntelligentToolVariant.BUILD, (IntelligentToolChatClientFactory) chatModel -> chatClient),
            chatClientFactory -> new ProjectWorkflowAgentToolCallback(chatClientFactory, null));
    }

    private static IntelligentToolDefinition converterDefinition() {
        ChatClient chatClient = mock(ChatClient.class);

        return new FakeIntelligentToolDefinition(
            "importWorkflow", Set.of(IntelligentToolScope.PROJECT),
            Map.of(IntelligentToolVariant.BUILD, (IntelligentToolChatClientFactory) chatModel -> chatClient),
            chatClientFactory -> new ConverterAgentToolCallback(chatClientFactory, null));
    }

    private static IntelligentToolDefinition clusterElementDefinition() {
        ChatClient chatClient = mock(ChatClient.class);

        return new FakeIntelligentToolDefinition(
            "configureClusterElement", Set.of(),
            Map.of(IntelligentToolVariant.BUILD, (IntelligentToolChatClientFactory) chatModel -> chatClient),
            chatClientFactory -> new ClusterElementAgentToolCallback(chatClientFactory, null));
    }

    private static IntelligentToolCatalog catalogOf(IntelligentToolDefinition... definitions) {
        IntelligentToolContributor contributor = () -> List.of(definitions);

        return new IntelligentToolCatalog(fixedObjectProvider(contributor));
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<IntelligentToolContributor> fixedObjectProvider(
        IntelligentToolContributor contributor) {

        ObjectProvider<IntelligentToolContributor> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.orderedStream()).thenReturn(Stream.of(contributor));

        return objectProvider;
    }

    private static final class FakeIntelligentToolDefinition implements IntelligentToolDefinition {

        private final String name;
        private final Set<IntelligentToolScope> panelScopes;
        private final Map<IntelligentToolVariant, IntelligentToolChatClientFactory> chatClientFactoriesByVariant;
        private final Function<IntelligentToolChatClientFactory, ToolCallback> toolCallbackFactory;

        private FakeIntelligentToolDefinition(
            String name, Set<IntelligentToolScope> panelScopes,
            Map<IntelligentToolVariant, IntelligentToolChatClientFactory> chatClientFactoriesByVariant,
            Function<IntelligentToolChatClientFactory, ToolCallback> toolCallbackFactory) {

            this.name = name;
            this.panelScopes = panelScopes;
            this.chatClientFactoriesByVariant = chatClientFactoriesByVariant;
            this.toolCallbackFactory = toolCallbackFactory;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String agentTypeKey() {
            return name;
        }

        @Override
        @SuppressFBWarnings("EI_EXPOSE_REP")
        public Set<IntelligentToolScope> panelScopes() {
            return panelScopes;
        }

        @Override
        @Nullable
        public IntelligentToolChatClientFactory chatClientFactory(IntelligentToolVariant variant) {
            return chatClientFactoriesByVariant.get(variant);
        }

        @Override
        public ToolCallback create(IntelligentToolChatClientFactory chatClientFactory) {
            return toolCallbackFactory.apply(chatClientFactory);
        }
    }
}
