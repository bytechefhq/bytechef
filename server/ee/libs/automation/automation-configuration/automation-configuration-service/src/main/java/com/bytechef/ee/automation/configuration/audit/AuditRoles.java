/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.audit;

import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * Role strings shared by the audit mappers: a built-in role name, {@code customRole:<id>}, and for per-environment rows
 * {@code <ENVIRONMENT>:<role>}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class AuditRoles {

    private static final String CUSTOM_ROLE_PREFIX = "customRole:";

    private AuditRoles() {
    }

    static @Nullable String describe(@Nullable WorkspaceRole workspaceRole, @Nullable Long customRoleId) {
        if (customRoleId != null) {
            return CUSTOM_ROLE_PREFIX + customRoleId;
        }

        return workspaceRole == null ? null : workspaceRole.name();
    }

    static String describe(WorkspaceUser workspaceUser) {
        Long customRoleId = workspaceUser.getCustomRoleId();

        if (customRoleId != null) {
            return CUSTOM_ROLE_PREFIX + customRoleId;
        }

        Integer workspaceRoleOrdinal = workspaceUser.getWorkspaceRole();
        WorkspaceRole[] workspaceRoles = WorkspaceRole.values();

        if (workspaceRoleOrdinal == null || workspaceRoleOrdinal < 0 || workspaceRoleOrdinal >= workspaceRoles.length) {
            return "UNKNOWN";
        }

        return workspaceRoles[workspaceRoleOrdinal].name();
    }

    static String describeAll(List<WorkspaceUser> workspaceUsers) {
        return workspaceUsers.stream()
            .map(AuditRoles::describeWithEnvironment)
            .collect(Collectors.joining(","));
    }

    private static String describeWithEnvironment(WorkspaceUser workspaceUser) {
        Environment environment = workspaceUser.getEnvironment();

        return environment == null ? describe(workspaceUser) : environment.name() + ":" + describe(workspaceUser);
    }
}
