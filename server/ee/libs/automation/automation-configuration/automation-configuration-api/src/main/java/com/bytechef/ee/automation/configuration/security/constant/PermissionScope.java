/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security.constant;

import com.bytechef.automation.configuration.security.constant.PermissionScopeType;

/**
 * Fine-grained permission scopes representing individual actions. Scopes are the lowest-level authorization unit,
 * checked via {@code @PreAuthorize("@permissionService.hasProjectScope(#projectId, 'SCOPE_NAME')")}. Built-in roles map
 * to scope sets in {@link BuiltInRoleScopes}; custom roles (EE) can define arbitrary scope combinations.
 *
 * <p>
 * Persistence is by name into {@code custom_role_scope.scope} (VARCHAR), so ordinal values are not load-bearing for
 * storage. Renames to existing constants are breaking changes; additions and reorders of the declaration list are not.
 * {@code EnumOrdinalPinTest} still pins the full shape so accidental insertions in the middle are caught at build time.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum PermissionScope implements PermissionScopeType {

    // Workflow
    WORKFLOW_VIEW,
    WORKFLOW_CREATE,
    WORKFLOW_EDIT,
    WORKFLOW_DELETE,
    WORKFLOW_TOGGLE,

    // Execution
    EXECUTION_VIEW,
    EXECUTION_DATA,
    EXECUTION_RETRY,

    // Connection
    CONNECTION_VIEW,
    CONNECTION_CREATE,
    CONNECTION_EDIT,
    CONNECTION_DELETE,
    CONNECTION_USE,

    // Agent / MCP
    AGENT_VIEW,
    AGENT_CREATE,
    AGENT_EDIT,
    AGENT_EXECUTE,

    // Project management
    PROJECT_VIEW_USERS,
    PROJECT_MANAGE_USERS,
    PROJECT_SETTINGS,

    // Deployment
    DEPLOYMENT_PUSH,
    DEPLOYMENT_PULL,

    // Project lifecycle (added separately so it isn't accidentally bundled with WORKFLOW_DELETE on a custom role)
    PROJECT_DELETE
}
