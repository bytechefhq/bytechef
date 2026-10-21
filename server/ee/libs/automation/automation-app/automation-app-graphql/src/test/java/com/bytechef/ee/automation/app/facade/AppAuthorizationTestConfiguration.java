/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.facade;

import static org.mockito.Mockito.mock;

import com.bytechef.automation.configuration.facade.WorkspaceConnectionFacade;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.repository.WorkspaceConnectionRepository;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.security.ConnectionEnvironmentResolver;
import com.bytechef.automation.configuration.security.ConnectionOwnershipResolver;
import com.bytechef.automation.configuration.security.ConnectionVisibilityProvider;
import com.bytechef.automation.configuration.service.ProjectDeploymentService;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.automation.apiplatform.configuration.service.ApiCollectionService;
import com.bytechef.ee.automation.app.audit.AppAuditPublisher;
import com.bytechef.ee.automation.app.security.AppEnvironmentResolver;
import com.bytechef.ee.automation.app.security.AppOwnershipResolver;
import com.bytechef.ee.automation.app.security.AppVisibilityPolicy;
import com.bytechef.ee.automation.app.security.AppVisibilityProvider;
import com.bytechef.ee.automation.app.service.AppApiCollectionService;
import com.bytechef.ee.automation.app.service.AppConnectionService;
import com.bytechef.ee.automation.app.service.AppService;
import com.bytechef.ee.automation.app.service.AppWorkflowService;
import com.bytechef.ee.automation.app.workflow.AppApiCollectionResolver;
import com.bytechef.ee.automation.app.workflow.AppCallableWorkflowResolver;
import com.bytechef.ee.automation.configuration.repository.EmbeddedUserRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.scope.AppPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.ConnectionPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.scope.DeploymentPermissionScopeProvider;
import com.bytechef.ee.automation.configuration.service.CurrentUserResolver;
import com.bytechef.ee.automation.configuration.service.CustomRoleScopeResolver;
import com.bytechef.ee.automation.configuration.service.EmbeddedScopeCacheService;
import com.bytechef.ee.automation.configuration.service.PermissionScopeRegistry;
import com.bytechef.ee.automation.configuration.service.PermissionServiceImpl;
import com.bytechef.ee.automation.configuration.service.ResourceVisibilityResolverImpl;
import com.bytechef.ee.automation.configuration.service.WorkspaceScopeCacheService;
import com.bytechef.ee.platform.resource.grant.service.ResourceGrantService;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicy;
import com.bytechef.platform.security.domain.ResourceVisibilityPolicyRegistry;
import com.bytechef.platform.user.service.UserService;
import java.util.List;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.bean.override.mockito.MockReset;

