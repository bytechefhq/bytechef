/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security.scope;

import com.bytechef.ee.automation.configuration.security.PermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Declares the API Platform permission scopes, common across API collections and their endpoints. Read is granted from
 * VIEWER; create/edit from EDITOR -- the same rank split {@link McpPermissionScopeProvider} uses, since the two
 * features expose comparable surfaces.
 *
 * <p>
 * The API Platform feature is itself EE, unlike MCP, but this provider still lives here rather than in
 * {@code automation-api-platform} for the reason that module's facade never needs it: a {@code @PreAuthorize}
 * expression names a scope by string, so only the provider requires a compile dependency on {@link WorkspaceRole}-based
 * RBAC. Keeping all 11 providers in one package is what makes the set greppable.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ApiPlatformPermissionScopeProvider implements PermissionScopeProvider {

    @Override
    public Set<ScopeDefinition> scopeDefinitions() {
        return Set.of(
            new ScopeDefinition(ApiPlatformPermissionScope.API_PLATFORM_VIEW, WorkspaceRole.VIEWER),
            new ScopeDefinition(ApiPlatformPermissionScope.API_PLATFORM_CREATE, WorkspaceRole.EDITOR),
            new ScopeDefinition(ApiPlatformPermissionScope.API_PLATFORM_EDIT, WorkspaceRole.EDITOR),
            new ScopeDefinition(ApiPlatformPermissionScope.API_PLATFORM_DELETE, WorkspaceRole.ADMIN));
    }
}
