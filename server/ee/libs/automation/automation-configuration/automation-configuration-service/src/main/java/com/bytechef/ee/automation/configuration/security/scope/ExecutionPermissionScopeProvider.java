/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security.scope;

import com.bytechef.ee.automation.configuration.security.PermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Declares the execution-domain permission scopes. Read is granted from VIEWER; delete from EDITOR.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class ExecutionPermissionScopeProvider implements PermissionScopeProvider {

    @Override
    public Set<ScopeDefinition> scopeDefinitions() {
        return Set.of(
            new ScopeDefinition("EXECUTION_VIEW", WorkspaceRole.VIEWER),
            new ScopeDefinition("EXECUTION_DELETE", WorkspaceRole.EDITOR));
    }
}
