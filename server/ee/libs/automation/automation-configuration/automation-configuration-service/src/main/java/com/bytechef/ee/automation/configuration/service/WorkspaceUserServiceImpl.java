/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditEvent;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditPublisher;
import com.bytechef.ee.automation.configuration.domain.CustomRole;
import com.bytechef.ee.automation.configuration.domain.ResolvedRole;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.exception.WorkspaceErrorType;
import com.bytechef.ee.automation.configuration.exception.WorkspaceUserErrorType;
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.security.scope.WorkspacePermissionScope;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserInvitationService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
@SuppressFBWarnings({
    "EI2", "NM"
})
public class WorkspaceUserServiceImpl implements WorkspaceUserService {

    private final CustomRoleRepository customRoleRepository;
    private final PermissionScopeRegistry permissionScopeRegistry;
    private final PermissionService permissionService;
    private final UserInvitationService userInvitationService;
    private final WorkspaceService workspaceService;
    private final UserService userService;
    private final WorkspaceUserAuditPublisher workspaceUserAuditPublisher;
    private final WorkspaceUserRepository workspaceUserRepository;

    @SuppressFBWarnings("EI")
    public WorkspaceUserServiceImpl(
        CustomRoleRepository customRoleRepository, PermissionScopeRegistry permissionScopeRegistry,
        PermissionService permissionService, UserInvitationService userInvitationService, UserService userService,
        WorkspaceService workspaceService, WorkspaceUserAuditPublisher workspaceUserAuditPublisher,
        WorkspaceUserRepository workspaceUserRepository) {

        this.customRoleRepository = customRoleRepository;
        this.permissionScopeRegistry = permissionScopeRegistry;
        this.permissionService = permissionService;
        this.userInvitationService = userInvitationService;
        this.userService = userService;
        this.workspaceService = workspaceService;
        this.workspaceUserAuditPublisher = workspaceUserAuditPublisher;
        this.workspaceUserRepository = workspaceUserRepository;
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEveryEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE')")
    public WorkspaceUser inviteWorkspaceUser(long workspaceId, String email, WorkspaceRole workspaceRole) {
        return inviteWorkspaceUser(workspaceId, email, workspaceRole, null);
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEveryEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE')")
    public WorkspaceUser inviteWorkspaceUser(
        long workspaceId, String email, WorkspaceRole workspaceRole, Long customRoleId) {

        validateWorkspaceExists(workspaceId);
        validateRoleSelection(workspaceRole, customRoleId);
        validateGrantWithinCallerScopes(workspaceId, List.of(Environment.values()), workspaceRole, customRoleId);

        Optional<User> existingUser = userService.fetchUserByEmail(email);

        User user = existingUser.orElseGet(() -> userInvitationService.inviteUser(email, AuthorityConstants.USER));

        WorkspaceUser workspaceUser = addWorkspaceUser(user.getId(), workspaceId, workspaceRole, customRoleId);

        if (existingUser.isPresent()) {
            String workspaceName = workspaceService.getWorkspaceName(workspaceId);

            userInvitationService.notifyAddedToWorkspace(user, workspaceName);
        }

        return workspaceUser;
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEveryEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE')")
    public WorkspaceUser assignCustomRole(long userId, long workspaceId, long customRoleId) {
        CustomRole customRole = validateCustomRoleExists(customRoleId);

        validateGrantWithinCallerScopes(workspaceId, List.of(Environment.values()), null, customRoleId);

        WorkspaceUser workspaceUser = workspaceUserRepository
            .findByUserIdAndWorkspaceIdAndEnvironmentIsNull(userId, workspaceId)
            .orElseThrow(() -> missingWorkspaceWideMembership(userId, workspaceId));

        validateNotSelfRoleChangeToCustomRole(workspaceId, userId, customRole);

        if (Objects.equals(workspaceUser.getWorkspaceRole(), WorkspaceRole.ADMIN.ordinal())) {
            validateNotLastAdmin(workspaceId);
        }

        workspaceUser.assignCustomRole(customRoleId);

        WorkspaceUser savedWorkspaceUser = workspaceUserRepository.save(workspaceUser);

        permissionService.evictWorkspaceScopeCache(userId, workspaceId);

        Map<String, Object> data = new HashMap<>();

        data.put("workspaceId", String.valueOf(workspaceId));
        data.put("userId", String.valueOf(userId));
        data.put("customRoleId", String.valueOf(customRoleId));

        workspaceUserAuditPublisher.publish(WorkspaceUserAuditEvent.WORKSPACE_USER_ROLE_UPDATED, data);

        return savedWorkspaceUser;
    }

    /**
     * The error for "no membership row here". Names the real cause when the target is a tenant admin: they administer
     * every workspace through {@code isTenantAdmin()} and appear in the members view as an inherited entry, so telling
     * an operator they are "not a member" describes the row rather than the access and reads as a bug.
     */
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
            "User " + userId + " is not a member of workspace " + workspaceId,
            WorkspaceUserErrorType.NOT_MEMBER);
    }

    private ConfigurationException missingWorkspaceWideMembership(long userId, long workspaceId) {
        if (workspaceUserRepository.existsByUserIdAndWorkspaceId(userId, workspaceId)) {
            return new ConfigurationException(
                "User " + userId + " holds per-environment roles in workspace " + workspaceId +
                    " and so has no workspace-wide role to change. Change their role in each environment instead, or " +
                    "remove them from the workspace and add them back to put them on a workspace-wide role.",
                WorkspaceUserErrorType.EXPLICIT_MODE_MEMBERSHIP);
        }

        return missingMembership(userId, workspaceId);
    }

    private ConfigurationException missingEnvironmentMembership(
        long userId, long workspaceId, Environment environment) {

        if (workspaceUserRepository.existsByUserIdAndWorkspaceId(userId, workspaceId)) {
            return new ConfigurationException(
                "User " + userId + " holds no role specific to environment " + environment.name() + " of workspace "
                    + workspaceId + ". Their access there comes from their workspace-wide role; change or remove that "
                    + "instead.",
                WorkspaceUserErrorType.NO_ENVIRONMENT_ROLE);
        }

        return missingMembership(userId, workspaceId);
    }

    private CustomRole validateCustomRoleExists(long customRoleId) {
        return customRoleRepository.findById(customRoleId)
            .orElseThrow(() -> new ConfigurationException(
                "Custom role " + customRoleId + " does not exist",
                WorkspaceUserErrorType.CUSTOM_ROLE_NOT_FOUND));
    }

    private void validateWorkspaceExists(long workspaceId) {
        if (!workspaceService.workspaceExists(workspaceId)) {
            throw new ConfigurationException(
                "Workspace " + workspaceId + " does not exist", WorkspaceErrorType.WORKSPACE_NOT_FOUND);
        }
    }

    private void validateGrantWithinCallerScopes(
        long workspaceId, List<Environment> environments, WorkspaceRole workspaceRole, Long customRoleId) {

        Set<String> grantedScopeNames = customRoleId == null
            ? permissionScopeRegistry.getScopeNames(workspaceRole)
            : validateCustomRoleExists(customRoleId).getScopeNames();

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

    private void validateRoleSelection(WorkspaceRole workspaceRole, Long customRoleId) {
        boolean hasBuiltInRole = workspaceRole != null;
        boolean hasCustomRole = customRoleId != null;

        if (hasBuiltInRole == hasCustomRole) {
            throw new ConfigurationException(
                "Exactly one of a built-in role or a custom role must be supplied",
                WorkspaceUserErrorType.ROLE_SELECTION_INVALID);
        }

        if (hasCustomRole) {
            validateCustomRoleExists(customRoleId);
        }
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEveryEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE')")
    public WorkspaceUser addWorkspaceUser(long userId, long workspaceId, WorkspaceRole workspaceRole) {
        return addWorkspaceUser(userId, workspaceId, workspaceRole, null);
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEveryEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE')")
    public WorkspaceUser addWorkspaceUser(
        long userId, long workspaceId, WorkspaceRole workspaceRole, Long customRoleId) {

        validateWorkspaceExists(workspaceId);
        validateRoleSelection(workspaceRole, customRoleId);
        validateGrantWithinCallerScopes(workspaceId, List.of(Environment.values()), workspaceRole, customRoleId);

        boolean hasCustomRole = customRoleId != null;

        if (workspaceUserRepository.existsByUserIdAndWorkspaceId(userId, workspaceId)) {
            throw new ConfigurationException(
                "User " + userId + " is already a member of workspace " + workspaceId,
                WorkspaceUserErrorType.ALREADY_MEMBER);
        }

        WorkspaceUser savedWorkspaceUser = workspaceUserRepository.save(
            hasCustomRole
                ? WorkspaceUser.forCustomRole(userId, workspaceId, customRoleId)
                : WorkspaceUser.forRole(userId, workspaceId, workspaceRole));

        permissionService.evictWorkspaceScopeCache(userId, workspaceId);

        Map<String, Object> data = new HashMap<>();

        data.put("workspaceId", String.valueOf(workspaceId));
        data.put("userId", String.valueOf(userId));
        data.put("role", hasCustomRole ? "customRole:" + customRoleId : workspaceRole.name());

        workspaceUserAuditPublisher.publish(WorkspaceUserAuditEvent.WORKSPACE_USER_ADDED, data);

        return savedWorkspaceUser;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResolvedRole> fetchRole(long userId, long workspaceId, Environment environment) {
        Optional<WorkspaceUser> environmentWorkspaceUser = workspaceUserRepository
            .findByUserIdAndWorkspaceIdAndEnvironment(userId, workspaceId, environment.ordinal());

        if (environmentWorkspaceUser.isPresent()) {
            return environmentWorkspaceUser.flatMap(WorkspaceUserServiceImpl::toResolvedRole);
        }

        return workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(userId, workspaceId)
            .flatMap(WorkspaceUserServiceImpl::toResolvedRole);
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE', #environment)")
    public WorkspaceUser setEnvironmentRole(
        long userId, long workspaceId, Environment environment, WorkspaceRole workspaceRole, Long customRoleId) {

        Assert.notNull(environment, "'environment' must not be null");

        validateWorkspaceExists(workspaceId);
        validateRoleSelection(workspaceRole, customRoleId);
        validateGrantWithinCallerScopes(workspaceId, List.of(environment), workspaceRole, customRoleId);

        boolean hasCustomRole = customRoleId != null;

        Optional<WorkspaceUser> implicitWorkspaceUser = workspaceUserRepository
            .findByUserIdAndWorkspaceIdAndEnvironmentIsNull(userId, workspaceId);

        if (implicitWorkspaceUser.isPresent()
            && !permissionService.hasWorkspaceScopeInEveryEnvironment(workspaceId, "WORKSPACE_MEMBER_MANAGE")) {

            throw new AccessDeniedException(
                "Replacing a workspace-wide role with an environment role requires WORKSPACE_MEMBER_MANAGE in every "
                    + "environment");
        }

        validateNotSelfPromotionInEnvironment(userId, workspaceId, environment, workspaceRole, customRoleId);

        Set<Environment> lostAdminEnvironments = resolveLostAdminEnvironments(
            userId, workspaceId, environment, workspaceRole);

        validateNotSelfDemotionFromEnvironments(workspaceId, userId, lostAdminEnvironments);
        validateEnvironmentsKeepAnAdmin(workspaceId, lostAdminEnvironments);

        implicitWorkspaceUser.ifPresent(workspaceUserRepository::delete);

        Optional<WorkspaceUser> existingWorkspaceUser = workspaceUserRepository
            .findByUserIdAndWorkspaceIdAndEnvironment(userId, workspaceId, environment.ordinal());

        WorkspaceUser savedWorkspaceUser;

        if (existingWorkspaceUser.isPresent()) {
            WorkspaceUser workspaceUser = existingWorkspaceUser.get();

            if (hasCustomRole) {
                workspaceUser.assignCustomRole(customRoleId);
            } else {
                workspaceUser.setWorkspaceRole(workspaceRole.ordinal());
            }

            savedWorkspaceUser = workspaceUserRepository.save(workspaceUser);
        } else {
            savedWorkspaceUser = workspaceUserRepository.save(
                hasCustomRole
                    ? WorkspaceUser.forCustomRole(userId, workspaceId, customRoleId, environment)
                    : WorkspaceUser.forRole(userId, workspaceId, workspaceRole, environment));
        }

        permissionService.evictWorkspaceScopeCache(userId, workspaceId);

        publishEnvironmentRoleAudit(
            WorkspaceUserAuditEvent.WORKSPACE_USER_ENVIRONMENT_ROLE_UPDATED, userId, workspaceId, environment,
            workspaceRole);

        return savedWorkspaceUser;
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE', #environment)")
    public void removeEnvironmentRole(long userId, long workspaceId, Environment environment) {
        Assert.notNull(environment, "'environment' must not be null");

        Optional<WorkspaceUser> environmentWorkspaceUser = workspaceUserRepository
            .findByUserIdAndWorkspaceIdAndEnvironment(userId, workspaceId, environment.ordinal());

        if (environmentWorkspaceUser.isEmpty()) {
            throw missingEnvironmentMembership(userId, workspaceId, environment);
        }

        List<WorkspaceUser> memberRows = workspaceUserRepository.findAllByUserIdAndWorkspaceId(userId, workspaceId);

        if (memberRows.size() <= 1) {
            if (!permissionService.hasWorkspaceScopeInEveryEnvironment(workspaceId, "WORKSPACE_MEMBER_MANAGE")) {
                throw new AccessDeniedException(
                    "Widening a member to every environment requires WORKSPACE_MEMBER_MANAGE in every environment");
            }

            WorkspaceUser lastEnvironmentWorkspaceUser = environmentWorkspaceUser.get();

            WorkspaceUser workspaceWideWorkspaceUser = lastEnvironmentWorkspaceUser.getCustomRoleId() == null
                ? WorkspaceUser.forRole(userId, workspaceId, fetchBuiltInRole(lastEnvironmentWorkspaceUser))
                : WorkspaceUser.forCustomRole(userId, workspaceId, lastEnvironmentWorkspaceUser.getCustomRoleId());

            workspaceUserRepository.delete(lastEnvironmentWorkspaceUser);

            workspaceUserRepository.save(workspaceWideWorkspaceUser);
        } else {
            Set<Environment> lostAdminEnvironments = isAdmin(environmentWorkspaceUser.get())
                ? Set.of(environment)
                : Set.of();

            validateNotSelfDemotionFromEnvironments(workspaceId, userId, lostAdminEnvironments);
            validateEnvironmentsKeepAnAdmin(workspaceId, lostAdminEnvironments);

            workspaceUserRepository.delete(environmentWorkspaceUser.get());
        }

        permissionService.evictWorkspaceScopeCache(userId, workspaceId);

        publishEnvironmentRoleAudit(
            WorkspaceUserAuditEvent.WORKSPACE_USER_ENVIRONMENT_ROLE_REMOVED, userId, workspaceId, environment, null);
    }

    private void publishEnvironmentRoleAudit(
        WorkspaceUserAuditEvent workspaceUserAuditEvent, long userId, long workspaceId, Environment environment,
        WorkspaceRole workspaceRole) {

        Map<String, Object> data = new HashMap<>();

        data.put("workspaceId", String.valueOf(workspaceId));
        data.put("userId", String.valueOf(userId));
        data.put("environment", environment.name());

        if (workspaceRole != null) {
            data.put("role", workspaceRole.name());
        }

        workspaceUserAuditPublisher.publish(workspaceUserAuditEvent, data);
    }

    private static Optional<ResolvedRole> toResolvedRole(WorkspaceUser workspaceUser) {
        Long customRoleId = workspaceUser.getCustomRoleId();

        if (customRoleId != null) {
            return Optional.of(new ResolvedRole(null, customRoleId));
        }

        return Optional.ofNullable(fetchBuiltInRole(workspaceUser))
            .map(workspaceRole -> new ResolvedRole(workspaceRole, null));
    }

    @Override
    @Transactional(readOnly = true)
    public long countByCustomRoleId(long customRoleId) {
        return workspaceUserRepository.countByCustomRoleId(customRoleId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isWorkspaceMember(long userId, long workspaceId) {
        return workspaceUserRepository.existsByUserIdAndWorkspaceId(userId, workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceUser> getUserWorkspaceUsers(long userId) {
        return workspaceUserRepository.findAllByUserId(userId);
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_VIEW')")
    @Transactional(readOnly = true)
    public List<WorkspaceUser> getWorkspaceWorkspaceUsers(long workspaceId) {
        return workspaceUserRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEveryEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE')")
    public boolean removeWorkspaceUser(long userId, long workspaceId) {
        List<WorkspaceUser> workspaceUsers = workspaceUserRepository.findAllByUserIdAndWorkspaceId(userId, workspaceId);

        if (workspaceUsers.isEmpty()) {
            throw missingMembership(userId, workspaceId);
        }

        boolean admin = workspaceUsers.stream()
            .anyMatch(WorkspaceUserServiceImpl::isAdmin);

        if (admin) {
            validateNotLastAdmin(workspaceId);
        }

        workspaceUserRepository.deleteByUserIdAndWorkspaceId(userId, workspaceId);

        permissionService.evictWorkspaceScopeCache(userId, workspaceId);

        Map<String, Object> data = new HashMap<>();

        data.put("workspaceId", String.valueOf(workspaceId));
        data.put("userId", String.valueOf(userId));

        workspaceUserAuditPublisher.publish(WorkspaceUserAuditEvent.WORKSPACE_USER_REMOVED, data);

        return true;
    }

    @Override
    @PreAuthorize("hasWorkspaceScopeInEveryEnvironment(#workspaceId, 'WORKSPACE_MEMBER_MANAGE')")
    public WorkspaceUser updateWorkspaceUserRole(long userId, long workspaceId, WorkspaceRole workspaceRole) {
        WorkspaceUser workspaceUser = workspaceUserRepository
            .findByUserIdAndWorkspaceIdAndEnvironmentIsNull(userId, workspaceId)
            .orElseThrow(() -> missingWorkspaceWideMembership(userId, workspaceId));

        validateGrantWithinCallerScopes(workspaceId, List.of(Environment.values()), workspaceRole, null);
        validateNotSelfDemotion(workspaceId, userId, workspaceUser, workspaceRole);
        validateNotSelfPromotion(workspaceId, userId, workspaceUser, workspaceRole);

        if (Objects.equals(workspaceUser.getWorkspaceRole(), WorkspaceRole.ADMIN.ordinal())
            && workspaceRole != WorkspaceRole.ADMIN) {

            validateNotLastAdmin(workspaceId);
        }

        workspaceUser.setWorkspaceRole(workspaceRole.ordinal());

        WorkspaceUser savedWorkspaceUser = workspaceUserRepository.save(workspaceUser);

        permissionService.evictWorkspaceScopeCache(userId, workspaceId);

        Map<String, Object> data = new HashMap<>();

        data.put("workspaceId", String.valueOf(workspaceId));
        data.put("userId", String.valueOf(userId));
        data.put("role", workspaceRole.name());

        workspaceUserAuditPublisher.publish(WorkspaceUserAuditEvent.WORKSPACE_USER_ROLE_UPDATED, data);

        return savedWorkspaceUser;
    }

    private void validateNotSelfDemotion(
        long workspaceId, long userId, WorkspaceUser workspaceUser, WorkspaceRole targetRole) {

        if (permissionService.isTenantAdmin() || !permissionService.isCurrentUser(userId)) {
            return;
        }

        if (Objects.equals(workspaceUser.getWorkspaceRole(), WorkspaceRole.ADMIN.ordinal())
            && targetRole != WorkspaceRole.ADMIN) {

            throw new ConfigurationException(
                "Cannot demote your own role on workspace " + workspaceId
                    + ". Ask another admin to change your role, or leave the workspace instead.",
                WorkspaceUserErrorType.SELF_DEMOTION_FORBIDDEN);
        }
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

    private WorkspaceRole fetchEnvironmentBuiltInRole(long userId, long workspaceId, Environment environment) {
        Optional<WorkspaceUser> environmentWorkspaceUser = workspaceUserRepository
            .findByUserIdAndWorkspaceIdAndEnvironment(userId, workspaceId, environment.ordinal());

        if (environmentWorkspaceUser.isPresent()) {
            return fetchBuiltInRole(environmentWorkspaceUser.get());
        }

        return workspaceUserRepository.findByUserIdAndWorkspaceIdAndEnvironmentIsNull(userId, workspaceId)
            .map(WorkspaceUserServiceImpl::fetchBuiltInRole)
            .orElse(null);
    }

    private void validateNotSelfRoleChangeToCustomRole(long workspaceId, long userId, CustomRole customRole) {
        if (permissionService.isTenantAdmin() || !permissionService.isCurrentUser(userId)) {
            return;
        }

        Set<String> scopeNames = customRole.getScopeNames();

        if (!scopeNames.contains(WorkspacePermissionScope.WORKSPACE_MEMBER_MANAGE.name())) {
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

    private Set<Environment> resolveLostAdminEnvironments(
        long userId, long workspaceId, Environment environment, WorkspaceRole workspaceRole) {

        Set<Environment> currentAdminEnvironments = resolveAdminEnvironments(userId, workspaceId);

        if (currentAdminEnvironments.isEmpty()) {
            return Set.of();
        }

        Set<Environment> lostAdminEnvironments = new LinkedHashSet<>(currentAdminEnvironments);

        if (workspaceRole == WorkspaceRole.ADMIN) {
            lostAdminEnvironments.remove(environment);
        }

        return lostAdminEnvironments;
    }

    private Set<Environment> resolveAdminEnvironments(long userId, long workspaceId) {
        List<WorkspaceUser> workspaceUsers = workspaceUserRepository.findAllByUserIdAndWorkspaceId(userId, workspaceId);

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

    private static boolean isAdmin(WorkspaceUser workspaceUser) {
        return Objects.equals(workspaceUser.getWorkspaceRole(), WorkspaceRole.ADMIN.ordinal());
    }

    private void validateEnvironmentsKeepAnAdmin(long workspaceId, Set<Environment> lostAdminEnvironments) {
        if (lostAdminEnvironments.isEmpty() || permissionService.isTenantAdmin()) {
            return;
        }

        for (Environment environment : lostAdminEnvironments) {
            long adminCount = workspaceUserRepository.countAdminsForEnvironment(
                workspaceId, WorkspaceRole.ADMIN.ordinal(), environment.ordinal());

            if (adminCount <= 1) {
                throw new ConfigurationException(
                    "Cannot leave environment " + environment.name() + " of workspace " + workspaceId
                        + " without an admin. Grant another member the ADMIN role there first.",
                    WorkspaceUserErrorType.LAST_ADMIN_PROTECTED);
            }
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

    private void validateNotLastAdmin(long workspaceId) {
        long adminCount = workspaceUserRepository.countDistinctMembersWithWorkspaceRole(
            workspaceId, WorkspaceRole.ADMIN.ordinal());

        if (adminCount <= 1) {
            throw new ConfigurationException(
                "Cannot remove or demote the last admin of workspace " + workspaceId +
                    ". At least one admin must remain.",
                WorkspaceUserErrorType.LAST_ADMIN_PROTECTED);
        }
    }
}
