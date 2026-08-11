/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.automation.configuration.domain.Workspace;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditEvent;
import com.bytechef.ee.automation.configuration.audit.WorkspaceUserAuditPublisher;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.exception.WorkspaceUserErrorType;
import com.bytechef.ee.automation.configuration.repository.CustomRoleRepository;
import com.bytechef.ee.automation.configuration.repository.WorkspaceUserRepository;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserInvitationService;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
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

    private final CustomRoleRepository customRoleRepository;
    private final PermissionService permissionService;
    private final UserInvitationService userInvitationService;
    private final WorkspaceService workspaceService;
    private final UserService userService;
    private final WorkspaceUserAuditPublisher workspaceUserAuditPublisher;
    private final WorkspaceUserRepository workspaceUserRepository;

    @SuppressFBWarnings("EI")
    public WorkspaceUserServiceImpl(
        CustomRoleRepository customRoleRepository, PermissionService permissionService,
        UserInvitationService userInvitationService, UserService userService,
        WorkspaceService workspaceService, WorkspaceUserAuditPublisher workspaceUserAuditPublisher,
        WorkspaceUserRepository workspaceUserRepository) {

        this.customRoleRepository = customRoleRepository;
        this.permissionService = permissionService;
        this.userInvitationService = userInvitationService;
        this.userService = userService;
        this.workspaceService = workspaceService;
        this.workspaceUserAuditPublisher = workspaceUserAuditPublisher;
        this.workspaceUserRepository = workspaceUserRepository;
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_MEMBER_MANAGE')")
    public WorkspaceUser inviteWorkspaceUser(long workspaceId, String email, WorkspaceRole workspaceRole) {
        return inviteWorkspaceUser(workspaceId, email, workspaceRole, null);
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_MEMBER_MANAGE')")
    public WorkspaceUser inviteWorkspaceUser(
        long workspaceId, String email, WorkspaceRole workspaceRole, Long customRoleId) {

        // An address that already has an account is reused rather than rejected: the caller's intent is "put this
        // person in my workspace", and whether they happen to have signed up already is not something a workspace
        // admin should have to know. The tenant-level invite differs deliberately -- there, an existing address means
        // the request is a mistake.
        Optional<User> existingUser = userService.fetchUserByEmail(email);

        User user = existingUser.orElseGet(() -> userInvitationService.inviteUser(email, AuthorityConstants.USER));

        // Self-invocation, so the annotation on addWorkspaceUser does not re-fire -- this method carries the same
        // guard. The call is still what writes the row, keeping the already-a-member check, the audit event and the
        // scope-cache eviction in one place.
        WorkspaceUser workspaceUser = addWorkspaceUser(user.getId(), workspaceId, workspaceRole, customRoleId);

        // A newly provisioned account already learned about this through the claim link. An existing one would
        // otherwise be added silently, so it gets its own notice -- after the membership is written, so a failed add
        // does not announce access nobody has.
        if (existingUser.isPresent()) {
            Workspace workspace = workspaceService.getWorkspace(workspaceId);

            userInvitationService.notifyAddedToWorkspace(user, workspace.getName());
        }

        return workspaceUser;
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_MEMBER_MANAGE')")
    public WorkspaceUser assignCustomRole(long userId, long workspaceId, long customRoleId) {
        validateCustomRoleExists(customRoleId);

        WorkspaceUser workspaceUser = workspaceUserRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
            .orElseThrow(() -> missingMembership(userId, workspaceId));

        // Demoting the last admin to a custom role locks the workspace out exactly as demoting them to EDITOR would,
        // and the custom role's scopes are no guarantee -- it may carry none of the management ones.
        if (Objects.equals(workspaceUser.getWorkspaceRole(), WorkspaceRole.ADMIN.ordinal())) {
            validateNotLastAdmin(workspaceId);
        }

        // The domain factory maintains the XOR invariant: assigning a custom role clears the built-in one.
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

    /**
     * Shared by {@code assignCustomRole} and the add/invite paths. Every custom role is tenant-global and assignable in
     * any workspace, so the only thing left to validate is existence — a dangling {@code custom_role_id} would fail
     * closed at permission-check time and invisibly lock the member out, so writes must reject it loudly.
     */
    private void validateCustomRoleExists(long customRoleId) {
        customRoleRepository.findById(customRoleId)
            .orElseThrow(() -> new ConfigurationException(
                "Custom role " + customRoleId + " does not exist",
                WorkspaceUserErrorType.CUSTOM_ROLE_NOT_IN_WORKSPACE));
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_MEMBER_MANAGE')")
    public WorkspaceUser addWorkspaceUser(long userId, long workspaceId, WorkspaceRole workspaceRole) {
        return addWorkspaceUser(userId, workspaceId, workspaceRole, null);
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_MEMBER_MANAGE')")
    public WorkspaceUser addWorkspaceUser(
        long userId, long workspaceId, WorkspaceRole workspaceRole, Long customRoleId) {

        // Mirrors the XOR the WorkspaceUser constructor enforces, but raises a typed ConfigurationException the API
        // can surface rather than the constructor's IllegalArgumentException, which would read as a 500.
        boolean hasBuiltInRole = workspaceRole != null;
        boolean hasCustomRole = customRoleId != null;

        if (hasBuiltInRole == hasCustomRole) {
            throw new ConfigurationException(
                "Exactly one of a built-in role or a custom role must be supplied",
                WorkspaceUserErrorType.CUSTOM_ROLE_NOT_IN_WORKSPACE);
        }

        Optional<WorkspaceUser> existing = workspaceUserRepository.findByUserIdAndWorkspaceId(userId, workspaceId);

        if (existing.isPresent()) {
            throw new ConfigurationException(
                "User " + userId + " is already a member of workspace " + workspaceId,
                WorkspaceUserErrorType.ALREADY_MEMBER);
        }

        // Validated before the row is written so an unassignable role fails the whole add rather than leaving a
        // member behind on a role the caller never asked for. There is no last-admin check here: a brand new
        // membership cannot be demoting anyone.
        if (hasCustomRole) {
            validateCustomRoleExists(customRoleId);
        }

        WorkspaceUser savedWorkspaceUser = workspaceUserRepository.save(
            hasCustomRole
                ? WorkspaceUser.forCustomRole(userId, workspaceId, customRoleId)
                : WorkspaceUser.forRole(userId, workspaceId, workspaceRole));

        // The scope cache may already hold an empty-set DENY for (userId, workspaceId) from a pre-membership probe;
        // evict so the new member's scopes are re-resolved on the next check instead of being pinned to "no access"
        // until the TTL expires.
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
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_VIEW')")
    @Transactional(readOnly = true)
    public List<WorkspaceUser> getWorkspaceWorkspaceUsers(long workspaceId) {
        return workspaceUserRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_MEMBER_MANAGE')")
    public boolean removeWorkspaceUser(long userId, long workspaceId) {
        WorkspaceUser workspaceUser = workspaceUserRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
            .orElseThrow(() -> missingMembership(userId, workspaceId));

        if (Objects.equals(workspaceUser.getWorkspaceRole(), WorkspaceRole.ADMIN.ordinal())) {
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
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'WORKSPACE_MEMBER_MANAGE')")
    public WorkspaceUser updateWorkspaceUserRole(long userId, long workspaceId, WorkspaceRole workspaceRole) {
        WorkspaceUser workspaceUser = workspaceUserRepository.findByUserIdAndWorkspaceId(userId, workspaceId)
            .orElseThrow(() -> missingMembership(userId, workspaceId));

        validateNotSelfDemotion(workspaceId, userId, workspaceUser, workspaceRole);

        if (Objects.equals(workspaceUser.getWorkspaceRole(), WorkspaceRole.ADMIN.ordinal())
            && workspaceRole != WorkspaceRole.ADMIN) {

            validateNotLastAdmin(workspaceId);
        }

        workspaceUser.setWorkspaceRole(workspaceRole.ordinal());

        WorkspaceUser savedWorkspaceUser = workspaceUserRepository.save(workspaceUser);

        // A role change alters the resolved scope set, so the cached (userId, workspaceId) scopes must be evicted or
        // the old role's scopes would be served until the TTL expires.
        permissionService.evictWorkspaceScopeCache(userId, workspaceId);

        Map<String, Object> data = new HashMap<>();

        data.put("workspaceId", String.valueOf(workspaceId));
        data.put("userId", String.valueOf(userId));
        data.put("role", workspaceRole.name());

        workspaceUserAuditPublisher.publish(WorkspaceUserAuditEvent.WORKSPACE_USER_ROLE_UPDATED, data);

        return savedWorkspaceUser;
    }

    /**
     * Refuses to let the current user demote their own workspace ADMIN role. The last-admin guard only blocks the
     * delete that would leave zero admins; without this check an admin could downgrade themselves to EDITOR while
     * another admin row exists, instantly losing workspace-management rights and the ability to reverse the change.
     * Require self-demotion to go through another admin so a human confirms the survivor is real. Tenant admins bypass
     * the guard (they can always restore themselves).
     */
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
