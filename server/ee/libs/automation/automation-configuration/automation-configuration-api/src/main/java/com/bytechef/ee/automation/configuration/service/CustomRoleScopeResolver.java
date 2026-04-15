/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.ee.automation.configuration.security.constant.PermissionScope;
import java.util.Optional;
import java.util.Set;

/**
 * SPI for resolving permission scopes from a custom role ID.
 *
 * <p>
 * The return type is intentionally {@link Optional} so callers can distinguish "role not found" (data corruption /
 * orphan reference) from "role exists but grants nothing." With the {@code setPermissionScopes} {@code notEmpty}
 * invariant enforced on {@code CustomRole}, the latter should be impossible at the persistence layer, but keeping the
 * two cases discriminable lets callers log loudly and increment a metric on the corruption path instead of silently
 * evaluating to {@code contains == false}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface CustomRoleScopeResolver {

    /**
     * Returns the scopes granted by the given custom role, or {@link Optional#empty()} if no such role exists.
     * Implementations MUST log at ERROR when returning empty so operators can alert on orphaned references.
     */
    Optional<Set<PermissionScope>> resolveScopes(long customRoleId);
}
