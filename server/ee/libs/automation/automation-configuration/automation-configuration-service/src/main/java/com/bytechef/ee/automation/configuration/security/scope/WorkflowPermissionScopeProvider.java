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
 * Declares the workflow-domain permission scopes. Read is granted from VIEWER; create/edit/delete from EDITOR.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class WorkflowPermissionScopeProvider implements PermissionScopeProvider {

    @Override
    public Set<ScopeDefinition> scopeDefinitions() {
        return Set.of(
            new ScopeDefinition(WorkflowPermissionScope.WORKFLOW_VIEW, WorkspaceRole.VIEWER),
            new ScopeDefinition(WorkflowPermissionScope.WORKFLOW_CREATE, WorkspaceRole.EDITOR),
            new ScopeDefinition(WorkflowPermissionScope.WORKFLOW_EDIT, WorkspaceRole.EDITOR),
            new ScopeDefinition(WorkflowPermissionScope.WORKFLOW_DELETE, WorkspaceRole.EDITOR));
    }
}
