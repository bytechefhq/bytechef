/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditEvents;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditMapper;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.exception.WorkspaceUserErrorType;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
@SuppressFBWarnings({
    "EI2", "NM"
})
public class WorkspaceUserServiceImpl implements WorkspaceUserService {

    private final WorkspaceUserRepository workspaceUserRepository;

    @SuppressFBWarnings("EI")
    public WorkspaceUserServiceImpl(WorkspaceUserRepository workspaceUserRepository) {
        this.workspaceUserRepository = workspaceUserRepository;
    }

    @Override
    @Audited(event = WorkspaceUserAuditEvents.WORKSPACE_USER_ADDED, mapper = WorkspaceUserAuditMapper.class)
    public WorkspaceUser addWorkspaceUser(long userId, long workspaceId, WorkspaceRole workspaceRole) {
        return addWorkspaceUser(userId, workspaceId, workspaceRole, null);
    }

    @Override
    public WorkspaceUser addWorkspaceUser(
        long userId, long workspaceId, WorkspaceRole workspaceRole, Long customRoleId) {

        workspaceUserRepository.lockWorkspace(workspaceId);

        if (workspaceUserRepository.existsByUserIdAndWorkspaceId(userId, workspaceId)) {
            throw new ConfigurationException(
                "User " + userId + " is already a member of workspace " + workspaceId,
                WorkspaceUserErrorType.ALREADY_MEMBER);
        }

        return workspaceUserRepository.save(
            customRoleId == null
                ? WorkspaceUser.forRole(userId, workspaceId, workspaceRole)
                : WorkspaceUser.forCustomRole(userId, workspaceId, customRoleId));
    }

