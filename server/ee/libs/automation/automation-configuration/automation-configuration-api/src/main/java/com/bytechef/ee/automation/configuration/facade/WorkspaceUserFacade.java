/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceUserFacade {

    /**
     * Places an existing account into the workspace under either a built-in role or a custom one. Exactly one of
     * {@code workspaceRole} and {@code customRoleId} must be supplied, mirroring the XOR invariant
     * {@link WorkspaceUser} enforces on its columns. Custom roles are tenant-global, so only their existence is
     * checked.
     *
     * @param userId        the user to place into the workspace
     * @param workspaceId   the workspace to join
     * @param workspaceRole the built-in role to hold, or {@code null} when {@code customRoleId} is given
     * @param customRoleId  the custom role to hold, or {@code null} when {@code workspaceRole} is given
     * @return the written membership
     * @throws com.bytechef.exception.ConfigurationException if the user is already a member, if neither or both role
     *                                                       arguments are supplied, or if the custom role does not
     *                                                       exist
     */
    WorkspaceUser addWorkspaceUser(long userId, long workspaceId, WorkspaceRole workspaceRole, Long customRoleId);

    /**
     * Replaces a member's built-in role with a custom one.
     *
     * <p>
     * Authorization: {@code WORKSPACE_MEMBER_MANAGE} on the workspace. Refuses to convert the last built-in admin — a
     * custom role's scopes are no guarantee it can manage anything. For the same reason a caller who is not a tenant
     * admin may not move their own membership onto a custom role at all: a custom role has no rank on the built-in
     * ladder, so neither direction can be shown to be safe. Stricter than
     * {@link #updateWorkspaceUserRole(long, long, WorkspaceRole)}, which permits a lowering between comparable
     * non-admin built-in roles.
     *
     * @param userId       the member whose role is replaced
     * @param workspaceId  the workspace the membership belongs to
     * @param customRoleId the custom role to assign
     * @return the updated membership
     * @throws com.bytechef.exception.ConfigurationException if the role does not exist, if the user holds no
     *                                                       workspace-wide role, if they are the last admin, or if the
     *                                                       caller is changing their own role
     */
    WorkspaceUser assignCustomRole(long userId, long workspaceId, long customRoleId);

    /**
     * Returns every membership row in the workspace. Authorization: {@code WORKSPACE_VIEW} on it.
     *
     * @param workspaceId the workspace whose members are wanted
     * @return every membership row in the workspace, several per member in explicit mode
     */
    List<WorkspaceUser> getWorkspaceWorkspaceUsers(long workspaceId);

    /**
     * Places the holder of {@code email} into the workspace under either a built-in role or a custom one, provisioning
     * a tenant account and mailing a claim link when no account exists yet.
     *
     * <p>
     * Authorization: the {@code WORKSPACE_MEMBER_MANAGE} scope on the workspace. Every refusal, the role arguments
     * included, is checked before an account is provisioned, and the claim-link mail goes out only once the transaction
     * commits, so a late rejection cannot roll the account back out from under a link the invitee already holds.
     *
     * @param workspaceId   the workspace to join
     * @param email         the address to invite
     * @param workspaceRole the built-in role to hold, or {@code null} when {@code customRoleId} is given
     * @param customRoleId  the custom role to hold, or {@code null} when {@code workspaceRole} is given
     * @return the written membership
     * @throws com.bytechef.exception.ConfigurationException if the workspace does not exist, if the address is already
     *                                                       a member of this workspace, if neither or both role
     *                                                       arguments are supplied, or if the custom role does not
     *                                                       exist
     */
    WorkspaceUser inviteWorkspaceUser(long workspaceId, String email, WorkspaceRole workspaceRole, Long customRoleId);

    /**
     * Revokes a member's role in one environment.
     *
     * <p>
     * Revoking a member's only environment role turns it into a workspace-wide role with the same built-in or custom
     * role, so they keep that role in every environment. That widening requires WORKSPACE_MEMBER_MANAGE in every
     * environment. Removing them from the workspace is {@link #removeWorkspaceUser(long, long)}.
     *
     * <p>
     * Refuses rather than succeeding quietly when there is nothing to revoke, so a caller acting on a stale members
     * view is told so. A member in implicit mode is reported separately from a non-member: their access comes from
     * their workspace-wide role, which is changed workspace-wide.
     *
     * @param userId      the member whose role is revoked
     * @param workspaceId the workspace the membership belongs to
     * @param environment the environment to revoke the role in
     * @throws com.bytechef.exception.ConfigurationException if the member holds no role specific to this environment,
     *                                                       if they are not a member at all, if the removal would leave
     *                                                       the environment without an admin, or if it would withdraw
     *                                                       the caller's own ADMIN role from it
     */
    void removeEnvironmentRole(long userId, long workspaceId, Environment environment);

    /**
     * Removes the given user from the given workspace.
     *
     * <p>
     * Authorization: the {@code WORKSPACE_MEMBER_MANAGE} scope in every environment of the workspace. Refuses to remove
     * the workspace's last admin, which would lock the workspace out. Evicts the impacted {@code (userId, workspaceId)}
     * scope cache entry so later permission checks re-resolve. Every row the member holds goes, implicit and
     * per-environment alike; removing only some would leave them resolving to a role while the members view shows them
     * gone.
     *
     * <p>
     * The last-admin guard is checked in every environment the member administers, so removing the only ADMIN of any
     * one environment is refused.
     *
     * @param userId      the member to remove
     * @param workspaceId the workspace to remove them from
     * @return {@code true} once the membership rows have been removed
     * @throws com.bytechef.exception.ConfigurationException if the user is not a member of the workspace, or if
     *                                                       removing them would leave an environment without an admin
     */
    boolean removeWorkspaceUser(long userId, long workspaceId);

    /**
     * Grants a member a role in one environment, switching them into explicit mode.
     *
     * <p>
     * Their implicit row, if any, is deleted in the same transaction — the two modes never coexist. An existing row for
     * that environment is updated in place. Exactly one of {@code workspaceRole} and {@code customRoleId} must be
     * supplied, mirroring the XOR invariant {@link WorkspaceUser} enforces on its columns.
     *
     * <p>
     * A caller who is not a tenant admin may not raise their own role here, nor move themselves onto a custom role: the
     * environment gate asks only for {@code WORKSPACE_MEMBER_MANAGE} in the environment being written, so its holder
     * could otherwise grant themselves ADMIN of Production. Lowering their own role is still allowed, subject to the
     * stranding guards below.
     *
     * @param userId        the member being granted the role
     * @param workspaceId   the workspace the membership belongs to
     * @param environment   the environment the role applies to
     * @param workspaceRole the built-in role to hold, or {@code null} when {@code customRoleId} is given
     * @param customRoleId  the custom role to hold, or {@code null} when {@code workspaceRole} is given
     * @return the written membership row for that environment
     * @throws com.bytechef.exception.ConfigurationException if the user is not a member of the workspace, if neither or
     *                                                       both role arguments are supplied, if the custom role is
     *                                                       unknown, if the write would leave an environment without an
     *                                                       admin, if it would withdraw the caller's own ADMIN role
     *                                                       from an environment, or if it would raise the caller's own
     *                                                       role
     */
    WorkspaceUser setEnvironmentRole(
        long userId, long workspaceId, Environment environment, WorkspaceRole workspaceRole, Long customRoleId);

    /**
     * Changes the built-in role a member holds workspace-wide — that is, on their implicit row. A member in explicit
     * mode is rejected rather than having one of their environment rows picked arbitrarily; use
     * {@link #setEnvironmentRole(long, long, Environment, WorkspaceRole, Long)} for those.
     *
     * <p>
     * A caller who is not a tenant admin may not raise their own role, which would make {@code WORKSPACE_MEMBER_MANAGE}
     * self-escalating to workspace ADMIN, nor give up their own ADMIN role, which removes the rights needed to undo the
     * change. A lowering between non-admin built-in roles is allowed.
     *
     * @param userId        the member whose role is changed
     * @param workspaceId   the workspace the membership belongs to
     * @param workspaceRole the built-in role to hold
     * @return the updated membership
     * @throws com.bytechef.exception.ConfigurationException if the user holds no workspace-wide role, if the write
     *                                                       would leave the workspace without an admin, or if the
     *                                                       caller is changing their own role
     */
    WorkspaceUser updateWorkspaceUserRole(long userId, long workspaceId, WorkspaceRole workspaceRole);
}
