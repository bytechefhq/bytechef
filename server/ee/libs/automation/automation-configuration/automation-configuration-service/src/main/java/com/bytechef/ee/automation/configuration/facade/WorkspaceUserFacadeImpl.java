/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.automation.configuration.event.WorkspaceUserRemovedEvent;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditEvents;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditMapper;
import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.exception.WorkspaceErrorType;
import com.bytechef.ee.automation.configuration.exception.WorkspaceUserErrorType;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.security.scope.WorkspacePermissionScope;
import com.bytechef.ee.automation.configuration.service.CustomRoleService;
import com.bytechef.ee.automation.configuration.service.PermissionScopeRegistry;
import com.bytechef.ee.automation.configuration.service.WorkspaceService;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.facade.UserInvitationFacade;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
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
public class WorkspaceUserFacadeImpl implements WorkspaceUserFacade {

    private static final String WORKSPACE_MEMBER_MANAGE = WorkspacePermissionScope.WORKSPACE_MEMBER_MANAGE.name();

    private final ApplicationEventPublisher applicationEventPublisher;
    private final CustomRoleService customRoleService;
    private final PermissionScopeRegistry permissionScopeRegistry;
    private final PermissionService permissionService;
    private final UserInvitationFacade userInvitationFacade;
    private final UserService userService;
    private final WorkspaceService workspaceService;
    private final WorkspaceUserService workspaceUserService;

    @SuppressFBWarnings("EI")
    public WorkspaceUserFacadeImpl(
        ApplicationEventPublisher applicationEventPublisher, CustomRoleService customRoleService,
        PermissionScopeRegistry permissionScopeRegistry, PermissionService permissionService,
        UserInvitationFacade userInvitationFacade, UserService userService, WorkspaceService workspaceService,
        WorkspaceUserService workspaceUserService) {

        this.applicationEventPublisher = applicationEventPublisher;
        this.customRoleService = customRoleService;
        this.permissionScopeRegistry = permissionScopeRegistry;
        this.permissionService = permissionService;
        this.userInvitationFacade = userInvitationFacade;
        this.userService = userService;
        this.workspaceService = workspaceService;
        this.workspaceUserService = workspaceUserService;
    }

    @Override
    @Audited(event = WorkspaceUserAuditEvents.WORKSPACE_USER_ADDED, mapper = WorkspaceUserAuditMapper.class)
    @PreAuthorize("hasWorkspaceScopeInEveryEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE')")
    public WorkspaceUser addWorkspaceUser(
        long userId, long workspaceId, WorkspaceRole workspaceRole, Long customRoleId) {

        validateWorkspaceExists(workspaceId);
        validateRoleSelection(workspaceRole, customRoleId);
        validateGrantWithinCallerScopes(workspaceId, List.of(Environment.values()), workspaceRole, customRoleId);

        return doAddWorkspaceUser(userId, workspaceId, workspaceRole, customRoleId);
    }

