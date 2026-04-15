/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScope;
import java.util.List;
import java.util.Set;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface CustomRoleService {

    CustomRole createCustomRole(String name, String description, Set<PermissionScope> scopes);

    void deleteCustomRole(long roleId);

    CustomRole getCustomRole(long roleId);

    List<CustomRole> getCustomRoles();

    CustomRole updateCustomRole(long roleId, String name, String description, Set<PermissionScope> scopes);
}
