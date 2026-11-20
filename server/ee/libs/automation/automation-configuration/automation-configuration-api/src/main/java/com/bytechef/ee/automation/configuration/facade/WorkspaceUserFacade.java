/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.facade;

import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceUserFacade {

    WorkspaceUser addWorkspaceUser(long userId, long workspaceId, WorkspaceRole workspaceRole);

    /**
     * Removes the given user from the given workspace and evicts the impacted {@code (userId, workspaceId)} scope cache
     * entry so subsequent permission checks re-resolve from the (now-removed) membership.
     *
     * @return {@code true} once the membership row has been removed
     * @throws com.bytechef.exception.ConfigurationException if the user is not a member of the workspace, or if
     *                                                       removing them would leave the workspace without an admin
     */
    boolean removeWorkspaceUser(long userId, long workspaceId);

    WorkspaceUser updateWorkspaceUserRole(long userId, long workspaceId, WorkspaceRole workspaceRole);
}
