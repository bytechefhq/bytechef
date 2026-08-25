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
 * Declares the project-lifecycle permission scopes. Create is granted from EDITOR; settings and delete are ADMIN-only
 * (destructive project-level operations stay with workspace administrators).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ProjectPermissionScopeProvider implements PermissionScopeProvider {

    @Override
    public Set<ScopeDefinition> scopeDefinitions() {
        return Set.of(
            new ScopeDefinition(ProjectPermissionScope.PROJECT_CREATE, WorkspaceRole.EDITOR),
            new ScopeDefinition(ProjectPermissionScope.PROJECT_SETTINGS, WorkspaceRole.ADMIN),
            new ScopeDefinition(ProjectPermissionScope.PROJECT_DELETE, WorkspaceRole.ADMIN));
    }
}
