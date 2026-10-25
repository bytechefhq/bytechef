/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.audit.EmbeddedUserAuditEvents;
import com.bytechef.ee.automation.configuration.audit.EmbeddedUserAuditMapper;
import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.domain.EmbeddedUser;
import com.bytechef.ee.automation.configuration.exception.WorkspaceUserErrorType;
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.ee.automation.configuration.repository.EmbeddedUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScopeSide;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class EmbeddedUserServiceImpl implements EmbeddedUserService {

    private final CustomRoleRepository customRoleRepository;
    private final EmbeddedUserRepository embeddedUserRepository;
    private final PermissionScopeRegistry permissionScopeRegistry;
    private final PermissionService permissionService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public EmbeddedUserServiceImpl(
        CustomRoleRepository customRoleRepository, EmbeddedUserRepository embeddedUserRepository,
        PermissionScopeRegistry permissionScopeRegistry, PermissionService permissionService, UserService userService) {

        this.customRoleRepository = customRoleRepository;
        this.embeddedUserRepository = embeddedUserRepository;
        this.permissionScopeRegistry = permissionScopeRegistry;
        this.permissionService = permissionService;
        this.userService = userService;
    }

    @Override
    public EmbeddedUser addEmbeddedUser(long userId, WorkspaceRole embeddedRole) {
        Assert.notNull(embeddedRole, "'embeddedRole' must not be null");

        return addEmbeddedUser(userId, embeddedRole, null);
    }

    @Override
    public EmbeddedUser addEmbeddedUser(
        long userId, @Nullable WorkspaceRole embeddedRole, @Nullable Long customRoleId) {

        validateRoleSelection(embeddedRole, customRoleId);

        embeddedUserRepository.lockEmbeddedUsers(userId);

        userService.getUser(userId);

        validateGrantWithinCallerScopes(List.of(Environment.values()), embeddedRole, customRoleId);

        if (embeddedUserRepository.existsByUserId(userId)) {
            throw new ConfigurationException(
                "User " + userId + " is already an embedded member", WorkspaceUserErrorType.ALREADY_MEMBER);
        }

        EmbeddedUser savedEmbeddedUser = embeddedUserRepository.save(
            customRoleId == null
                ? EmbeddedUser.forRole(userId, embeddedRole)
                : EmbeddedUser.forCustomRole(userId, customRoleId));

        permissionService.evictEmbeddedScopeCache(userId);

        return savedEmbeddedUser;
    }

    @Override
    @Transactional(readOnly = true)
    public long countByCustomRoleId(long customRoleId) {
        return embeddedUserRepository.countByCustomRoleId(customRoleId);
    }

    @Override
    @Audited(event = EmbeddedUserAuditEvents.EMBEDDED_USER_REMOVED, mapper = EmbeddedUserAuditMapper.class)
    public void deleteEmbeddedMemberships(long userId) {
        embeddedUserRepository.deleteByUserId(userId);

        permissionService.evictEmbeddedScopeCache(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmbeddedUser> getEmbeddedUsers() {
        return embeddedUserRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmbeddedUser> getUserEmbeddedUsers(long userId) {
        return embeddedUserRepository.findAllByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmbeddedMember(long userId) {
        return embeddedUserRepository.existsByUserId(userId);
    }

    @Override
    public void removeEnvironmentRole(long userId, Environment environment) {
        Assert.notNull(environment, "'environment' must not be null");

        lockEmbeddedAdmins();

        embeddedUserRepository.lockEmbeddedUsers(userId);

        Optional<EmbeddedUser> environmentEmbeddedUser = embeddedUserRepository
            .findByUserIdAndEnvironment(userId, environment.ordinal());

        if (environmentEmbeddedUser.isEmpty()) {
            throw missingEnvironmentMembership(userId, environment);
        }

        EmbeddedUser removedEmbeddedUser = environmentEmbeddedUser.get();

        List<EmbeddedUser> memberRows = embeddedUserRepository.findAllByUserId(userId);

        if (memberRows.size() <= 1) {
            if (!permissionService.hasEmbeddedScopeInEveryEnvironment("EMBEDDED_MEMBER_MANAGE")) {
                throw new AccessDeniedException(
                    "Widening a member's last environment role to an embedded-wide role requires "
                        + "EMBEDDED_MEMBER_MANAGE in every environment");
            }

            WorkspaceRole embeddedRole = removedEmbeddedUser.getEmbeddedRole();
            Long customRoleId = removedEmbeddedUser.getCustomRoleId();

            validateGrantWithinCallerScopes(List.of(Environment.values()), embeddedRole, customRoleId);

            embeddedUserRepository.delete(removedEmbeddedUser);

            embeddedUserRepository.save(copyRole(removedEmbeddedUser, userId, null));
        } else {
            Set<Environment> remainingAdminEnvironments = resolveAdminEnvironments(memberRows);

            remainingAdminEnvironments.remove(environment);

            validateEnvironmentsKeepAnAdmin(memberRows, remainingAdminEnvironments);

            embeddedUserRepository.delete(removedEmbeddedUser);
        }

        permissionService.evictEmbeddedScopeCache(userId);
    }

    @Override
    public boolean removeEmbeddedUser(long userId) {
        lockEmbeddedAdmins();

        embeddedUserRepository.lockEmbeddedUsers(userId);

        List<EmbeddedUser> embeddedUsers = embeddedUserRepository.findAllByUserId(userId);

        validateEnvironmentsKeepAnAdmin(embeddedUsers, Set.of());

        boolean existed = !embeddedUsers.isEmpty();

        embeddedUserRepository.deleteByUserId(userId);

        permissionService.evictEmbeddedScopeCache(userId);

        return existed;
    }

    @Override
    public EmbeddedUser setEnvironmentRole(long userId, Environment environment, WorkspaceRole embeddedRole) {
        Assert.notNull(embeddedRole, "'embeddedRole' must not be null");

        return setEnvironmentRole(userId, environment, embeddedRole, null);
    }

    @Override
    public EmbeddedUser setEnvironmentRole(
        long userId, Environment environment, @Nullable WorkspaceRole embeddedRole, @Nullable Long customRoleId) {

        Assert.notNull(environment, "'environment' must not be null");

        validateRoleSelection(embeddedRole, customRoleId);

        lockEmbeddedAdmins();

        embeddedUserRepository.lockEmbeddedUsers(userId);

        validateGrantWithinCallerScopes(List.of(environment), embeddedRole, customRoleId);

        List<EmbeddedUser> memberRows = embeddedUserRepository.findAllByUserId(userId);

        if (memberRows.isEmpty()) {
            throw missingMembership(userId);
        }

        Set<Environment> remainingAdminEnvironments = resolveAdminEnvironments(memberRows);

        if (embeddedRole == WorkspaceRole.ADMIN) {
            remainingAdminEnvironments.add(environment);
        } else {
            remainingAdminEnvironments.remove(environment);
        }

        validateEnvironmentsKeepAnAdmin(memberRows, remainingAdminEnvironments);

        Optional<EmbeddedUser> implicitEmbeddedUser = embeddedUserRepository.findByUserIdAndEnvironmentIsNull(userId);

        if (implicitEmbeddedUser.isPresent()) {
            EmbeddedUser implicit = implicitEmbeddedUser.get();

            embeddedUserRepository.delete(implicit);

            for (Environment otherEnvironment : Environment.values()) {
                if (otherEnvironment != environment) {
                    embeddedUserRepository.save(copyRole(implicit, userId, otherEnvironment));
                }
            }
        }

        Optional<EmbeddedUser> existingEmbeddedUser = embeddedUserRepository
            .findByUserIdAndEnvironment(userId, environment.ordinal());

        EmbeddedUser savedEmbeddedUser;

        if (existingEmbeddedUser.isPresent()) {
            EmbeddedUser embeddedUser = existingEmbeddedUser.get();

            assignRole(embeddedUser, embeddedRole, customRoleId);

            savedEmbeddedUser = embeddedUserRepository.save(embeddedUser);
        } else {
            savedEmbeddedUser = embeddedUserRepository.save(
                customRoleId == null
                    ? EmbeddedUser.forRole(userId, embeddedRole, environment)
                    : EmbeddedUser.forCustomRole(userId, customRoleId, environment));
        }

        permissionService.evictEmbeddedScopeCache(userId);

        return savedEmbeddedUser;
    }

    @Override
    public EmbeddedUser updateEmbeddedUserRole(long userId, WorkspaceRole embeddedRole) {
        Assert.notNull(embeddedRole, "'embeddedRole' must not be null");

        return updateEmbeddedUserRole(userId, embeddedRole, null);
    }

    @Override
    public EmbeddedUser updateEmbeddedUserRole(
        long userId, @Nullable WorkspaceRole embeddedRole, @Nullable Long customRoleId) {

        validateRoleSelection(embeddedRole, customRoleId);

        lockEmbeddedAdmins();

        embeddedUserRepository.lockEmbeddedUsers(userId);

        EmbeddedUser embeddedUser = embeddedUserRepository.findByUserIdAndEnvironmentIsNull(userId)
            .orElseThrow(() -> missingEmbeddedWideMembership(userId));

        validateGrantWithinCallerScopes(List.of(Environment.values()), embeddedRole, customRoleId);

        validateEnvironmentsKeepAnAdmin(
            List.of(embeddedUser),
            embeddedRole == WorkspaceRole.ADMIN ? EnumSet.allOf(Environment.class) : Set.of());

        assignRole(embeddedUser, embeddedRole, customRoleId);

        EmbeddedUser savedEmbeddedUser = embeddedUserRepository.save(embeddedUser);

        permissionService.evictEmbeddedScopeCache(userId);

        return savedEmbeddedUser;
    }

    private static void assignRole(
        EmbeddedUser embeddedUser, @Nullable WorkspaceRole embeddedRole, @Nullable Long customRoleId) {

        if (customRoleId == null) {
            embeddedUser.assignRole(embeddedRole);
        } else {
            embeddedUser.assignCustomRole(customRoleId);
        }
    }

    private static EmbeddedUser copyRole(
        EmbeddedUser embeddedUser, long userId, @Nullable Environment environment) {

        Long customRoleId = embeddedUser.getCustomRoleId();

        if (customRoleId != null) {
            return EmbeddedUser.forCustomRole(userId, customRoleId, environment);
        }

        WorkspaceRole embeddedRole = embeddedUser.getEmbeddedRole();

        Assert.state(embeddedRole != null, "An embedded membership must hold a built-in or a custom role");

        return EmbeddedUser.forRole(userId, embeddedRole, environment);
    }

    private Set<String> resolveGrantedScopeNames(@Nullable WorkspaceRole embeddedRole, @Nullable Long customRoleId) {
        if (customRoleId == null) {
            Assert.state(embeddedRole != null, "An embedded membership must hold a built-in or a custom role");

            return permissionScopeRegistry.getScopeNames(embeddedRole, PermissionScopeSide.EMBEDDED);
        }

        return resolveEmbeddedScopeNames(validateEmbeddedCustomRole(customRoleId));
    }

    private Set<String> resolveEmbeddedScopeNames(CustomRole customRole) {
        Set<String> embeddedScopeNames = new HashSet<>(customRole.getScopeNames());

        embeddedScopeNames.retainAll(permissionScopeRegistry.getAllScopeNames(PermissionScopeSide.EMBEDDED));

        return embeddedScopeNames;
    }

    private CustomRole validateEmbeddedCustomRole(long customRoleId) {
        CustomRole customRole = customRoleRepository.findById(customRoleId)
            .orElseThrow(() -> new ConfigurationException(
                "Custom role " + customRoleId + " does not exist", WorkspaceUserErrorType.CUSTOM_ROLE_NOT_FOUND));

        if (customRole.getSide() != PermissionScopeSide.EMBEDDED) {
            throw new ConfigurationException(
                "Custom role " + customRoleId + " is an automation role and cannot be assigned to an embedded member",
                WorkspaceUserErrorType.CUSTOM_ROLE_SIDE_MISMATCH);
        }

        return customRole;
    }

    private void validateRoleSelection(@Nullable WorkspaceRole embeddedRole, @Nullable Long customRoleId) {
        if ((embeddedRole == null) == (customRoleId == null)) {
            throw new ConfigurationException(
                "Exactly one of a built-in role or a custom role must be supplied",
                WorkspaceUserErrorType.ROLE_SELECTION_INVALID);
        }
    }

    private void lockEmbeddedAdmins() {
        embeddedUserRepository.lockEmbeddedUsersWithRole(WorkspaceRole.ADMIN.ordinal());
    }

    private static Set<Environment> resolveAdminEnvironments(List<EmbeddedUser> embeddedUsers) {
        Set<Environment> adminEnvironments = EnumSet.noneOf(Environment.class);

        for (EmbeddedUser embeddedUser : embeddedUsers) {
            if (embeddedUser.getEmbeddedRole() != WorkspaceRole.ADMIN) {
                continue;
            }

            Environment environment = embeddedUser.getEnvironment();

            if (environment == null) {
                return EnumSet.allOf(Environment.class);
            }

            adminEnvironments.add(environment);
        }

        return adminEnvironments;
    }

    private void validateEnvironmentsKeepAnAdmin(
        List<EmbeddedUser> embeddedUsers, Set<Environment> remainingAdminEnvironments) {

        for (Environment environment : resolveAdminEnvironments(embeddedUsers)) {
            if (remainingAdminEnvironments.contains(environment)) {
                continue;
            }

            long adminCount = embeddedUserRepository.countUsersWithRoleForEnvironment(
                WorkspaceRole.ADMIN.ordinal(), environment.ordinal());

            if (adminCount <= 1) {
                throw new ConfigurationException(
                    "Cannot remove or demote the last embedded admin of environment " + environment.name()
                        + ". At least one admin must remain there.",
                    WorkspaceUserErrorType.LAST_ADMIN_PROTECTED);
            }
        }
    }

    private ConfigurationException missingEmbeddedWideMembership(long userId) {
        if (embeddedUserRepository.existsByUserId(userId)) {
            return new ConfigurationException(
                "User " + userId + " holds per-environment embedded roles and so has no embedded-wide role to "
                    + "change. Change their role in each environment instead.",
                WorkspaceUserErrorType.EXPLICIT_MODE_MEMBERSHIP);
        }

        return missingMembership(userId);
    }

    private ConfigurationException missingEnvironmentMembership(long userId, Environment environment) {
        if (embeddedUserRepository.existsByUserId(userId)) {
            return new ConfigurationException(
                "User " + userId + " holds no role specific to environment " + environment.name()
                    + ". Their access there comes from their embedded-wide role; change or remove that instead.",
                WorkspaceUserErrorType.NO_ENVIRONMENT_ROLE);
        }

        return missingMembership(userId);
    }

    private ConfigurationException missingMembership(long userId) {
        return new ConfigurationException(
            "User " + userId + " is not an embedded member", WorkspaceUserErrorType.NOT_MEMBER);
    }

    private void validateGrantWithinCallerScopes(
        List<Environment> environments, @Nullable WorkspaceRole embeddedRole, @Nullable Long customRoleId) {

        Set<String> grantedScopeNames = resolveGrantedScopeNames(embeddedRole, customRoleId);

        for (Environment environment : environments) {
            Set<String> callerScopeNames = permissionService.getMyEmbeddedScopes(environment);

            List<String> missingScopeNames = grantedScopeNames.stream()
                .filter(scopeName -> !callerScopeNames.contains(scopeName))
                .sorted()
                .toList();

            if (!missingScopeNames.isEmpty()) {
                throw new AccessDeniedException(
                    "Cannot grant a role in environment " + environment.name()
                        + " that carries scopes you do not hold there: " + missingScopeNames);
            }
        }
    }
}
