/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.security;

import com.bytechef.automation.configuration.security.constant.PermissionScopeType;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import java.util.Set;

/**
 * SPI contributed once per module to declare the permission scopes that module owns, together with the lowest built-in
 * {@link WorkspaceRole} granted each scope. A {@code PermissionScopeRegistry} aggregates every provider so the central
 * set of valid scopes — and the built-in role &rarr; scope mapping — is assembled from the modules rather than
 * hardcoded in one enum. Mirrors the {@code ResourceOwnershipResolver} per-module SPI.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface PermissionScopeProvider {

    /**
     * The scopes this module owns. Must be unique across all providers; a scope declared by two providers (or twice
     * with different {@link ScopeDefinition#minimumRole()}) is a registration error.
     */
    Set<ScopeDefinition> scopeDefinitions();

    /**
     * A single scope: its {@link PermissionScopeType} and the lowest built-in {@link WorkspaceRole} that is granted it.
     * Higher-privilege roles inherit it by rank (VIEWER-min &rArr; VIEWER/EDITOR/ADMIN; EDITOR-min &rArr; EDITOR/ADMIN;
     * ADMIN-min &rArr; ADMIN only), which keeps the VIEWER &sub; EDITOR &sub; ADMIN invariant by construction.
     */
    record ScopeDefinition(PermissionScopeType scopeType, WorkspaceRole minimumRole) {
    }
}
