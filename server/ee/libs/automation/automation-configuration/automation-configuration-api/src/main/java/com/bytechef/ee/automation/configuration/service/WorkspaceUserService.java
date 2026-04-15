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

    Optional<WorkspaceUser> fetchWorkspaceUser(long userId, long workspaceId);

    List<WorkspaceUser> getUserWorkspaceUsers(long userId);

    List<WorkspaceUser> getWorkspaceWorkspaceUsers(long workspaceId);

    /**
     * Removes the given user from the given workspace.
     *
     * <p>
     * Authorization: caller must hold {@code WorkspaceRole.ADMIN} on the workspace. Refuses to remove the last admin
     * (would otherwise lock the workspace out). Cascades the deletion to {@code project_user} rows for every project in
     * the workspace and evicts each impacted {@code (userId, projectId)} cache entry, so subsequent permission checks
     * miss the cache and re-resolve from the (now-empty) membership. Project ids are snapshotted before the cascade
     * delete to avoid querying an empty result set after the rows are gone.
     *
     * @return {@code true} if a row was removed, {@code false} if the user was not a member of the workspace.
     */
    boolean removeWorkspaceUser(long userId, long workspaceId);

    WorkspaceUser updateWorkspaceUserRole(long userId, long workspaceId, WorkspaceRole workspaceRole);
}
