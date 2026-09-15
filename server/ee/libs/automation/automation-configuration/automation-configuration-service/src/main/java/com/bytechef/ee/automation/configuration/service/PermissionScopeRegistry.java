/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.security.constant.PermissionScopeType;
import com.bytechef.ee.automation.configuration.dto.BuiltInRoleDTO;
import com.bytechef.ee.automation.configuration.dto.PermissionScopeGroupDTO;
import com.bytechef.ee.automation.configuration.security.PermissionScopeProvider;
import com.bytechef.ee.automation.configuration.security.PermissionScopeProvider.ScopeDefinition;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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

    private static final String PERMISSION_SCOPE_SUFFIX = "PermissionScope";

    private final Set<String> allScopeNames;
    private final List<BuiltInRoleDTO> builtInRoles;
    private final Map<WorkspaceRole, Set<String>> roleScopeNames;
    private final List<PermissionScopeGroupDTO> scopeGroups;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public PermissionScopeRegistry(List<PermissionScopeProvider> permissionScopeProviders) {
        Map<String, WorkspaceRole> minimumRoleByScope = new HashMap<>();

        // Sorted by module name, so the catalogue a role editor renders is stable across restarts. The scope set
        // itself is assembled in a HashMap, whose iteration order is neither declaration order nor alphabetical.
        Map<String, List<PermissionScopeType>> scopeTypesByGroup = new TreeMap<>();

        for (PermissionScopeProvider permissionScopeProvider : permissionScopeProviders) {
            for (ScopeDefinition scopeDefinition : permissionScopeProvider.scopeDefinitions()) {
                PermissionScopeType permissionScopeType = scopeDefinition.scopeType();

                WorkspaceRole existing = minimumRoleByScope.putIfAbsent(
                    permissionScopeType.name(), scopeDefinition.minimumRole());

                if (existing != null && existing != scopeDefinition.minimumRole()) {
                    throw new IllegalStateException(
                        "Permission scope '" + permissionScopeType.name() +
                            "' declared with conflicting minimum roles " + existing + " and " +
                            scopeDefinition.minimumRole());
                }

                if (existing == null) {
                    List<PermissionScopeType> groupScopeTypes = scopeTypesByGroup.computeIfAbsent(
                        getGroupName(permissionScopeType), groupName -> new ArrayList<>());

                    groupScopeTypes.add(permissionScopeType);
                }
            }
        }

        List<PermissionScopeGroupDTO> permissionScopeGroups = new ArrayList<>();

        for (Map.Entry<String, List<PermissionScopeType>> entry : scopeTypesByGroup.entrySet()) {
            List<PermissionScopeType> groupScopeTypes = entry.getValue();

            // A provider declares its scopes as a Set, so the order they arrive in is not the order they were
            // written. Ordinal restores the enum's own declaration order, which reads as an escalation.
            groupScopeTypes.sort(Comparator.comparingInt(PermissionScopeRegistry::getOrdinal));

            List<String> groupScopeNames = groupScopeTypes.stream()
                .map(PermissionScopeType::name)
                .toList();

            permissionScopeGroups.add(new PermissionScopeGroupDTO(entry.getKey(), groupScopeNames));
        }

        this.scopeGroups = Collections.unmodifiableList(permissionScopeGroups);

        // One catalogue order -- module, then the enum's own -- for every view of a scope set, so a built-in role's
        // permissions and the editor's checkboxes cannot disagree about sequence. Deriving it from the groups rather
        // than from minimumRoleByScope also keeps it off that HashMap's iteration order, which is neither.
        List<String> orderedScopeNames = permissionScopeGroups.stream()
            .flatMap(permissionScopeGroup -> permissionScopeGroup.scopes()
                .stream())
            .toList();

        this.allScopeNames = Collections.unmodifiableSet(new LinkedHashSet<>(orderedScopeNames));

        EnumMap<WorkspaceRole, Set<String>> map = new EnumMap<>(WorkspaceRole.class);

        for (WorkspaceRole role : WorkspaceRole.values()) {
            Set<String> scopeNames = orderedScopeNames.stream()
                .filter(scopeName -> role.hasAtLeast(minimumRoleByScope.get(scopeName)))
                .collect(Collectors.toCollection(LinkedHashSet::new));

            map.put(role, Collections.unmodifiableSet(scopeNames));
        }

        this.roleScopeNames = Collections.unmodifiableMap(map);

        // Least privileged first: the sets nest, so read downwards each tier shows what the one above adds.
        // WorkspaceRole declares itself in rank order, which is the opposite.
        List<BuiltInRoleDTO> roles = Arrays.stream(WorkspaceRole.values())
            .sorted(Comparator.comparingInt(WorkspaceRole::getPrivilegeRank)
                .reversed())
            .map(role -> new BuiltInRoleDTO(role.name(), List.copyOf(map.get(role))))
            .toList();

        this.builtInRoles = Collections.unmodifiableList(roles);
    }

    /**
     * The built-in roles and everything each grants, least privileged first. Read-only reference for an operator
     * deciding whether a custom role is needed at all; the tiers come from the same {@code minimumRole} declarations
     * the authorization checks use.
     */
    public List<BuiltInRoleDTO> getBuiltInRoles() {
        return builtInRoles;
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

    /**
     * Every registered scope, grouped by the module that owns it and ordered so the catalogue reads the same on every
     * boot. Used by the custom-role editor; validation uses {@link #getAllScopeNames()} against the same registration.
     */
    public List<PermissionScopeGroupDTO> getScopeGroups() {
        return scopeGroups;
    }

    /**
     * The module a scope belongs to, taken from the enum declaring it — {@code WorkspacePermissionScope} owns
     * {@code WORKSPACE}, {@code ApiKeyPermissionScope} owns {@code API_KEY}. Derived rather than declared so a module
     * contributing a new scope enum is grouped without touching this class, and rendered in the scope-name style the
     * client already title-cases.
     */
    private static String getGroupName(PermissionScopeType permissionScopeType) {
        Class<?> scopeClass = permissionScopeType instanceof Enum<?> enumScope
            ? enumScope.getDeclaringClass() : permissionScopeType.getClass();

        String simpleName = scopeClass.getSimpleName();

        if (simpleName.endsWith(PERMISSION_SCOPE_SUFFIX)) {
            simpleName = simpleName.substring(0, simpleName.length() - PERMISSION_SCOPE_SUFFIX.length());
        }

        StringBuilder groupName = new StringBuilder();

        for (int index = 0; index < simpleName.length(); index++) {
            char character = simpleName.charAt(index);

            if (index > 0 && Character.isUpperCase(character)) {
                groupName.append('_');
            }

            groupName.append(Character.toUpperCase(character));
        }

        return groupName.toString();
    }

    private static int getOrdinal(PermissionScopeType permissionScopeType) {
        return permissionScopeType instanceof Enum<?> enumScope ? enumScope.ordinal() : 0;
    }
}
