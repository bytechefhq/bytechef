/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.facade;

import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
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
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface UserManagementFacade {

    void deleteUser(String login);

    void inviteUser(String email, String password, String role);

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
