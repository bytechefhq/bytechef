/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.config;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.ai.mcp.server.facade.EmbeddedMcpToolFacade;
import com.bytechef.ee.embedded.ai.mcp.server.security.web.configurer.EmbeddedMcpServerSecurityConfigurer;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.server.FilterableMcpAsyncServer;
import com.bytechef.platform.mcp.server.FilterableMcpServerBuilder;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.security.web.config.SecurityConfigurerContributor;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.tenant.domain.TenantKey;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnEEVersion
public class EmbeddedMcpServerConfiguration {

    private static final String ENVIRONMENT = "environment";
    private static final String EXTERNAL_USER_ID = "externalUserId";
    private static final String SECRET_KEY = "secretKey";

    @Bean
    WebMvcStreamableServerTransportProvider embeddedWebMvcStreamableHttpServerTransportProvider() {
        return WebMvcStreamableServerTransportProvider.builder()
            .mcpEndpoint("/api/embedded/{secretKey}/mcp")
            .contextExtractor(serverRequest -> {
                String externalUserId = SecurityUtils.getCurrentUserLogin();
                String secretKey = serverRequest.pathVariable(SECRET_KEY);
                HttpServletRequest httpServletRequest = serverRequest.servletRequest();

                String environment = httpServletRequest.getHeader("X-Environment");

                return McpTransportContext.create(
                    Map.of(
                        ENVIRONMENT, environment,
                        EXTERNAL_USER_ID, externalUserId,
                        SECRET_KEY, secretKey));
            })
            .build();
    }

    @Bean
    RouterFunction<ServerResponse> embeddedMcpRouterFunction() {
        return embeddedWebMvcStreamableHttpServerTransportProvider().getRouterFunction();
    }

    @Bean
    EmbeddedMcpToolFacade embeddedMcpToolFacade(
        ApplicationProperties applicationProperties,
        ClusterElementDefinitionFacade clusterElementDefinitionFacade,
        ClusterElementDefinitionService clusterElementDefinitionService,
        ComponentDefinitionService componentDefinitionService, ConnectedUserService connectedUserService,
        TaskFileStorage durableTaskFileStorage, Evaluator evaluator,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceService integrationInstanceService,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService, IntegrationService integrationService,
        JobCompletionAwaiter jobCompletionAwaiter, JwtTokenService jwtTokenService,
        McpComponentService mcpComponentService,
        McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService,
        McpIntegrationInstanceToolService mcpIntegrationInstanceToolService, McpServerService mcpServerService,
        PrincipalJobFacade principalJobFacade, TaskExecutionService taskExecutionService,
        WorkflowService workflowService) {

        return new EmbeddedMcpToolFacade(
            clusterElementDefinitionFacade, clusterElementDefinitionService, componentDefinitionService,
            connectedUserService, evaluator, integrationInstanceConfigurationService,
            integrationInstanceConfigurationWorkflowService, integrationInstanceService,
            integrationInstanceWorkflowService, integrationService, jobCompletionAwaiter, jwtTokenService,
            mcpComponentService, mcpIntegrationInstanceConfigurationWorkflowService, mcpIntegrationInstanceToolService,
            mcpServerService, principalJobFacade, applicationProperties.getPublicUrl(), taskExecutionService,
            durableTaskFileStorage, workflowService);
    }

    @Bean
    FilterableMcpAsyncServer embeddedMcpAsyncServer(
        McpComponentService mcpComponentService,
        McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService,
        McpServerService mcpServerService, McpToolService mcpToolService,
        EmbeddedMcpToolFacade embeddedMcpToolFacade) {

        return new FilterableMcpServerBuilder(embeddedWebMvcStreamableHttpServerTransportProvider())
            .serverInfo("embedded-mcp-server", "1.0.0")
            .capabilities(
                McpSchema.ServerCapabilities.builder()
                    .resources(false, true)
                    .tools(true)
                    .prompts(true)
                    .logging()
                    .build())
            .toolFilter((exchange) -> {
                List<McpServerFeatures.AsyncToolSpecification> toolSpecifications = new ArrayList<>();

                McpTransportContext mcpTransportContext = exchange.transportContext();

                String externalUserId = (String) mcpTransportContext.get(EXTERNAL_USER_ID);
                com.bytechef.platform.configuration.domain.Environment environment = getEnvironment(
                    (String) mcpTransportContext.get(ENVIRONMENT));
                McpServer mcpServer = mcpServerService.getMcpServer((String) mcpTransportContext.get(SECRET_KEY));

                TenantKey tenantKey = TenantKey.parse(mcpServer.getSecretKey());

                String tenantId = tenantKey.getTenantId();

                mcpComponentService.getMcpServerMcpComponents(mcpServer.getId())
                    .stream()
                    .flatMap(
                        mcpComponent -> CollectionUtils.stream(
                            mcpToolService.getMcpComponentMcpTools(mcpComponent.getId())))
                    .forEach(mcpTool -> {
                        var callback = embeddedMcpToolFacade.getFunctionToolCallback(
                            mcpTool, externalUserId, environment, tenantId);

                        if (callback != null) {
                            toolSpecifications.add(McpToolUtils.toAsyncToolSpecification(callback));
                        }
                    });

                mcpIntegrationInstanceConfigurationService
                    .getMcpServerMcpIntegrationInstanceConfigurations(mcpServer.getId())
                    .stream()
                    .flatMap(mcpIntegrationInstanceConfiguration -> CollectionUtils.stream(
                        embeddedMcpToolFacade.getFunctionToolCallbacks(
                            mcpIntegrationInstanceConfiguration, externalUserId, environment, tenantId)))
                    .map(McpToolUtils::toAsyncToolSpecification)
                    .forEach(toolSpecifications::add);

                return toolSpecifications;
            })
            .build();
    }

    @Bean
    SecurityConfigurerContributor embeddedMcpServerSecurityConfigurerContributor(
        ConnectedUserService connectedUserService, SigningKeyService signingKeyService) {

        return new SecurityConfigurerContributor() {

            @Override
            @SuppressWarnings("unchecked")
            public <T extends AbstractHttpConfigurer<T, B>, B extends HttpSecurityBuilder<B>> T
                getSecurityConfigurerAdapter() {

                return (T) new EmbeddedMcpServerSecurityConfigurer(connectedUserService, signingKeyService);
            }
        };
    }

    private com.bytechef.platform.configuration.domain.Environment getEnvironment(String environment) {
        if (StringUtils.isNotBlank(environment)) {
            try {
                return com.bytechef.platform.configuration.domain.Environment.valueOf(environment.toUpperCase());
            } catch (IllegalArgumentException illegalArgumentException) {
                return com.bytechef.platform.configuration.domain.Environment.PRODUCTION;
            }
        }

        return com.bytechef.platform.configuration.domain.Environment.PRODUCTION;
    }
}