    @Override
    @Audited(event = WorkspaceUserAuditEvents.WORKSPACE_USER_ROLE_UPDATED, mapper = WorkspaceUserAuditMapper.class)
    @PreAuthorize("hasWorkspaceScopeInEveryEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE')")
    public WorkspaceUser assignCustomRole(long userId, long workspaceId, long customRoleId) {
        CustomRole customRole = getCustomRole(customRoleId);

        validateGrantWithinCallerScopes(
            workspaceId, List.of(Environment.values()), customRole.getScopeNames());

        workspaceUserService.lockWorkspace(workspaceId);

        getWorkspaceWideWorkspaceUser(userId, workspaceId);

        validateNotSelfRoleChangeToCustomRole(workspaceId, userId, customRole);

        WorkspaceUser savedWorkspaceUser = workspaceUserService.assignCustomRole(userId, workspaceId, customRoleId);

        permissionService.evictWorkspaceScopeCache(userId, workspaceId);

        return savedWorkspaceUser;
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_VIEW')")
    @Transactional(readOnly = true)
    public List<WorkspaceUser> getWorkspaceWorkspaceUsers(long workspaceId) {
        return workspaceUserService.getWorkspaceWorkspaceUsers(workspaceId);
    }

    @Override
    @Audited(event = WorkspaceUserAuditEvents.WORKSPACE_USER_ADDED, mapper = WorkspaceUserAuditMapper.class)
    @PreAuthorize("hasWorkspaceScopeInEveryEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE')")
    public WorkspaceUser inviteWorkspaceUser(
        long workspaceId, String email, WorkspaceRole workspaceRole, Long customRoleId) {

        validateWorkspaceExists(workspaceId);
        validateRoleSelection(workspaceRole, customRoleId);
        validateGrantWithinCallerScopes(workspaceId, List.of(Environment.values()), workspaceRole, customRoleId);

        Optional<User> existingUser = userService.fetchUserByEmail(email);

        User user = existingUser.orElseGet(() -> userInvitationFacade.inviteUser(email, AuthorityConstants.USER));

        WorkspaceUser workspaceUser = doAddWorkspaceUser(user.getId(), workspaceId, workspaceRole, customRoleId);

        if (existingUser.isPresent()) {
            String workspaceName = workspaceService.getWorkspaceName(workspaceId);

            userInvitationFacade.notifyAddedToWorkspace(user, workspaceName);
        }

        return workspaceUser;
    }

    @Override
    @Audited(
        event = WorkspaceUserAuditEvents.WORKSPACE_USER_ENVIRONMENT_ROLE_REMOVED,
        mapper = WorkspaceUserAuditMapper.class)
    @PreAuthorize("hasWorkspaceScopeInEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE', #environment)")
    public void removeEnvironmentRole(long userId, long workspaceId, Environment environment) {
        Assert.notNull(environment, "'environment' must not be null");

        workspaceUserService.lockWorkspace(workspaceId);

        WorkspaceUser environmentWorkspaceUser = getEnvironmentWorkspaceUser(userId, workspaceId, environment);

        List<WorkspaceUser> memberWorkspaceUsers = workspaceUserService.getWorkspaceUsers(userId, workspaceId);

        if (memberWorkspaceUsers.size() <= 1) {
            if (!permissionService.hasWorkspaceScopeInEveryEnvironment(workspaceId, WORKSPACE_MEMBER_MANAGE)) {
                throw new AccessDeniedException(
                    "Widening a member's last environment role to a workspace-wide role requires "
                        + "WORKSPACE_MEMBER_MANAGE in every environment");
            }

            validateGrantWithinCallerScopes(
                workspaceId, List.of(Environment.values()), fetchBuiltInRole(environmentWorkspaceUser),
                environmentWorkspaceUser.getCustomRoleId());
        } else {
            Set<Environment> lostAdminEnvironments = isAdmin(environmentWorkspaceUser)
                ? Set.of(environment)
                : Set.of();

            validateNotSelfDemotionFromEnvironments(workspaceId, userId, lostAdminEnvironments);
            validateEnvironmentsKeepAnAdmin(workspaceId, lostAdminEnvironments);
        }

        workspaceUserService.removeEnvironmentRole(userId, workspaceId, environment);

        permissionService.evictWorkspaceScopeCache(userId, workspaceId);
    }

    @Override
    @Audited(event = WorkspaceUserAuditEvents.WORKSPACE_USER_REMOVED, mapper = WorkspaceUserAuditMapper.class)
    @PreAuthorize("hasWorkspaceScopeInEveryEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE')")
    public boolean removeWorkspaceUser(long userId, long workspaceId) {
        workspaceUserService.lockWorkspace(workspaceId);

        List<WorkspaceUser> workspaceUsers = workspaceUserService.getWorkspaceUsers(userId, workspaceId);

        if (workspaceUsers.isEmpty()) {
            throw missingMembership(userId, workspaceId);
        }

        boolean removed = workspaceUserService.removeWorkspaceUser(userId, workspaceId);

        permissionService.evictWorkspaceScopeCache(userId, workspaceId);

        User user = userService.getUser(userId);

        applicationEventPublisher.publishEvent(new WorkspaceUserRemovedEvent(workspaceId, userId, user.getLogin()));

        return removed;
    }

    @Override
    @Audited(
        event = WorkspaceUserAuditEvents.WORKSPACE_USER_ENVIRONMENT_ROLE_UPDATED,
        mapper = WorkspaceUserAuditMapper.class)
    @PreAuthorize("hasWorkspaceScopeInEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE', #environment)")
    public WorkspaceUser setEnvironmentRole(
        long userId, long workspaceId, Environment environment, WorkspaceRole workspaceRole, Long customRoleId) {

        Assert.notNull(environment, "'environment' must not be null");

        validateWorkspaceExists(workspaceId);
        validateRoleSelection(workspaceRole, customRoleId);
        validateGrantWithinCallerScopes(workspaceId, List.of(environment), workspaceRole, customRoleId);

        workspaceUserService.lockWorkspace(workspaceId);

        validateMembership(userId, workspaceId);

        Optional<WorkspaceUser> implicitWorkspaceUser = workspaceUserService.fetchWorkspaceWideWorkspaceUser(
            userId, workspaceId);

        if (implicitWorkspaceUser.isPresent()
            && !permissionService.hasWorkspaceScopeInEveryEnvironment(workspaceId, WORKSPACE_MEMBER_MANAGE)) {

            throw new AccessDeniedException(
                "Replacing a workspace-wide role with an environment role requires WORKSPACE_MEMBER_MANAGE in every "
                    + "environment");
        }

        validateNotSelfPromotionInEnvironment(userId, workspaceId, environment, workspaceRole, customRoleId);

        Set<Environment> lostAdminEnvironments = workspaceUserService.getLostAdminEnvironments(
            userId, workspaceId, environment, workspaceRole);

        validateNotSelfDemotionFromEnvironments(workspaceId, userId, lostAdminEnvironments);
        validateEnvironmentsKeepAnAdmin(workspaceId, lostAdminEnvironments);

        WorkspaceUser savedWorkspaceUser = workspaceUserService.setEnvironmentRole(
            userId, workspaceId, environment, workspaceRole, customRoleId);

        permissionService.evictWorkspaceScopeCache(userId, workspaceId);

        return savedWorkspaceUser;
    }

    @Override
    @Audited(event = WorkspaceUserAuditEvents.WORKSPACE_USER_ROLE_UPDATED, mapper = WorkspaceUserAuditMapper.class)
    @PreAuthorize("hasWorkspaceScopeInEveryEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE')")
    public WorkspaceUser updateWorkspaceUserRole(long userId, long workspaceId, WorkspaceRole workspaceRole) {
        workspaceUserService.lockWorkspace(workspaceId);

        WorkspaceUser workspaceUser = getWorkspaceWideWorkspaceUser(userId, workspaceId);

        validateGrantWithinCallerScopes(workspaceId, List.of(Environment.values()), workspaceRole, null);
        validateNotSelfDemotion(workspaceId, userId, workspaceUser, workspaceRole);
        validateNotSelfPromotion(workspaceId, userId, workspaceUser, workspaceRole);

        WorkspaceUser savedWorkspaceUser = workspaceUserService.updateWorkspaceUserRole(
            userId, workspaceId, workspaceRole);

        permissionService.evictWorkspaceScopeCache(userId, workspaceId);

        return savedWorkspaceUser;
    }

    private WorkspaceUser doAddWorkspaceUser(
        long userId, long workspaceId, WorkspaceRole workspaceRole, Long customRoleId) {

        WorkspaceUser savedWorkspaceUser = workspaceUserService.addWorkspaceUser(
            userId, workspaceId, workspaceRole, customRoleId);

        permissionService.evictWorkspaceScopeCache(userId, workspaceId);

        return savedWorkspaceUser;
    }

    private WorkspaceRole fetchEnvironmentBuiltInRole(long userId, long workspaceId, Environment environment) {
        Optional<WorkspaceUser> environmentWorkspaceUser = workspaceUserService.fetchEnvironmentWorkspaceUser(
            userId, workspaceId, environment);

        if (environmentWorkspaceUser.isPresent()) {
            return fetchBuiltInRole(environmentWorkspaceUser.get());
        }

        if (workspaceUserService.hasEnvironmentWorkspaceUsers(userId, workspaceId)) {
            return null;
        }

        return workspaceUserService.fetchWorkspaceWideWorkspaceUser(userId, workspaceId)
            .map(WorkspaceUserFacadeImpl::fetchBuiltInRole)
            .orElse(null);
    }

    private CustomRole getCustomRole(long customRoleId) {
        return customRoleService.fetchCustomRole(customRoleId)
            .orElseThrow(() -> new ConfigurationException(
                "Custom role " + customRoleId + " does not exist",
                WorkspaceUserErrorType.CUSTOM_ROLE_NOT_FOUND));
    }

    private WorkspaceUser getEnvironmentWorkspaceUser(long userId, long workspaceId, Environment environment) {
        Optional<WorkspaceUser> environmentWorkspaceUser = workspaceUserService.fetchEnvironmentWorkspaceUser(
            userId, workspaceId, environment);

        if (environmentWorkspaceUser.isPresent()) {
            return environmentWorkspaceUser.get();
        }

        validateMembership(userId, workspaceId);

        return workspaceUserService.getEnvironmentWorkspaceUser(userId, workspaceId, environment);
    }

    private WorkspaceUser getWorkspaceWideWorkspaceUser(long userId, long workspaceId) {
        Optional<WorkspaceUser> workspaceUser = workspaceUserService.fetchWorkspaceWideWorkspaceUser(
            userId, workspaceId);

        if (workspaceUser.isPresent()) {
            return workspaceUser.get();
        }

        validateMembership(userId, workspaceId);

        return workspaceUserService.getWorkspaceWideWorkspaceUser(userId, workspaceId);
    }

    private ConfigurationException missingMembership(long userId, long workspaceId) {
        boolean tenantAdmin = userService.getUsersByAuthorityName(AuthorityConstants.ADMIN)
            .stream()
            .anyMatch(user -> Objects.equals(user.getId(), userId));

        if (tenantAdmin) {
            return new ConfigurationException(
                "User " + userId + " administers workspace " + workspaceId +
                    " as a tenant admin, which is not a workspace membership and cannot be changed here. " +
                    "Revoke their tenant admin role instead.",
                WorkspaceUserErrorType.INHERITED_MEMBERSHIP);
        }

        return new ConfigurationException(
            "User " + userId + " is not a member of workspace " + workspaceId, WorkspaceUserErrorType.NOT_MEMBER);
    }

    private void validateEnvironmentsKeepAnAdmin(long workspaceId, Set<Environment> lostAdminEnvironments) {
        if (lostAdminEnvironments.isEmpty() || permissionService.isTenantAdmin()) {
            return;
        }

        for (Environment environment : lostAdminEnvironments) {
            long adminCount = workspaceUserService.countEnvironmentAdmins(workspaceId, environment);

            if (adminCount <= 1) {
                throw new ConfigurationException(
                    "Cannot leave environment " + environment.name() + " of workspace " + workspaceId
                        + " without an admin. Grant another member the ADMIN role there first.",
                    WorkspaceUserErrorType.LAST_ADMIN_PROTECTED);
            }
        }
    }

    private void validateGrantWithinCallerScopes(
        long workspaceId, List<Environment> environments, WorkspaceRole workspaceRole, Long customRoleId) {

        Set<String> grantedScopeNames = customRoleId == null
            ? permissionScopeRegistry.getScopeNames(workspaceRole)
            : getCustomRole(customRoleId).getScopeNames();

        validateGrantWithinCallerScopes(workspaceId, environments, grantedScopeNames);
    }

    private void validateGrantWithinCallerScopes(
        long workspaceId, List<Environment> environments, Set<String> grantedScopeNames) {

        for (Environment environment : environments) {
            List<String> missingScopeNames = grantedScopeNames.stream()
                .filter(scopeName -> !permissionService.hasWorkspaceScope(workspaceId, scopeName, environment))
                .sorted()
                .toList();

            if (!missingScopeNames.isEmpty()) {
                throw new AccessDeniedException(
                    "Cannot grant a role in environment " + environment.name() + " of workspace " + workspaceId
                        + " that carries scopes you do not hold there: " + missingScopeNames);
            }
        }
    }

    private void validateMembership(long userId, long workspaceId) {
        if (!workspaceUserService.isWorkspaceMember(userId, workspaceId)) {
            throw missingMembership(userId, workspaceId);
        }
    }

    private void validateNotSelfDemotion(
        long workspaceId, long userId, WorkspaceUser workspaceUser, WorkspaceRole targetRole) {

        if (permissionService.isTenantAdmin() || !permissionService.isCurrentUser(userId)) {
            return;
        }

        if (isAdmin(workspaceUser) && targetRole != WorkspaceRole.ADMIN) {
            throw new ConfigurationException(
                "Cannot demote your own role on workspace " + workspaceId
                    + ". Ask another admin to change your role, or leave the workspace instead.",
                WorkspaceUserErrorType.SELF_DEMOTION_FORBIDDEN);
        }
    }

    private void validateNotSelfDemotionFromEnvironments(
        long workspaceId, long userId, Set<Environment> lostAdminEnvironments) {

        if (lostAdminEnvironments.isEmpty() || permissionService.isTenantAdmin()
            || !permissionService.isCurrentUser(userId)) {

            return;
        }

        throw new ConfigurationException(
            "Cannot withdraw your own ADMIN role from " + lostAdminEnvironments + " on workspace " + workspaceId
                + ". Ask another admin to make the change.",
            WorkspaceUserErrorType.SELF_DEMOTION_FORBIDDEN);
    }

    private void validateNotSelfPromotion(
        long workspaceId, long userId, WorkspaceUser workspaceUser, WorkspaceRole targetRole) {

        if (permissionService.isTenantAdmin() || !permissionService.isCurrentUser(userId)) {
            return;
        }

        WorkspaceRole currentRole = fetchBuiltInRole(workspaceUser);

        if (currentRole == null) {
            throw new ConfigurationException(
                "Cannot change your own role on workspace " + workspaceId
                    + " while you hold a custom role. Ask another admin to change it.",
                WorkspaceUserErrorType.SELF_PROMOTION_FORBIDDEN);
        }

        if (!currentRole.hasAtLeast(targetRole)) {
            throw new ConfigurationException(
                "Cannot raise your own role on workspace " + workspaceId + " from " + currentRole.name() + " to "
                    + targetRole.name() + ". Ask another admin to grant it.",
                WorkspaceUserErrorType.SELF_PROMOTION_FORBIDDEN);
        }
    }

    private void validateNotSelfPromotionInEnvironment(
        long userId, long workspaceId, Environment environment, WorkspaceRole workspaceRole, Long customRoleId) {

        if (permissionService.isTenantAdmin() || !permissionService.isCurrentUser(userId)) {
            return;
        }

        if (customRoleId != null) {
            throw new ConfigurationException(
                "Cannot move your own role in environment " + environment.name() + " of workspace " + workspaceId
                    + " onto a custom role. Ask another admin to make the change.",
                WorkspaceUserErrorType.SELF_PROMOTION_FORBIDDEN);
        }

        WorkspaceRole currentRole = fetchEnvironmentBuiltInRole(userId, workspaceId, environment);

        if (currentRole == null || !currentRole.hasAtLeast(workspaceRole)) {
            throw new ConfigurationException(
                "Cannot raise your own role in environment " + environment.name() + " of workspace " + workspaceId
                    + " to " + workspaceRole.name() + ". Ask another admin to grant it.",
                WorkspaceUserErrorType.SELF_PROMOTION_FORBIDDEN);
        }
    }

    private void validateNotSelfRoleChangeToCustomRole(long workspaceId, long userId, CustomRole customRole) {
        if (permissionService.isTenantAdmin() || !permissionService.isCurrentUser(userId)) {
            return;
        }

        Set<String> scopeNames = customRole.getScopeNames();

        if (!scopeNames.contains(WORKSPACE_MEMBER_MANAGE)) {
            throw new ConfigurationException(
                "Cannot move your own membership on workspace " + workspaceId + " onto custom role "
                    + customRole.getName() + ", which does not grant WORKSPACE_MEMBER_MANAGE. Ask another admin to "
                    + "change your role, or leave the workspace instead.",
                WorkspaceUserErrorType.SELF_DEMOTION_FORBIDDEN);
        }

        throw new ConfigurationException(
            "Cannot move your own membership on workspace " + workspaceId + " onto custom role " + customRole.getName()
                + ". A custom role's scopes cannot be shown to be no more than you already hold. Ask another admin to "
                + "make the change.",
            WorkspaceUserErrorType.SELF_PROMOTION_FORBIDDEN);
    }

    private void validateRoleSelection(WorkspaceRole workspaceRole, Long customRoleId) {
        boolean hasBuiltInRole = workspaceRole != null;
        boolean hasCustomRole = customRoleId != null;

        if (hasBuiltInRole == hasCustomRole) {
            throw new ConfigurationException(
                "Exactly one of a built-in role or a custom role must be supplied",
                WorkspaceUserErrorType.ROLE_SELECTION_INVALID);
        }

        if (hasCustomRole) {
            getCustomRole(customRoleId);
        }
    }

    private void validateWorkspaceExists(long workspaceId) {
        if (!workspaceService.workspaceExists(workspaceId)) {
            throw new ConfigurationException(
                "Workspace " + workspaceId + " does not exist", WorkspaceErrorType.WORKSPACE_NOT_FOUND);
        }
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
}
