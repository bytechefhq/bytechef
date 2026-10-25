/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.dto.BuiltInRoleDTO;
import com.bytechef.ee.automation.configuration.dto.PermissionScopeGroupDTO;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScopeSide;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface CustomRoleFacade {

    CustomRole createCustomRole(String name, String description, PermissionScopeSide side, Set<String> scopeNames);

    void deleteCustomRole(long roleId);

    List<BuiltInRoleDTO> getBuiltInRoles();

    List<CustomRole> getCustomRoles(Long workspaceId, @Nullable PermissionScopeSide side);

    List<PermissionScopeGroupDTO> getPermissionScopeGroups(PermissionScopeSide side);

    CustomRole updateCustomRole(long roleId, String name, String description, Set<String> scopeNames);
}