    @Override
    public WorkspaceUser assignCustomRole(long userId, long workspaceId, long customRoleId) {
        WorkspaceUser workspaceUser = getWorkspaceWideWorkspaceUser(userId, workspaceId);

        if (isAdmin(workspaceUser)) {
            validateNotLastAdmin(workspaceId, EnumSet.allOf(Environment.class));
        }

        workspaceUser.assignCustomRole(customRoleId);

        return workspaceUserRepository.save(workspaceUser);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByCustomRoleId(long customRoleId) {
        return workspaceUserRepository.countByCustomRoleId(customRoleId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countEnvironmentAdmins(long workspaceId, Environment environment) {
        return workspaceUserRepository.countAdminsForEnvironment(
            workspaceId, WorkspaceRole.ADMIN.ordinal(), environment.ordinal());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkspaceUser> fetchEnvironmentWorkspaceUser(
        long userId, long workspaceId, Environment environment) {

        return workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironment(
            userId, workspaceId, environment.ordinal());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkspaceUser> fetchWorkspaceWideWorkspaceUser(long userId, long workspaceId) {
        return workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(userId, workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceUser getEnvironmentWorkspaceUser(long userId, long workspaceId, Environment environment) {
        Optional<WorkspaceUser> environmentWorkspaceUser = fetchEnvironmentWorkspaceUser(
            userId, workspaceId, environment);

        if (environmentWorkspaceUser.isPresent()) {
            return environmentWorkspaceUser.get();
        }

        if (workspaceUserRepository.existsByUserIdAndWorkspaceId(userId, workspaceId)) {
            throw new ConfigurationException(
                "User " + userId + " holds no role specific to environment " + environment.name() + " of workspace "
                    + workspaceId + ". Their access there comes from their workspace-wide role; change or remove that "
                    + "instead.",
                WorkspaceUserErrorType.NO_ENVIRONMENT_ROLE);
        }

        throw missingMembership(userId, workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Environment> getLostAdminEnvironments(
        long userId, long workspaceId, Environment environment, WorkspaceRole workspaceRole) {

        List<WorkspaceUser> workspaceUsers = workspaceUserRepository.findAllByUserIdAndWorkspaceId(userId, workspaceId);

        Set<Environment> currentAdminEnvironments = resolveAdminEnvironments(workspaceUsers);

        if (currentAdminEnvironments.isEmpty()) {
            return Set.of();
        }

        Set<Environment> keptAdminEnvironments = new LinkedHashSet<>();

        for (WorkspaceUser workspaceUser : workspaceUsers) {
            Environment workspaceUserEnvironment = workspaceUser.getEnvironment();

            if (workspaceUserEnvironment != null && workspaceUserEnvironment != environment && isAdmin(workspaceUser)) {
                keptAdminEnvironments.add(workspaceUserEnvironment);
            }
        }

        if (workspaceRole == WorkspaceRole.ADMIN) {
            keptAdminEnvironments.add(environment);
        }

        Set<Environment> lostAdminEnvironments = new LinkedHashSet<>(currentAdminEnvironments);

        lostAdminEnvironments.removeAll(keptAdminEnvironments);

        return lostAdminEnvironments;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceUser> getUserWorkspaceUsers(long userId) {
        return workspaceUserRepository.findAllByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceUser getWorkspaceWideWorkspaceUser(long userId, long workspaceId) {
        Optional<WorkspaceUser> workspaceUser = fetchWorkspaceWideWorkspaceUser(userId, workspaceId);

        if (workspaceUser.isPresent()) {
            return workspaceUser.get();
        }

        if (workspaceUserRepository.existsByUserIdAndWorkspaceId(userId, workspaceId)) {
            throw new ConfigurationException(
                "User " + userId + " holds per-environment roles in workspace " + workspaceId +
                    " and so has no workspace-wide role to change. Change their role in each environment instead, or " +
                    "remove them from the workspace and add them back to put them on a workspace-wide role.",
                WorkspaceUserErrorType.EXPLICIT_MODE_MEMBERSHIP);
        }

        throw missingMembership(userId, workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceUser> getWorkspaceUsers(long userId, long workspaceId) {
        return workspaceUserRepository.findAllByUserIdAndWorkspaceId(userId, workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceUser> getWorkspaceWorkspaceUsers(long workspaceId) {
        return workspaceUserRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasEnvironmentWorkspaceUsers(long userId, long workspaceId) {
        return workspaceUserRepository.existsByUserIdAndWorkspaceIdAndEnvironmentIsNotNull(userId, workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isWorkspaceMember(long userId, long workspaceId) {
        return workspaceUserRepository.existsByUserIdAndWorkspaceId(userId, workspaceId);
    }

    @Override
    public void lockWorkspace(long workspaceId) {
        workspaceUserRepository.lockWorkspace(workspaceId);
    }

    @Override
    public void removeEnvironmentRole(long userId, long workspaceId, Environment environment) {
        Assert.notNull(environment, "'environment' must not be null");

        WorkspaceUser environmentWorkspaceUser = getEnvironmentWorkspaceUser(userId, workspaceId, environment);

        List<WorkspaceUser> memberWorkspaceUsers = workspaceUserRepository.findAllByUserIdAndWorkspaceId(
            userId, workspaceId);

        workspaceUserRepository.delete(environmentWorkspaceUser);

        if (memberWorkspaceUsers.size() <= 1) {
            Long customRoleId = environmentWorkspaceUser.getCustomRoleId();

            workspaceUserRepository.save(
                customRoleId == null
                    ? WorkspaceUser.forRole(userId, workspaceId, fetchBuiltInRole(environmentWorkspaceUser))
                    : WorkspaceUser.forCustomRole(userId, workspaceId, customRoleId));
        }
    }

    @Override
    public boolean removeWorkspaceUser(long userId, long workspaceId) {
        List<WorkspaceUser> workspaceUsers = workspaceUserRepository.findAllByUserIdAndWorkspaceId(userId, workspaceId);

        if (workspaceUsers.isEmpty()) {
            throw missingMembership(userId, workspaceId);
        }

        validateNotLastAdmin(workspaceId, resolveAdminEnvironments(workspaceUsers));

        workspaceUserRepository.deleteByUserIdAndWorkspaceId(userId, workspaceId);

        return true;
    }

    @Override
    public WorkspaceUser setEnvironmentRole(
        long userId, long workspaceId, Environment environment, WorkspaceRole workspaceRole, Long customRoleId) {

        Assert.notNull(environment, "'environment' must not be null");

        Optional<WorkspaceUser> implicitWorkspaceUser = fetchWorkspaceWideWorkspaceUser(userId, workspaceId);

        implicitWorkspaceUser.ifPresent(workspaceUserRepository::delete);

        boolean hasCustomRole = customRoleId != null;

        Optional<WorkspaceUser> existingWorkspaceUser = fetchEnvironmentWorkspaceUser(
            userId, workspaceId, environment);

        if (existingWorkspaceUser.isPresent()) {
            WorkspaceUser workspaceUser = existingWorkspaceUser.get();

            if (hasCustomRole) {
                workspaceUser.assignCustomRole(customRoleId);
            } else {
                workspaceUser.setWorkspaceRole(workspaceRole.ordinal());
            }

            return workspaceUserRepository.save(workspaceUser);
        }

        return workspaceUserRepository.save(
            hasCustomRole
                ? WorkspaceUser.forCustomRole(userId, workspaceId, customRoleId, environment)
                : WorkspaceUser.forRole(userId, workspaceId, workspaceRole, environment));
    }

    @Override
    public WorkspaceUser updateWorkspaceUserRole(long userId, long workspaceId, WorkspaceRole workspaceRole) {
        WorkspaceUser workspaceUser = getWorkspaceWideWorkspaceUser(userId, workspaceId);

        if (isAdmin(workspaceUser) && workspaceRole != WorkspaceRole.ADMIN) {
            validateNotLastAdmin(workspaceId, EnumSet.allOf(Environment.class));
        }

        workspaceUser.setWorkspaceRole(workspaceRole.ordinal());

        return workspaceUserRepository.save(workspaceUser);
    }

    private static WorkspaceRole fetchBuiltInRole(WorkspaceUser workspaceUser) {
        Integer workspaceRoleOrdinal = workspaceUser.getWorkspaceRole();

        if (workspaceRoleOrdinal == null) {
            return null;
        }

        WorkspaceRole[] workspaceRoles = WorkspaceRole.values();

        if (workspaceRoleOrdinal < 0 || workspaceRoleOrdinal >= workspaceRoles.length) {
            return null;
        }

        return workspaceRoles[workspaceRoleOrdinal];
    }

    private static boolean isAdmin(WorkspaceUser workspaceUser) {
        return Objects.equals(workspaceUser.getWorkspaceRole(), WorkspaceRole.ADMIN.ordinal());
    }

    private static ConfigurationException missingMembership(long userId, long workspaceId) {
        return new ConfigurationException(
            "User " + userId + " is not a member of workspace " + workspaceId, WorkspaceUserErrorType.NOT_MEMBER);
    }

    private static Set<Environment> resolveAdminEnvironments(List<WorkspaceUser> workspaceUsers) {
        Set<Environment> adminEnvironments = new LinkedHashSet<>();

        for (WorkspaceUser workspaceUser : workspaceUsers) {
            if (!isAdmin(workspaceUser)) {
                continue;
            }

            Environment environment = workspaceUser.getEnvironment();

            if (environment == null) {
                return new LinkedHashSet<>(List.of(Environment.values()));
            }

            adminEnvironments.add(environment);
        }

        return adminEnvironments;
    }

    private void validateNotLastAdmin(long workspaceId, Set<Environment> adminEnvironments) {
        for (Environment environment : adminEnvironments) {
            long adminCount = countEnvironmentAdmins(workspaceId, environment);

            if (adminCount <= 1) {
                throw new ConfigurationException(
                    "Cannot remove or demote the last admin of environment " + environment.name() + " of workspace "
                        + workspaceId + ". At least one admin must remain there.",
                    WorkspaceUserErrorType.LAST_ADMIN_PROTECTED);
            }
        }
    }
}
