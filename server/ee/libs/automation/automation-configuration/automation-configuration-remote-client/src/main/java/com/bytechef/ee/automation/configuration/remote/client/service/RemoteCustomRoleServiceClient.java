/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.service;

import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.dto.BuiltInRoleDTO;
import com.bytechef.ee.automation.configuration.dto.PermissionScopeGroupDTO;
import com.bytechef.ee.automation.configuration.service.CustomRoleService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Remote client stub for {@link CustomRoleService}. Wired into lightweight EE app variants that do not host the
 * authoritative service. Every method throws: an empty answer would read as "no roles" rather than as a misconfigured
 * deployment.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@SuppressWarnings("PMD.UnusedFormalParameter")
public class RemoteCustomRoleServiceClient implements CustomRoleService {

    private static final Logger log = LoggerFactory.getLogger(RemoteCustomRoleServiceClient.class);

    @Override
    public CustomRole createCustomRole(String name, String description, Set<String> scopeNames) {
        logError("createCustomRole");

        throw new UnsupportedOperationException(
            "CustomRoleService.createCustomRole is not available on this app variant");
    }

    @Override
    public void deleteCustomRole(long roleId) {
        logError("deleteCustomRole");

        throw new UnsupportedOperationException(
            "CustomRoleService.deleteCustomRole is not available on this app variant");
    }

    @Override
    public List<BuiltInRoleDTO> getBuiltInRoles() {
        logError("getBuiltInRoles");

        throw new UnsupportedOperationException(
            "CustomRoleService.getBuiltInRoles is not available on this app variant");
    }

    @Override
    public List<CustomRole> getCustomRoles(Long workspaceId) {
        logError("getCustomRoles");

        throw new UnsupportedOperationException(
            "CustomRoleService.getCustomRoles is not available on this app variant");
    }

    @Override
    public List<PermissionScopeGroupDTO> getPermissionScopeGroups() {
        logError("getPermissionScopeGroups");

        throw new UnsupportedOperationException(
            "CustomRoleService.getPermissionScopeGroups is not available on this app variant");
    }

    @Override
    public CustomRole updateCustomRole(long roleId, String name, String description, Set<String> scopeNames) {
        logError("updateCustomRole");

        throw new UnsupportedOperationException(
            "CustomRoleService.updateCustomRole is not available on this app variant");
    }

    private static void logError(String method) {
        log.error(
            "CustomRoleService.{} invoked on a remote client stub. App variant does not host the authoritative "
                + "CustomRoleService.",
            method);
    }
}