/**
 * Assembles the app management facades behind the REAL method-security chain: {@link EnableMethodSecurity} with the
 * production {@link AutomationMethodSecurityConfiguration} (expression root and permission evaluator), the EE
 * {@link PermissionServiceImpl}, its {@link WorkspaceScopeCacheService} and {@link PermissionScopeRegistry} fed by the
 * production scope providers (so {@code APP_MANAGE} is ADMIN and {@code CONNECTION_EDIT} is EDITOR because the
 * providers say so, not because a stub does), the EE {@link ResourceVisibilityResolverImpl}, and the production App and
 * Connection ownership, environment and visibility resolvers.
 *
 * <p>
 * Only the data underneath is stubbed: who the current user is ({@link UserService}), which workspaces they belong to
 * and with which role ({@link WorkspaceUserRepository}), and the app and connection rows the resolvers read
 * ({@link AppService}, {@link ConnectionService}, {@link WorkspaceConnectionRepository}), and the app's binding
 * services and resolvers. Every mock is reset after each test.
 *
 * <p>
 * It lives in {@code automation-app-graphql}'s tests, the one module that can carry both sides: the real permission
 * chain is the EE configuration service ({@code PermissionServiceImpl}, the scope registry and providers) plus the CE
 * configuration service (the expression root and the Connection resolvers), and putting those on
 * {@code automation-app-service}'s test classpath, or the app service on theirs, adds Liquibase changelogs that
 * reference tables the other side's IntTests never create. This module has no Liquibase-backed tests.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@EnableMethodSecurity
@Import({
    AppApiCollectionFacadeImpl.class, AppEnvironmentResolver.class, AppFacadeImpl.class, AppOwnershipResolver.class,
    AppPermissionScopeProvider.class,
    AppVisibilityPolicy.class, AppVisibilityProvider.class, AppWorkflowFacadeImpl.class,
    ConnectionEnvironmentResolver.class, ConnectionOwnershipResolver.class, ConnectionPermissionScopeProvider.class,
    ConnectionVisibilityProvider.class, CurrentUserResolver.class, DeploymentPermissionScopeProvider.class,
    PermissionScopeRegistry.class, PermissionServiceImpl.class, ResourceVisibilityResolverImpl.class,
    WorkspaceScopeCacheService.class
})
@ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
public class AppAuthorizationTestConfiguration {

    @Bean
    ActionDefinitionService actionDefinitionService() {
        return resettingMock(ActionDefinitionService.class);
    }

    @Bean
    ApiCollectionService apiCollectionService() {
        return resettingMock(ApiCollectionService.class);
    }

    @Bean
    AppApiCollectionResolver appApiCollectionResolver() {
        return resettingMock(AppApiCollectionResolver.class);
    }

    @Bean
    AppApiCollectionService appApiCollectionService() {
        return resettingMock(AppApiCollectionService.class);
    }

    @Bean
    AppAuditPublisher appAuditPublisher() {
        return resettingMock(AppAuditPublisher.class);
    }

    @Bean
    AppCallableWorkflowResolver appCallableWorkflowResolver() {
        return resettingMock(AppCallableWorkflowResolver.class);
    }

    @Bean
    AppConnectionService appConnectionService() {
        return resettingMock(AppConnectionService.class);
    }

    @Bean
    AppService appService() {
        return resettingMock(AppService.class);
    }

    @Bean
    AppWorkflowService appWorkflowService() {
        return resettingMock(AppWorkflowService.class);
    }

    @Bean
    CacheManager cacheManager() {
        return new ConcurrentMapCacheManager();
    }

    @Bean
    ClusterElementDefinitionService clusterElementDefinitionService() {
        return resettingMock(ClusterElementDefinitionService.class);
    }

    @Bean
    ComponentDefinitionService componentDefinitionService() {
        return resettingMock(ComponentDefinitionService.class);
    }

    @Bean
    ConnectionService connectionService() {
        return resettingMock(ConnectionService.class);
    }

    @Bean
    CustomRoleScopeResolver customRoleScopeResolver() {
        return resettingMock(CustomRoleScopeResolver.class);
    }

    @Bean
    EmbeddedScopeCacheService embeddedScopeCacheService() {
        return resettingMock(EmbeddedScopeCacheService.class);
    }

    @Bean
    EmbeddedUserRepository embeddedUserRepository() {
        return resettingMock(EmbeddedUserRepository.class);
    }

    @Bean
    ProjectDeploymentService projectDeploymentService() {
        return resettingMock(ProjectDeploymentService.class);
    }

    @Bean
    ProjectRepository projectRepository() {
        return resettingMock(ProjectRepository.class);
    }

    @Bean
    ProjectService projectService() {
        return resettingMock(ProjectService.class);
    }

    @Bean
    ResourceGrantService resourceGrantService() {
        return resettingMock(ResourceGrantService.class);
    }

    @Bean
    ResourceVisibilityPolicyRegistry resourceVisibilityPolicyRegistry(
        List<ResourceVisibilityPolicy> resourceVisibilityPolicies) {

        return new ResourceVisibilityPolicyRegistry(resourceVisibilityPolicies);
    }

    @Bean
    UserService userService() {
        return resettingMock(UserService.class);
    }

    @Bean
    WorkspaceConnectionFacade workspaceConnectionFacade() {
        return resettingMock(WorkspaceConnectionFacade.class);
    }

    @Bean
    WorkspaceConnectionRepository workspaceConnectionRepository() {
        return resettingMock(WorkspaceConnectionRepository.class);
    }

    @Bean
    WorkspaceUserRepository workspaceUserRepository() {
        return resettingMock(WorkspaceUserRepository.class);
    }

    private static <T> T resettingMock(Class<T> type) {
        return mock(type, MockReset.withSettings(MockReset.AFTER));
    }
}
