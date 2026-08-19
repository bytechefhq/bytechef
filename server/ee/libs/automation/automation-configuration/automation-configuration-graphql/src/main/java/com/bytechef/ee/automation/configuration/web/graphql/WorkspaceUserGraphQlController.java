/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.automation.configuration.domain.WorkspaceUser;
import com.bytechef.ee.automation.configuration.security.constant.WorkspaceRole;
import com.bytechef.ee.automation.configuration.service.WorkspaceUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.UserService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for workspace-user management. Authorization is enforced at the service layer
 * ({@code WorkspaceUserService} methods are {@code @PreAuthorize}-annotated) and pinned by
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
public class WorkspaceUserGraphQlController {

    private final PermissionService permissionService;
    private final UserService userService;
    private final WorkspaceUserService workspaceUserService;

    public WorkspaceUserGraphQlController(
        PermissionService permissionService, UserService userService, WorkspaceUserService workspaceUserService) {

        this.permissionService = permissionService;
        this.userService = userService;
        this.workspaceUserService = workspaceUserService;
    }

    /**
     * Stored memberships, plus every tenant admin projected as an inherited ADMIN.
     *
     * <p>
     * Tenant admins already administer every workspace — {@code isTenantAdmin()} short-circuits every check in
     * {@code PermissionServiceImpl} — but hold no membership row, so without this the page shows four people when six
     * can administer the workspace. The entries are synthesized on read rather than materialized as rows: a stored copy
     * would drift the moment someone is demoted, and a stale row there would grant real access rather than merely
     * display wrongly.
     */
    @QueryMapping
    public List<WorkspaceUserView> workspaceUsers(@Argument long workspaceId) {
        List<WorkspaceUser> workspaceUsers = workspaceUserService.getWorkspaceWorkspaceUsers(workspaceId);

        Set<Long> memberUserIds = workspaceUsers.stream()
            .map(WorkspaceUser::getUserId)
            .collect(Collectors.toSet());

        List<WorkspaceUserView> views = new ArrayList<>(
            workspaceUsers.stream()
                .map(WorkspaceUserView::stored)
                .toList());

        for (User tenantAdmin : userService.getUsersByAuthorityName(AuthorityConstants.ADMIN)) {
            // A tenant admin who also holds a real membership appears once, with the stored role: that is what the
            // authorization path would use if they lost tenant admin, so it is the truer answer.
            if (!memberUserIds.contains(tenantAdmin.getId())) {
                views.add(WorkspaceUserView.inherited(workspaceId, tenantAdmin.getId()));
            }
        }

        return views;
    }

    /**
     * Returns the current user's scopes in the given workspace, or an empty set if the user is not a member. Requires
     * authentication — the query is about the <em>current</em> user (no userId argument), and an unauthenticated call
     * would hit an NPE in the current-user resolver. Rejecting anonymous access explicitly also routes the query
     * through {@code PermissionAuditAspect} so any unexpected enumeration attempt is recorded.
     */
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Set<String> myWorkspaceScopes(@Argument long workspaceId) {
        return permissionService.getMyWorkspaceScopes(workspaceId);
    }

    /**
     * Returns the current user's workspace role name, or {@code null} if the user is not a member. Same authentication
     * requirement as {@link #myWorkspaceScopes(long)}.
     */
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public String myWorkspaceRole(@Argument long workspaceId) {
        return permissionService.getMyWorkspaceRole(workspaceId);
    }

    @MutationMapping
    public WorkspaceUserView addWorkspaceUser(
        @Argument long workspaceId, @Argument long userId, @Argument WorkspaceRole role,
        @Argument Long customRoleId) {

        return WorkspaceUserView.stored(
            workspaceUserService.addWorkspaceUser(userId, workspaceId, role, customRoleId));
    }

    @MutationMapping
    public WorkspaceUserView inviteWorkspaceUser(
        @Argument long workspaceId, @Argument String email, @Argument WorkspaceRole role,
        @Argument Long customRoleId) {

        return WorkspaceUserView.stored(
            workspaceUserService.inviteWorkspaceUser(workspaceId, email, role, customRoleId));
    }

    @MutationMapping
    public WorkspaceUserView updateWorkspaceUserRole(
        @Argument long workspaceId, @Argument long userId, @Argument WorkspaceRole role) {

        // Every WorkspaceUser-returning operation must go through the view: the schema's `inherited` field has no
        // counterpart on the domain object, so returning that directly fails the moment a query selects it.
        return WorkspaceUserView.stored(workspaceUserService.updateWorkspaceUserRole(userId, workspaceId, role));
    }

    @MutationMapping
    public WorkspaceUserView assignWorkspaceUserCustomRole(
        @Argument long workspaceId, @Argument long userId, @Argument long customRoleId) {

        return WorkspaceUserView.stored(workspaceUserService.assignCustomRole(userId, workspaceId, customRoleId));
    }

    @MutationMapping
    public boolean removeWorkspaceUser(@Argument long workspaceId, @Argument long userId) {
        return workspaceUserService.removeWorkspaceUser(userId, workspaceId);
    }

    @MutationMapping
    public WorkspaceUserView setWorkspaceUserEnvironmentRole(
        @Argument long workspaceId, @Argument long userId, @Argument Environment environment,
        @Argument WorkspaceRole role, @Argument Long customRoleId) {

        return WorkspaceUserView.stored(
            workspaceUserService.setEnvironmentRole(userId, workspaceId, environment, role, customRoleId));
    }

    @MutationMapping
    public boolean removeWorkspaceUserEnvironmentRole(
        @Argument long workspaceId, @Argument long userId, @Argument Environment environment) {

        workspaceUserService.removeEnvironmentRole(userId, workspaceId, environment);

        return true;
    }

    @SchemaMapping(typeName = "WorkspaceUser", field = "user")
    public WorkspaceUserInfo user(WorkspaceUserView workspaceUserView) {
        User user = userService.getUser(workspaceUserView.userId());

        return new WorkspaceUserInfo(user.getEmail(), user.getFirstName(), user.getLastName());
    }

    public record WorkspaceUserInfo(String email, String firstName, String lastName) {
    }

    /**
     * A row in the membership view: either a stored {@code WorkspaceUser} or a tenant admin synthesized as an inherited
     * workspace admin. An inherited entry has no {@code id} because no row backs it.
     */
    public record WorkspaceUserView(
        Long id, long workspaceId, long userId, String workspaceRole, Long customRoleId, boolean inherited,
        String createdDate, String environment) {

        static WorkspaceUserView stored(WorkspaceUser workspaceUser) {
            Integer roleOrdinal = workspaceUser.getWorkspaceRole();
            Environment environment = workspaceUser.getEnvironment();

            return new WorkspaceUserView(
                workspaceUser.getId(), workspaceUser.getWorkspaceId(), workspaceUser.getUserId(),
                roleOrdinal == null ? null : WorkspaceRole.values()[roleOrdinal].name(),
                workspaceUser.getCustomRoleId(), false,
                workspaceUser.getCreatedDate() == null ? null : String.valueOf(workspaceUser.getCreatedDate()),
                environment == null ? null : environment.name());
        }

        /**
         * Synthesizes an entry for a tenant admin, who administers every workspace and has no row. Their environment is
         * always null: a tenant admin is not subject to per-environment roles at all, because every scope check
         * short-circuits on isTenantAdmin() before any row is read.
         */
        static WorkspaceUserView inherited(long workspaceId, long userId) {
            return new WorkspaceUserView(
                null, workspaceId, userId, WorkspaceRole.ADMIN.name(), null, true, null, null);
        }
    }
}
