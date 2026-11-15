/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.config;

import static com.bytechef.ee.embedded.ai.mcp.server.facade.EmbeddedMcpToolCallback.INTEGRATION_ID_META_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.embedded.ai.mcp.server.facade.EmbeddedMcpToolCallback;
import com.bytechef.ee.embedded.ai.mcp.server.facade.EmbeddedMcpToolFacade;
import com.bytechef.ee.embedded.ai.mcp.service.McpAutomationProjectConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpAutomationProjectConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationComponentService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserMcpAutomationResolver;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.ee.embedded.security.service.JwtTokenService;
import com.bytechef.ee.embedded.security.service.SigningKeyService;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.external.invocation.config.ExternalInvocationConfiguration;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.security.web.mcp.McpAnonymousAuthenticationToken;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class EmbeddedMcpServerConfigurationIntTest {

    private static final List<Class<?>> MOCKED_DEPENDENCY_TYPES = List.of(
        ApplicationProperties.class, ClusterElementDefinitionFacade.class, ClusterElementDefinitionService.class,
        ComponentDefinitionService.class, ConnectedUserMcpAutomationResolver.class, ConnectedUserService.class,
        EnvironmentService.class, Evaluator.class,
        IntegrationInstanceConfigurationService.class, IntegrationInstanceConfigurationWorkflowService.class,
        IntegrationInstanceService.class, IntegrationInstanceWorkflowService.class, IntegrationService.class,
        JobCompletionAwaiter.class, JwtTokenService.class, McpAutomationProjectConfigurationService.class,
        McpAutomationProjectConfigurationWorkflowService.class, McpComponentService.class,
        McpConnectedUserProjectWorkflowService.class,
        McpIntegrationComponentService.class, McpIntegrationInstanceConfigurationService.class,
        McpIntegrationInstanceConfigurationWorkflowService.class,
        McpIntegrationInstanceToolService.class, McpIntegrationInstanceWorkflowService.class, McpServerService.class,
        McpToolService.class,
        PrincipalJobFacade.class, SigningKeyService.class, TaskExecutionService.class, WorkflowService.class);

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testEmbeddedMcpToolFacadeUsesTheDurableTaskFileStorageWithoutAPrivateCoordinator() {
        ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
            .withPropertyValues("bytechef.edition=ee")
            .withBean("durableTaskFileStorage", TaskFileStorage.class, () -> mock(TaskFileStorage.class))
            .withUserConfiguration(EmbeddedMcpServerConfiguration.class, ExternalInvocationConfiguration.class);

        for (Class<?> mockedDependencyType : MOCKED_DEPENDENCY_TYPES) {
            applicationContextRunner = withMockBean(applicationContextRunner, mockedDependencyType);
        }

        applicationContextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(EmbeddedMcpToolFacade.class);

            ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();

            assertThat(beanFactory.getDependenciesForBean("embeddedMcpToolFacade"))
                .contains("durableTaskFileStorage")
                .doesNotContain("syncWorkerExecutor");
        });
    }

    @Test
    void testTransportContextCarriesConnectedUser() {
        User user = new User("ext-user-1", "", List.of());

        setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));

        McpTransportContext mcpTransportContext = EmbeddedMcpServerConfiguration.createMcpTransportContext(
            "server-secret", "PRODUCTION");

        assertThat(mcpTransportContext.get("externalUserId")).isEqualTo("ext-user-1");
        assertThat(mcpTransportContext.get("environment")).isEqualTo("PRODUCTION");
        assertThat(mcpTransportContext.get("secretKey")).isEqualTo("server-secret");
    }

    @Test
    void testTransportContextOmitsExternalUserForAnonymousCaller() {
        setAuthentication(McpAnonymousAuthenticationToken.of("mcp-anonymous:embedded:1050"));

        McpTransportContext mcpTransportContext = EmbeddedMcpServerConfiguration.createMcpTransportContext(
            "server-secret", null);

        assertThat(mcpTransportContext.get("externalUserId")).isNull();
        assertThat(mcpTransportContext.get("environment")).isNull();
        assertThat(mcpTransportContext.get("secretKey")).isEqualTo("server-secret");
    }

    private static void setAuthentication(Authentication authentication) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    private static <T> ApplicationContextRunner withMockBean(
        ApplicationContextRunner applicationContextRunner, Class<T> beanType) {

        return applicationContextRunner.withBean(beanType, () -> mock(beanType));
    }

    @Test
    void testToolSpecificationCarriesTheMetaOfAnEmbeddedToolCallback() {
        McpServerFeatures.AsyncToolSpecification toolSpecification =
            EmbeddedMcpServerConfiguration.toAsyncToolSpecification(
                EmbeddedMcpToolCallback.ofIntegration(createToolCallback(), 7L));

        McpSchema.Tool tool = toolSpecification.tool();

        assertThat(tool.name()).isEqualTo("myTool");
        assertThat(tool.description()).isEqualTo("My tool");
        assertThat(tool.meta()).containsExactly(entry(INTEGRATION_ID_META_KEY, 7L));
    }

    @Test
    void testToolSpecificationOfAPlainToolCallbackHasNoMeta() {
        McpServerFeatures.AsyncToolSpecification toolSpecification =
            EmbeddedMcpServerConfiguration.toAsyncToolSpecification(createToolCallback());

        McpSchema.Tool tool = toolSpecification.tool();

        assertThat(tool.meta()).isNull();
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private static ToolCallback createToolCallback() {
        return FunctionToolCallback.builder("myTool", (Map<String, Object> input) -> "ok")
            .inputType(Map.class)
            .description("My tool")
            .build();
    }
}
