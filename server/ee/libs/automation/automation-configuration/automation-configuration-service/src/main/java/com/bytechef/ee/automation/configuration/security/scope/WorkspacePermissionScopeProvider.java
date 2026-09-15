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
 * Declares the workspace-administration permission scopes. Read is granted from VIEWER; workspace and member management
 * are ADMIN-only.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class WorkspacePermissionScopeProvider implements PermissionScopeProvider {

    @Override
    public Set<ScopeDefinition> scopeDefinitions() {
        return Set.of(
            new ScopeDefinition(WorkspacePermissionScope.WORKSPACE_VIEW, WorkspaceRole.VIEWER),
            new ScopeDefinition(WorkspacePermissionScope.WORKSPACE_MANAGE, WorkspaceRole.ADMIN),
            new ScopeDefinition(WorkspacePermissionScope.WORKSPACE_MEMBER_MANAGE, WorkspaceRole.ADMIN));
    }
}
