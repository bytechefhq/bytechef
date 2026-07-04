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

package com.bytechef.automation.ai.mcp.server.config;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.ai.mcp.server.facade.AutomationMcpToolFacade;
import com.bytechef.automation.ai.mcp.server.security.web.configurer.AutomationMcpServerSecurityConfigurer;
import com.bytechef.automation.ai.mcp.server.spi.McpServerWorkspaceToolCallbackContributor;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.server.FilterableMcpAsyncServer;
import com.bytechef.platform.mcp.server.FilterableMcpServerBuilder;
import com.bytechef.platform.mcp.server.McpAppWorkflowViewer;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.security.web.config.SecurityConfigurerContributor;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @author Ivica Cardic
 */
@Configuration
public class AutomationMcpServerConfiguration {

    public static final String SECRET_KEY = "secretKey";

    @Bean
    WebMvcStreamableServerTransportProvider automationWebMvcStreamableHttpServerTransportProvider() {
        return WebMvcStreamableServerTransportProvider.builder()
            .mcpEndpoint("/api/automation/{secretKey}/mcp")
            .contextExtractor(serverRequest -> {
                String secretKey = serverRequest.pathVariable(SECRET_KEY);

                return McpTransportContext.create(Map.of(SECRET_KEY, secretKey));
            })
            .build();
    }

    @Bean
    RouterFunction<ServerResponse> automationMcpRouterFunction() {
        return automationWebMvcStreamableHttpServerTransportProvider().getRouterFunction();
    }

    @Bean
    AutomationMcpToolFacade mcpToolFacade(
        ClusterElementDefinitionFacade clusterElementDefinitionFacade,
        ClusterElementDefinitionService clusterElementDefinitionService, TaskFileStorage durableTaskFileStorage,
        Evaluator evaluator, JobCompletionAwaiter jobCompletionAwaiter, McpComponentService mcpComponentService,
        McpProjectWorkflowService mcpProjectWorkflowService, McpServerService mcpServerService,
        PrincipalJobFacade principalJobFacade, ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        TaskExecutionService taskExecutionService, WorkflowService workflowService) {

        return new AutomationMcpToolFacade(
            clusterElementDefinitionFacade, clusterElementDefinitionService, evaluator, jobCompletionAwaiter,
            mcpComponentService, mcpProjectWorkflowService, mcpServerService, principalJobFacade,
            projectDeploymentWorkflowService, taskExecutionService, durableTaskFileStorage, workflowService);
    }

    @Bean
    FilterableMcpAsyncServer automationMcpAsyncServer(
        ApplicationProperties applicationProperties, McpComponentService mcpComponentService,
        McpProjectService mcpProjectService, McpServerService mcpServerService, McpToolService mcpToolService,
        AutomationMcpToolFacade mcpToolFacade,
        ObjectProvider<McpServerWorkspaceToolCallbackContributor> workspaceToolProviders,
        WorkspaceMcpServerService workspaceMcpServerService) {

        return new FilterableMcpServerBuilder(automationWebMvcStreamableHttpServerTransportProvider())
            .serverInfo("automation-mcp-server", "1.0.0")
            .capabilities(
                McpSchema.ServerCapabilities.builder()
                    .resources(false, true)
                    .tools(true)
                    .prompts(true)
                    .logging()
                    .build())
            .resourceSpecifications(
                McpAppWorkflowViewer.getResourceSpecifications(applicationProperties.getPublicUrl()))
            .toolFilter((exchange) -> {
                McpTransportContext mcpTransportContext = exchange.transportContext();

                Object secretKeyObject = mcpTransportContext.get(SECRET_KEY);

                if (secretKeyObject == null) {
                    return List.of();
                }

                return buildToolSpecifications(
                    secretKeyObject.toString(), mcpComponentService, mcpProjectService, mcpServerService,
                    mcpToolService, mcpToolFacade, workspaceToolProviders, workspaceMcpServerService);
            })
            .build();
    }

    static List<McpServerFeatures.AsyncToolSpecification> buildToolSpecifications(
        String secretKey, McpComponentService mcpComponentService, McpProjectService mcpProjectService,
        McpServerService mcpServerService, McpToolService mcpToolService, AutomationMcpToolFacade mcpToolFacade,
        ObjectProvider<McpServerWorkspaceToolCallbackContributor> workspaceToolProviders,
        WorkspaceMcpServerService workspaceMcpServerService) {

        McpServer mcpServer = mcpServerService.getMcpServer(secretKey);

        List<McpServerFeatures.AsyncToolSpecification> tools = new ArrayList<>();

        mcpComponentService.getMcpServerMcpComponents(mcpServer.getId())
            .stream()
            .flatMap(
                mcpComponent -> CollectionUtils.stream(
                    mcpToolService.getMcpComponentMcpTools(mcpComponent.getId())))
            .map(mcpTool -> McpToolUtils.toAsyncToolSpecification(mcpToolFacade.getFunctionToolCallback(mcpTool)))
            .forEach(tools::add);

        mcpProjectService.getMcpServerMcpProjects(mcpServer.getId())
            .stream()
            .flatMap(mcpProject -> CollectionUtils.stream(mcpToolFacade.getFunctionToolCallbacks(mcpProject)))
            .map(McpToolUtils::toAsyncToolSpecification)
            .forEach(tools::add);

        workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(mcpServer.getId())
            .ifPresent(workspaceId -> workspaceToolProviders.orderedStream()
                .flatMap(provider -> CollectionUtils.stream(provider.getFunctionToolCallbacks(workspaceId)))
                .map(McpToolUtils::toAsyncToolSpecification)
                .forEach(tools::add));

        return tools;
    }

    @Bean
    SecurityConfigurerContributor automationMcpServerSecurityConfigurerContributor(McpServerService mcpServerService) {
        return new SecurityConfigurerContributor() {

            @Override
            @SuppressWarnings("unchecked")
            public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>> T
                getSecurityConfigurerAdapter() {

                return (T) new AutomationMcpServerSecurityConfigurer(mcpServerService);
            }
        };
    }
}
