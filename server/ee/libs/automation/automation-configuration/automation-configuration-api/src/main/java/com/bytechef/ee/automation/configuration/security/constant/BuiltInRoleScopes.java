/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security.constant;

import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.AGENT_CREATE;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.AGENT_EDIT;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.AGENT_EXECUTE;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.AGENT_VIEW;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.CONNECTION_CREATE;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.CONNECTION_DELETE;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.CONNECTION_EDIT;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.CONNECTION_USE;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.CONNECTION_VIEW;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.DEPLOYMENT_PULL;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.DEPLOYMENT_PUSH;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.EXECUTION_DATA;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.EXECUTION_RETRY;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.EXECUTION_VIEW;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.PROJECT_VIEW_USERS;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.WORKFLOW_CREATE;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.WORKFLOW_DELETE;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.WORKFLOW_EDIT;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.WORKFLOW_TOGGLE;
import static com.bytechef.ee.automation.configuration.security.constant.PermissionScope.WORKFLOW_VIEW;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Maps built-in {@link ProjectRole} values to their granted {@link PermissionScope} sets. The role-to-scope sets follow
 * a strict subset hierarchy: VIEWER &sub; OPERATOR &sub; EDITOR &sub; ADMIN. The hierarchy is enforced <em>by
 * construction</em> — each tier is built as the previous tier's scope set unioned with that tier's delta — so a
 * violation would require deleting from the parent set, which is statically prevented because each tier reads from an
 * unmodifiable view. ADMIN receives {@code EnumSet.allOf(PermissionScope.class)}, so newly added scopes are
 * automatically granted to ADMIN.
 *
 * <p>
 * Scope assignment rationale:
 * <ul>
 * <li>VIEWER: read-only access to project artifacts.
 * <li>OPERATOR: VIEWER + ability to toggle and re-run executions (operations work, no edits).
 * <li>EDITOR: OPERATOR + create/edit/delete of workflows, connections, agents, plus deployment push/pull. Does not get
 * user management or project deletion.
 * <li>ADMIN: everything via {@code EnumSet.allOf}.
 * </ul>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class BuiltInRoleScopes {

    private static final Map<ProjectRole, Set<PermissionScope>> ROLE_SCOPES;

    static {
        EnumSet<PermissionScope> viewer = EnumSet.of(
            WORKFLOW_VIEW,
            EXECUTION_VIEW,
            CONNECTION_VIEW,
            AGENT_VIEW,
            PROJECT_VIEW_USERS);

        EnumSet<PermissionScope> operator = EnumSet.copyOf(viewer);

        operator.addAll(EnumSet.of(
            WORKFLOW_TOGGLE,
            EXECUTION_DATA, EXECUTION_RETRY,
            CONNECTION_USE,
            AGENT_EXECUTE));

        EnumSet<PermissionScope> editor = EnumSet.copyOf(operator);

        editor.addAll(EnumSet.of(
            WORKFLOW_CREATE, WORKFLOW_EDIT, WORKFLOW_DELETE,
            CONNECTION_CREATE, CONNECTION_EDIT, CONNECTION_DELETE,
            AGENT_CREATE, AGENT_EDIT,
            DEPLOYMENT_PUSH, DEPLOYMENT_PULL));

        EnumMap<ProjectRole, Set<PermissionScope>> map = new EnumMap<>(ProjectRole.class);

        map.put(ProjectRole.VIEWER, Collections.unmodifiableSet(viewer));
        map.put(ProjectRole.OPERATOR, Collections.unmodifiableSet(operator));
        map.put(ProjectRole.EDITOR, Collections.unmodifiableSet(editor));
        map.put(ProjectRole.ADMIN, Collections.unmodifiableSet(EnumSet.allOf(PermissionScope.class)));

        ROLE_SCOPES = Collections.unmodifiableMap(map);
    }

    /**
     * Returns the unmodifiable scope set for the given built-in role. Throws if {@code role} is unknown — silently
     * returning an empty set would mask a programming error (a newly added {@link ProjectRole} that nobody updated the
     * map for).
     */
    public static Set<PermissionScope> getScopesForRole(ProjectRole role) {
        Objects.requireNonNull(role, "role must not be null");

        Set<PermissionScope> scopes = ROLE_SCOPES.get(role);

        if (scopes == null) {
            throw new IllegalStateException(
                "No scope mapping defined for ProjectRole." + role.name()
                    + " — update BuiltInRoleScopes.ROLE_SCOPES");
        }

        return Collections.unmodifiableSet(scopes);
    }

    private BuiltInRoleScopes() {
    }
}
