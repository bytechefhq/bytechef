/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.dto.BuiltInRoleDTO;
import com.bytechef.ee.automation.configuration.dto.PermissionScopeGroupDTO;
import com.bytechef.ee.automation.configuration.exception.CustomRoleErrorType;
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class CustomRoleServiceImpl implements CustomRoleService {

    private final CustomRoleRepository customRoleRepository;
    private final PermissionScopeRegistry permissionScopeRegistry;
    private final PermissionService permissionService;
    private final WorkspaceUserService workspaceUserService;

    @SuppressFBWarnings("EI")
    public CustomRoleServiceImpl(CustomRoleRepository customRoleRepository,
        PermissionScopeRegistry permissionScopeRegistry, PermissionService permissionService,
        WorkspaceUserService workspaceUserService) {

        this.customRoleRepository = customRoleRepository;
        this.permissionScopeRegistry = permissionScopeRegistry;
        this.permissionService = permissionService;
        this.workspaceUserService = workspaceUserService;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public CustomRole createCustomRole(String name, String description, Set<String> scopeNames) {
        validateScopeNames(scopeNames);
        validateNameIsAvailable(name, null);

        CustomRole customRole = new CustomRole(name, scopeNames);

        customRole.setDescription(description);

        CustomRole savedCustomRole = customRoleRepository.save(customRole);

        return savedCustomRole;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public void deleteCustomRole(long roleId) {
        // Checked, not assumed: deleteById on an unknown id is a silent no-op in Spring Data, so without this an
        // operator deleting a role twice — or acting on a stale list — got a success response for a deletion that
        // never happened.
        if (!customRoleRepository.existsById(roleId)) {
            throw new ConfigurationException(
                "Custom role " + roleId + " does not exist", CustomRoleErrorType.CUSTOM_ROLE_NOT_FOUND);
        }

        long memberCount = workspaceUserService.countByCustomRoleId(roleId);

        if (memberCount > 0) {
            throw new ConfigurationException(
                "Custom role is still assigned to " + memberCount + " workspace member(s)",
                CustomRoleErrorType.CUSTOM_ROLE_IN_USE);
        }

        customRoleRepository.deleteById(roleId);

    }

    @Override
    @PreAuthorize("(#workspaceId == null and isTenantAdmin()) or " +
        "(#workspaceId != null and hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_MEMBER_MANAGE'))")
    @Transactional(readOnly = true)
    public List<CustomRole> getCustomRoles(Long workspaceId) {
        // The workspaceId is authorization context, not a filter: a workspace member manager reads the list to
        // populate the assignment picker, a tenant admin reads it to manage roles. Every role is tenant-global.
        return customRoleRepository.findAll();
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public CustomRole updateCustomRole(long roleId, String name, String description, Set<String> scopeNames) {
        validateScopeNames(scopeNames);

        CustomRole customRole = customRoleRepository.findById(roleId)
            .orElseThrow(() -> new ConfigurationException(
                "Custom role " + roleId + " does not exist", CustomRoleErrorType.CUSTOM_ROLE_NOT_FOUND));

        validateNameIsAvailable(name, customRole.getName());

        // Capture the scope set BEFORE mutation so we only evict the cache when permissions actually change. A
        // name-only or description-only edit does not affect any user's resolved permissions, and the eviction below is
        // evictAllWorkspaceScopeCache(), which bottoms out in cache.clear() on the single workspaceScopes cache. Keys
        // are tenant-prefixed by TenantKeyGenerator but clear() ignores keys, so this drops every (userId, workspaceId)
        // entry of every tenant, not just the members holding this role. Doing that on every rename is a significant
        // performance regression well beyond the tenant being edited.
        //
        // Both sides are normalized to immutable Set copies so equality is order- and source-independent: a HashSet
        // and a List-backed Set with the same names compare equal, and neither side can mutate underneath us between
        // the snapshot and the comparison.
        Set<String> previousScopeNames = Set.copyOf(customRole.getScopeNames());
        Set<String> requestedScopeNames = Set.copyOf(scopeNames);

        customRole.setName(name);
        customRole.setDescription(description);
        customRole.setScopeNames(requestedScopeNames);

        CustomRole saved = customRoleRepository.save(customRole);

        if (!previousScopeNames.equals(requestedScopeNames)) {
            permissionService.evictAllWorkspaceScopeCache();
        }

        return saved;
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<BuiltInRoleDTO> getBuiltInRoles() {
        // Static metadata, identical for every tenant -- which tier the server grants which scope, not anyone's
        // assignments. Gating it harder than authentication would hide the tiers from the page that compares to them.
        return permissionScopeRegistry.getBuiltInRoles();
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<PermissionScopeGroupDTO> getPermissionScopeGroups() {
        // Static metadata, identical for every tenant -- the set of scopes the server was built with, not anyone's
        // data. Gating it harder than authentication would stop a role editor listing what it may compose from.
        return permissionScopeRegistry.getScopeGroups();
    }

    /**
     * Rejects a name another role already holds, before anything is written.
     *
     * <p>
     * {@code uk_custom_role_name} enforces this in the database too, but letting it fire is not an option: PostgreSQL
     * marks the transaction aborted the moment a constraint is violated, so catching the resulting
     * {@code DuplicateKeyException} would produce a handled error whose transaction still fails at commit. Asking first
     * is the only way the caller gets an answerable error.
     *
     * <p>
     * There is a window between this read and the write in which a concurrent create can take the name; the constraint
     * is what still holds then, and the loser sees a 500. That is the right trade — the check turns the common case (a
     * name somebody already used) into a 400 without pretending to be a lock.
     *
     * @param currentName the name the role holds today, so an edit that leaves the name alone does not collide with
     *                    itself, or {@code null} on create
     */
    private void validateNameIsAvailable(String name, String currentName) {
        // A description-only or scope-only edit resends the name the role already holds. Comparing first also means no
        // query at all on that path, which is the common one.
        if (Objects.equals(name, currentName)) {
            return;
        }

        for (WorkspaceRole workspaceRole : WorkspaceRole.values()) {
            if (workspaceRole.name()
                .equalsIgnoreCase(name.strip())) {

                throw new ConfigurationException(
                    "'" + name + "' is the name of a built-in role", CustomRoleErrorType.RESERVED_NAME);
            }
        }

        if (customRoleRepository.existsByName(name)) {
            throw new ConfigurationException(
                "A custom role named '" + name + "' already exists", CustomRoleErrorType.DUPLICATE_NAME);
        }
    }

    /**
     * Replaces the compile-time guarantee the old {@code PermissionScope} enum gave for free: every requested scope
     * name must be one a module actually contributed via {@code PermissionScopeProvider}. A name no provider declares
     * is rejected here (rather than persisted and silently inert) so a typo or a removed module surfaces at write time.
     */
    private void validateScopeNames(Set<String> scopeNames) {
        Set<String> allScopeNames = permissionScopeRegistry.getAllScopeNames();

        Set<String> unknownScopeNames = new TreeSet<>();

        for (String scopeName : scopeNames) {
            if (!allScopeNames.contains(scopeName)) {
                unknownScopeNames.add(scopeName);
            }
        }

        if (!unknownScopeNames.isEmpty()) {
            throw new ConfigurationException(
                "Invalid permission scope(s): " + unknownScopeNames + ". Valid scopes: " +
                    new TreeSet<>(allScopeNames),
                CustomRoleErrorType.INVALID_SCOPE);
        }
    }
}
