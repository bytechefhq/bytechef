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
import com.bytechef.ai.copilot.tool.ProjectWorkflowAgentToolCallback;
import com.bytechef.ai.copilot.tool.SkillsAgentToolCallback;
import com.bytechef.ai.copilot.tool.WorkflowExecutionAgentToolCallback;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolChatClientFactory;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolContributor;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.SimpleIntelligentToolDefinition;
import java.util.List;
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
        @Qualifier("workflowExecutionBuildSubAgentChatClientFactory") ObjectProvider<IntelligentToolChatClientFactory> workflowExecutionBuildFactoryProvider) {

        List<IntelligentToolDefinition> definitions = List.of(
            new SimpleIntelligentToolDefinition(
                "buildWorkflow", variant -> workflowEditorBuildFactoryProvider.getIfAvailable(),
                ProjectWorkflowAgentToolCallback::new),
            new SimpleIntelligentToolDefinition(
                "importWorkflow", variant -> converterBuildFactoryProvider.getIfAvailable(),
                ConverterAgentToolCallback::new),
            new SimpleIntelligentToolDefinition(
                "configureClusterElement", variant -> clusterElementBuildFactoryProvider.getIfAvailable(),
                ClusterElementAgentToolCallback::new),
            new SimpleIntelligentToolDefinition(
                "writeScript", variant -> codeEditorBuildFactoryProvider.getIfAvailable(),
                CodeEditorAgentToolCallback::new),
            new SimpleIntelligentToolDefinition(
                "authorSkill", variant -> skillsBuildFactoryProvider.getIfAvailable(),
                SkillsAgentToolCallback::new),
            new SimpleIntelligentToolDefinition(
                "debugWorkflowExecution", variant -> workflowExecutionBuildFactoryProvider.getIfAvailable(),
                WorkflowExecutionAgentToolCallback::new));

        return () -> definitions;
    }
}
