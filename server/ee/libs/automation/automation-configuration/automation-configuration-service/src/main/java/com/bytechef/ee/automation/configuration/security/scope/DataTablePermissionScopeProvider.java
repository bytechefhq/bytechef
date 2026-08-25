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
 * Declares the data-table permission scopes. Read is granted from VIEWER; create/edit from EDITOR.
 *
 * <p>
 * The data-table feature itself is a CE module, but {@link WorkspaceRole}-based RBAC is EE, so this provider lives in
 * the EE automation-configuration module rather than in the CE data-table module.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class DataTablePermissionScopeProvider implements PermissionScopeProvider {

    @Override
    public Set<ScopeDefinition> scopeDefinitions() {
        return Set.of(
            new ScopeDefinition(DataTablePermissionScope.DATA_TABLE_VIEW, WorkspaceRole.VIEWER),
            new ScopeDefinition(DataTablePermissionScope.DATA_TABLE_CREATE, WorkspaceRole.EDITOR),
            new ScopeDefinition(DataTablePermissionScope.DATA_TABLE_EDIT, WorkspaceRole.EDITOR));
    }
}
