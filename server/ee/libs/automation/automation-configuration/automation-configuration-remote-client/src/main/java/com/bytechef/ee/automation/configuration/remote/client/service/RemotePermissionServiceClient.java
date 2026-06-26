/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.service;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.io.Serializable;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Remote client stub for {@link PermissionService}. Wired into lightweight EE app variants (worker, webhook, execution,
 * coordinator, connection) that do not host the authoritative permission service. These methods are not expected to be
 * reached from those apps; if they are, it typically means a {@code @PreAuthorize("hasPermission(...)")} method (whose
 * evaluator resolves the check through this {@link PermissionService}) is being invoked inside an app variant that
 * should not be exposing the guarded endpoint in the first place.
 *
 * <p>
 * Design choice: fail closed rather than throw. Throwing {@link UnsupportedOperationException} from SpEL evaluation
 * surfaces as an opaque HTTP 500 with no audit trail. Returning {@code false} from every check makes Spring Security
 * translate the invocation into an {@link org.springframework.security.access.AccessDeniedException}, which the
 * {@code PermissionAuditAspect} records as a DENIED event. Each call also emits an ERROR log so operators detect the
 * misconfiguration. Cache evictions are silent no-ops (idempotent).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@SuppressWarnings("PMD.UnusedFormalParameter")
public class RemotePermissionServiceClient implements PermissionService {

    private static final Logger log = LoggerFactory.getLogger(RemotePermissionServiceClient.class);

    @Override
    public boolean isTenantAdmin() {
        logError("isTenantAdmin");

        return false;
    }

    @Override
    public boolean isCurrentUser(long userId) {
        logError("isCurrentUser");

        return false;
    }

    @Override
    public boolean hasWorkspaceRole(long workspaceId, String minimumRole) {
        logError("hasWorkspaceRole");

        return false;
    }

    @Override
    public boolean hasWorkspaceScope(long workspaceId, String scope) {
        logError("hasWorkspaceScope");

        return false;
    }

    @Override
    public boolean hasWorkspaceScopeForProject(long projectId, String scope) {
        logError("hasWorkspaceScopeForProject");

        return false;
    }

    @Override
    public boolean hasResourceScope(Serializable id, String resourceType, String scope) {
        logError("hasResourceScope");

        return false;
    }

    @Override
    public boolean isResourceOwner(String resourceType, long id) {
        logError("isResourceOwner");

        return false;
    }

    @Override
    public boolean hasResourceRole(long id, String resourceType, String minimumRole) {
        logError("hasResourceRole");

        return false;
    }

    @Override
    public boolean hasWorkflowScope(String workflowId, String scope) {
        logError("hasWorkflowScope");

        return false;
    }

    @Override
    public Set<String> getMyWorkspaceScopes(long workspaceId) {
        logError("getMyWorkspaceScopes");

        return Set.of();
    }

    @Override
    public @Nullable String getMyWorkspaceRole(long workspaceId) {
        logError("getMyWorkspaceRole");

        return null;
    }

    @Override
    public void evictWorkspaceScopeCache(long userId, long workspaceId) {
        logError("evictWorkspaceScopeCache");
    }

    @Override
    public void evictAllWorkspaceScopeCache() {
        logError("evictAllWorkspaceScopeCache");
    }

    private static void logError(String method) {
        log.error(
            "PermissionService.{} invoked on a remote client stub. Returning fail-closed default. This app variant "
                + "does not host the authoritative PermissionService \u2014 check @PreAuthorize wiring.",
            method);
    }
}
