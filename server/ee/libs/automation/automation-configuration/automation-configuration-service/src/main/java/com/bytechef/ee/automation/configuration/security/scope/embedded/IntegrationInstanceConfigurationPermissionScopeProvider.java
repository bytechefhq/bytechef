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
 * Declares the embedded integration instance configuration permission scopes.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class IntegrationInstanceConfigurationPermissionScopeProvider implements PermissionScopeProvider {

    @Override
    public PermissionScopeSide side() {
        return PermissionScopeSide.EMBEDDED;
    }

    @Override
    public Set<ScopeDefinition> scopeDefinitions() {
        return Set.of(
            new ScopeDefinition(IntegrationInstanceConfigurationPermissionScope.INTEGRATION_INSTANCE_CONFIGURATION_VIEW,
                WorkspaceRole.VIEWER),
            new ScopeDefinition(
                IntegrationInstanceConfigurationPermissionScope.INTEGRATION_INSTANCE_CONFIGURATION_CREATE,
                WorkspaceRole.EDITOR),
            new ScopeDefinition(IntegrationInstanceConfigurationPermissionScope.INTEGRATION_INSTANCE_CONFIGURATION_EDIT,
                WorkspaceRole.EDITOR),
            new ScopeDefinition(
                IntegrationInstanceConfigurationPermissionScope.INTEGRATION_INSTANCE_CONFIGURATION_DELETE,
                WorkspaceRole.ADMIN));
    }
}
