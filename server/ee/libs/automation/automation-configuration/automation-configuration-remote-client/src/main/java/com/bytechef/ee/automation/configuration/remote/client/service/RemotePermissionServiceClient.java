/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.service;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Remote client stub for {@link PermissionService}. Wired into lightweight EE app variants (worker, webhook, execution,
 * coordinator, connection) that do not host the authoritative permission service. These methods are not expected to be
 * reached from those apps; if they are, it typically means a {@code @PreAuthorize("@permissionService
 * ...")} method is being invoked inside an app variant that should not be exposing the guarded endpoint in the first
 * place.
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

    private static final Logger logger = LoggerFactory.getLogger(RemotePermissionServiceClient.class);

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
    public boolean hasProjectScope(long projectId, String scope) {
        logError("hasProjectScope");

        return false;
    }

    @Override
    public boolean hasProjectRole(long projectId, String minimumRole) {
        logError("hasProjectRole");

        return false;
    }

    @Override
    public Set<String> getMyProjectScopes(long projectId) {
        logError("getMyProjectScopes");

        return Set.of();
    }

    @Override
    public @Nullable String getMyWorkspaceRole(long workspaceId) {
        logError("getMyWorkspaceRole");

        return null;
    }

    @Override
    public void evictProjectScopeCache(long userId, long projectId) {
        logError("evictProjectScopeCache");
    }

    @Override
    public void evictAllProjectScopeCache() {
        logError("evictAllProjectScopeCache");
    }

    private static void logError(String method) {
        logger.error(
            "PermissionService.{} invoked on a remote client stub. Returning fail-closed default. This app variant "
                + "does not host the authoritative PermissionService \u2014 check @PreAuthorize wiring.",
            method);
    }
}
