/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security.constant;

import com.bytechef.automation.configuration.security.constant.ProjectRoleType;
import java.util.Objects;

/**
 * Project-level roles. Two invariants are encoded independently:
 * <ul>
 * <li><b>Ordinal</b> (declaration order) — persisted as INT in {@code project_user.project_role}. Reordering silently
 * corrupts every stored row, so new roles MUST be appended to the end of the declaration list.</li>
 * <li><b>Privilege rank</b> (explicit {@code privilegeRank} field, lower = more privileged) — used for hierarchy
 * comparisons via {@link #hasAtLeast(ProjectRole)} and {@code PermissionServiceImpl}. Decoupling rank from ordinal
 * means a new role can be appended at the end (ordinal-safe) and still be positioned anywhere in the hierarchy by
 * choosing its rank.</li>
 * </ul>
 * {@code EnumOrdinalPinTest} pins both ordinals and privilege ranks; keep that test up to date.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum ProjectRole implements ProjectRoleType {

    ADMIN(0),
    EDITOR(1),
    OPERATOR(2),
    VIEWER(3);

    private final int privilegeRank;

    ProjectRole(int privilegeRank) {
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
    public boolean hasAtLeast(ProjectRole required) {
        Objects.requireNonNull(required, "'required' role must not be null");

        return privilegeRank <= required.privilegeRank;
    }
}
