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
import com.bytechef.ai.copilot.tool.ConverterAgentToolCallback.TargetWorkflowManager;
import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.PersistedWorkflowChecker;
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
import com.bytechef.automation.ai.tool.ProjectWorkflowLifecycleTools;
import com.bytechef.automation.ai.tool.model.ProjectWorkflowInfo;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
class CopilotIntelligentToolContributorConfiguration {

    private static final String IMPORTED_WORKFLOW_NAME = "Imported workflow";

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
        @Qualifier("workflowExecutionBuildSubAgentChatClientFactory") //
        ObjectProvider<IntelligentToolChatClientFactory> workflowExecutionBuildFactoryProvider,
        ObjectProvider<SubAgentChatModelResolver> chatModelResolverProvider,
        ObjectProvider<ProjectWorkflowLifecycleTools> projectWorkflowLifecycleToolsProvider,
        ObjectProvider<PersistedWorkflowChecker> persistedWorkflowCheckerProvider) {

        SubAgentChatModelResolver chatModelResolver = chatModelResolverProvider.getIfAvailable();
        TargetWorkflowManager targetWorkflowManager = getTargetWorkflowManager(
            projectWorkflowLifecycleToolsProvider.getIfAvailable());

        List<IntelligentToolDefinition> definitions = List.of(
            new SimpleIntelligentToolDefinition(
                "buildWorkflow", CopilotAgentType.BUILD_WORKFLOW,
                Set.of(IntelligentToolScope.PROJECT),
                variant -> factoryForVariant(
                    variant, workflowEditorAskFactoryProvider, workflowEditorBuildFactoryProvider),
                chatClientFactory -> new ProjectWorkflowAgentToolCallback(
                    chatClientFactory, chatModelResolver, persistedWorkflowCheckerProvider.getIfAvailable())),
            new SimpleIntelligentToolDefinition(
                "importWorkflow", CopilotAgentType.IMPORT_WORKFLOW, Set.of(IntelligentToolScope.PROJECT),
                variant -> variant == IntelligentToolVariant.BUILD
                    ? converterBuildFactoryProvider.getIfAvailable() : null,
                chatClientFactory -> new ConverterAgentToolCallback(
                    chatClientFactory, chatModelResolver, targetWorkflowManager),
                false),
            new SimpleIntelligentToolDefinition(
                "configureClusterElement", CopilotAgentType.CONFIGURE_CLUSTER_ELEMENT, Set.of(),
                variant -> factoryForVariant(
                    variant, clusterElementAskFactoryProvider, clusterElementBuildFactoryProvider),
                chatClientFactory -> new ClusterElementAgentToolCallback(chatClientFactory, chatModelResolver)),
            new SimpleIntelligentToolDefinition(
                "writeScript", CopilotAgentType.WRITE_SCRIPT, Set.of(),
                variant -> factoryForVariant(variant, codeEditorAskFactoryProvider, codeEditorBuildFactoryProvider),
                chatClientFactory -> new CodeEditorAgentToolCallback(chatClientFactory, chatModelResolver)),
            new SimpleIntelligentToolDefinition(
                "authorSkill", CopilotAgentType.SKILLS, Set.of(),
                variant -> factoryForVariant(variant, skillsAskFactoryProvider, skillsBuildFactoryProvider),
                chatClientFactory -> new SkillsAgentToolCallback(chatClientFactory, chatModelResolver)),
            new SimpleIntelligentToolDefinition(
                "debugWorkflowExecution", CopilotAgentType.DEBUG_WORKFLOW_EXECUTION, Set.of(),
                variant -> variant == IntelligentToolVariant.BUILD
                    ? workflowExecutionBuildFactoryProvider.getIfAvailable() : null,
                chatClientFactory -> new WorkflowExecutionAgentToolCallback(chatClientFactory, chatModelResolver)));

        return () -> definitions;
    }

    @Nullable
    private static TargetWorkflowManager getTargetWorkflowManager(
        @Nullable ProjectWorkflowLifecycleTools projectWorkflowLifecycleTools) {

        if (projectWorkflowLifecycleTools == null) {
            return null;
        }

        return new TargetWorkflowManager() {

            @Override
            public String createWorkflow(long projectId, ToolContext toolContext) {
                ProjectWorkflowInfo projectWorkflowInfo = projectWorkflowLifecycleTools.createProjectWorkflow(
                    projectId, IMPORTED_WORKFLOW_NAME, null, toolContext);

                return projectWorkflowInfo.workflowId();
            }

            @Override
            public void deleteWorkflow(String workflowId, ToolContext toolContext) {
                projectWorkflowLifecycleTools.deleteWorkflow(workflowId, toolContext);
            }
        };
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
