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

import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolContributor;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import com.bytechef.automation.configuration.tool.ProjectWorkflowLifecycleTools;
import com.bytechef.automation.configuration.tool.WorkflowIntelligentToolContributorConfiguration;
import com.bytechef.automation.workflow.execution.tool.WorkflowExecutionIntelligentToolContributorConfiguration;
import com.bytechef.platform.ai.skill.tool.SkillsIntelligentToolContributorConfiguration;
import com.bytechef.platform.component.tool.ClusterElementIntelligentToolContributorConfiguration;
import com.bytechef.platform.workflow.tool.CodeEditorIntelligentToolContributorConfiguration;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
final class IntelligentToolAskCapabilityTest {

    @Test
    void testOnlyImportWorkflowIsNotAskCapable() {
        List<IntelligentToolDefinition> definitions = definitions();

        assertThat(definitions).hasSize(6);

        assertThat(definitions).filteredOn(definition -> !definition.askCapable())
            .extracting(IntelligentToolDefinition::name)
            .containsExactly("importWorkflow");
    }

    @Test
    void testConverterPromptStillForbidsAsking() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/prompt_converter_build.txt")) {
            assertThat(inputStream).isNotNull();

            String prompt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(prompt).contains("You must NEVER ask the user any questions");
        }
    }

    @Test
    void testSubAgentPromptSaysItHasNoConnectionPickers() throws IOException {
        try (InputStream inputStream =
            getClass().getResourceAsStream("/prompt_workflow_editor_build_sub_agent.txt")) {

            assertThat(inputStream).isNotNull();

            String prompt = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(prompt).contains("You do not have these tools: selectConnection")
                .doesNotContain("askUserQuestion");
        }
    }

    private static List<IntelligentToolDefinition> definitions() {
        IntelligentToolContributor workflowContributor = workflowIntelligentToolContributor(
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(SubAgentChatModelResolver.class)),
            absent());

        IntelligentToolContributor platformContributor = platformIntelligentToolContributor(
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(IntelligentToolChatClientFactory.class)),
            present(mock(SubAgentChatModelResolver.class)));

        List<IntelligentToolDefinition> definitions = new ArrayList<>(
            workflowContributor.getIntelligentToolDefinitions());

        definitions.addAll(platformContributor.getIntelligentToolDefinitions());

        return definitions;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> present(T value) {
        ObjectProvider<T> objectProvider = mock(ObjectProvider.class);

        when(objectProvider.getIfAvailable()).thenReturn(value);

        return objectProvider;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> absent() {
        return mock(ObjectProvider.class);
    }

    private static IntelligentToolContributor platformIntelligentToolContributor(
        ObjectProvider<IntelligentToolChatClientFactory> clusterElementAskFactoryProvider,
        ObjectProvider<IntelligentToolChatClientFactory> clusterElementBuildFactoryProvider,
        ObjectProvider<IntelligentToolChatClientFactory> codeEditorAskFactoryProvider,
        ObjectProvider<IntelligentToolChatClientFactory> codeEditorBuildFactoryProvider,
        ObjectProvider<IntelligentToolChatClientFactory> skillsAskFactoryProvider,
        ObjectProvider<IntelligentToolChatClientFactory> skillsBuildFactoryProvider,
        ObjectProvider<SubAgentChatModelResolver> chatModelResolverProvider) {

        IntelligentToolContributor contributor1 =
            new ClusterElementIntelligentToolContributorConfiguration().clusterElementIntelligentToolContributor(
                clusterElementAskFactoryProvider, clusterElementBuildFactoryProvider, chatModelResolverProvider);
        IntelligentToolContributor contributor2 =
            new CodeEditorIntelligentToolContributorConfiguration().codeEditorIntelligentToolContributor(
                codeEditorAskFactoryProvider, codeEditorBuildFactoryProvider, chatModelResolverProvider);
        IntelligentToolContributor contributor3 =
            new SkillsIntelligentToolContributorConfiguration().skillsIntelligentToolContributor(
                skillsAskFactoryProvider, skillsBuildFactoryProvider, chatModelResolverProvider);

        List<String> order = List.of("configureClusterElement", "writeScript", "authorSkill");

        return () -> Stream.of(contributor1, contributor2, contributor3)
            .flatMap(contributor -> contributor.getIntelligentToolDefinitions()
                .stream())
            .sorted(Comparator.comparingInt(definition -> order.indexOf(definition.name())))
            .toList();
    }

    private static IntelligentToolContributor workflowIntelligentToolContributor(
        ObjectProvider<IntelligentToolChatClientFactory> workflowEditorAskFactoryProvider,
        ObjectProvider<IntelligentToolChatClientFactory> workflowEditorBuildFactoryProvider,
        ObjectProvider<IntelligentToolChatClientFactory> converterBuildFactoryProvider,
        ObjectProvider<IntelligentToolChatClientFactory> workflowExecutionBuildFactoryProvider,
        ObjectProvider<SubAgentChatModelResolver> chatModelResolverProvider,
        ObjectProvider<ProjectWorkflowLifecycleTools> projectWorkflowLifecycleToolsProvider) {

        IntelligentToolContributor contributor1 =
            new WorkflowExecutionIntelligentToolContributorConfiguration().workflowExecutionIntelligentToolContributor(
                workflowExecutionBuildFactoryProvider, chatModelResolverProvider);
        IntelligentToolContributor contributor2 = new WorkflowIntelligentToolContributorConfiguration()
            .workflowIntelligentToolContributor(workflowEditorAskFactoryProvider, workflowEditorBuildFactoryProvider,
                converterBuildFactoryProvider, chatModelResolverProvider, projectWorkflowLifecycleToolsProvider);

        List<String> order = List.of("buildWorkflow", "importWorkflow", "debugWorkflowExecution");

        return () -> Stream.of(contributor1, contributor2)
            .flatMap(contributor -> contributor.getIntelligentToolDefinitions()
                .stream())
            .sorted(Comparator.comparingInt(definition -> order.indexOf(definition.name())))
            .toList();
    }
}
