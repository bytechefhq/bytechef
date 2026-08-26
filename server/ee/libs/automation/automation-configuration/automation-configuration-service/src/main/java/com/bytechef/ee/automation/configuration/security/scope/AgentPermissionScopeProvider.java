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
 * Declares the AI-agent permission scopes. Read is granted from VIEWER; create/edit/delete from EDITOR.
 *
 * <p>
 * The AI-agent feature itself is a CE module, but {@link WorkspaceRole}-based RBAC is EE, so this provider lives in the
 * EE automation-configuration module rather than in the CE agent module — the same placement, for the same reason, as
 * {@link KnowledgeBasePermissionScopeProvider}.
 *
 * <p>
 * {@code AGENT_DELETE} is EDITOR-rank, matching {@code WORKFLOW_DELETE} rather than the ADMIN-rank
 * {@code PROJECT_DELETE}. An agent is a workflow-scale entity: it owns one generated workflow inside a hidden
 * {@code __AI_AGENT__} project, and that project is an implementation detail of the workflow rather than a project the
 * user manages. Deleting an agent is therefore closer to deleting a workflow than to deleting a project.
 *
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
