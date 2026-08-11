/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.automation.configuration.audit.CustomRoleAuditEvent;
import com.bytechef.ee.automation.configuration.audit.CustomRoleAuditPublisher;
import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.exception.CustomRoleErrorType;
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private final CustomRoleAuditPublisher customRoleAuditPublisher;
    private final CustomRoleRepository customRoleRepository;
    private final PermissionScopeRegistry permissionScopeRegistry;
    private final PermissionService permissionService;
    private final WorkspaceUserService workspaceUserService;

    @SuppressFBWarnings("EI")
    public CustomRoleServiceImpl(
        CustomRoleAuditPublisher customRoleAuditPublisher, CustomRoleRepository customRoleRepository,
        PermissionScopeRegistry permissionScopeRegistry, PermissionService permissionService,
        WorkspaceUserService workspaceUserService) {

        this.customRoleAuditPublisher = customRoleAuditPublisher;
        this.customRoleRepository = customRoleRepository;
        this.permissionScopeRegistry = permissionScopeRegistry;
        this.permissionService = permissionService;
        this.workspaceUserService = workspaceUserService;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public CustomRole createCustomRole(String name, String description, Set<String> scopeNames) {
        validateScopeNames(scopeNames);

        CustomRole customRole = new CustomRole(name, scopeNames);

        customRole.setDescription(description);

        CustomRole savedCustomRole = customRoleRepository.save(customRole);

        Map<String, Object> data = new HashMap<>();

        data.put("roleId", String.valueOf(savedCustomRole.getId()));
        data.put("name", savedCustomRole.getName());

        customRoleAuditPublisher.publish(CustomRoleAuditEvent.CUSTOM_ROLE_CREATED, data);

        return savedCustomRole;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public void deleteCustomRole(long roleId) {
        long memberCount = workspaceUserService.countByCustomRoleId(roleId);

        if (memberCount > 0) {
            throw new ConfigurationException(
                "Custom role is still assigned to " + memberCount + " workspace member(s)",
                CustomRoleErrorType.CUSTOM_ROLE_IN_USE);
        }

        customRoleRepository.deleteById(roleId);

        Map<String, Object> data = new HashMap<>();

        data.put("roleId", String.valueOf(roleId));

        customRoleAuditPublisher.publish(CustomRoleAuditEvent.CUSTOM_ROLE_DELETED, data);
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

        CustomRole customRole = OptionalUtils.get(customRoleRepository.findById(roleId));

        // Capture the scope set BEFORE mutation so we only evict the cache when permissions actually change. A
        // name-only or description-only edit does not affect any user's resolved permissions, and blowing away the
        // entire (userId, projectId) cache on every rename is a significant performance regression for tenants with
        // thousands of projects.
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

        Map<String, Object> data = new HashMap<>();

        data.put("roleId", String.valueOf(saved.getId()));
        data.put("name", saved.getName());

        customRoleAuditPublisher.publish(CustomRoleAuditEvent.CUSTOM_ROLE_UPDATED, data);

        return saved;
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<String> getPermissionScopeNames() {
        // Static metadata, identical for every tenant -- the set of scopes the server was built with, not anyone's
        // data. Gating it harder than authentication would stop a role editor listing what it may compose from.
        return List.copyOf(permissionScopeRegistry.getAllScopeNames());
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
