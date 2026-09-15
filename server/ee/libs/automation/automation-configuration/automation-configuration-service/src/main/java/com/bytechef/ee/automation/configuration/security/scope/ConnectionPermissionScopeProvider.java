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
 * Declares the connection-domain permission scopes. Read is granted from VIEWER; edit and delete from ADMIN.
 *
 * <p>
 * A connection is shared: every workflow that uses it depends on it, including workflows other members built. So
 * changing one (its name and tags on this branch) or deleting one is not something any editor may do to a connection
 * that is not theirs. The exception is the connection's owner, who may edit and delete their own connection without
 * either scope - an editor keeps full control of the connections they created. That exception needs the connection's
 * owner, which the {@code Connection} ownership resolver supplies; the resolver and the connection guards arrive with
 * #4750, so none of the three scopes is enforced on this branch yet.
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
            new ScopeDefinition(ConnectionPermissionScope.CONNECTION_EDIT, WorkspaceRole.ADMIN),
            new ScopeDefinition(ConnectionPermissionScope.CONNECTION_DELETE, WorkspaceRole.ADMIN));
    }
}
