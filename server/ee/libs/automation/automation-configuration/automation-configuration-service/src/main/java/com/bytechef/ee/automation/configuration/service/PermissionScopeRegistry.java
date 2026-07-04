/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.ee.automation.configuration.security.PermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.PermissionScopeProvider.ScopeDefinition;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Aggregates every {@link PermissionScopeProvider} into the central view of permission scopes: the full set of valid
 * scope names (for custom-role validation) and the built-in {@link WorkspaceRole} &rarr; scope-name mapping. The scopes
 * are assembled at startup from the per-module providers rather than from any central enum.
 *
 * <p>
 * A role is granted every scope whose declared {@link ScopeDefinition#minimumRole()} it is at least as privileged as
 * ({@code role.hasAtLeast(minimumRole)}), so {@code VIEWER ⊆ EDITOR ⊆ ADMIN} holds by rank with no explicit-delta
 * construction. Fails fast on a duplicate scope name declared with a conflicting tier.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class PermissionScopeRegistry {

    private final Set<String> allScopeNames;
    private final Map<WorkspaceRole, Set<String>> roleScopeNames;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public PermissionScopeRegistry(List<PermissionScopeProvider> permissionScopeProviders) {
        Map<String, WorkspaceRole> minimumRoleByScope = new HashMap<>();

        for (PermissionScopeProvider permissionScopeProvider : permissionScopeProviders) {
            for (ScopeDefinition scopeDefinition : permissionScopeProvider.scopeDefinitions()) {
                WorkspaceRole existing = minimumRoleByScope.putIfAbsent(
                    scopeDefinition.name(), scopeDefinition.minimumRole());

                if (existing != null && existing != scopeDefinition.minimumRole()) {
                    throw new IllegalStateException(
                        "Permission scope '" + scopeDefinition.name() +
                            "' declared with conflicting minimum roles " + existing + " and " +
                            scopeDefinition.minimumRole());
                }
            }
        }

        this.allScopeNames = Collections.unmodifiableSet(new LinkedHashSet<>(minimumRoleByScope.keySet()));

        EnumMap<WorkspaceRole, Set<String>> map = new EnumMap<>(WorkspaceRole.class);

        for (WorkspaceRole role : WorkspaceRole.values()) {
            Set<String> scopeNames = minimumRoleByScope.entrySet()
                .stream()
                .filter(entry -> role.hasAtLeast(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

            map.put(role, Collections.unmodifiableSet(scopeNames));
        }

        this.roleScopeNames = Collections.unmodifiableMap(map);
    }

    /**
     * Every registered scope name, across all providers. Used to validate custom-role scope assignments.
     */
    public Set<String> getAllScopeNames() {
        return allScopeNames;
    }

    /**
     * The scope names granted to the given built-in role by rank.
     */
    public Set<String> getScopeNames(WorkspaceRole role) {
        return roleScopeNames.getOrDefault(role, Set.of());
    }
}
