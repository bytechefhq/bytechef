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
 * Declares the Data Sync permission scopes. Read is granted from VIEWER; create/edit/delete/publish from EDITOR.
 *
 * <p>
 * The Data Sync feature itself is a CE module, but {@link WorkspaceRole}-based RBAC is EE, so this provider lives in
 * the EE automation-configuration module rather than in the CE data-sync module — the same placement, for the same
 * reason, as {@link AgentPermissionScopeProvider}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class DataSyncPermissionScopeProvider implements PermissionScopeProvider {

    @Override
    public Set<ScopeDefinition> scopeDefinitions() {
        return Set.of(
            new ScopeDefinition(DataSyncPermissionScope.DATA_SYNC_VIEW, WorkspaceRole.VIEWER),
            new ScopeDefinition(DataSyncPermissionScope.DATA_SYNC_CREATE, WorkspaceRole.EDITOR),
            new ScopeDefinition(DataSyncPermissionScope.DATA_SYNC_EDIT, WorkspaceRole.EDITOR),
            new ScopeDefinition(DataSyncPermissionScope.DATA_SYNC_DELETE, WorkspaceRole.EDITOR),
            new ScopeDefinition(DataSyncPermissionScope.DATA_SYNC_PUBLISH, WorkspaceRole.EDITOR));
    }
}
