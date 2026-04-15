/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.domain.ProjectUser;
import com.bytechef.ee.automation.configuration.security.constant.ProjectRole;
import com.bytechef.ee.automation.configuration.service.ProjectUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Set;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for project-user management. Authorization is enforced at the service layer
 * ({@code ProjectUserService} methods are {@code @PreAuthorize}-annotated) and pinned by
 * {@code PreAuthorizeAnnotationTest}. Do NOT add caching or transform logic to controller methods that could precede
 * the service call without also adding a matching {@code @PreAuthorize} here.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@ConditionalOnEEVersion
@SuppressFBWarnings("EI2")
public class ProjectUserGraphQlController {

    private final PermissionService permissionService;
    private final ProjectUserService projectUserService;
    private final UserService userService;

    public ProjectUserGraphQlController(
        PermissionService permissionService, ProjectUserService projectUserService, UserService userService) {

        this.permissionService = permissionService;
        this.projectUserService = projectUserService;
        this.userService = userService;
    }

    @QueryMapping
    public List<ProjectUser> projectUsers(@Argument long projectId) {
        return projectUserService.getProjectUsers(projectId);
    }

    /**
     * Returns the current user's scopes on the given project, or an empty set if the user is not a member. Requires
     * authentication — the query is about the <em>current</em> user (no userId argument), and an unauthenticated call
     * would hit an NPE in the current-user resolver. Rejecting anonymous access explicitly also routes the query
     * through {@code PermissionAuditAspect} so any unexpected enumeration attempt is recorded.
     */
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Set<String> myProjectScopes(@Argument long projectId) {
        return permissionService.getMyProjectScopes(projectId);
    }

    /**
     * Returns the current user's workspace role name, or {@code null} if the user is not a member. Same authentication
     * requirement as {@link #myProjectScopes(long)}.
     */
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public String myWorkspaceRole(@Argument long workspaceId) {
        return permissionService.getMyWorkspaceRole(workspaceId);
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Controller-level @PreAuthorize on mutations is defense-in-depth on top of the service-layer guards. The duplicate
    // expressions are deliberate: the service-layer enforcement is the source of truth (and is reflection-pinned by
    // PreAuthorizeAnnotationTest), but landing the gate at the controller boundary as well means a regression that
    // bypasses the service proxy (e.g., the GraphQL pipeline accidentally invoking the service directly without the
    // proxy chain) is still blocked here, AND audit events fire from the controller frame so investigators see the
    // exact GraphQL surface that was probed instead of an opaque internal service call.
    // -----------------------------------------------------------------------------------------------------------------

    @MutationMapping
    @PreAuthorize("@permissionService.hasProjectScope(#projectId, 'PROJECT_MANAGE_USERS')")
    public ProjectUser addProjectUser(
        @Argument long projectId, @Argument long userId, @Argument ProjectRole role) {

        return projectUserService.addProjectUser(projectId, userId, role.ordinal());
    }

    @MutationMapping
    @PreAuthorize("@permissionService.hasProjectScope(#projectId, 'PROJECT_MANAGE_USERS')")
    public ProjectUser updateProjectUserRole(
        @Argument long projectId, @Argument long userId, @Argument ProjectRole role) {

        return projectUserService.updateProjectUserRole(projectId, userId, role.ordinal());
    }

    @MutationMapping
    @PreAuthorize("@permissionService.hasProjectScope(#projectId, 'PROJECT_MANAGE_USERS')")
    public boolean removeProjectUser(@Argument long projectId, @Argument long userId) {
        projectUserService.deleteProjectUser(projectId, userId);

        return true;
    }

    @SchemaMapping(typeName = "ProjectUser", field = "user")
    public ProjectUserInfo user(ProjectUser projectUser) {
        User user = userService.getUser(projectUser.getUserId());

        return new ProjectUserInfo(user.getEmail(), user.getFirstName(), user.getLastName());
    }

    public record ProjectUserInfo(String email, String firstName, String lastName) {
    }
}
