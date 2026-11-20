/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.exception.WorkspaceUserErrorType;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
    public WorkspaceUser addWorkspaceUser(long userId, long workspaceId, WorkspaceRole workspaceRole) {
        Optional<WorkspaceUser> existing = workspaceUserRepository.findByUserIdAndWorkspaceId(userId, workspaceId);

        if (existing.isPresent()) {
            throw new ConfigurationException(
                "User " + userId + " is already a member of workspace " + workspaceId,
                WorkspaceUserErrorType.ALREADY_MEMBER);
        }

        return workspaceUserRepository.save(WorkspaceUser.forRole(userId, workspaceId, workspaceRole));
    }

    @Override
    @Transactional(readOnly = true)
    public long countByCustomRoleId(long customRoleId) {
        return workspaceUserRepository.countByCustomRoleId(customRoleId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkspaceUser> fetchWorkspaceUser(long userId, long workspaceId) {
        return workspaceUserRepository.findByUserIdAndWorkspaceId(userId, workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceUser> getUserWorkspaceUsers(long userId) {
        return workspaceUserRepository.findAllByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceUser> getWorkspaceWorkspaceUsers(long workspaceId) {
        return workspaceUserRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    public boolean removeWorkspaceUser(long userId, long workspaceId) {
        WorkspaceUser workspaceUser = workspaceUserRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
            .orElseThrow(() -> new ConfigurationException(
                "User " + userId + " is not a member of workspace " + workspaceId,
                WorkspaceUserErrorType.NOT_MEMBER));

        if (Objects.equals(workspaceUser.getWorkspaceRole(), WorkspaceRole.ADMIN.ordinal())) {
            validateNotLastAdmin(workspaceId);
        }

        workspaceUserRepository.deleteByUserIdAndWorkspaceId(userId, workspaceId);

        return true;
    }

    @Override
    public WorkspaceUser updateWorkspaceUserRole(long userId, long workspaceId, WorkspaceRole workspaceRole) {
        WorkspaceUser workspaceUser = workspaceUserRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
            .orElseThrow(() -> new ConfigurationException(
                "User " + userId + " is not a member of workspace " + workspaceId,
                WorkspaceUserErrorType.NOT_MEMBER));

        if (Objects.equals(workspaceUser.getWorkspaceRole(), WorkspaceRole.ADMIN.ordinal())
            && workspaceRole != WorkspaceRole.ADMIN) {

            validateNotLastAdmin(workspaceId);
        }

        workspaceUser.setWorkspaceRole(workspaceRole.ordinal());

        return workspaceUserRepository.save(workspaceUser);
    }

    private void validateNotLastAdmin(long workspaceId) {
        long adminCount = workspaceUserRepository.countByWorkspaceIdAndWorkspaceRole(
            workspaceId, WorkspaceRole.ADMIN.ordinal());

        if (adminCount <= 1) {
            throw new ConfigurationException(
                "Cannot remove or demote the last admin of workspace " + workspaceId
                    + ". At least one admin must remain.",
                WorkspaceUserErrorType.LAST_ADMIN_PROTECTED);
        }
    }
}
