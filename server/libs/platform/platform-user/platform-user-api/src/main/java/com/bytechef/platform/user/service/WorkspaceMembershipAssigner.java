/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.user.service;

import java.util.List;

/**
 * SPI seam letting the platform user modules reach workspace membership, which lives in the automation modules that
 * already depend on this one. Implemented there, injected here as an optional bean; embedded deployments register none
 * and reject any invite naming a workspace.
 *
 * @author Ivica Cardic
 */
public interface WorkspaceMembershipAssigner {

    /**
     * Places {@code userId} into each requested workspace at the requested role. Runs inside the invite's transaction,
     * so a failed assignment rolls the provisioned account back with it. An empty list is a no-op.
     *
     * @param userId      the freshly provisioned (or already existing) user
     * @param assignments the workspaces to join and the role to hold in each
     */
    void assign(long userId, List<WorkspaceAssignment> assignments);

    /**
     * Rejects anything {@link #assign(long, List)} would reject, without writing. Lets the invite fail before it
     * provisions an account and mails a claim link the rollback would invalidate.
     *
     * @param assignments the workspaces and role names the caller asked for
     */
    void validateAssignments(List<WorkspaceAssignment> assignments);

    /**
     * Removes every workspace membership the user holds. Called by the user delete before the user row goes: the
     * {@code workspace_user.user_id} foreign key exists only on monolithic deployments, so elsewhere the rows would
     * outlive their user and break the members view, which resolves each row's user.
     *
     * @param userId the user being deleted
     */
    void removeMemberships(long userId);

    /**
     * One workspace placement. The role travels as its enum name because {@code WorkspaceRole} is declared in the
     * automation modules this interface deliberately cannot see.
     *
     * @param workspaceId the workspace to join
     * @param roleName    the {@code WorkspaceRole} name, e.g. {@code "EDITOR"}
     */
    record WorkspaceAssignment(long workspaceId, String roleName) {
    }
}
