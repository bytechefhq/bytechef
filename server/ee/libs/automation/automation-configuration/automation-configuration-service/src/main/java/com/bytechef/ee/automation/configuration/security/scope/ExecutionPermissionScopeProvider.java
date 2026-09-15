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
 * Declares the execution-domain permission scopes. Read is granted from VIEWER; delete from ADMIN.
 *
 * <p>
 * Delete sits at ADMIN rather than EDITOR. Deleting an execution destroys the record of what a workflow did -- the
 * audit trail an operator reaches for after an incident -- and is not undoable. Editing a workflow is ordinary work;
 * erasing the evidence of a past run is not.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ExecutionPermissionScopeProvider implements PermissionScopeProvider {

    @Override
    public Set<ScopeDefinition> scopeDefinitions() {
        return Set.of(
            new ScopeDefinition(ExecutionPermissionScope.EXECUTION_VIEW, WorkspaceRole.VIEWER),
            new ScopeDefinition(ExecutionPermissionScope.EXECUTION_DELETE, WorkspaceRole.ADMIN));
    }
}
