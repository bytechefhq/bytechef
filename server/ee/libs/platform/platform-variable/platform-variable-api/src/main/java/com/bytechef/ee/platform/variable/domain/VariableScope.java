/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.variable.domain;

import org.apache.commons.lang3.Validate;
import org.jspecify.annotations.Nullable;

/**
 * Identifies one variable set. {@code workspaceId} is non-null iff {@code type == WORKSPACE}.
 *
 * @version ee
 */
public record VariableScope(VariableScopeType type, @Nullable Long workspaceId) {

    public VariableScope {
        Validate.notNull(type, "type");

        if (type == VariableScopeType.WORKSPACE) {
            Validate.notNull(workspaceId, "workspaceId");
        } else {
            Validate.isTrue(workspaceId == null, "workspaceId must be null for scope type %s", type);
        }
    }

    public static VariableScope workspace(long workspaceId) {
        return new VariableScope(VariableScopeType.WORKSPACE, workspaceId);
    }

    public static VariableScope embedded() {
        return new VariableScope(VariableScopeType.EMBEDDED, null);
    }
}
