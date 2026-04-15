/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.remote.client.service;

import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScope;
import com.bytechef.ee.automation.configuration.service.CustomRoleService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Remote client stub for {@link CustomRoleService}. Wired into lightweight EE app variants that do not host the
 * authoritative service. Read operations return fail-closed defaults with an ERROR log; mutations throw because a
 * silent no-op mutation is worse than an audible failure.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@SuppressWarnings("PMD.UnusedFormalParameter")
public class RemoteCustomRoleServiceClient implements CustomRoleService {

    private static final Logger logger = LoggerFactory.getLogger(RemoteCustomRoleServiceClient.class);

    @Override
    public CustomRole createCustomRole(String name, String description, Set<PermissionScope> scopes) {
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
    public CustomRole getCustomRole(long roleId) {
        logError("getCustomRole");

        throw new UnsupportedOperationException(
            "CustomRoleService.getCustomRole is not available on this app variant");
    }

    @Override
    public List<CustomRole> getCustomRoles() {
        logError("getCustomRoles");

        return List.of();
    }

    @Override
    public CustomRole updateCustomRole(long roleId, String name, String description, Set<PermissionScope> scopes) {
        logError("updateCustomRole");

        throw new UnsupportedOperationException(
            "CustomRoleService.updateCustomRole is not available on this app variant");
    }

    private static void logError(String method) {
        logger.error(
            "CustomRoleService.{} invoked on a remote client stub. App variant does not host the authoritative "
                + "CustomRoleService.",
            method);
    }
}
