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
import com.bytechef.ee.automation.configuration.domain.EmbeddedUser;
import com.bytechef.ee.automation.configuration.exception.WorkspaceUserErrorType;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

    private final EmbeddedUserRepository embeddedUserRepository;
    private final PermissionScopeRegistry permissionScopeRegistry;
    private final PermissionService permissionService;
    private final UserService userService;

    @SuppressFBWarnings("EI")
    public EmbeddedUserServiceImpl(
        EmbeddedUserRepository embeddedUserRepository, PermissionScopeRegistry permissionScopeRegistry,
        PermissionService permissionService, UserService userService) {

        this.embeddedUserRepository = embeddedUserRepository;
        this.permissionScopeRegistry = permissionScopeRegistry;
        this.permissionService = permissionService;
        this.userService = userService;
    }

    @Override
    public EmbeddedUser addEmbeddedUser(long userId, WorkspaceRole embeddedRole) {
        Assert.notNull(embeddedRole, "'embeddedRole' must not be null");

        embeddedUserRepository.lockEmbeddedUsers(userId);

        userService.getUser(userId);

        validateGrantWithinCallerScopes(List.of(Environment.values()), embeddedRole);

        if (embeddedUserRepository.existsByUserId(userId)) {
            throw new ConfigurationException(
                "User " + userId + " is already an embedded member", WorkspaceUserErrorType.ALREADY_MEMBER);
        }

        EmbeddedUser savedEmbeddedUser = embeddedUserRepository.save(EmbeddedUser.forRole(userId, embeddedRole));

        permissionService.evictEmbeddedScopeCache(userId);

        return savedEmbeddedUser;
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

            Assert.state(embeddedRole != null, "An embedded membership must hold a built-in role");

            validateGrantWithinCallerScopes(List.of(Environment.values()), embeddedRole);

            embeddedUserRepository.delete(removedEmbeddedUser);

            embeddedUserRepository.save(EmbeddedUser.forRole(userId, embeddedRole));
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
        Assert.notNull(environment, "'environment' must not be null");
        Assert.notNull(embeddedRole, "'embeddedRole' must not be null");

        lockEmbeddedAdmins();

        embeddedUserRepository.lockEmbeddedUsers(userId);

        validateGrantWithinCallerScopes(List.of(environment), embeddedRole);

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

            WorkspaceRole implicitRole = implicit.getEmbeddedRole();

            Assert.state(implicitRole != null, "An embedded membership must hold a built-in role");

            embeddedUserRepository.delete(implicit);

            for (Environment otherEnvironment : Environment.values()) {
                if (otherEnvironment != environment) {
                    embeddedUserRepository.save(EmbeddedUser.forRole(userId, implicitRole, otherEnvironment));
                }
            }
        }

        Optional<EmbeddedUser> existingEmbeddedUser = embeddedUserRepository
            .findByUserIdAndEnvironment(userId, environment.ordinal());

        EmbeddedUser savedEmbeddedUser;

        if (existingEmbeddedUser.isPresent()) {
            EmbeddedUser embeddedUser = existingEmbeddedUser.get();

            embeddedUser.assignRole(embeddedRole);

            savedEmbeddedUser = embeddedUserRepository.save(embeddedUser);
        } else {
            savedEmbeddedUser = embeddedUserRepository.save(EmbeddedUser.forRole(userId, embeddedRole, environment));
        }

        permissionService.evictEmbeddedScopeCache(userId);

        return savedEmbeddedUser;
    }

    @Override
    public EmbeddedUser updateEmbeddedUserRole(long userId, WorkspaceRole embeddedRole) {
        Assert.notNull(embeddedRole, "'embeddedRole' must not be null");

        lockEmbeddedAdmins();

        embeddedUserRepository.lockEmbeddedUsers(userId);

        EmbeddedUser embeddedUser = embeddedUserRepository.findByUserIdAndEnvironmentIsNull(userId)
            .orElseThrow(() -> missingEmbeddedWideMembership(userId));

        validateGrantWithinCallerScopes(List.of(Environment.values()), embeddedRole);

        validateEnvironmentsKeepAnAdmin(
            List.of(embeddedUser),
            embeddedRole == WorkspaceRole.ADMIN ? EnumSet.allOf(Environment.class) : Set.of());

        embeddedUser.assignRole(embeddedRole);

        EmbeddedUser savedEmbeddedUser = embeddedUserRepository.save(embeddedUser);

        permissionService.evictEmbeddedScopeCache(userId);

        return savedEmbeddedUser;
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

    private void validateGrantWithinCallerScopes(List<Environment> environments, WorkspaceRole embeddedRole) {
        Set<String> grantedScopeNames =
            permissionScopeRegistry.getScopeNames(embeddedRole, PermissionScopeSide.EMBEDDED);

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
