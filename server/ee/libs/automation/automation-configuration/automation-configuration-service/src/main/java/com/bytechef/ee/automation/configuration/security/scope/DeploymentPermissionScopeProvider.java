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
 * Declares the deployment-domain permission scopes. Read is granted from VIEWER; push/pull/edit from EDITOR.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class DeploymentPermissionScopeProvider implements PermissionScopeProvider {

    @Override
    public Set<ScopeDefinition> scopeDefinitions() {
        return Set.of(
            new ScopeDefinition(DeploymentPermissionScope.DEPLOYMENT_VIEW, WorkspaceRole.VIEWER),
            new ScopeDefinition(DeploymentPermissionScope.DEPLOYMENT_PUSH, WorkspaceRole.EDITOR),
            new ScopeDefinition(DeploymentPermissionScope.DEPLOYMENT_PULL, WorkspaceRole.EDITOR),
            new ScopeDefinition(DeploymentPermissionScope.DEPLOYMENT_EDIT, WorkspaceRole.EDITOR));
    }
}
