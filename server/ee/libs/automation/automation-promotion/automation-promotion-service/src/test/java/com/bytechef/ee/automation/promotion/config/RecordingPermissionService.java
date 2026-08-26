/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.promotion.config;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.configuration.domain.Environment;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * The {@link PermissionService} every {@code hasPermission(...)} guard in the promotion integration-test context
 * resolves to. It records each {@link #hasResourceScope} call so a test can assert on the id the
 * {@code @promotionAuthorizer} bean reference produced, and answers every check with a single switchable verdict.
 *
 * <p>
 * <b>A hand-written {@code @Component} rather than a {@code @Bean}-supplied Mockito mock, deliberately.</b>
 * {@code AutomationMethodSecurityConfiguration} — the auto-configuration that contributes
 * {@code AutomationPermissionEvaluator} and the expression handler — is {@code @ConditionalOnBean(PermissionService
 * .class)}, and Spring Boot evaluates that condition while SELECTING auto-configurations, long before any {@code @Bean}
 * method of the test configuration has been registered. A component-scanned definition exists by then; a {@code @Bean}
 * one does not. With the auto-configuration filtered out, method security silently falls back to Spring's deny-all
 * permission evaluator and every guarded call in the context refuses.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component("permissionService")
public class RecordingPermissionService implements PermissionService {

    /**
     * One {@code hasPermission(id, resourceType, scope)} evaluation, as it reached the permission evaluator.
     */
    public record ResourceScopeCheck(Serializable id, String resourceType, String scope) {
    }

    private final List<ResourceScopeCheck> resourceScopeChecks = new ArrayList<>();

    private boolean granted = true;

    public List<ResourceScopeCheck> getResourceScopeChecks() {
        return List.copyOf(resourceScopeChecks);
    }

    public void reset() {
        resourceScopeChecks.clear();

        granted = true;
    }

    public void setGranted(boolean granted) {
        this.granted = granted;
    }

    @Override
    public void evictWorkspaceScopeCache(long userId, long workspaceId) {
        // Deliberately a no-op. Clearing the recording here would let an eviction fired from somewhere inside a
        // promotion path wipe the evidence the authorization test asserts on, failing it for a reason that has
        // nothing to do with what it checks. Only reset() clears the recording.
    }

    @Override
    public void evictAllWorkspaceScopeCache() {
        // Deliberately a no-op, for the reason given on evictWorkspaceScopeCache.
    }

    @Override
    public Set<String> getMyWorkspaceScopes(long workspaceId) {
        return Set.of();
    }

    @Override
    public @Nullable String getMyWorkspaceRole(long workspaceId) {
        return null;
    }

    @Override
    public boolean hasResourceRole(long id, String resourceType, String minimumRole) {
        return granted;
    }

    @Override
    public boolean hasResourceScope(Serializable id, String resourceType, String scope) {
        resourceScopeChecks.add(new ResourceScopeCheck(id, resourceType, scope));

        return granted;
    }

    @Override
    public boolean hasWorkflowScope(String workflowId, String scope) {
        return granted;
    }

    @Override
    public boolean hasWorkflowScope(String workflowId, String scope, Environment environment) {
        return granted;
    }

    @Override
    public boolean hasWorkspaceRole(long workspaceId, String minimumRole) {
        return granted;
    }

    @Override
    public boolean hasWorkspaceScope(long workspaceId, String scope) {
        return granted;
    }

    @Override
    public boolean hasWorkspaceScope(long workspaceId, String scope, Environment environment) {
        return granted;
    }

    @Override
    public boolean hasWorkspaceScopeInEveryEnvironment(long workspaceId, String scope) {
        return granted;
    }

    @Override
    public boolean hasWorkspaceScopeForProject(long projectId, String scope) {
        return granted;
    }

    @Override
    public boolean hasWorkspaceScopeForProject(long projectId, String scope, Environment environment) {
        return granted;
    }

    @Override
    public boolean isCurrentUser(long userId) {
        return granted;
    }

    @Override
    public boolean isResourceOwner(String resourceType, long id) {
        return granted;
    }

    @Override
    public boolean isTenantAdmin() {
        return granted;
    }
}
