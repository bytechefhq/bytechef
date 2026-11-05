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
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class AgentPermissionScopeProvider implements PermissionScopeProvider {

    @Override
    public Set<ScopeDefinition> scopeDefinitions() {
        return Set.of(
            new ScopeDefinition(AgentPermissionScope.AGENT_VIEW, WorkspaceRole.VIEWER),
            new ScopeDefinition(AgentPermissionScope.AGENT_CREATE, WorkspaceRole.EDITOR),
            new ScopeDefinition(AgentPermissionScope.AGENT_EDIT, WorkspaceRole.EDITOR),
            new ScopeDefinition(AgentPermissionScope.AGENT_DELETE, WorkspaceRole.EDITOR));
    }
}
