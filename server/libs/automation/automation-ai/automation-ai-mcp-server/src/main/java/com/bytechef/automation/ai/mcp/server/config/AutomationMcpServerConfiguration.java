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
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.ai.mcp.server.facade.AutomationMcpToolFacade;
import com.bytechef.automation.ai.mcp.server.spi.McpServerWorkspaceToolCallbackContributor;
import com.bytechef.automation.ai.mcp.service.McpProjectService;
import com.bytechef.automation.ai.mcp.service.McpProjectWorkflowService;
import com.bytechef.automation.ai.mcp.service.WorkspaceMcpServerService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.server.FilterableMcpAsyncServer;
import com.bytechef.platform.mcp.server.FilterableMcpServerBuilder;
import com.bytechef.platform.mcp.server.McpToolAuthorizationEvaluator;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.plan.provider.PlanLimitsProvider;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.facade.JobResumeFacade;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.token.ApprovalTokens;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @author Ivica Cardic
 */
@Configuration
public class AutomationMcpServerConfiguration {

    public static final String SECRET_KEY = "secretKey";
    private static final String AUTHORITIES = "authorities";

    private static final McpToolAuthorizationEvaluator TOOL_AUTHORIZATION_EVALUATOR =
        new McpToolAuthorizationEvaluator();

    @Bean
    WebMvcStreamableServerTransportProvider automationWebMvcStreamableHttpServerTransportProvider() {
        return WebMvcStreamableServerTransportProvider.builder()
            .mcpEndpoint("/api/automation/{secretKey}/mcp")
            .contextExtractor(serverRequest -> {
                String secretKey = serverRequest.pathVariable(SECRET_KEY);

                return McpTransportContext.create(
                    Map.of(SECRET_KEY, secretKey, AUTHORITIES, SecurityUtils.fetchCurrentUserAuthorities()));
            })
            .build();
    }

    @Bean
    RouterFunction<ServerResponse> automationMcpRouterFunction() {
        return automationWebMvcStreamableHttpServerTransportProvider().getRouterFunction();
    }

    @Bean
    AutomationMcpToolFacade mcpToolFacade(
        ObjectProvider<ApprovalTokens> approvalTokensObjectProvider,
        ClusterElementDefinitionFacade clusterElementDefinitionFacade,
        ClusterElementDefinitionService clusterElementDefinitionService, TaskFileStorage durableTaskFileStorage,
        Evaluator evaluator, JobCompletionAwaiter jobCompletionAwaiter, JobResumeFacade jobResumeFacade,
        JobService jobService, McpComponentService mcpComponentService, McpProjectService mcpProjectService,
        McpProjectWorkflowService mcpProjectWorkflowService, McpServerService mcpServerService,
        McpToolService mcpToolService, ObjectProvider<PlanLimitsProvider> planLimitsProviderObjectProvider,
        PrincipalJobFacade principalJobFacade, ProjectDeploymentWorkflowService projectDeploymentWorkflowService,
        @Value("${bytechef.public-url:#{null}}") @Nullable String publicUrl,
        TaskExecutionService taskExecutionService, ToolExecutionRecorder toolExecutionRecorder,
        WorkflowService workflowService, WorkspaceMcpServerService workspaceMcpServerService) {

        return new AutomationMcpToolFacade(
            approvalTokensObjectProvider, clusterElementDefinitionFacade, clusterElementDefinitionService, evaluator,
            jobCompletionAwaiter, jobResumeFacade, jobService, mcpComponentService, mcpProjectService,
            mcpProjectWorkflowService, mcpServerService, mcpToolService, planLimitsProviderObjectProvider,
            principalJobFacade, projectDeploymentWorkflowService, publicUrl, taskExecutionService,
            durableTaskFileStorage, toolExecutionRecorder, workflowService, workspaceMcpServerService);
    }

