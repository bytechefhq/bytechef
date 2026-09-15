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
 * Declares the connection-domain permission scopes. Read is granted from VIEWER, create from EDITOR, and edit and
 * delete from ADMIN.
 *
 * <p>
 * A connection is shared: every workflow that uses it depends on it, including workflows other members built. So
 * changing one (its name and tags) or deleting one is not something any editor may do. Creating one is, because
 * building a workflow needs connections; it is checked in the environment the new connection belongs to.
 *
 * <p>
 * There is no CONNECTION_USE. It named a real distinction -- using a connection in a workflow without being able to
 * read or change its credentials -- but nothing anywhere honoured it, so the editor offered a checkbox that neither
 * granted nor withheld anything. Withdrawn rather than left standing, on the same grounds as the scopes removed before
 * it: the distinction can return alongside the guard that enforces it.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ConnectionPermissionScopeProvider implements PermissionScopeProvider {

    @Override
    public Set<ScopeDefinition> scopeDefinitions() {
        return Set.of(
            new ScopeDefinition(ConnectionPermissionScope.CONNECTION_VIEW, WorkspaceRole.VIEWER),
            new ScopeDefinition(ConnectionPermissionScope.CONNECTION_CREATE, WorkspaceRole.EDITOR),
            new ScopeDefinition(ConnectionPermissionScope.CONNECTION_EDIT, WorkspaceRole.ADMIN),
            new ScopeDefinition(ConnectionPermissionScope.CONNECTION_DELETE, WorkspaceRole.ADMIN));
    }
}
