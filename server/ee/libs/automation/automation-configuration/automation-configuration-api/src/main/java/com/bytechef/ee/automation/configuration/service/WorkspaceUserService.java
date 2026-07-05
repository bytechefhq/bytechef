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

    WorkspaceUser addWorkspaceUser(long userId, long workspaceId, WorkspaceRole workspaceRole);

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
