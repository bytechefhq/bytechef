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

import com.bytechef.ai.copilot.tool.ClusterElementAgentToolCallback;
import com.bytechef.ai.copilot.tool.CodeEditorAgentToolCallback;
import com.bytechef.ai.copilot.tool.ConverterAgentToolCallback;
import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.ProjectWorkflowAgentToolCallback;
import com.bytechef.ai.copilot.tool.SkillsAgentToolCallback;
import com.bytechef.ai.copilot.tool.WorkflowExecutionAgentToolCallback;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolContributor;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolScope;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.copilot.tool.catalog.SimpleIntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Contributes the six CE Copilot intelligent delegate tools — {@code buildWorkflow}, {@code importWorkflow},
 * {@code configureClusterElement}, {@code writeScript}, {@code authorSkill}, and {@code debugWorkflowExecution} — to
 * the {@link com.bytechef.ai.copilot.tool.catalog.IntelligentToolCatalog}.
 *
 * <p>
 * Every {@link IntelligentToolChatClientFactory} is resolved via an {@link ObjectProvider} so a missing bean simply
 * yields a {@code null} {@link IntelligentToolDefinition#chatClientFactory(IntelligentToolVariant)} for that variant,
 * which the catalog skips. Providers are looked up lazily — inside {@code chatClientFactory}, not while this bean
 * method builds the definition list — so declaring the six definitions never forces a {@code ChatClient} bean to be
 * instantiated; instantiation happens only when a surface (AI Hub, the management MCP contributor, a Copilot panel)
 * actually asks the catalog for a callback. Each {@link IntelligentToolChatClientFactory} bean is declared in
 * {@code CopilotConfiguration}, beside the collaborators its builder closes over.
 *
 * @author Ivica Cardic
 */
@Configuration
class CopilotIntelligentToolContributorConfiguration {

    @Bean
    IntelligentToolContributor copilotIntelligentToolContributor(
        @Qualifier("workflowEditorAskSubAgentChatClientFactory") //
        ObjectProvider<IntelligentToolChatClientFactory> workflowEditorAskFactoryProvider,
        @Qualifier("workflowEditorBuildSubAgentChatClientFactory") //
        ObjectProvider<IntelligentToolChatClientFactory> workflowEditorBuildFactoryProvider,
        @Qualifier("converterBuildSubAgentChatClientFactory") //
        ObjectProvider<IntelligentToolChatClientFactory> converterBuildFactoryProvider,
        @Qualifier("clusterElementAskSubAgentChatClientFactory") //
        ObjectProvider<IntelligentToolChatClientFactory> clusterElementAskFactoryProvider,
        @Qualifier("clusterElementBuildSubAgentChatClientFactory") //
        ObjectProvider<IntelligentToolChatClientFactory> clusterElementBuildFactoryProvider,
        @Qualifier("codeEditorAskSubAgentChatClientFactory") //
        ObjectProvider<IntelligentToolChatClientFactory> codeEditorAskFactoryProvider,
        @Qualifier("codeEditorBuildSubAgentChatClientFactory") //
        ObjectProvider<IntelligentToolChatClientFactory> codeEditorBuildFactoryProvider,
        @Qualifier("skillsAskSubAgentChatClientFactory") //
        ObjectProvider<IntelligentToolChatClientFactory> skillsAskFactoryProvider,
        @Qualifier("skillsBuildSubAgentChatClientFactory") //
        ObjectProvider<IntelligentToolChatClientFactory> skillsBuildFactoryProvider,
        @Qualifier("workflowExecutionAskSubAgentChatClientFactory") //
        ObjectProvider<IntelligentToolChatClientFactory> workflowExecutionAskFactoryProvider,
        @Qualifier("workflowExecutionBuildSubAgentChatClientFactory") //
        ObjectProvider<IntelligentToolChatClientFactory> workflowExecutionBuildFactoryProvider,
        ObjectProvider<SubAgentChatModelResolver> chatModelResolverProvider) {

        SubAgentChatModelResolver chatModelResolver = chatModelResolverProvider.getIfAvailable();

        List<IntelligentToolDefinition> definitions = List.of(
            new SimpleIntelligentToolDefinition(
                "buildWorkflow", CopilotAgentType.BUILD_WORKFLOW.key(),
                Set.of(IntelligentToolScope.PROJECT),
                variant -> factoryForVariant(
                    variant, workflowEditorAskFactoryProvider, workflowEditorBuildFactoryProvider),
                chatClientFactory -> new ProjectWorkflowAgentToolCallback(chatClientFactory, chatModelResolver)),
            // Copilot ships only a BUILD-mode Converter agent; there is deliberately no ASK client, so
            // chatClientFactory(ASK) is always null and the catalog skips this definition for ASK surfaces.
            // askCapable = false, the one opt-out in the catalog: prompt_converter_build.txt tells this agent to
            // never ask the user anything and to answer with valid JSON and no explanations. Handing it the
            // specialist askUserQuestion tool would contradict its own prompt, and a question envelope returned
            // where the caller expects a workflow definition is a silent contract break.
            new SimpleIntelligentToolDefinition(
                "importWorkflow", CopilotAgentType.IMPORT_WORKFLOW.key(), Set.of(IntelligentToolScope.PROJECT),
                variant -> variant == IntelligentToolVariant.BUILD
                    ? converterBuildFactoryProvider.getIfAvailable() : null,
                chatClientFactory -> new ConverterAgentToolCallback(chatClientFactory, chatModelResolver), false),
            new SimpleIntelligentToolDefinition(
                "configureClusterElement", CopilotAgentType.CONFIGURE_CLUSTER_ELEMENT.key(), Set.of(),
                variant -> factoryForVariant(
                    variant, clusterElementAskFactoryProvider, clusterElementBuildFactoryProvider),
                chatClientFactory -> new ClusterElementAgentToolCallback(chatClientFactory, chatModelResolver)),
            new SimpleIntelligentToolDefinition(
                "writeScript", CopilotAgentType.WRITE_SCRIPT.key(), Set.of(),
                variant -> factoryForVariant(variant, codeEditorAskFactoryProvider, codeEditorBuildFactoryProvider),
                chatClientFactory -> new CodeEditorAgentToolCallback(chatClientFactory, chatModelResolver)),
            // agentTypeKey is deliberately CopilotAgentType.SKILLS.key() ("skills"), NOT the callback's
            // "authorSkill" tool name. There is no "authorSkill"/"skills_agent" AgentType registered with
            // AgentTypeRegistry, and keying the per-conversation session memory on an unregistered key would
            // leave a session that task delete's registry-driven purge could never reconstruct and clean up.
            new SimpleIntelligentToolDefinition(
                "authorSkill", CopilotAgentType.SKILLS.key(), Set.of(),
                variant -> factoryForVariant(variant, skillsAskFactoryProvider, skillsBuildFactoryProvider),
                chatClientFactory -> new SkillsAgentToolCallback(chatClientFactory, chatModelResolver)),
            new SimpleIntelligentToolDefinition(
                "debugWorkflowExecution", CopilotAgentType.DEBUG_WORKFLOW_EXECUTION.key(), Set.of(),
                variant -> factoryForVariant(
                    variant, workflowExecutionAskFactoryProvider, workflowExecutionBuildFactoryProvider),
                chatClientFactory -> new WorkflowExecutionAgentToolCallback(chatClientFactory, chatModelResolver)));

        return () -> definitions;
    }

    @Nullable
    private static IntelligentToolChatClientFactory factoryForVariant(
        IntelligentToolVariant variant, ObjectProvider<IntelligentToolChatClientFactory> askProvider,
        ObjectProvider<IntelligentToolChatClientFactory> buildProvider) {

        return switch (variant) {
            case ASK -> askProvider.getIfAvailable();
            case BUILD -> buildProvider.getIfAvailable();
        };
    }
}
