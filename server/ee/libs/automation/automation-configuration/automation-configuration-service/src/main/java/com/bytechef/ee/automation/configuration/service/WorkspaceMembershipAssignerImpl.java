/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.event.WorkspaceUserRemovedEvent;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.PermissionService.UserWorkspacePair;
import com.bytechef.ee.automation.configuration.audit.WorkspaceAuditEvents;
import com.bytechef.ee.automation.configuration.audit.WorkspaceAuditMapper;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.exception.WorkspaceErrorType;
import com.bytechef.ee.automation.configuration.exception.WorkspaceUserErrorType;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.platform.user.service.WorkspaceMembershipAssigner;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
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

    private final ApplicationEventPublisher applicationEventPublisher;
    private final EmbeddedUserService embeddedUserService;
    private final PermissionService permissionService;
    private final UserService userService;
    private final WorkspaceService workspaceService;
    private final WorkspaceUserRepository workspaceUserRepository;
    private final WorkspaceUserService workspaceUserService;

    @SuppressFBWarnings("EI")
    public WorkspaceMembershipAssignerImpl(
        ApplicationEventPublisher applicationEventPublisher, EmbeddedUserService embeddedUserService,
        PermissionService permissionService, UserService userService, WorkspaceService workspaceService,
        WorkspaceUserRepository workspaceUserRepository, WorkspaceUserService workspaceUserService) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.embeddedUserService = embeddedUserService;
        this.permissionService = permissionService;
        this.userService = userService;
        this.workspaceService = workspaceService;
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
    @Audited(event = WorkspaceAuditEvents.WORKSPACE_MEMBERSHIPS_REMOVED, mapper = WorkspaceAuditMapper.class)
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
        }

        permissionService.evictWorkspaceScopeCaches(cacheEvictionTargets);

        embeddedUserService.deleteEmbeddedMemberships(userId);

        if (workspaceIds.isEmpty()) {
            return;
        }

        User user = userService.getUser(userId);

        for (Long workspaceId : workspaceIds) {
            applicationEventPublisher.publishEvent(new WorkspaceUserRemovedEvent(workspaceId, userId, user.getLogin()));
        }
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
