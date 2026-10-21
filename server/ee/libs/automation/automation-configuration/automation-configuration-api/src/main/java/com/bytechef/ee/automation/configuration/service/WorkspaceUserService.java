/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.platform.configuration.domain.Environment;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceUserService {

    WorkspaceUser addWorkspaceUser(long userId, long workspaceId, WorkspaceRole workspaceRole);

    WorkspaceUser addWorkspaceUser(long userId, long workspaceId, WorkspaceRole workspaceRole, Long customRoleId);

    WorkspaceUser assignCustomRole(long userId, long workspaceId, long customRoleId);

    /**
     * Returns the number of workspace memberships currently backed by the given custom role. Used by
     * {@code CustomRoleService} to refuse deletion of a custom role that is still in use.
     *
     * @param customRoleId the custom role to count memberships for
     * @return the number of memberships backed by the role
     */
    long countByCustomRoleId(long customRoleId);

    long countEnvironmentAdmins(long workspaceId, Environment environment);

    Optional<WorkspaceUser> fetchEnvironmentWorkspaceUser(long userId, long workspaceId, Environment environment);

    Optional<WorkspaceUser> fetchWorkspaceWideWorkspaceUser(long userId, long workspaceId);

    WorkspaceUser getEnvironmentWorkspaceUser(long userId, long workspaceId, Environment environment);

    Set<Environment> getLostAdminEnvironments(
        long userId, long workspaceId, Environment environment, WorkspaceRole workspaceRole);

    /**
     * Returns every membership row the user holds, across all workspaces. Trusted-caller method (no
     * {@code @PreAuthorize}): it is what {@code WorkspaceFacade} uses to narrow the unfiltered workspace list down to
     * the caller's own memberships, so gating it on a workspace scope would be circular.
     *
     * @param userId the user whose memberships are wanted
     * @return every membership row the user holds, one per granted environment in explicit mode
     */
    List<WorkspaceUser> getUserWorkspaceUsers(long userId);

    WorkspaceUser getWorkspaceWideWorkspaceUser(long userId, long workspaceId);

    List<WorkspaceUser> getWorkspaceUsers(long userId, long workspaceId);

    List<WorkspaceUser> getWorkspaceWorkspaceUsers(long workspaceId);

    boolean hasEnvironmentWorkspaceUsers(long userId, long workspaceId);

    /**
     * Whether the user is a member of the workspace, in either mode. Answers membership rather than handing back a row,
     * because a member in explicit mode holds one row per granted environment.
     *
     * @param userId      the user to check
     * @param workspaceId the workspace to check membership in
     * @return {@code true} if the user holds any membership row in the workspace
     */
    boolean isWorkspaceMember(long userId, long workspaceId);

    void lockWorkspace(long workspaceId);

    void removeEnvironmentRole(long userId, long workspaceId, Environment environment);

    boolean removeWorkspaceUser(long userId, long workspaceId);

    WorkspaceUser setEnvironmentRole(
        long userId, long workspaceId, Environment environment, WorkspaceRole workspaceRole, Long customRoleId);

    WorkspaceUser updateWorkspaceUserRole(long userId, long workspaceId, WorkspaceRole workspaceRole);
}
