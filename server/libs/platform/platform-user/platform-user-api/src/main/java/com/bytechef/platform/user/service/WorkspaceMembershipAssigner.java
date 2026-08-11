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
 * SPI seam letting the tenant-level invite place a freshly provisioned user into workspaces without depending on the
 * automation modules. Workspace membership lives in {@code automation-configuration}, which already depends on
 * {@code platform-user-api} — so the reverse call has to invert through an interface declared here and implemented
 * there, the same CE-SPI/EE-impl idiom as {@code AiGuardrailsAdvisorProvider}.
 *
 * <p>
 * Consumers pull an implementation through an optional Spring bean. Embedded deployments register none: embedded has no
 * workspace concept, so an invite there simply provisions the account and assigns nothing.
 *
 * @author Ivica Cardic
 */
public interface WorkspaceMembershipAssigner {

    /**
     * Places {@code userId} into each requested workspace at the requested role.
     *
     * <p>
     * Called inside the invite's transaction so an account is never left half-placed: if any assignment fails, the
     * provisioned user is rolled back with it. An empty list is a no-op rather than an error — provisioning an account
     * with no workspace is legitimate, and is how a second tenant admin is created.
     *
     * @param userId      the freshly provisioned (or already existing) user
     * @param assignments the workspaces to join and the role to hold in each
     */
    void assign(long userId, List<WorkspaceAssignment> assignments);

    /**
     * One workspace placement. The role travels as its enum name rather than the enum itself because
     * {@code WorkspaceRole} is declared in the automation modules this interface deliberately cannot see — the same
     * reason {@code PermissionService.hasWorkspaceRole} takes a {@code String}.
     *
     * @param workspaceId the workspace to join
     * @param roleName    the {@code WorkspaceRole} name, e.g. {@code "EDITOR"}
     */
    record WorkspaceAssignment(long workspaceId, String roleName) {
    }
}
