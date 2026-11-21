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
import com.bytechef.ee.automation.configuration.service.CustomRoleService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom roles are tenant-global and assignable in every workspace, so creating, updating and deleting one is reserved
 * for tenant admins. Listing them is also open to a member holding {@code WORKSPACE_MEMBER_MANAGE} on the named
 * workspace, who needs the list to assign a role there, and the built-in roles and the scope catalogue are static
 * metadata open to any authenticated user.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class CustomRoleFacadeImpl implements CustomRoleFacade {

    private final CustomRoleService customRoleService;

    @SuppressFBWarnings("EI")
    public CustomRoleFacadeImpl(CustomRoleService customRoleService) {
        this.customRoleService = customRoleService;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public CustomRole createCustomRole(String name, String description, Set<String> scopeNames) {
        return customRoleService.createCustomRole(name, description, scopeNames);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public void deleteCustomRole(long roleId) {
        customRoleService.deleteCustomRole(roleId);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<BuiltInRoleDTO> getBuiltInRoles() {
        return customRoleService.getBuiltInRoles();
    }

    @Override
    @PreAuthorize("(#workspaceId == null and isTenantAdmin()) or " +
        "(#workspaceId != null and hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_MEMBER_MANAGE'))")
    @Transactional(readOnly = true)
    public List<CustomRole> getCustomRoles(Long workspaceId) {
        return customRoleService.getCustomRoles(workspaceId);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<PermissionScopeGroupDTO> getPermissionScopeGroups() {
        return customRoleService.getPermissionScopeGroups();
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public CustomRole updateCustomRole(long roleId, String name, String description, Set<String> scopeNames) {
        return customRoleService.updateCustomRole(roleId, name, description, scopeNames);
    }
}
