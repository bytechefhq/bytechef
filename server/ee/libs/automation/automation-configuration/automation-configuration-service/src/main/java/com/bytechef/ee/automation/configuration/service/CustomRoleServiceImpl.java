/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.exception.CustomRoleErrorType;
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScope;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
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
    private final PermissionService permissionService;
    private final ProjectUserService projectUserService;

    @SuppressFBWarnings("EI")
    public CustomRoleServiceImpl(
        CustomRoleRepository customRoleRepository, PermissionService permissionService,
        ProjectUserService projectUserService) {

        this.customRoleRepository = customRoleRepository;
        this.permissionService = permissionService;
        this.projectUserService = projectUserService;
    }

    @Override
    @PreAuthorize("@permissionService.isTenantAdmin()")
    public CustomRole createCustomRole(String name, String description, Set<PermissionScope> scopes) {
        CustomRole customRole = new CustomRole(name, scopes);

        customRole.setDescription(description);

        return customRoleRepository.save(customRole);
    }

    @Override
    @PreAuthorize("@permissionService.isTenantAdmin()")
    public void deleteCustomRole(long roleId) {
        long memberCount = projectUserService.countByCustomRoleId(roleId);

        if (memberCount > 0) {
            throw new ConfigurationException(
                "Custom role is still assigned to " + memberCount + " project member(s)",
                CustomRoleErrorType.CUSTOM_ROLE_IN_USE);
        }

        customRoleRepository.deleteById(roleId);
    }

    @Override
    @PreAuthorize("@permissionService.isTenantAdmin()")
    @Transactional(readOnly = true)
    public CustomRole getCustomRole(long roleId) {
        return OptionalUtils.get(customRoleRepository.findById(roleId));
    }

    @Override
    @PreAuthorize("@permissionService.isTenantAdmin()")
    @Transactional(readOnly = true)
    public List<CustomRole> getCustomRoles() {
        return customRoleRepository.findAll();
    }

    @Override
    @PreAuthorize("@permissionService.isTenantAdmin()")
    public CustomRole updateCustomRole(
        long roleId, String name, String description, Set<PermissionScope> scopes) {

        CustomRole customRole = OptionalUtils.get(customRoleRepository.findById(roleId));

        // Capture the scope set BEFORE mutation so we only evict the cache when permissions actually change. A
        // name-only or description-only edit does not affect any user's resolved permissions, and blowing away the
        // entire (userId, projectId) cache on every rename is a significant performance regression for tenants with
        // thousands of projects.
        //
        // Snapshot both sides as EnumSet: (1) comparison drops to bit-mask equality instead of hash-bucket walk,
        // important on a tenant-admin hot path that this aspect audits; (2) we defend against the caller passing
        // an empty/immutable Set or a HashSet that could later mutate underneath us — EnumSet.copyOf of an empty
        // collection throws, so we special-case the empty-input case to an EnumSet.noneOf for safety.
        EnumSet<PermissionScope> previousScopes = toEnumSet(customRole.getPermissionScopes());
        EnumSet<PermissionScope> requestedScopes = toEnumSet(scopes);

        customRole.setName(name);
        customRole.setDescription(description);
        customRole.setPermissionScopes(requestedScopes);

        CustomRole saved = customRoleRepository.save(customRole);

        if (!previousScopes.equals(requestedScopes)) {
            permissionService.evictAllProjectScopeCache();
        }

        return saved;
    }

    private static EnumSet<PermissionScope> toEnumSet(Set<PermissionScope> scopes) {
        return scopes.isEmpty() ? EnumSet.noneOf(PermissionScope.class) : EnumSet.copyOf(scopes);
    }
}
