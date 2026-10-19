/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.repository.ProjectRepository;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.security.ConnectedUserAccessDecider;
import com.bytechef.automation.configuration.security.ConnectedUserAccessDecider.Decision;
import com.bytechef.automation.configuration.security.EmbeddedResourceResolver;
import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver.ResourceOwner;
import com.bytechef.automation.configuration.security.ResourceVisibilityProvider;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.ResourceVisibilityResolver;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.EmbeddedUserRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScopeSide;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.util.SecurityUtils;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service("permissionService")
@ConditionalOnEEVersion
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionServiceImpl.class);

    private static final String CONNECTION = "Connection";
    private static final String CONNECTION_VIEW = "CONNECTION_VIEW";
    private static final String PROJECT = "Project";
    private static final String WORKFLOW = "Workflow";
    private static final String WORKFLOW_EDIT = "WORKFLOW_EDIT";

    private final ObjectProvider<ConnectedUserAccessDecider> connectedUserAccessDeciderProvider;
    private final ObjectProvider<ConnectionService> connectionServiceProvider;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectProvider<EmbeddedResourceResolver> embeddedResourceResolverProvider;
    private final EmbeddedScopeCacheService embeddedScopeCacheService;
    private final EmbeddedUserRepository embeddedUserRepository;
    private final PermissionScopeRegistry permissionScopeRegistry;
    private final ProjectRepository projectRepository;
    private final WorkspaceScopeCacheService workspaceScopeCacheService;
    private final WorkspaceUserRepository workspaceUserRepository;
    private final Map<String, List<ResourceEnvironmentResolver>> resourceEnvironmentResolvers;
    private final Map<String, List<ResourceOwnershipResolver>> resourceOwnershipResolvers;
    private final Map<String, ResourceVisibilityProvider> resourceVisibilityProviders;
    private final ResourceVisibilityResolver resourceVisibilityResolver;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public PermissionServiceImpl(
        CurrentUserResolver currentUserResolver, PermissionScopeRegistry permissionScopeRegistry,
        ProjectRepository projectRepository, WorkspaceScopeCacheService workspaceScopeCacheService,
        WorkspaceUserRepository workspaceUserRepository,
        List<ResourceOwnershipResolver> resourceOwnershipResolvers,
        List<ResourceVisibilityProvider> resourceVisibilityProviders,
        ResourceVisibilityResolver resourceVisibilityResolver,
        List<ResourceEnvironmentResolver> resourceEnvironmentResolvers,
        ObjectProvider<ConnectedUserAccessDecider> connectedUserAccessDeciderProvider,
        EmbeddedScopeCacheService embeddedScopeCacheService, EmbeddedUserRepository embeddedUserRepository,
        ObjectProvider<EmbeddedResourceResolver> embeddedResourceResolverProvider,
        ObjectProvider<ConnectionService> connectionServiceProvider) {

        this.connectedUserAccessDeciderProvider = connectedUserAccessDeciderProvider;
        this.connectionServiceProvider = connectionServiceProvider;
        this.currentUserResolver = currentUserResolver;
        this.embeddedResourceResolverProvider = embeddedResourceResolverProvider;
        this.embeddedScopeCacheService = embeddedScopeCacheService;
        this.embeddedUserRepository = embeddedUserRepository;
        this.permissionScopeRegistry = permissionScopeRegistry;
        this.projectRepository = projectRepository;
        this.workspaceScopeCacheService = workspaceScopeCacheService;
        this.workspaceUserRepository = workspaceUserRepository;
        this.resourceOwnershipResolvers = resourceOwnershipResolvers.stream()
            .collect(Collectors.groupingBy(ResourceOwnershipResolver::resourceType));
        this.resourceVisibilityProviders = resourceVisibilityProviders.stream()
            .collect(Collectors.toMap(ResourceVisibilityProvider::resourceType, Function.identity()));
        this.resourceVisibilityResolver = resourceVisibilityResolver;

        this.resourceEnvironmentResolvers = resourceEnvironmentResolvers.stream()
            .collect(Collectors.groupingBy(ResourceEnvironmentResolver::resourceType));
    }

    @Override
    public boolean isAuthorizationSkipped() {
        if (isGovernedPrincipal()) {
            return false;
        }

        return AutomationAuthorizationContext.isSkipChecks();
    }

    @Override
    public boolean isTenantAdmin() {
        if (isGovernedPrincipal()) {
            return false;
        }

        return SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN);
    }

    @Override
    public boolean isCurrentUser(long userId) {
        if (isGovernedPrincipal()) {
            return false;
        }

        OptionalLong currentUserId = currentUserResolver.fetchCurrentUserId();

        return currentUserId.isPresent() && currentUserId.getAsLong() == userId;
    }

    @Override
    public boolean hasWorkspaceRole(long workspaceId, String minimumRole) {
        if (isGovernedPrincipal()) {
            return false;
        }

        if (isTenantAdmin()) {
            return true;
        }

        WorkspaceRole minimum = parseWorkspaceRole(minimumRole);

        if (minimum == null) {
            return false;
        }

        OptionalLong userId = currentUserResolver.fetchCurrentUserId();

        if (userId.isEmpty()) {
            return false;
        }

        // The implicit row only. A member in explicit mode holds one row per environment and no workspace-wide role, so
        // there is nothing here to compare against a minimum: they are denied, and the environment-aware scope checks
        // are what serve them. Reading "whichever row comes first" would answer one environment's role for all of them.
        return fetchImplicitWorkspaceUser(userId.getAsLong(), workspaceId)
            .map(member -> toWorkspaceRole(member.getWorkspaceRole()))
            .map(role -> role.hasAtLeast(minimum))
            .orElse(false);
    }

    @Override
    public boolean hasWorkspaceScope(long workspaceId, String scope) {
        Optional<Boolean> connectedUserDecision = decideForConnectedUser(
            decider -> decider.decideWorkspace(workspaceId, scope));

        if (connectedUserDecision.isPresent()) {
            return connectedUserDecision.get();
        }

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        OptionalLong userId = currentUserResolver.fetchCurrentUserId();

        if (userId.isEmpty()) {
            return false;
        }

        Set<String> scopeNames = workspaceScopeCacheService.getWorkspaceScopes(userId.getAsLong(), workspaceId);

        return scopeNames.contains(scope);
    }

    /**
     * Mirrors the environment-unaware overload, including the skip-checks and tenant-admin short circuits, and differs
     * only in resolving the member's role for {@code environment}. A tenant admin is deliberately not subject to
     * per-environment roles.
     */
    @Override
    public boolean hasWorkspaceScope(long workspaceId, String scope, Environment environment) {
        Optional<Boolean> connectedUserDecision = decideForConnectedUser(
            decider -> decider.decideWorkspace(workspaceId, scope));

        if (connectedUserDecision.isPresent()) {
            return connectedUserDecision.get();
        }

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        return checkWorkspaceScope(workspaceId, scope, environment);
    }

    /**
     * Requires the scope in every environment, so that an operation whose effect is not confined to one environment
     * cannot be authorised by a role held in only one of them.
     */
    @Override
    public boolean hasWorkspaceScopeInEveryEnvironment(long workspaceId, String scope) {
        if (isGovernedPrincipal()) {
            return false;
        }

        if (isTenantAdmin()) {
            return true;
        }

        for (Environment environment : Environment.values()) {
            if (!checkWorkspaceScope(workspaceId, scope, environment)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean hasWorkspaceScopeForProject(long projectId, String scope) {
        Optional<Boolean> connectedUserDecision = decideForConnectedUser(
            decider -> decider.decide(projectId, PROJECT, scope));

        if (connectedUserDecision.isPresent()) {
            return connectedUserDecision.get();
        }

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        if (isEmbeddedProject(projectId)) {
            return false;
        }

        if (!isResourceVisible(projectId, PROJECT)) {
            return false;
        }

        Long workspaceId = projectRepository.findById(projectId)
            .map(Project::getWorkspaceId)
            .orElse(null);

        if (workspaceId == null) {
            return false;
        }

        return hasWorkspaceScope(workspaceId, scope);
    }

    @Override
    public boolean hasWorkspaceScopeForProject(long projectId, String scope, Environment environment) {
        Optional<Boolean> connectedUserDecision = decideForConnectedUser(
            decider -> decider.decideInEnvironment(projectId, PROJECT, scope, environment));

        if (connectedUserDecision.isPresent()) {
            return connectedUserDecision.get();
        }

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        if (isEmbeddedProject(projectId)) {
            return false;
        }

        if (!isResourceVisible(projectId, PROJECT)) {
            return false;
        }

        Long workspaceId = projectRepository.findById(projectId)
            .map(Project::getWorkspaceId)
            .orElse(null);

        if (workspaceId == null) {
            return false;
        }

        return hasWorkspaceScope(workspaceId, scope, environment);
    }

    @Override
    public boolean hasResourceScope(Serializable id, String resourceType, String scope) {
        Optional<Boolean> connectedUserDecision = decideForConnectedUser(
            decider -> decider.decide(id, resourceType, scope));

        if (connectedUserDecision.isPresent()) {
            return connectedUserDecision.get();
        }

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        if (!resourceOwnershipResolvers.containsKey(resourceType)) {
            return false;
        }

        ResourceOwner resourceOwner = resolveOwner(resourceType, id);

        if (resourceOwner.embeddedProject()) {
            return false;
        }

        // Visibility is a precondition of the scope check, not a filter running beside it. Holding
        // CONNECTION_EDIT in a workspace does not entitle a member to a colleague's PRIVATE connection, even
        // though the workspace-scope check below would otherwise pass for every row in that workspace.
        if (!resourceOwner.embedded() && !isResourceVisible(id, resourceType)) {
            return false;
        }

        OptionalLong workspaceId = resourceOwner.workspaceId();

        if (!resourceOwner.embedded() && workspaceId.isEmpty()) {
            return scope.endsWith("_VIEW") && isOrganizationVisible(id, resourceType);
        }

        Optional<Environment> environment;

        try {
            environment = resolveEnvironment(resourceType, id);
        } catch (RuntimeException exception) {
            log.error("Denying {} on {} id={}: resolving its environment failed", scope, resourceType, id, exception);

            return false;
        }

        if (resourceOwner.embedded()) {
            if (environment.isPresent()) {
                return hasEmbeddedScope(scope, environment.get());
            }

            if (resourceEnvironmentResolvers.containsKey(resourceType)) {
                return hasEmbeddedScopeInEveryEnvironment(scope);
            }

            return hasEmbeddedScope(scope);
        }

        if (environment.isPresent()) {
            return hasWorkspaceScope(workspaceId.getAsLong(), scope, environment.get());
        }

        return hasWorkspaceScope(workspaceId.getAsLong(), scope);
    }

    /**
     * Deliberately does NOT consult {@link ResourceEnvironmentResolver}, unlike the environment-unaware sibling above.
     * The caller named the environment the operation acts on, and that is the one to authorise; asking a resolver what
     * environment the resource "is in" would answer a different question and, for a resource that spans environments,
     * would answer nothing at all.
     */
    @Override
    public boolean hasResourceScopeInEnvironment(
        Serializable id, String resourceType, String scope, Environment environment) {

        Optional<Boolean> connectedUserDecision = decideForConnectedUser(
            decider -> decider.decideInEnvironment(id, resourceType, scope, environment));

        if (connectedUserDecision.isPresent()) {
            return connectedUserDecision.get();
        }

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        if (!resourceOwnershipResolvers.containsKey(resourceType)) {
            return false;
        }

        ResourceOwner resourceOwner = resolveOwner(resourceType, id);

        if (resourceOwner.embeddedProject()) {
            return false;
        }

        if (resourceOwner.embedded()) {
            return hasEmbeddedScope(scope, environment);
        }

        // Visibility is a precondition here for the same reason as in hasResourceScope: naming an environment must not
        // reach a resource the caller cannot see.
        if (!isResourceVisible(id, resourceType)) {
            return false;
        }

        OptionalLong workspaceId = resourceOwner.workspaceId();

        if (workspaceId.isEmpty()) {
            return false;
        }

        return hasWorkspaceScope(workspaceId.getAsLong(), scope, environment);
    }

    /**
     * Whether the current principal may see the resource at all, delegating to the same resolver the list path uses so
     * the two cannot drift. A resource type with no registered provider has not opted into visibility and is
     * unrestricted by it; a registered type whose resource does not exist fails closed.
     */
    private boolean isOrganizationVisible(Serializable id, String resourceType) {
        ResourceVisibilityProvider resourceVisibilityProvider = resourceVisibilityProviders.get(resourceType);

        if (resourceVisibilityProvider == null || !(id instanceof Number number)) {
            return false;
        }

        return resourceVisibilityProvider.fetchVisibility(number.longValue())
            .map(visibilityRecord -> visibilityRecord.visibility() == ResourceVisibility.ORGANIZATION)
            .orElse(false);
    }

    private boolean isResourceVisible(Serializable id, String resourceType) {
        ResourceVisibilityProvider resourceVisibilityProvider = resourceVisibilityProviders.get(resourceType);

        if (resourceVisibilityProvider == null) {
            return true;
        }

        return resourceVisibilityProvider.fetchVisibility(id)
            .map(visibilityRecord -> {
                // workspaceId is unused by both resolver implementations — they resolve against the current
                // principal, not the argument — so 0 is safe. The parameter exists for a future SQL-predicate
                // implementation.
                Set<Long> visibleIds = resourceVisibilityResolver.filterVisibleIds(
                    resourceVisibilityProvider.visibilityResourceType(), 0L, List.of(visibilityRecord));

                return !visibleIds.isEmpty();
            })
            .orElse(false);
    }

    @Override
    public boolean isResourceOwner(String resourceType, long id) {
        if (isGovernedPrincipal()) {
            return false;
        }

        if (isTenantAdmin()) {
            return true;
        }

        if (!resourceOwnershipResolvers.containsKey(resourceType)) {
            return false;
        }

        ResourceOwner resourceOwner = resolveOwner(resourceType, id);

        OptionalLong ownerUserId = resourceOwner.ownerUserId();

        if (ownerUserId.isEmpty() || !isCurrentUser(ownerUserId.getAsLong())) {
            return false;
        }

        OptionalLong workspaceId = resourceOwner.workspaceId();

        return workspaceId.isEmpty() ||
            workspaceUserRepository.existsByUserIdAndWorkspaceId(ownerUserId.getAsLong(), workspaceId.getAsLong());
    }

    private boolean isEmbeddedProject(long projectId) {
        EmbeddedResourceResolver embeddedResourceResolver = embeddedResourceResolverProvider.getIfAvailable();

        return embeddedResourceResolver != null && embeddedResourceResolver.isEmbeddedProject(projectId);
    }

    private boolean isEmbeddedProjectWorkflow(String workflowId) {
        EmbeddedResourceResolver embeddedResourceResolver = embeddedResourceResolverProvider.getIfAvailable();

        return embeddedResourceResolver != null && embeddedResourceResolver.isEmbeddedProjectWorkflow(workflowId);
    }

    private boolean isHubProjectWorkflow(String workflowId) {
        EmbeddedResourceResolver embeddedResourceResolver = embeddedResourceResolverProvider.getIfAvailable();

        return embeddedResourceResolver != null && embeddedResourceResolver.isHubProjectWorkflow(workflowId);
    }

    private boolean isIntegrationWorkflow(String workflowId) {
        EmbeddedResourceResolver embeddedResourceResolver = embeddedResourceResolverProvider.getIfAvailable();

        return embeddedResourceResolver != null && embeddedResourceResolver.isIntegrationWorkflow(workflowId);
    }

    private ResourceOwner resolveOwner(String resourceType, Serializable id) {
        for (ResourceOwnershipResolver resourceOwnershipResolver : resourceOwnershipResolvers.get(resourceType)) {
            ResourceOwner resourceOwner = resourceOwnershipResolver.resolveOwner(id);

            if (!resourceOwner.isUnknown()) {
                return resourceOwner;
            }
        }

        return ResourceOwner.unknown();
    }

    private Optional<Environment> resolveEnvironment(String resourceType, Serializable id) {
        for (ResourceEnvironmentResolver resourceEnvironmentResolver : resourceEnvironmentResolvers
            .getOrDefault(resourceType, List.of())) {

            Optional<Environment> environment = resourceEnvironmentResolver.fetchEnvironment(id);

            if (environment.isPresent()) {
                return environment;
            }
        }

        return Optional.empty();
    }

    @Override
    public boolean hasResourceRole(long id, String resourceType, String minimumRole) {
        if (isGovernedPrincipal()) {
            return false;
        }

        if (isTenantAdmin()) {
            return true;
        }

        if (!resourceOwnershipResolvers.containsKey(resourceType)) {
            return false;
        }

        OptionalLong workspaceId = resolveOwner(resourceType, id).workspaceId();

        if (workspaceId.isEmpty()) {
            return false;
        }

        return hasWorkspaceRole(workspaceId.getAsLong(), minimumRole);
    }

    @Override
    public boolean hasWorkflowScope(String workflowId, String scope) {
        Optional<Boolean> connectedUserDecision = decideForConnectedUser(
            decider -> decider.decideWorkflow(workflowId, scope));

        if (connectedUserDecision.isPresent()) {
            return connectedUserDecision.get();
        }

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        if (isIntegrationWorkflow(workflowId) || isHubProjectWorkflow(workflowId)) {
            return hasEmbeddedScope(scope);
        }

        if (isEmbeddedProjectWorkflow(workflowId)) {
            return false;
        }

        return projectRepository.findByWorkflowId(workflowId)
            .map(Project::getId)
            .map(projectId -> hasResourceScope(projectId, PROJECT, scope))
            .orElse(false);
    }

    @Override
    public boolean hasWorkflowScope(String workflowId, String scope, Environment environment) {
        Optional<Boolean> connectedUserDecision = decideForConnectedUser(
            decider -> decider.decideWorkflow(workflowId, scope));

        if (connectedUserDecision.isPresent()) {
            return connectedUserDecision.get();
        }

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        if (isIntegrationWorkflow(workflowId) || isHubProjectWorkflow(workflowId)) {
            return hasEmbeddedScope(scope, environment);
        }

        if (isEmbeddedProjectWorkflow(workflowId)) {
            return false;
        }

        Long workspaceId = projectRepository.findByWorkflowId(workflowId)
            .map(Project::getWorkspaceId)
            .orElse(null);

        if (workspaceId == null) {
            return false;
        }

        return hasWorkspaceScope(workspaceId, scope, environment);
    }

    @Override
    public boolean hasWorkflowScopeIfProjectWorkflow(String workflowId, String scope, Environment environment) {
        Optional<Boolean> connectedUserDecision = decideForConnectedUser(
            decider -> decider.decideWorkflow(workflowId, scope));

        if (connectedUserDecision.isPresent()) {
            return connectedUserDecision.get();
        }

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        if (isIntegrationWorkflow(workflowId) || isHubProjectWorkflow(workflowId)) {
            return hasEmbeddedScope(scope, environment);
        }

        if (isEmbeddedProjectWorkflow(workflowId)) {
            return false;
        }

        Long workspaceId = projectRepository.findByWorkflowId(workflowId)
            .map(Project::getWorkspaceId)
            .orElse(null);

        if (workspaceId == null) {
            return false;
        }

        return hasWorkspaceScope(workspaceId, scope, environment);
    }

    @Override
    public boolean canUseConnectionInWorkflow(long connectionId, String workflowId, Environment environment) {
        Optional<Boolean> connectedUserDecision = decideForConnectedUser(
            decider -> decideConnectionInWorkflow(decider, connectionId, workflowId));

        if (connectedUserDecision.isPresent()) {
            return connectedUserDecision.get();
        }

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        if (isIntegrationWorkflow(workflowId) || isHubProjectWorkflow(workflowId)) {
            return canUseEmbeddedConnection(connectionId, environment);
        }

        if (isEmbeddedProjectWorkflow(workflowId)) {
            return false;
        }

        Long workspaceId = projectRepository.findByWorkflowId(workflowId)
            .map(Project::getWorkspaceId)
            .orElse(null);

        if (workspaceId == null) {
            return false;
        }

        return canUseConnectionInWorkspace(connectionId, workspaceId, environment);
    }

    private boolean canUseEmbeddedConnection(long connectionId, Environment environment) {
        ConnectionService connectionService = connectionServiceProvider.getIfAvailable();

        if (connectionService == null) {
            return false;
        }

        Optional<Connection> connectionOptional = connectionService.fetchConnection(connectionId);

        if (connectionOptional.isEmpty()) {
            return false;
        }

        Connection connection = connectionOptional.get();

        if (connection.getType() != PlatformType.EMBEDDED) {
            return false;
        }

        Environment connectionEnvironment = Environment.values()[connection.getEnvironmentId()];

        if (environment != null && environment != connectionEnvironment) {
            return false;
        }

        return hasEmbeddedScope(CONNECTION_VIEW, connectionEnvironment);
    }

    @Override
    public boolean canUseConnectionInWorkspace(long connectionId, long workspaceId, Environment environment) {
        Optional<Boolean> connectedUserDecision = decideForConnectedUser(
            decider -> decider.decideWorkspace(workspaceId, CONNECTION_VIEW));

        if (connectedUserDecision.isPresent()) {
            return connectedUserDecision.get();
        }

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        if (!resourceOwnershipResolvers.containsKey(CONNECTION)
            || !resourceEnvironmentResolvers.containsKey(CONNECTION)) {
            return false;
        }

        ResourceOwner connectionOwner = resolveOwner(CONNECTION, connectionId);

        if (connectionOwner.embedded()) {
            return false;
        }

        if (!isResourceVisible(connectionId, CONNECTION)) {
            return false;
        }

        OptionalLong connectionWorkspaceId = connectionOwner.workspaceId();

        if (connectionWorkspaceId.isEmpty()) {
            if (!isOrganizationVisible(connectionId, CONNECTION)) {
                return false;
            }
        } else if (connectionWorkspaceId.getAsLong() != workspaceId) {
            return false;
        }

        Optional<Environment> connectionEnvironment;

        try {
            connectionEnvironment = resolveEnvironment(CONNECTION, connectionId);
        } catch (RuntimeException exception) {
            log.error("Denying connection id={}: resolving its environment failed", connectionId, exception);

            return false;
        }

        if (connectionEnvironment.isEmpty() || connectionEnvironment.get() != environment) {
            return false;
        }

        return checkWorkspaceScope(workspaceId, CONNECTION_VIEW, environment);
    }

    @Override
    public boolean hasResourceScopeIfProjectWorkflow(
        String workflowId, Serializable id, String resourceType, String scope) {

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        if (projectRepository.findByWorkflowId(workflowId)
            .isEmpty()) {

            return true;
        }

        return hasResourceScope(id, resourceType, scope);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public String getMyWorkspaceRole(long workspaceId) {
        if (isGovernedPrincipal()) {
            return null;
        }

        if (isTenantAdmin()) {
            return WorkspaceRole.ADMIN.name();
        }

        OptionalLong userId = currentUserResolver.fetchCurrentUserId();

        if (userId.isEmpty()) {
            return null;
        }

        // Null for a member in explicit mode, which is the honest answer: they hold no one role across the workspace.
        return fetchImplicitWorkspaceUser(userId.getAsLong(), workspaceId)
            .map(member -> toWorkspaceRole(member.getWorkspaceRole()))
            .map(WorkspaceRole::name)
            .orElse(null);
    }

    private Optional<WorkspaceUser> fetchImplicitWorkspaceUser(long userId, long workspaceId) {
        if (workspaceUserRepository.existsByUserIdAndWorkspaceIdAndEnvironmentIsNotNull(userId, workspaceId)) {
            return Optional.empty();
        }

        return workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(userId, workspaceId);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public Set<String> getMyWorkspaceScopes(long workspaceId) {
        if (isGovernedPrincipal()) {
            return Collections.emptySet();
        }

        if (isTenantAdmin()) {
            return Set.copyOf(permissionScopeRegistry.getAllScopeNames());
        }

        OptionalLong userId = currentUserResolver.fetchCurrentUserId();

        if (userId.isEmpty()) {
            return Collections.emptySet();
        }

        return Set.copyOf(workspaceScopeCacheService.getWorkspaceScopes(userId.getAsLong(), workspaceId));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public Set<String> getMyWorkspaceScopes(long workspaceId, Environment environment) {
        if (isGovernedPrincipal()) {
            return Collections.emptySet();
        }

        if (isTenantAdmin()) {
            return Set.copyOf(permissionScopeRegistry.getAllScopeNames());
        }

        OptionalLong userId = currentUserResolver.fetchCurrentUserId();

        if (userId.isEmpty()) {
            return Collections.emptySet();
        }

        return Set.copyOf(workspaceScopeCacheService.getWorkspaceScopes(userId.getAsLong(), workspaceId, environment));
    }

    @Override
    public void evictWorkspaceScopeCache(long userId, long workspaceId) {
        workspaceScopeCacheService.evictWorkspaceScopeCache(userId, workspaceId);
    }

    @Override
    public void evictWorkspaceScopeCaches(Collection<UserWorkspacePair> userWorkspacePairs) {
        workspaceScopeCacheService.evictWorkspaceScopeCaches(userWorkspacePairs);
    }

    @Override
    public void evictAllWorkspaceScopeCache() {
        workspaceScopeCacheService.evictAllWorkspaceScopeCache();
    }

    @Override
    public boolean hasEmbeddedScope(String scope) {
        return checkEmbeddedScope(scope, null);
    }

    @Override
    public boolean hasEmbeddedScope(String scope, Environment environment) {
        if (environment == null) {
            return hasEmbeddedScopeInEveryEnvironment(scope);
        }

        return checkEmbeddedScope(scope, environment);
    }

    @Override
    public boolean hasEmbeddedScopeInEveryEnvironment(String scope) {
        if (isGovernedPrincipal()) {
            return false;
        }

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        OptionalLong userId = currentUserResolver.fetchCurrentUserId();

        if (userId.isEmpty()) {
            return false;
        }

        for (Environment environment : Environment.values()) {
            Set<String> scopeNames = embeddedScopeCacheService.getEmbeddedScopes(userId.getAsLong(), environment);

            if (!scopeNames.contains(scope)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean isEmbeddedMember() {
        if (isGovernedPrincipal()) {
            return false;
        }

        if (isTenantAdmin()) {
            return true;
        }

        OptionalLong userId = currentUserResolver.fetchCurrentUserId();

        if (userId.isEmpty()) {
            return false;
        }

        return embeddedUserRepository.existsByUserId(userId.getAsLong());
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public Set<String> getMyEmbeddedScopes(@Nullable Environment environment) {
        if (isGovernedPrincipal()) {
            return Collections.emptySet();
        }

        if (isTenantAdmin()) {
            return Set.copyOf(permissionScopeRegistry.getAllScopeNames(PermissionScopeSide.EMBEDDED));
        }

        OptionalLong userId = currentUserResolver.fetchCurrentUserId();

        if (userId.isEmpty()) {
            return Collections.emptySet();
        }

        if (environment == null) {
            return Set.copyOf(embeddedScopeCacheService.getEmbeddedScopes(userId.getAsLong()));
        }

        return Set.copyOf(embeddedScopeCacheService.getEmbeddedScopes(userId.getAsLong(), environment));
    }

    @Override
    public void evictEmbeddedScopeCache(long userId) {
        embeddedScopeCacheService.evictEmbeddedScopeCache(userId);
    }

    @Override
    public void evictAllEmbeddedScopeCache() {
        embeddedScopeCacheService.evictAllEmbeddedScopeCache();
    }

    private boolean checkEmbeddedScope(String scope, @Nullable Environment environment) {
        if (isGovernedPrincipal()) {
            return false;
        }

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        OptionalLong userId = currentUserResolver.fetchCurrentUserId();

        if (userId.isEmpty()) {
            return false;
        }

        Set<String> scopeNames = environment == null
            ? embeddedScopeCacheService.getEmbeddedScopes(userId.getAsLong())
            : embeddedScopeCacheService.getEmbeddedScopes(userId.getAsLong(), environment);

        return scopeNames.contains(scope);
    }

    private boolean checkWorkspaceScope(long workspaceId, String scope, Environment environment) {
        if (isTenantAdmin()) {
            return true;
        }

        OptionalLong userId = currentUserResolver.fetchCurrentUserId();

        if (userId.isEmpty()) {
            return false;
        }

        Set<String> scopeNames =
            workspaceScopeCacheService.getWorkspaceScopes(userId.getAsLong(), workspaceId, environment);

        return scopeNames.contains(scope);
    }

    private Optional<Boolean> decideForConnectedUser(Function<ConnectedUserAccessDecider, Decision> decision) {
        ConnectedUserAccessDecider connectedUserAccessDecider = connectedUserAccessDeciderProvider.getIfAvailable();

        if (connectedUserAccessDecider == null) {
            return Optional.empty();
        }

        Decision outcome = decision.apply(connectedUserAccessDecider);

        if (outcome == Decision.NOT_GOVERNED) {
            return Optional.empty();
        }

        return Optional.of(outcome == Decision.GRANT);
    }

    private boolean isGovernedPrincipal() {
        ConnectedUserAccessDecider connectedUserAccessDecider = connectedUserAccessDeciderProvider.getIfAvailable();

        if (connectedUserAccessDecider == null) {
            return false;
        }

        return connectedUserAccessDecider.decideWorkspace(0L, "") != Decision.NOT_GOVERNED;
    }

    private static Decision decideConnectionInWorkflow(
        ConnectedUserAccessDecider connectedUserAccessDecider, long connectionId, String workflowId) {

        Decision connectionDecision = connectedUserAccessDecider.decide(connectionId, CONNECTION, CONNECTION_VIEW);

        if (connectionDecision == Decision.DENY) {
            return Decision.DENY;
        }

        Decision workflowDecision = connectedUserAccessDecider.decideWorkflow(workflowId, WORKFLOW_EDIT);

        if (connectionDecision == Decision.NOT_GOVERNED && workflowDecision == Decision.NOT_GOVERNED) {
            return Decision.NOT_GOVERNED;
        }

        if (connectionDecision == Decision.GRANT && workflowDecision == Decision.GRANT) {
            return Decision.GRANT;
        }

        return Decision.DENY;
    }

    private static boolean isAutomationAuthorizationSkipped() {
        return AutomationAuthorizationContext.isSkipChecks();
    }

    private static WorkspaceRole parseWorkspaceRole(String roleName) {
        try {
            return WorkspaceRole.valueOf(roleName);
        } catch (IllegalArgumentException exception) {
            log.error("Unknown WorkspaceRole '{}' in @PreAuthorize — failing closed.", roleName);

            return null;
        }
    }

    private static WorkspaceRole toWorkspaceRole(Integer ordinal) {
        if (ordinal == null) {
            return null;
        }

        WorkspaceRole[] values = WorkspaceRole.values();

        if (ordinal < 0 || ordinal >= values.length) {
            log.error(
                "Invalid workspace_role ordinal={} — outside the range [0, {}). Failing closed. " +
                    "This indicates a corrupted or legacy workspace_role value.",
                ordinal, values.length);

            return null;
        }

        return values[ordinal];
    }
}
