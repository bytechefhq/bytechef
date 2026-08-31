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

package com.bytechef.platform.user.facade;

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
