/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ai.copilot.service.CopilotWorkflowGenerator;
import com.bytechef.atlas.execution.facade.JobFacade;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.facade.WorkspaceFacade;
import com.bytechef.automation.connection.event.ConnectionAfterSaveEventListener;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationComponentService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.listener.AutomationProjectConfigurationUpdatedEventListener;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.trigger.subscription.TriggerSubscriptionEventSender;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.TriggerDefinitionFacade;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.configuration.cache.WorkflowCacheManager;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.configuration.facade.WorkflowNodeParameterFacade;
import com.bytechef.platform.configuration.facade.WorkflowTestConfigurationFacade;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.configuration.service.WorkflowNodeTestOutputService;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.githubproxy.client.GitHubProxyClient;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import com.bytechef.platform.security.facade.ApiKeyFacade;
import com.bytechef.platform.security.service.ApiKeyService;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessor;
import com.bytechef.platform.workflow.execution.accessor.JobPrincipalAccessorRegistry;
import com.bytechef.platform.workflow.execution.facade.ConnectionLifecycleFacade;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.platform.workflow.execution.facade.TriggerLifecycleFacade;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import com.bytechef.platform.workflow.execution.service.TriggerExecutionService;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.test.config.testcontainers.PostgreSQLContainerConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Spring context for tests that run the trigger subscription lifecycle hooks against the real
 * {@link TriggerSubscriptionManager}, the real connection services and Postgres, with only the remote trigger calls and
 * the event sender mocked. Every test class that uses it shares one cached context.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    classes = {
        AutomationProjectFacadeIntTestConfiguration.class,
        TriggerSubscriptionLifecycleIntTest.LifecycleConfiguration.class
    },
    properties = {
        "bytechef.edition=EE",
        "bytechef.workflow.repository.jdbc.enabled=true",
        "bytechef.webhook-url=/webhooks/{id}",
        "spring.liquibase.contexts=configuration,connection,user",
        "spring.main.allow-bean-definition-overriding=true"
    })
@Import(PostgreSQLContainerConfiguration.class)
@MockitoBean(types = {
    ActionDefinitionFacade.class, ApiKeyFacade.class, ApiKeyService.class, AuthorityService.class,
    AutomationProjectConfigurationUpdatedEventListener.class, ClusterElementDefinitionService.class,
    ComponentConnectionFacade.class, ComponentDefinitionService.class, ConnectionDefinitionService.class,
    ConnectionLifecycleFacade.class, CopilotWorkflowGenerator.class, EmbeddedPermissionEvaluator.class,
    EnvironmentService.class, GitHubProxyClient.class, JobFacade.class, JobService.class, McpComponentService.class,
    McpIntegrationComponentService.class, McpIntegrationInstanceConfigurationService.class,
    McpIntegrationInstanceConfigurationWorkflowService.class, McpIntegrationInstanceToolService.class,
    McpServerService.class, McpToolService.class, OAuth2ParametersFacade.class, OAuth2Service.class,
    PrincipalJobFacade.class, PrincipalJobService.class, TaskDispatcherDefinitionService.class,
    TaskExecutionService.class, TriggerDefinitionFacade.class, TriggerDefinitionService.class,
    TriggerExecutionService.class, TriggerLifecycleFacade.class, TriggerSubscriptionEventSender.class,
    UserService.class, WorkflowCacheManager.class, WorkflowNodeParameterFacade.class,
    WorkflowNodeTestOutputService.class, WorkflowTestConfigurationFacade.class, WorkflowTestConfigurationService.class,
    WorkspaceConnectionFacade.class, WorkspaceFacade.class
})
public @interface TriggerSubscriptionLifecycleIntTest {

    @Configuration
    @ComponentScan({
        "com.bytechef.ee.embedded.connected.user.facade", "com.bytechef.ee.embedded.connected.user.service"
    })
    @Import(ConnectionAfterSaveEventListener.class)
    class LifecycleConfiguration {

        @Bean
        JobPrincipalAccessorRegistry jobPrincipalAccessorRegistry() {
            JobPrincipalAccessor jobPrincipalAccessor = mock(JobPrincipalAccessor.class);

            when(jobPrincipalAccessor.getType()).thenReturn(PlatformType.EMBEDDED);

            return new JobPrincipalAccessorRegistry(List.of(jobPrincipalAccessor));
        }
    }
}
