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
import org.jspecify.annotations.Nullable;
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
     * Tenant admins already administer every workspace — {@code isTenantAdmin()} short-circuits every workspace and
     * resource check in {@code PermissionServiceImpl} — but hold no membership row, so without this the page shows four
     * people when six can administer the workspace. The entries are synthesized on read rather than materialized as
     * rows: a stored copy would drift the moment someone is demoted, and a stale row there would grant real access
     * rather than merely display wrongly.
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
     * Returns the current user's scopes in the given workspace, or an empty set if the user is not a member.
     *
     * <p>
     * Requires authentication. {@code PermissionServiceImpl.getMyWorkspaceScopes} carries the same
     * {@code isAuthenticated()} gate, so an anonymous call is denied either way — but one frame deeper, from inside the
     * service. Gating here rejects at the API boundary instead, which is what the class-level rule about not letting
     * anything precede the service call asks for.
     */
    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public Set<String> myWorkspaceScopes(@Argument long workspaceId, @Argument @Nullable Environment environment) {
        if (environment == null) {
            return permissionService.getMyWorkspaceScopes(workspaceId);
        }

        return permissionService.getMyWorkspaceScopes(workspaceId, environment);
    }

    /**
     * Returns the current user's workspace role name, or {@code null} if the user is not a member. Same authentication
     * requirement as {@link #myWorkspaceScopes(long, Environment)}.
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

    /**
     * Resolves the person behind a membership row, or {@code null} when no account answers to that id.
     *
     * <p>
     * Leniently, because this fetcher runs once per row: {@code getUser} throws {@code UserNotFoundException}, so a
     * single row whose user is gone would take the whole members page down for every viewer rather than showing one
     * incomplete entry. The user delete now withdraws memberships before the account goes, so such a row should not
     * exist — but the {@code workspace_user.user_id} foreign key is created only in the {@code mono} Liquibase context,
     * so nothing at the schema level makes that a guarantee elsewhere, and a page that fails closed on it is the worse
     * of the two failures. The schema declares {@code user} nullable for exactly this.
     */
    @SchemaMapping(typeName = "WorkspaceUser", field = "user")
    public WorkspaceUserInfo user(WorkspaceUserView workspaceUserView) {
        return userService.fetchUser(workspaceUserView.userId())
            .map(user -> new WorkspaceUserInfo(user.getEmail(), user.getFirstName(), user.getLastName()))
            .orElse(null);
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
            Environment environment = workspaceUser.getEnvironment();

            return new WorkspaceUserView(
                workspaceUser.getId(), workspaceUser.getWorkspaceId(), workspaceUser.getUserId(),
                roleName(workspaceUser.getWorkspaceRole()),
                workspaceUser.getCustomRoleId(), false,
                workspaceUser.getCreatedDate() == null ? null : String.valueOf(workspaceUser.getCreatedDate()),
                environment == null ? null : environment.name());
        }

        /**
         * The built-in role's name, or {@code null} when the row carries none — either a custom role, or an ordinal the
         * enum has no member for. Spring Data JDBC hydrates {@code workspace_role} straight into the field and bypasses
         * the constructor's range check, so indexing {@code values()} here would let one corrupted row take the whole
         * members page down with an {@code ArrayIndexOutOfBoundsException} for every viewer. The schema declares the
         * field nullable, and rendering one entry without a role is the better of the two failures — the same reading
         * the {@code user} fetcher above takes.
         */
        private static String roleName(Integer roleOrdinal) {
            if (roleOrdinal == null) {
                return null;
            }

            WorkspaceRole[] workspaceRoles = WorkspaceRole.values();

            if (roleOrdinal < 0 || roleOrdinal >= workspaceRoles.length) {
                return null;
            }

            return workspaceRoles[roleOrdinal].name();
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
