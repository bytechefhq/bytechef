/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.PermissionService.UserWorkspacePair;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditEvent;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditPublisher;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.exception.WorkspaceErrorType;
import com.bytechef.ee.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
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
@SuppressFBWarnings("NM")
public class WorkspaceServiceImpl implements WorkspaceService {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceServiceImpl.class);

    private final PermissionService permissionService;
    private final UserService userService;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceUserAuditPublisher workspaceUserAuditPublisher;
    private final WorkspaceUserRepository workspaceUserRepository;

    @SuppressFBWarnings("EI")
    public WorkspaceServiceImpl(
        PermissionService permissionService, UserService userService,
        WorkspaceRepository workspaceRepository, WorkspaceUserAuditPublisher workspaceUserAuditPublisher,
        WorkspaceUserRepository workspaceUserRepository) {

        this.permissionService = permissionService;
        this.userService = userService;
        this.workspaceRepository = workspaceRepository;
        this.workspaceUserAuditPublisher = workspaceUserAuditPublisher;
        this.workspaceUserRepository = workspaceUserRepository;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public Workspace create(Workspace workspace) {
        Assert.notNull(workspace, "'workspace' must not be null");
        Assert.isTrue(workspace.getId() == null, "'workspace.id' must be null");

        Workspace savedWorkspace = workspaceRepository.save(workspace);

        SecurityUtils.fetchCurrentUserLogin()
            .ifPresentOrElse(
                login -> workspaceUserRepository.save(
                    WorkspaceUser.forRole(
                        userService.getUser(login)
                            .getId(),
                        savedWorkspace.getId(), WorkspaceRole.ADMIN)),
                () -> log.error(
                    "ORPHAN WORKSPACE WARNING: Created workspace id={} without an authenticated creator. "
                        + "No workspace_user ADMIN row was seeded. Only tenant admins will be able to access "
                        + "this workspace until membership is repaired.",
                    savedWorkspace.getId()));

        return savedWorkspace;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public void delete(long id) {
        if (id == Workspace.DEFAULT_WORKSPACE_ID) {
            throw new ConfigurationException(
                "Default workspace cannot be deleted", WorkspaceErrorType.DEFAULT_WORKSPACE_NOT_DELETABLE);
        }

        List<WorkspaceUser> members = workspaceUserRepository.findAllByWorkspaceId(id);
        List<UserWorkspacePair> cacheEvictionTargets = new ArrayList<>();

        for (WorkspaceUser member : members) {
            cacheEvictionTargets.add(new UserWorkspacePair(member.getUserId(), id));

            workspaceUserRepository.deleteByUserIdAndWorkspaceId(member.getUserId(), id);

            Map<String, Object> data = new HashMap<>();

            data.put("workspaceId", String.valueOf(id));
            data.put("userId", String.valueOf(member.getUserId()));

            workspaceUserAuditPublisher.publish(WorkspaceUserAuditEvent.WORKSPACE_USER_REMOVED, data);
        }

        workspaceRepository.deleteById(id);

        permissionService.evictWorkspaceScopeCaches(cacheEvictionTargets);
    }

    @Override
    public Workspace getProjectWorkspace(long projectId) {
        return workspaceRepository.findByProjectId(projectId);
    }

    @Override
    public List<Workspace> getWorkspaces() {
        return workspaceRepository.findAll();
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'Workspace', 'WORKSPACE_VIEW')")
    public Workspace getWorkspace(long id) {
        return OptionalUtils.get(workspaceRepository.findById(id));
    }

    @Override
    public String getWorkspaceName(long id) {
        Workspace workspace = OptionalUtils.get(workspaceRepository.findById(id));

        return workspace.getName();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean workspaceExists(long id) {
        return workspaceRepository.existsById(id);
    }

    @Override
    @PreAuthorize("hasPermission(#workspace.id, 'Workspace', 'WORKSPACE_MANAGE')")
    public Workspace update(Workspace workspace) {
        Assert.notNull(workspace, "'workspace' must not be null");
        Assert.isTrue(workspace.getId() != null, "'workspace.id' must not be null");

        if (workspace.getId() == Workspace.DEFAULT_WORKSPACE_ID) {
            throw new ConfigurationException(
                "Default workspace cannot be updated", WorkspaceErrorType.DEFAULT_WORKSPACE_NOT_CHANGEABLE);
        }

        Workspace curWorkspace = OptionalUtils.get(workspaceRepository.findById(workspace.getId()));

        curWorkspace.setDescription(workspace.getDescription());
        curWorkspace.setName(workspace.getName());
        curWorkspace.setVersion(workspace.getVersion());

        return workspaceRepository.save(curWorkspace);
    }
}
