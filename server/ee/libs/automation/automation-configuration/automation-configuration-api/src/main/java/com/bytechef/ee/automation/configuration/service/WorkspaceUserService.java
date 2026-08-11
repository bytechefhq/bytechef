/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceUserService {

    /**
     * Declared abstract rather than defaulted to the four-argument overload: a {@code default} method carries no
     * {@code @PreAuthorize} of its own, and its call to the overload runs against the target rather than the proxy, so
     * defaulting would silently hand out an unguarded way in. {@code PreAuthorizeAnnotationTest} pins this.
     */
    WorkspaceUser addWorkspaceUser(long userId, long workspaceId, WorkspaceRole workspaceRole);

    /**
     * Places an existing account into the workspace under either a built-in role or a custom one.
     *
     * <p>
     * Exactly one of {@code workspaceRole} and {@code customRoleId} must be supplied, mirroring the XOR invariant
     * {@link WorkspaceUser} enforces on its own columns. A custom role must be tenant-global or owned by this workspace
     * — the id travels in the request, so hiding another workspace's roles from the picker is not enough.
     *
     * @throws com.bytechef.exception.ConfigurationException if the user is already a member, if neither or both role
     *                                                       arguments are supplied, or if the custom role is unknown or
     *                                                       belongs to another workspace
     */
    WorkspaceUser addWorkspaceUser(long userId, long workspaceId, WorkspaceRole workspaceRole, Long customRoleId);

    /**
     * Replaces a member's built-in role with a custom one.
     *
     * <p>
     * Authorization: {@code WORKSPACE_MEMBER_MANAGE} on the workspace. The role must be tenant-global or owned by this
     * workspace — hiding another workspace's roles from the list is not enough, since the id travels in the request.
     * Refuses to convert the last built-in admin, which would lock the workspace out just as demoting them to EDITOR
     * would; a custom role's scopes are no guarantee it can manage anything.
     *
     * @throws com.bytechef.exception.ConfigurationException if the role is unknown or belongs to another workspace, if
     *                                                       the user is not a member, or if they are the last admin
     */
    WorkspaceUser assignCustomRole(long userId, long workspaceId, long customRoleId);

    /**
     * Places the holder of {@code email} into the workspace, provisioning a tenant account and mailing a claim link
     * when no account exists yet.
     *
     * <p>
     * Authorization: caller must hold the {@code WORKSPACE_MEMBER_MANAGE} scope on the workspace — a scope rather than
     * a role, so a custom role carrying it works without a special case. Provisioning consumes a seat against the
     * plan's {@code maxMembers}, which rejects with {@code QuotaLimitExceededException} at the ceiling.
     *
     * @throws com.bytechef.exception.ConfigurationException if the address is already a member of this workspace
     */
    WorkspaceUser inviteWorkspaceUser(long workspaceId, String email, WorkspaceRole workspaceRole);

    /**
     * Invites the holder of {@code email} into the workspace under either a built-in role or a custom one, with the
     * same provisioning and authorization semantics as {@link #inviteWorkspaceUser(long, String, WorkspaceRole)}.
     *
     * <p>
     * The custom role is validated and applied inside the same transaction that writes the membership, so an invite
     * naming an unassignable role fails outright rather than landing the member on a fallback role after the mail has
     * gone out.
     *
     * @throws com.bytechef.exception.ConfigurationException if the address is already a member of this workspace, if
     *                                                       neither or both role arguments are supplied, or if the
     *                                                       custom role is unknown or belongs to another workspace
     */
    WorkspaceUser inviteWorkspaceUser(long workspaceId, String email, WorkspaceRole workspaceRole, Long customRoleId);

    /**
     * Returns the number of workspace memberships currently backed by the given custom role. Used by
     * {@code CustomRoleService} to refuse deletion of a custom role that is still in use.
     */
    long countByCustomRoleId(long customRoleId);

    Optional<WorkspaceUser> fetchWorkspaceUser(long userId, long workspaceId);

    List<WorkspaceUser> getUserWorkspaceUsers(long userId);

    List<WorkspaceUser> getWorkspaceWorkspaceUsers(long workspaceId);

    /**
     * Removes the given user from the given workspace.
     *
     * <p>
     * Authorization: caller must hold {@code WorkspaceRole.ADMIN} on the workspace. Refuses to remove the last admin
     * (would otherwise lock the workspace out). Evicts the impacted {@code (userId, workspaceId)} scope cache entry so
     * subsequent permission checks miss the cache and re-resolve from the (now-removed) membership.
     *
     * @return {@code true} once the membership row has been removed
     * @throws com.bytechef.exception.ConfigurationException if the user is not a member of the workspace, or if
     *                                                       removing them would leave the workspace without an admin
     */
    boolean removeWorkspaceUser(long userId, long workspaceId);

    WorkspaceUser updateWorkspaceUserRole(long userId, long workspaceId, WorkspaceRole workspaceRole);
}
