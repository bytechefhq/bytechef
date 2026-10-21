/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.config;

import com.bytechef.async.config.AsyncConfiguration;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.atlas.execution.service.TaskExecutionService;
import com.bytechef.atlas.file.storage.TaskFileStorage;
import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectDeploymentWorkflowService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.automation.configuration.service.WorkspaceConnectionService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionEndpointService;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.jackson.config.JacksonConfiguration;
import com.bytechef.liquibase.config.LiquibaseConfiguration;
import com.bytechef.platform.component.context.ContextFactory;
import com.bytechef.platform.component.facade.ActionDefinitionFacade;
import com.bytechef.platform.component.facade.ClusterElementDefinitionFacade;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.file.storage.TempFileStorage;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicy;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import com.bytechef.platform.tool.execution.ToolExecutionRecorder;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.workflow.execution.JobCompletionAwaiter;
import com.bytechef.platform.workflow.execution.facade.PrincipalJobFacade;
import com.bytechef.tenant.concurrent.TenantTaskDecorator;
import com.bytechef.test.config.jdbc.AbstractIntTestJdbcConfiguration;
import java.time.Duration;
import java.util.List;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;

/**
 * The API collection, connection, connection-definition, context-factory, temp-file-storage, workspace-connection,
 * component-definition, resource-grant, user, project, project-deployment, project-workflow and workflow services, and
 * the job, job-completion, principal-job, task-execution and task-file-storage collaborators, are stubbed:
 * {@code AppConnectionServiceImpl}, {@code AppFacadeImpl}, {@code AppExecutionFacadeImpl}, {@code AppProxyExecutor},
 * {@code AppCallableWorkflowResolver}, {@code AppApiCollectionResolver} and {@code AppWorkflowExecutionFacadeImpl} need
 * them and their real implementations live in modules this one does not depend on. The app lane is a real executor
 * carrying the same tenant and context-propagation decoration as production (so the request's security context reaches
 * the lane), so a test can prove the tenant reaches it.
 *
 * Repositories are not enabled here: the production {@code AppJdbcRepositoryConfiguration} auto-configuration is gated
 * on an {@code AbstractJdbcConfiguration} bean, which the nested class supplies.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ComponentScan(basePackages = "com.bytechef.ee.automation.app")
@EnableAutoConfiguration
@Import({
    JacksonConfiguration.class, LiquibaseConfiguration.class
})
@Configuration
public class AppIntTestConfiguration {

    @Bean
    ActionDefinitionFacade actionDefinitionFacade() {
        return Mockito.mock(ActionDefinitionFacade.class);
    }

    @Bean
    ApiCollectionEndpointService apiCollectionEndpointService() {
        return Mockito.mock(ApiCollectionEndpointService.class);
    }

    @Bean
    ApiCollectionService apiCollectionService() {
        return Mockito.mock(ApiCollectionService.class);
    }

    @Bean
    ApplicationProperties applicationProperties() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.App.Execution execution = applicationProperties.getApp()
            .getExecution();

        execution.setConcurrencyLimit(10);
        execution.setTimeout(Duration.ofSeconds(2));

        ApplicationProperties.App.WorkflowRun workflow = applicationProperties.getApp()
            .getWorkflow();

        workflow.setDefaultWait(Duration.ofSeconds(30));
        workflow.setMaxWait(Duration.ofSeconds(60));

        return applicationProperties;
    }

    @Bean
    ClusterElementDefinitionFacade clusterElementDefinitionFacade() {
        return Mockito.mock(ClusterElementDefinitionFacade.class);
    }

    @Bean
    ComponentDefinitionService componentDefinitionService() {
        return Mockito.mock(ComponentDefinitionService.class);
    }

    @Bean(AsyncConfiguration.APP_EXECUTOR)
    SimpleAsyncTaskExecutor appExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("app-");

        TenantTaskDecorator tenantTaskDecorator = new TenantTaskDecorator();
        ContextPropagatingTaskDecorator contextPropagatingTaskDecorator = new ContextPropagatingTaskDecorator();

        executor.setTaskDecorator(
            runnable -> tenantTaskDecorator.decorate(contextPropagatingTaskDecorator.decorate(runnable)));

        return executor;
    }

    @Bean
    JobCompletionAwaiter jobCompletionAwaiter() {
        return Mockito.mock(JobCompletionAwaiter.class);
    }

    @Bean
    JobService jobService() {
        return Mockito.mock(JobService.class);
    }

    @Bean
    PrincipalJobFacade principalJobFacade() {
        return Mockito.mock(PrincipalJobFacade.class);
    }

    @Bean
    TaskExecutionService taskExecutionService() {
        return Mockito.mock(TaskExecutionService.class);
    }

    @Bean
    TaskFileStorage taskFileStorage() {
        return Mockito.mock(TaskFileStorage.class);
    }

    @Bean
    TempFileStorage tempFileStorage() {
        return Mockito.mock(TempFileStorage.class);
    }

    @Bean
    ToolExecutionRecorder toolExecutionRecorder() {
        return Mockito.mock(ToolExecutionRecorder.class);
    }

    @Bean
    ConnectionDefinitionService connectionDefinitionService() {
        return Mockito.mock(ConnectionDefinitionService.class);
    }

    @Bean
    ConnectionService connectionService() {
        return Mockito.mock(ConnectionService.class);
    }

    @Bean
    ContextFactory contextFactory() {
        return Mockito.mock(ContextFactory.class);
    }

    @Bean
    ActionDefinitionService actionDefinitionService() {
        return Mockito.mock(ActionDefinitionService.class);
    }

    @Bean
    ClusterElementDefinitionService clusterElementDefinitionService() {
        return Mockito.mock(ClusterElementDefinitionService.class);
    }

    @Bean
    PermissionService permissionService() {
        return Mockito.mock(PermissionService.class);
    }

    @Bean
    WorkspaceConnectionFacade workspaceConnectionFacade() {
        return Mockito.mock(WorkspaceConnectionFacade.class);
    }

    @Bean
    ProjectDeploymentService projectDeploymentService() {
        return Mockito.mock(ProjectDeploymentService.class);
    }

    @Bean
    ProjectDeploymentWorkflowService projectDeploymentWorkflowService() {
        return Mockito.mock(ProjectDeploymentWorkflowService.class);
    }

    @Bean
    ProjectService projectService() {
        return Mockito.mock(ProjectService.class);
    }

    @Bean
    ProjectWorkflowService projectWorkflowService() {
        return Mockito.mock(ProjectWorkflowService.class);
    }

    @Bean
    ResourceGrantService resourceGrantService() {
        return Mockito.mock(ResourceGrantService.class);
    }

    @Bean
    ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry(
        List<ResourceVisibilityPolicy> resourceVisibilityPolicies) {

        return new ResourceVisibilityPolicyRegistry(resourceVisibilityPolicies);
    }

    @Bean
    ResourceVisibilityResolver resourceVisibilityResolver() {
        return Mockito.mock(ResourceVisibilityResolver.class);
    }

    @Bean
    UserService userService() {
        return Mockito.mock(UserService.class);
    }

    @Bean
    WorkspaceConnectionService workspaceConnectionService() {
        return Mockito.mock(WorkspaceConnectionService.class);
    }

    @Bean
    WorkflowService workflowService() {
        return Mockito.mock(WorkflowService.class);
    }

    @EnableJdbcAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "auditingDateTimeProvider")
    public static class AppIntTestJdbcConfiguration extends AbstractIntTestJdbcConfiguration {
    }
}
