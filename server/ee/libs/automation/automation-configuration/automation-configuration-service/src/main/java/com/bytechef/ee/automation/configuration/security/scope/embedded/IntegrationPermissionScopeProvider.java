/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security.scope.embedded;

import com.bytechef.ee.automation.configuration.security.PermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScopeSide;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Declares the embedded integration permission scopes.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class IntegrationPermissionScopeProvider implements PermissionScopeProvider {

    @Override
    public PermissionScopeSide side() {
        return PermissionScopeSide.EMBEDDED;
    }

    @Override
    public Set<ScopeDefinition> scopeDefinitions() {
        return Set.of(
            new ScopeDefinition(IntegrationPermissionScope.INTEGRATION_CREATE, WorkspaceRole.EDITOR),
            new ScopeDefinition(IntegrationPermissionScope.INTEGRATION_PUBLISH, WorkspaceRole.EDITOR),
            new ScopeDefinition(IntegrationPermissionScope.INTEGRATION_SETTINGS, WorkspaceRole.ADMIN),
            new ScopeDefinition(IntegrationPermissionScope.INTEGRATION_DELETE, WorkspaceRole.ADMIN));
    }
}
