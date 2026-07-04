/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security.constant;

import com.bytechef.automation.configuration.security.constant.WorkspaceRoleType;
import java.util.Objects;

/**
 * Workspace-level roles.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum WorkspaceRole implements WorkspaceRoleType {

    ADMIN(0),
    EDITOR(1),
    VIEWER(2);

    private final int privilegeRank;

    WorkspaceRole(int privilegeRank) {
        this.privilegeRank = privilegeRank;
    }

    public int getPrivilegeRank() {
        return privilegeRank;
    }

    /**
     * Returns {@code true} when this role is at least as privileged as {@code required}. Encapsulates the "lower rank =
     * higher privilege" rule so call sites can say {@code role.hasAtLeast(EDITOR)} without hard-coding the comparison
     * direction.
     */
    public boolean hasAtLeast(WorkspaceRole required) {
        Objects.requireNonNull(required, "'required' role must not be null");

        return privilegeRank <= required.privilegeRank;
    }
}