    @Bean
    FilterableMcpAsyncServer automationMcpAsyncServer(
        McpComponentService mcpComponentService, McpProjectService mcpProjectService,
        McpServerService mcpServerService, McpToolService mcpToolService, AutomationMcpToolFacade mcpToolFacade,
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
            .toolFilter((exchange) -> {
                McpTransportContext mcpTransportContext = exchange.transportContext();

                Object secretKeyObject = mcpTransportContext.get(SECRET_KEY);

                if (secretKeyObject == null) {
                    return List.of();
                }

                return buildToolSpecifications(
                    secretKeyObject.toString(), authorities(mcpTransportContext), mcpComponentService,
                    mcpProjectService, mcpServerService, mcpToolService, mcpToolFacade, workspaceToolProviders,
                    workspaceMcpServerService);
            })
            .build();
    }

    @SuppressWarnings("unchecked")
    private static Set<String> authorities(McpTransportContext mcpTransportContext) {
        Object authoritiesObject = mcpTransportContext.get(AUTHORITIES);

        return authoritiesObject instanceof Set ? (Set<String>) authoritiesObject : Set.of();
    }

    /**
     * The components whose tools the principal may see. When the server does not enforce tool authorization, every
     * component is returned (legacy behavior); when it does, only components the principal is authorized for are kept
     * (deny-by-default).
     */
    static List<McpComponent> authorizedComponents(
        McpServer mcpServer, List<McpComponent> mcpComponents, Set<String> principalAuthorities) {

        if (!mcpServer.isEnforceToolAuthorization()) {
            return mcpComponents;
        }

        return mcpComponents.stream()
            .filter(
                mcpComponent -> TOOL_AUTHORIZATION_EVALUATOR.isComponentAuthorized(
                    principalAuthorities, mcpComponent.getRequiredAuthorities()))
            .toList();
    }

    static List<McpServerFeatures.AsyncToolSpecification> buildToolSpecifications(
        String secretKey, Set<String> principalAuthorities, McpComponentService mcpComponentService,
        McpProjectService mcpProjectService, McpServerService mcpServerService, McpToolService mcpToolService,
        AutomationMcpToolFacade mcpToolFacade,
        ObjectProvider<McpServerWorkspaceToolCallbackContributor> workspaceToolProviders,
        WorkspaceMcpServerService workspaceMcpServerService) {

        McpServer mcpServer = mcpServerService.getMcpServer(secretKey);

        List<McpServerFeatures.AsyncToolSpecification> tools = new ArrayList<>();

        authorizedComponents(
            mcpServer, mcpComponentService.getMcpServerMcpComponents(mcpServer.getId()), principalAuthorities)
                .stream()
                .flatMap(
                    mcpComponent -> CollectionUtils.stream(
                        mcpToolService.getMcpComponentMcpTools(mcpComponent.getId())))
                .filter(McpTool::isEnabled)
                .map(mcpTool -> McpToolUtils.toAsyncToolSpecification(mcpToolFacade.getFunctionToolCallback(mcpTool)))
                .forEach(tools::add);

        // Workflow-backed tools run synchronously and can pause on a human approval — decorate them with URL-mode
        // elicitation so capable clients get pointed at the hosted approval form and receive the resumed run's
        // real output in the same tools/call.
        mcpProjectService.getMcpServerMcpProjects(mcpServer.getId())
            .stream()
            .flatMap(mcpProject -> CollectionUtils.stream(mcpToolFacade.getFunctionToolCallbacks(mcpProject)))
            .map(McpToolUtils::toAsyncToolSpecification)
            .map(
                toolSpecification -> ApprovalElicitingToolSpecifications.decorate(
                    toolSpecification, mcpToolFacade, mcpServer.getId()))
            .forEach(tools::add);

        workspaceMcpServerService.fetchWorkspaceIdByMcpServerId(mcpServer.getId())
            .ifPresent(workspaceId -> workspaceToolProviders.orderedStream()
                .flatMap(provider -> CollectionUtils.stream(provider.getFunctionToolCallbacks(workspaceId)))
                .map(McpToolUtils::toAsyncToolSpecification)
                .forEach(tools::add));

        return tools;
    }
}
