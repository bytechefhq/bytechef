/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.automation.configuration.service.PermissionService.UserProjectPair;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.exception.WorkspaceErrorType;
import com.bytechef.ee.automation.configuration.repository.ProjectUserRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
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

    private static final Logger logger = LoggerFactory.getLogger(WorkspaceServiceImpl.class);

    private final PermissionService permissionService;
    private final ProjectUserRepository projectUserRepository;
    private final UserService userService;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceUserRepository workspaceUserRepository;

    @SuppressFBWarnings("EI")
    public WorkspaceServiceImpl(
        PermissionService permissionService, ProjectUserRepository projectUserRepository, UserService userService,
        WorkspaceRepository workspaceRepository, WorkspaceUserRepository workspaceUserRepository) {

        this.permissionService = permissionService;
        this.projectUserRepository = projectUserRepository;
        this.userService = userService;
        this.workspaceRepository = workspaceRepository;
        this.workspaceUserRepository = workspaceUserRepository;
    }

    @Override
    @PreAuthorize("@permissionService.isTenantAdmin()")
    public Workspace create(Workspace workspace) {
        Assert.notNull(workspace, "'workspace' must not be null");
        Assert.isTrue(workspace.getId() == null, "'workspace.id' must be null");

        Workspace savedWorkspace = workspaceRepository.save(workspace);

        // Auto-admin assignment requires an authenticated creator. Without a SecurityContext the workspace ends up
        // orphaned (no workspace_user ADMIN row) and is unreachable through @PreAuthorize for non-tenant-admins.
        // Tenant admins still satisfy hasWorkspaceRole via the short-circuit so the workspace is recoverable, but
        // log loudly so this surfaces in operations dashboards rather than silently rotting.
        SecurityUtils.fetchCurrentUserLogin()
            .ifPresentOrElse(
                login -> workspaceUserRepository.save(
                    WorkspaceUser.forRole(
                        userService.getUser(login)
                            .getId(),
                        savedWorkspace.getId(), WorkspaceRole.ADMIN)),
                () -> logger.error(
                    "ORPHAN WORKSPACE WARNING: Created workspace id={} without an authenticated creator. "
                        + "No workspace_user ADMIN row was seeded. Only tenant admins will be able to access "
                        + "this workspace until membership is repaired.",
                    savedWorkspace.getId()));

        return savedWorkspace;
    }

    @Override
    @PreAuthorize("@permissionService.isTenantAdmin()")
    public void delete(long id) {
        if (id == Workspace.DEFAULT_WORKSPACE_ID) {
            throw new ConfigurationException(
                "Default workspace cannot be deleted", WorkspaceErrorType.DEFAULT_WORKSPACE_NOT_DELETABLE);
        }

        // The workspace_user.workspace_id FK has no ON DELETE CASCADE, and there is no FK at all from project_user to
        // workspace_user. A naive workspaceRepository.deleteById(id) therefore either raises a PSQLException on the
        // first non-empty workspace, or \u2014 if workspace_user happens to be empty but project_user is not \u2014
        // cascades project rows without running the per-member cache eviction, leaving stale ALLOW decisions in the
        // permission cache for up to the TTL. The per-member iteration below mirrors what removeWorkspaceUser does,
        // minus the last-admin and orphan guards (both moot when the workspace itself is going away).
        //
        // Snapshot of (userId, projectId) pairs happens BEFORE any delete \u2014 once cascade fires, the join
        // project_user → project disappears and we cannot reconstruct which cache entries to evict. The @PreAuthorize
        // on this method already routes the delete through PermissionAuditAspect, so the workspace-wide blast radius
        // is audited as a single DENIED/ALLOWED event before any row is touched.
        List<WorkspaceUser> members = workspaceUserRepository.findAllByWorkspaceId(id);
        List<UserProjectPair> cacheEvictionTargets = new ArrayList<>();

        for (WorkspaceUser member : members) {
            List<Long> projectIds = projectUserRepository.findProjectIdsByUserIdAndWorkspaceId(
                member.getUserId(), id);

            for (Long projectId : projectIds) {
                cacheEvictionTargets.add(new UserProjectPair(member.getUserId(), projectId));
            }
        }

        for (WorkspaceUser member : members) {
            // project_user first — there is no DB-level FK from project_user to workspace_user to cascade for us.
            projectUserRepository.deleteByUserIdAndWorkspaceId(member.getUserId(), id);

            workspaceUserRepository.deleteByUserIdAndWorkspaceId(member.getUserId(), id);
        }

        workspaceRepository.deleteById(id);

        // Single afterCommit handler for all pairs — see PermissionService#evictProjectScopeCaches. Workspace-wide
        // deletes can produce O(members × projects) pairs and the per-pair TransactionSynchronization overhead is
        // why this is batched rather than looped through evictProjectScopeCache.
        permissionService.evictProjectScopeCaches(cacheEvictionTargets);
    }

    /**
     * Returns the workspace owning the given project. Trusted-caller method (no {@code @PreAuthorize}) — callers must
     * already hold a project scope on {@code projectId} to have reached this code path. External entry points (REST,
     * GraphQL) must enforce their own authorization.
     */
    @Override
    public Workspace getProjectWorkspace(long projectId) {
        return workspaceRepository.findByProjectId(projectId);
    }

    /**
     * Returns every workspace in the tenant. Trusted-caller method (no {@code @PreAuthorize}) — the standard entry
     * point is {@code WorkspaceFacadeImpl.getUserWorkspaces} which filters by membership for non-admins. External
     * REST/GraphQL controllers must enforce their own authorization.
     */
    @Override
    public List<Workspace> getWorkspaces() {
        return workspaceRepository.findAll();
    }

    @Override
    @PreAuthorize("@permissionService.hasWorkspaceRole(#id, 'VIEWER')")
    public Workspace getWorkspace(long id) {
        return OptionalUtils.get(workspaceRepository.findById(id));
    }

    @Override
    @PreAuthorize("@permissionService.hasWorkspaceRole(#workspace.id, 'ADMIN')")
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
