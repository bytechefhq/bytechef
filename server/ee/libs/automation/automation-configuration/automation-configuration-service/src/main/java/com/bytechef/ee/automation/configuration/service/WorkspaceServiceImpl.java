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
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.exception.WorkspaceErrorType;
import com.bytechef.ee.automation.configuration.repository.WorkspaceRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.exception.QuotaLimitExceededException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.plan.provider.PlanLimitsProvider;
import com.bytechef.platform.ratelimit.PlanLimitRejectionCounter;
import com.bytechef.platform.security.util.SecurityUtils;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
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
    private final ObjectProvider<PlanLimitRejectionCounter> planLimitRejectionCounterObjectProvider;
    private final ObjectProvider<PlanLimitsProvider> planLimitsProviderObjectProvider;
    private final UserService userService;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceUserRepository workspaceUserRepository;

    @SuppressFBWarnings("EI")
    public WorkspaceServiceImpl(
        PermissionService permissionService,
        ObjectProvider<PlanLimitRejectionCounter> planLimitRejectionCounterObjectProvider,
        ObjectProvider<PlanLimitsProvider> planLimitsProviderObjectProvider,
        UserService userService, WorkspaceRepository workspaceRepository,
        WorkspaceUserRepository workspaceUserRepository) {

        this.permissionService = permissionService;
        this.planLimitRejectionCounterObjectProvider = planLimitRejectionCounterObjectProvider;
        this.planLimitsProviderObjectProvider = planLimitsProviderObjectProvider;
        this.userService = userService;
        this.workspaceRepository = workspaceRepository;
        this.workspaceUserRepository = workspaceUserRepository;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public Workspace create(Workspace workspace) {
        Assert.notNull(workspace, "'workspace' must not be null");
        Assert.isTrue(workspace.getId() == null, "'workspace.id' must be null");

        enforceWorkspaceQuota();

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

        // The workspace_user.workspace_id FK has no ON DELETE CASCADE. A naive workspaceRepository.deleteById(id)
        // would raise a PSQLException on the first non-empty workspace, and would also skip the per-member cache
        // eviction, leaving stale ALLOW decisions in the permission cache for up to the TTL. The per-member iteration
        // below mirrors what removeWorkspaceUser does, minus the last-admin guard (moot when the workspace itself is
        // going away).
        //
        // The (userId, workspaceId) eviction pairs are collected before the deletes. The @PreAuthorize on this method
        // already routes the delete through PermissionAuditAspect, so the workspace-wide blast radius is audited as a
        // single DENIED/ALLOWED event before any row is touched.
        List<WorkspaceUser> members = workspaceUserRepository.findAllByWorkspaceId(id);
        List<UserWorkspacePair> cacheEvictionTargets = new ArrayList<>();

        for (WorkspaceUser member : members) {
            cacheEvictionTargets.add(new UserWorkspacePair(member.getUserId(), id));

            workspaceUserRepository.deleteByUserIdAndWorkspaceId(member.getUserId(), id);
        }

        workspaceRepository.deleteById(id);

        // Single afterCommit handler for all pairs — see PermissionService#evictWorkspaceScopeCaches. Workspace-wide
        // deletes produce one pair per member and the per-pair TransactionSynchronization overhead is why this is
        // batched rather than looped through evictWorkspaceScopeCache.
        permissionService.evictWorkspaceScopeCaches(cacheEvictionTargets);
    }

    /**
     * Rejects the creation when the tenant already holds as many workspaces as its plan's {@code maxWorkspaces} allows.
     * A null limit (or no {@link PlanLimitsProvider} bean) means unlimited — the pre-plan behavior.
     */
    private void enforceWorkspaceQuota() {
        PlanLimitsProvider planLimitsProvider = planLimitsProviderObjectProvider.getIfAvailable();

        if (planLimitsProvider == null) {
            return;
        }

        Integer maxWorkspaces = planLimitsProvider.getPlanLimits(TenantContext.getCurrentTenantId())
            .maxWorkspaces();

        if (maxWorkspaces == null) {
            return;
        }

        long workspaceCount = workspaceRepository.count();

        if (workspaceCount >= maxWorkspaces) {
            countQuotaRejection();

            throw new QuotaLimitExceededException(
                "Workspace quota exceeded: the plan allows at most %d workspace(s)".formatted(maxWorkspaces));
        }
    }

    private void countQuotaRejection() {
        PlanLimitRejectionCounter planLimitRejectionCounter = planLimitRejectionCounterObjectProvider.getIfAvailable();

        if (planLimitRejectionCounter != null) {
            planLimitRejectionCounter.increment("workspace");
        }
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
    @PreAuthorize("hasPermission(#id, 'Workspace', 'WORKSPACE_VIEW')")
    public Workspace getWorkspace(long id) {
        return OptionalUtils.get(workspaceRepository.findById(id));
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
