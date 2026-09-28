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
import com.bytechef.automation.configuration.security.ResourceEnvironmentResolver;
import com.bytechef.automation.configuration.security.ResourceOwnershipResolver;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.config.ApplicationProperties;
import com.bytechef.config.ApplicationProperties.Security.ConnectedUserAuthorizationMode;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.constant.AuthorityConstants;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    private final ApplicationProperties applicationProperties;
    private final ObjectProvider<ConnectedUserAccessDecider> connectedUserAccessDeciderProvider;
    private final CurrentUserResolver currentUserResolver;
    private final Set<String> loggedWouldBeDenials = ConcurrentHashMap.newKeySet();
    private final PermissionScopeRegistry permissionScopeRegistry;
    private final ProjectRepository projectRepository;
    private final WorkspaceScopeCacheService workspaceScopeCacheService;
    private final WorkspaceUserRepository workspaceUserRepository;
    private final Map<String, ResourceEnvironmentResolver> resourceEnvironmentResolvers;
    private final Map<String, ResourceOwnershipResolver> resourceOwnershipResolvers;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public PermissionServiceImpl(
        CurrentUserResolver currentUserResolver, PermissionScopeRegistry permissionScopeRegistry,
        ProjectRepository projectRepository, WorkspaceScopeCacheService workspaceScopeCacheService,
        WorkspaceUserRepository workspaceUserRepository,
        List<ResourceOwnershipResolver> resourceOwnershipResolvers,
        List<ResourceEnvironmentResolver> resourceEnvironmentResolvers,
        ObjectProvider<ConnectedUserAccessDecider> connectedUserAccessDeciderProvider,
        ApplicationProperties applicationProperties) {

        this.applicationProperties = applicationProperties;
        this.connectedUserAccessDeciderProvider = connectedUserAccessDeciderProvider;
        this.currentUserResolver = currentUserResolver;
        this.permissionScopeRegistry = permissionScopeRegistry;
        this.projectRepository = projectRepository;
        this.workspaceScopeCacheService = workspaceScopeCacheService;
        this.workspaceUserRepository = workspaceUserRepository;
        this.resourceOwnershipResolvers = resourceOwnershipResolvers.stream()
            .collect(Collectors.toMap(ResourceOwnershipResolver::resourceType, Function.identity()));

        this.resourceEnvironmentResolvers = resourceEnvironmentResolvers.stream()
            .collect(Collectors.toMap(ResourceEnvironmentResolver::resourceType, Function.identity()));
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
            decider -> decider.decideWorkspace(workspaceId, scope), "Workspace:" + scope);

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
            decider -> decider.decideWorkspace(workspaceId, scope), "Workspace:" + scope);

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
            decider -> decider.decide(projectId, PROJECT, scope), PROJECT + ":" + scope);

        if (connectedUserDecision.isPresent()) {
            return connectedUserDecision.get();
        }

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
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
            decider -> decider.decideInEnvironment(projectId, PROJECT, scope, environment), PROJECT + ":" + scope);

        if (connectedUserDecision.isPresent()) {
            return connectedUserDecision.get();
        }

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
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
            decider -> decider.decide(id, resourceType, scope), resourceType + ":" + scope);

        if (connectedUserDecision.isPresent()) {
            return connectedUserDecision.get();
        }

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        ResourceOwnershipResolver resourceOwnershipResolver = resourceOwnershipResolvers.get(resourceType);

        if (resourceOwnershipResolver == null) {
            return false;
        }

        OptionalLong workspaceId = resourceOwnershipResolver.resolveOwner(id)
            .workspaceId();

        if (workspaceId.isEmpty()) {
            return false;
        }

        // A resource that lives in an environment is checked against the role the caller holds THERE. Without this,
        // a member who is viewer in Production would still pass a by-id check on a Production deployment, because the
        // environment-unaware check unions the environments they can reach. A type with no resolver, or a resolver
        // that reports no environment, keeps the environment-unaware check. A resolver that fails denies: falling back
        // would answer the check from that union.
        ResourceEnvironmentResolver resourceEnvironmentResolver = resourceEnvironmentResolvers.get(resourceType);

        if (resourceEnvironmentResolver != null) {
            Optional<Environment> environment;

            try {
                environment = resourceEnvironmentResolver.fetchEnvironment(id);
            } catch (RuntimeException exception) {
                log.error(
                    "Denying {} on {} id={}: resolving its environment failed", scope, resourceType, id, exception);

                return false;
            }

            if (environment.isPresent()) {
                return hasWorkspaceScope(workspaceId.getAsLong(), scope, environment.get());
            }
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
            decider -> decider.decideInEnvironment(id, resourceType, scope, environment),
            resourceType + ":" + scope);

        if (connectedUserDecision.isPresent()) {
            return connectedUserDecision.get();
        }

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        ResourceOwnershipResolver resourceOwnershipResolver = resourceOwnershipResolvers.get(resourceType);

        if (resourceOwnershipResolver == null) {
            return false;
        }

        OptionalLong workspaceId = resourceOwnershipResolver.resolveOwner(id)
            .workspaceId();

        if (workspaceId.isEmpty()) {
            return false;
        }

        return hasWorkspaceScope(workspaceId.getAsLong(), scope, environment);
    }

    @Override
    public boolean isResourceOwner(String resourceType, long id) {
        if (isGovernedPrincipal()) {
            return false;
        }

        if (isTenantAdmin()) {
            return true;
        }

        ResourceOwnershipResolver resourceOwnershipResolver = resourceOwnershipResolvers.get(resourceType);

        if (resourceOwnershipResolver == null) {
            return false;
        }

        OptionalLong ownerUserId = resourceOwnershipResolver.resolveOwner(id)
            .ownerUserId();

        return ownerUserId.isPresent() && isCurrentUser(ownerUserId.getAsLong());
    }

    @Override
    public boolean hasResourceRole(long id, String resourceType, String minimumRole) {
        if (isGovernedPrincipal()) {
            return false;
        }

        if (isTenantAdmin()) {
            return true;
        }

        ResourceOwnershipResolver resourceOwnershipResolver = resourceOwnershipResolvers.get(resourceType);

        if (resourceOwnershipResolver == null) {
            return false;
        }

        OptionalLong workspaceId = resourceOwnershipResolver.resolveOwner(id)
            .workspaceId();

        if (workspaceId.isEmpty()) {
            return false;
        }

        return hasWorkspaceRole(workspaceId.getAsLong(), minimumRole);
    }

    @Override
    public boolean hasWorkflowScope(String workflowId, String scope) {
        Optional<Boolean> connectedUserDecision = decideForConnectedUser(
            decider -> decider.decideWorkflow(workflowId, scope), WORKFLOW + ":" + scope);

        if (connectedUserDecision.isPresent()) {
            return connectedUserDecision.get();
        }

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        Long workspaceId = projectRepository.findByWorkflowId(workflowId)
            .map(Project::getWorkspaceId)
            .orElse(null);

        if (workspaceId == null) {
            return false;
        }

        return hasWorkspaceScope(workspaceId, scope);
    }

    @Override
    public boolean hasWorkflowScope(String workflowId, String scope, Environment environment) {
        Optional<Boolean> connectedUserDecision = decideForConnectedUser(
            decider -> decider.decideWorkflow(workflowId, scope), WORKFLOW + ":" + scope);

        if (connectedUserDecision.isPresent()) {
            return connectedUserDecision.get();
        }

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
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
            decider -> decider.decideWorkflow(workflowId, scope), WORKFLOW + ":" + scope);

        if (connectedUserDecision.isPresent()) {
            return connectedUserDecision.get();
        }

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
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
            decider -> decideConnectionInWorkflow(decider, connectionId, workflowId),
            "ConnectionInWorkflow:" + WORKFLOW_EDIT);

        if (connectedUserDecision.isPresent()) {
            return connectedUserDecision.get();
        }

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        Long workspaceId = projectRepository.findByWorkflowId(workflowId)
            .map(Project::getWorkspaceId)
            .orElse(null);

        if (workspaceId == null) {
            return false;
        }

        return canUseConnectionInWorkspace(connectionId, workspaceId, environment);
    }

    @Override
    public boolean canUseConnectionInWorkspace(long connectionId, long workspaceId, Environment environment) {
        Optional<Boolean> connectedUserDecision = decideForConnectedUser(
            decider -> decider.decideWorkspace(workspaceId, CONNECTION_VIEW), "Workspace:" + CONNECTION_VIEW);

        if (connectedUserDecision.isPresent()) {
            return connectedUserDecision.get();
        }

        if (isAutomationAuthorizationSkipped()) {
            return true;
        }

        if (isTenantAdmin()) {
            return true;
        }

        ResourceOwnershipResolver connectionOwnershipResolver = resourceOwnershipResolvers.get(CONNECTION);
        ResourceEnvironmentResolver connectionEnvironmentResolver = resourceEnvironmentResolvers.get(CONNECTION);

        if (connectionOwnershipResolver == null || connectionEnvironmentResolver == null) {
            return false;
        }

        OptionalLong connectionWorkspaceId = connectionOwnershipResolver.resolveOwner(connectionId)
            .workspaceId();

        if (connectionWorkspaceId.isEmpty() || connectionWorkspaceId.getAsLong() != workspaceId) {
            return false;
        }

        Optional<Environment> connectionEnvironment;

        try {
            connectionEnvironment = connectionEnvironmentResolver.fetchEnvironment(connectionId);
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

    private Optional<Boolean> decideForConnectedUser(
        Function<ConnectedUserAccessDecider, Decision> decision, String subject) {

        ConnectedUserAccessDecider connectedUserAccessDecider = connectedUserAccessDeciderProvider.getIfAvailable();

        if (connectedUserAccessDecider == null) {
            return Optional.empty();
        }

        Decision outcome = decision.apply(connectedUserAccessDecider);

        if (outcome == Decision.NOT_GOVERNED) {
            return Optional.empty();
        }

        if (isConnectedUserAuthorizationEnforced()) {
            return Optional.of(outcome == Decision.GRANT);
        }

        boolean skipped = AutomationAuthorizationContext.isSkipChecks();

        if (outcome == Decision.DENY && skipped && loggedWouldBeDenials.add(subject)) {
            log.warn("Connected-user authorization would deny {} (LOG mode; skip still applies)", subject);
        } else if (outcome == Decision.DENY && !skipped && loggedWouldBeDenials.add(subject + ":outside-skip")) {
            log.warn("Connected-user authorization denied {} outside skip mode (LOG mode)", subject);
        }

        return Optional.of(skipped || outcome == Decision.GRANT);
    }

    private boolean isConnectedUserAuthorizationEnforced() {
        ApplicationProperties.Security security = applicationProperties.getSecurity();

        return security != null
            && security.getConnectedUserAuthorizationMode() == ConnectedUserAuthorizationMode.ENFORCE;
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
