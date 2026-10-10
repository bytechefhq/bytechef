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
import com.bytechef.ai.copilot.tool.catalog.SimpleIntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.SubAgentChatModelResolver;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Ivica Cardic
 */
@Configuration
class CopilotIntelligentToolContributorConfiguration {

    @Bean
    IntelligentToolContributor copilotIntelligentToolContributor(
        @Qualifier("workflowEditorBuildSubAgentChatClientFactory") ObjectProvider<IntelligentToolChatClientFactory> workflowEditorBuildFactoryProvider,
        @Qualifier("converterBuildSubAgentChatClientFactory") ObjectProvider<IntelligentToolChatClientFactory> converterBuildFactoryProvider,
        @Qualifier("clusterElementBuildSubAgentChatClientFactory") ObjectProvider<IntelligentToolChatClientFactory> clusterElementBuildFactoryProvider,
        @Qualifier("codeEditorBuildSubAgentChatClientFactory") ObjectProvider<IntelligentToolChatClientFactory> codeEditorBuildFactoryProvider,
        @Qualifier("skillsBuildSubAgentChatClientFactory") ObjectProvider<IntelligentToolChatClientFactory> skillsBuildFactoryProvider,
        @Qualifier("workflowExecutionBuildSubAgentChatClientFactory") ObjectProvider<IntelligentToolChatClientFactory> workflowExecutionBuildFactoryProvider,
        ObjectProvider<SubAgentChatModelResolver> chatModelResolverProvider) {

        SubAgentChatModelResolver chatModelResolver = chatModelResolverProvider.getIfAvailable();

        List<IntelligentToolDefinition> definitions = List.of(
            new SimpleIntelligentToolDefinition(
                "buildWorkflow", CopilotAgentType.BUILD_WORKFLOW.key(), Set.of(IntelligentToolScope.PROJECT),
                variant -> workflowEditorBuildFactoryProvider.getIfAvailable(),
                chatClientFactory -> new ProjectWorkflowAgentToolCallback(chatClientFactory, chatModelResolver)),
            new SimpleIntelligentToolDefinition(
                "importWorkflow", CopilotAgentType.IMPORT_WORKFLOW.key(), Set.of(IntelligentToolScope.PROJECT),
                variant -> converterBuildFactoryProvider.getIfAvailable(),
                chatClientFactory -> new ConverterAgentToolCallback(chatClientFactory, chatModelResolver)),
            new SimpleIntelligentToolDefinition(
                "configureClusterElement", CopilotAgentType.CONFIGURE_CLUSTER_ELEMENT.key(), Set.of(),
                variant -> clusterElementBuildFactoryProvider.getIfAvailable(),
                chatClientFactory -> new ClusterElementAgentToolCallback(chatClientFactory, chatModelResolver)),
            new SimpleIntelligentToolDefinition(
                "writeScript", CopilotAgentType.WRITE_SCRIPT.key(), Set.of(),
                variant -> codeEditorBuildFactoryProvider.getIfAvailable(),
                chatClientFactory -> new CodeEditorAgentToolCallback(chatClientFactory, chatModelResolver)),
            new SimpleIntelligentToolDefinition(
                "authorSkill", CopilotAgentType.SKILLS.key(), Set.of(),
                variant -> skillsBuildFactoryProvider.getIfAvailable(),
                chatClientFactory -> new SkillsAgentToolCallback(chatClientFactory, chatModelResolver)),
            new SimpleIntelligentToolDefinition(
                "debugWorkflowExecution", CopilotAgentType.DEBUG_WORKFLOW_EXECUTION.key(), Set.of(),
                variant -> workflowExecutionBuildFactoryProvider.getIfAvailable(),
                chatClientFactory -> new WorkflowExecutionAgentToolCallback(chatClientFactory, chatModelResolver)));

        return () -> definitions;
    }
}
