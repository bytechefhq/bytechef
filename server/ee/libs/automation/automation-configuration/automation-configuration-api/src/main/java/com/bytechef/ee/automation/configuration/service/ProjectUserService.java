/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.service;

import com.bytechef.ee.automation.configuration.domain.ProjectUser;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ProjectUserService {

    long countByCustomRoleId(long customRoleId);

    /**
     * Adds a user to the project with a built-in role. {@code projectRoleOrdinal} is the
     * {@link com.bytechef.ee.automation.configuration.security.constant.ProjectRole} ordinal — a raw int is used here
     * because the GraphQL layer receives the enum value as an int on the wire. Callers are range-checked before the
     * ordinal is dereferenced; out-of-range values surface as {@code ProjectUserErrorType.INVALID_ROLE} rather than an
     * {@link ArrayIndexOutOfBoundsException}.
     */
    ProjectUser addProjectUser(long projectId, long userId, int projectRoleOrdinal);

    void deleteProjectUser(long projectId, long userId);

    /**
     * Invoked by {@code WorkspaceUserServiceImpl.removeWorkspaceUser} before the cascade delete runs. For every project
     * in {@code projectIds} where the given user is an effective admin (built-in ADMIN or a custom role granting
     * {@code PROJECT_MANAGE_USERS}), verifies they are not the last one. Throws if removing the user would leave any of
     * those projects with zero effective admins \u2014 the cascade-delete path previously bypassed
     * {@link #deleteProjectUser(long, long)}'s per-project last-admin guard, silently orphaning projects.
     */
    void validateCascadeRemovalDoesNotOrphanAdminProjects(long userId, List<Long> projectIds);

    Optional<ProjectUser> fetchProjectUser(long projectId, long userId);

    List<ProjectUser> getProjectUsers(long projectId);

    List<ProjectUser> getUserProjectMemberships(long userId);

    /**
     * Updates a project member's built-in role. {@code projectRoleOrdinal} is the
     * {@link com.bytechef.ee.automation.configuration.security.constant.ProjectRole} ordinal; see
     * {@link #addProjectUser(long, long, int)} for the rationale on using a raw int.
     */
    ProjectUser updateProjectUserRole(long projectId, long userId, int projectRoleOrdinal);
}
