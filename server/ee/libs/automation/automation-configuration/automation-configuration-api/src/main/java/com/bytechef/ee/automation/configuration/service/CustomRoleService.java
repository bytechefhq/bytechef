/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.ee.automation.configuration.domain.CustomRole;
import java.util.List;
import java.util.Set;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface CustomRoleService {

    /**
     * Creates a tenant-global custom role, assignable in every workspace. Requires tenant admin — a global role is
     * assignable everywhere, so creating one is a tenant-wide act.
     */
    CustomRole createCustomRole(String name, String description, Set<String> scopeNames);

    /**
     * Deletes a custom role. Fails while the role is still assigned to any workspace member. Requires tenant admin.
     */
    void deleteCustomRole(long roleId);

    /**
     * Every permission scope name a module contributed through {@code PermissionScopeProvider}.
     *
     * <p>
     * Exists so a role editor offers the scopes the server will actually accept. A hardcoded client list silently omits
     * any scope a module adds later, and the server rejects anything it does not recognise — the two must come from the
     * same place.
     */
    List<String> getPermissionScopeNames();

    /**
     * Every custom role in the tenant. The read has two audiences — tenant admins managing roles, and workspace member
     * managers populating the assignment picker — so {@code workspaceId} is pure authorization context: pass the
     * workspace being managed to read as its member manager, or {@code null} for the tenant-admin view. It never
     * filters; every role is tenant-global.
     */
    List<CustomRole> getCustomRoles(Long workspaceId);

    /**
     * Updates a custom role. Requires tenant admin.
     */
    CustomRole updateCustomRole(long roleId, String name, String description, Set<String> scopeNames);
}
