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
 * Manages the members of a workspace on behalf of the signed-in user. Every method is authorized here; the underlying
 * {@code WorkspaceUserService} carries no checks.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface WorkspaceUserFacade {

    WorkspaceUser addWorkspaceUser(long userId, long workspaceId, WorkspaceRole workspaceRole, Long customRoleId);

    WorkspaceUser assignCustomRole(long userId, long workspaceId, long customRoleId);

    List<WorkspaceUser> getWorkspaceWorkspaceUsers(long workspaceId);

    WorkspaceUser inviteWorkspaceUser(long workspaceId, String email, WorkspaceRole workspaceRole, Long customRoleId);

    void removeEnvironmentRole(long userId, long workspaceId, Environment environment);

    boolean removeWorkspaceUser(long userId, long workspaceId);

    WorkspaceUser setEnvironmentRole(
        long userId, long workspaceId, Environment environment, WorkspaceRole workspaceRole, Long customRoleId);

    WorkspaceUser updateWorkspaceUserRole(long userId, long workspaceId, WorkspaceRole workspaceRole);
}
