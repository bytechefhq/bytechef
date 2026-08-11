/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.facade;

import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.WorkspaceMembershipAssigner.WorkspaceAssignment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;

/**
 * Facade for admin user management (delete/invite/update/list). Hosts the {@code ADMIN} authorization guard so it
 * applies to every caller of the facade rather than only the GraphQL entry point, and keeps it off the shared
 * {@code UserService}/{@code AuthorityService}/{@code MailService}/{@code TenantService} which non-admin flows (account
 * self-service, SCIM, login) rely on.
 *
 * <p>
 * The admin-flow orchestration (tenant/email-collision checks, password validation, registration, role assignment,
 * invitation mail) lives here so the {@code ADMIN} check runs before any of it. Pure DTO assembly stays in the GraphQL
 * controller; methods that need authorities for DTO assembly return them alongside the user(s).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface UserManagementFacade {

    void deleteUser(String login);

    /**
     * Provisions a tenant account for {@code email} and mails a claim link on which the recipient sets their own
     * password. Takes no password: an administrator must not be able to choose, or learn, another person's credential.
     *
     * @param email      the invitee's email address
     * @param role       the tenant authority to grant, e.g. {@code ROLE_USER}
     * @param workspaces the workspaces to place the invitee in, each with the role to hold there; may be empty, which
     *                   provisions an account belonging to no workspace — legitimate when creating a second tenant
     *                   admin
     */
    void inviteUser(String email, String role, List<WorkspaceAssignment> workspaces);

    Optional<UserWithAuthorities> fetchUser(String login);

    UsersWithAuthorities getUsers(Integer pageNumber, Integer pageSize);

    UserWithAuthorities updateUserRole(String login, String role);

    @SuppressFBWarnings({
        "EI", "EI2"
    })
    record UserWithAuthorities(User user, List<Authority> authorities) {
    }

    @SuppressFBWarnings({
        "EI", "EI2"
    })
    record UsersWithAuthorities(Page<User> users, List<Authority> authorities) {
    }
}
