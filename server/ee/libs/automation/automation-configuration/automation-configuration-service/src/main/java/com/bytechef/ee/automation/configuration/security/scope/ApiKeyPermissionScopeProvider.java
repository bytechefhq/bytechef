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
 * Declares the API-key permission scopes, all from EDITOR.
 *
 * <p>
 * Read sits at the same tier as create deliberately. A key authenticates as the user who owns it, so the read is of
 * one's own keys — and a VIEWER can never mint one, which would make the scope a permanently empty list. Reading
 * somebody else's is not what it grants: that is the tenant admin's short-circuit.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ApiKeyPermissionScopeProvider implements PermissionScopeProvider {

    @Override
    public Set<ScopeDefinition> scopeDefinitions() {
        return Set.of(
            new ScopeDefinition(ApiKeyPermissionScope.API_KEY_VIEW, WorkspaceRole.EDITOR),
            new ScopeDefinition(ApiKeyPermissionScope.API_KEY_CREATE, WorkspaceRole.EDITOR),
            new ScopeDefinition(ApiKeyPermissionScope.API_KEY_DELETE, WorkspaceRole.EDITOR));
    }
}
