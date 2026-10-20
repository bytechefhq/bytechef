/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.domain.EmbeddedUser;
import com.bytechef.ee.automation.configuration.exception.WorkspaceUserErrorType;
import com.bytechef.ee.automation.configuration.repository.EmbeddedUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.PermissionScopeSide;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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

        Optional<EmbeddedUser> environmentEmbeddedUser = embeddedUserRepository
            .findByUserIdAndEnvironment(userId, environment.ordinal());

        if (environmentEmbeddedUser.isEmpty()) {
            throw missingEnvironmentMembership(userId, environment);
        }

        EmbeddedUser removedEmbeddedUser = environmentEmbeddedUser.get();

        List<EmbeddedUser> memberRows = embeddedUserRepository.findAllByUserId(userId);

        if (memberRows.size() <= 1) {
            WorkspaceRole embeddedRole = removedEmbeddedUser.getEmbeddedRole();

            Assert.state(embeddedRole != null, "An embedded membership must hold a built-in role");

            validateGrantWithinCallerScopes(List.of(Environment.values()), embeddedRole);

            embeddedUserRepository.delete(removedEmbeddedUser);

            embeddedUserRepository.save(EmbeddedUser.forRole(userId, embeddedRole));
        } else {
            embeddedUserRepository.delete(removedEmbeddedUser);
        }

        permissionService.evictEmbeddedScopeCache(userId);
    }

    @Override
    public boolean removeEmbeddedUser(long userId) {
        boolean existed = embeddedUserRepository.existsByUserId(userId);

        embeddedUserRepository.deleteByUserId(userId);

        permissionService.evictEmbeddedScopeCache(userId);

        return existed;
    }

    @Override
    public EmbeddedUser setEnvironmentRole(long userId, Environment environment, WorkspaceRole embeddedRole) {
        Assert.notNull(environment, "'environment' must not be null");
        Assert.notNull(embeddedRole, "'embeddedRole' must not be null");

        validateGrantWithinCallerScopes(List.of(environment), embeddedRole);

        if (!embeddedUserRepository.existsByUserId(userId)) {
            throw missingMembership(userId);
        }

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

        EmbeddedUser embeddedUser = embeddedUserRepository.findByUserIdAndEnvironmentIsNull(userId)
            .orElseThrow(() -> missingEmbeddedWideMembership(userId));

        validateGrantWithinCallerScopes(List.of(Environment.values()), embeddedRole);

        embeddedUser.assignRole(embeddedRole);

        EmbeddedUser savedEmbeddedUser = embeddedUserRepository.save(embeddedUser);

        permissionService.evictEmbeddedScopeCache(userId);

        return savedEmbeddedUser;
    }

    private RuntimeException missingEmbeddedWideMembership(long userId) {
        if (embeddedUserRepository.existsByUserId(userId)) {
            return new IllegalStateException(
                "User " + userId + " holds per-environment embedded roles and so has no embedded-wide role to "
                    + "change. Change their role in each environment instead.");
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
