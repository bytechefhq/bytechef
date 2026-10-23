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
 * Declares the project-lifecycle permission scopes. Create, publish and push are granted from EDITOR; settings, delete
 * and pull are ADMIN-only.
 *
 * <p>
 * Push and pull split by what they do, not by what they need: both use the same workspace repository and project
 * branch. Push writes published workflows to that branch and changes nothing inside ByteChef, and publishing a
 * Git-synced project reaches it, so it stays with editors. Pull overwrites the project's workflows from the branch and
 * publishes a new version, which can discard other members' work, so it sits with the destructive project operations.
 * Configuring the repository and branch is separate again and needs WORKSPACE_MANAGE.
 *
 * <p>
 * Publish, push and pull live here rather than under deployment because that is the object they act on. Publishing cuts
 * a new <em>project</em> version; {@code ProjectGitFacadeImpl} syncs a <em>project</em>'s workflows with a branch and
 * never mentions a deployment. They were DEPLOYMENT_PUSH and DEPLOYMENT_PULL, which read as deployment operations and,
 * worse, put three unrelated privileges behind one checkbox: a custom role granting DEPLOYMENT_PUSH allowed publishing
 * a version, deploying it, and pushing to the customer's Git repository, with no way to grant one without the others.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class ProjectPermissionScopeProvider implements PermissionScopeProvider {

    @Override
    public Set<ScopeDefinition> scopeDefinitions() {
        return Set.of(
            new ScopeDefinition(ProjectPermissionScope.PROJECT_CREATE, WorkspaceRole.EDITOR),
            new ScopeDefinition(ProjectPermissionScope.PROJECT_SETTINGS, WorkspaceRole.ADMIN),
            new ScopeDefinition(ProjectPermissionScope.PROJECT_DELETE, WorkspaceRole.ADMIN),
            new ScopeDefinition(ProjectPermissionScope.PROJECT_PUBLISH, WorkspaceRole.EDITOR),
            new ScopeDefinition(ProjectPermissionScope.PROJECT_PUSH, WorkspaceRole.EDITOR),
            new ScopeDefinition(ProjectPermissionScope.PROJECT_PULL, WorkspaceRole.ADMIN));
    }
}
