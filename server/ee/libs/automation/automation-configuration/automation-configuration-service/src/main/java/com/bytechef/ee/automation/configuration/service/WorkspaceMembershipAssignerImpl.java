/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.PermissionService.UserWorkspacePair;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditEvent;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditPublisher;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.exception.WorkspaceErrorType;
import com.bytechef.ee.automation.configuration.exception.WorkspaceUserErrorType;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.service.WorkspaceMembershipAssigner;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnEEVersion
@Transactional
public class WorkspaceMembershipAssignerImpl implements WorkspaceMembershipAssigner {

    private final PermissionService permissionService;
    private final WorkspaceService workspaceService;
    private final WorkspaceUserAuditPublisher workspaceUserAuditPublisher;
    private final WorkspaceUserRepository workspaceUserRepository;
    private final WorkspaceUserService workspaceUserService;

    @SuppressFBWarnings("EI")
    public WorkspaceMembershipAssignerImpl(
        PermissionService permissionService, WorkspaceService workspaceService,
        WorkspaceUserAuditPublisher workspaceUserAuditPublisher, WorkspaceUserRepository workspaceUserRepository,
        WorkspaceUserService workspaceUserService) {

        this.permissionService = permissionService;
        this.workspaceService = workspaceService;
        this.workspaceUserAuditPublisher = workspaceUserAuditPublisher;
        this.workspaceUserRepository = workspaceUserRepository;
        this.workspaceUserService = workspaceUserService;
    }

    @Override
    public void assign(long userId, List<WorkspaceAssignment> assignments) {
        for (WorkspaceAssignment assignment : assignments) {
            workspaceUserService.addWorkspaceUser(userId, assignment.workspaceId(), parseRole(assignment.roleName()));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateAssignments(List<WorkspaceAssignment> assignments) {
        for (WorkspaceAssignment assignment : assignments) {
            parseRole(assignment.roleName());

            long workspaceId = assignment.workspaceId();

            if (!workspaceService.workspaceExists(workspaceId)) {
                throw new ConfigurationException(
                    "Workspace " + workspaceId + " does not exist", WorkspaceErrorType.WORKSPACE_NOT_FOUND);
            }
        }
    }

    @Override
    public void removeMemberships(long userId) {
        List<WorkspaceUser> workspaceUsers = workspaceUserRepository.findAllByUserId(userId);

        workspaceUserRepository.deleteByUserId(userId);

        Set<Long> workspaceIds = new LinkedHashSet<>();

        for (WorkspaceUser workspaceUser : workspaceUsers) {
            workspaceIds.add(workspaceUser.getWorkspaceId());
        }

        List<UserWorkspacePair> cacheEvictionTargets = new ArrayList<>();

        for (Long workspaceId : workspaceIds) {
            cacheEvictionTargets.add(new UserWorkspacePair(userId, workspaceId));

            Map<String, Object> data = new HashMap<>();

            data.put("workspaceId", String.valueOf(workspaceId));
            data.put("userId", String.valueOf(userId));

            workspaceUserAuditPublisher.publish(WorkspaceUserAuditEvent.WORKSPACE_USER_REMOVED, data);
        }

        permissionService.evictWorkspaceScopeCaches(cacheEvictionTargets);
    }

    private static WorkspaceRole parseRole(String roleName) {
        try {
            return WorkspaceRole.valueOf(roleName);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw new ConfigurationException(
                "Unknown workspace role '" + roleName + "'", WorkspaceUserErrorType.INVALID_WORKSPACE_ROLE);
        }
    }
}
