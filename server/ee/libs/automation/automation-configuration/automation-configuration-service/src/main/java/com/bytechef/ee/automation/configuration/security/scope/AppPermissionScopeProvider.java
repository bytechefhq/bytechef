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
 * Declares the App permission scopes. Read is granted from VIEWER; managing apps (create, update, delete, rotate the
 * secret key, manage bindings) from ADMIN.
 *
 * @version ee
 */
@Component
@ConditionalOnEEVersion
public class AppPermissionScopeProvider implements PermissionScopeProvider {

    @Override
    public Set<ScopeDefinition> scopeDefinitions() {
        return Set.of(
            new ScopeDefinition(AppPermissionScope.APP_VIEW, WorkspaceRole.VIEWER),
            new ScopeDefinition(AppPermissionScope.APP_MANAGE, WorkspaceRole.ADMIN));
    }
}
