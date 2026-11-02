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

import com.bytechef.automation.ai.agent.facade.AiAgentFacade;
import com.bytechef.automation.ai.mcp.facade.McpProjectFacade;
import com.bytechef.automation.ai.mcp.facade.McpProjectWorkflowFacade;
import com.bytechef.automation.ai.mcp.facade.WorkspaceMcpServerFacade;
import com.bytechef.automation.ai.tool.DeploymentToolCallbacksFactory;
import com.bytechef.automation.ai.tool.McpServerToolCallbacksFactory;
import com.bytechef.automation.ai.tool.aiagent.AiAgentToolCallbacksFactory;
import com.bytechef.automation.ai.tool.knowledgebase.KnowledgeBaseToolCallbacksFactory;
import com.bytechef.automation.configuration.facade.ProjectDeploymentFacade;
import com.bytechef.automation.knowledgebase.facade.WorkspaceKnowledgeBaseFacade;
import com.bytechef.platform.ai.skill.facade.AiSkillFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseDocumentFacade;
import com.bytechef.platform.knowledgebase.facade.KnowledgeBaseFacade;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;

/**
 * @author Ivica Cardic
 */
@AutoConfiguration
public class ToolCallbacksFactoryConfiguration {

    @Bean
    @ConditionalOnBean(AiAgentFacade.class)
    AiAgentToolCallbacksFactory aiAgentToolCallbacksFactory(
        AiAgentFacade aiAgentFacade, ClusterElementDefinitionService clusterElementDefinitionService,
        ObjectProvider<AiSkillFacade> aiSkillFacadeProvider,
        ObjectProvider<WorkspaceKnowledgeBaseFacade> workspaceKnowledgeBaseFacadeProvider) {

        return new AiAgentToolCallbacksFactory(
            aiAgentFacade, clusterElementDefinitionService, aiSkillFacadeProvider.getIfAvailable(),
            workspaceKnowledgeBaseFacadeProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnBean({
        WorkspaceKnowledgeBaseFacade.class, KnowledgeBaseFacade.class, KnowledgeBaseDocumentFacade.class
    })
    KnowledgeBaseToolCallbacksFactory knowledgeBaseToolCallbacksFactory(
        WorkspaceKnowledgeBaseFacade workspaceKnowledgeBaseFacade, KnowledgeBaseFacade knowledgeBaseFacade,
        KnowledgeBaseDocumentFacade knowledgeBaseDocumentFacade) {

        return new KnowledgeBaseToolCallbacksFactory(
            workspaceKnowledgeBaseFacade, knowledgeBaseFacade, knowledgeBaseDocumentFacade);
    }

    @Bean
    @ConditionalOnBean(ProjectDeploymentFacade.class)
    DeploymentToolCallbacksFactory deploymentToolCallbacksFactory(ProjectDeploymentFacade projectDeploymentFacade) {
        return new DeploymentToolCallbacksFactory(projectDeploymentFacade);
    }

    @Bean
    @ConditionalOnBean({
        McpProjectFacade.class, McpProjectWorkflowFacade.class, WorkspaceMcpServerFacade.class
    })
    McpServerToolCallbacksFactory mcpServerToolCallbacksFactory(
        McpProjectFacade mcpProjectFacade, McpProjectWorkflowFacade mcpProjectWorkflowFacade,
        WorkspaceMcpServerFacade workspaceMcpServerFacade) {

        return new McpServerToolCallbacksFactory(mcpProjectFacade, mcpProjectWorkflowFacade, workspaceMcpServerFacade);
    }
}
