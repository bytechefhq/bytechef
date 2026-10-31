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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bytechef.cache.config.CacheConfiguration;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.config.UserIntTestConfiguration;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.exception.EmailAlreadyUsedException;
import com.bytechef.platform.user.exception.InvalidEmailException;
import com.bytechef.platform.user.exception.InvalidPasswordException;
import com.bytechef.platform.user.facade.UserManagementFacade.UserWithAuthorities;
import com.bytechef.platform.user.facade.UserManagementFacade.UsersWithAuthorities;
import com.bytechef.platform.user.repository.AuthorityRepository;
import com.bytechef.platform.user.repository.UserRepository;
import com.bytechef.tenant.service.TenantService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exercises {@link UserManagementFacadeImpl} against the real user/authority tables and through the real Spring method
 * security proxy, so the {@code @PreAuthorize(hasAuthority("ROLE_ADMIN"))} guards are enforced exactly as in
 * production.
 *
 * @author Ivica Cardic
 */
@SpringBootTest(
    classes = {
        UserIntTestConfiguration.class, UserManagementFacadeIntTest.UserManagementFacadeIntTestConfiguration.class
    })
@Transactional
class UserManagementFacadeIntTest {

    private static final String PASSWORD = "Password123";

    @Autowired
    private AuthorityRepository authorityRepository;

    @MockitoBean
    private MailService mailService;

    @MockitoBean
    private TenantService tenantService;

    @Autowired
    private UserManagementFacade userManagementFacade;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void beforeEach() {
        authenticateAs(AuthorityConstants.ADMIN);
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testInviteUserSendsInvitationEmail() {
        userManagementFacade.inviteUser("newuser@example.com", PASSWORD, AuthorityConstants.USER);

        User user = userRepository.findByEmailIgnoreCase("newuser@example.com")
            .orElseThrow();

        assertThat(getAuthorityNames(user.getLogin())).containsExactly(AuthorityConstants.USER);

        verify(mailService).sendInvitationEmail(any(User.class), eq(PASSWORD));
    }

    @Test
    void testInviteUserUsesLowerCasedEmailAsLogin() {
        userManagementFacade.inviteUser("Jane.Doe@Example.com", PASSWORD, AuthorityConstants.USER);

        User user = userRepository.findByEmailIgnoreCase("jane.doe@example.com")
            .orElseThrow();

        assertThat(user.getLogin()).isEqualTo("jane.doe@example.com");
        assertThat(userRepository.findByLogin("jane.doe@example.com")).isPresent();
    }

    @Test
    void testInviteUserWithEmailLongerThanTheLoginLimitDoesNotRegister() {
        String email = "firstname.middlename.lastname.department@subdomain.example.com";

        assertThatThrownBy(() -> userManagementFacade.inviteUser(email, PASSWORD, AuthorityConstants.USER))
            .isInstanceOf(InvalidEmailException.class);

        assertThat(userRepository.findByEmailIgnoreCase(email)).isEmpty();

        verify(mailService, never()).sendInvitationEmail(any(), anyString());
    }

    @Test
    void testInviteUserWithPasswordTooShortDoesNotRegister() {
        assertThatThrownBy(() -> userManagementFacade.inviteUser("test@example.com", "Pass1", AuthorityConstants.USER))
            .isInstanceOf(InvalidPasswordException.class);

        assertThat(userRepository.findByEmailIgnoreCase("test@example.com")).isEmpty();
    }

    @Test
    void testInviteUserWithPasswordMissingUppercaseDoesNotRegister() {
        assertThatThrownBy(
            () -> userManagementFacade.inviteUser("test@example.com", "password123", AuthorityConstants.USER))
                .isInstanceOf(InvalidPasswordException.class);

        assertThat(userRepository.findByEmailIgnoreCase("test@example.com")).isEmpty();
    }

    @Test
    void testInviteUserWithPasswordMissingDigitDoesNotRegister() {
        assertThatThrownBy(
            () -> userManagementFacade.inviteUser("test@example.com", "PasswordABC", AuthorityConstants.USER))
                .isInstanceOf(InvalidPasswordException.class);

        assertThat(userRepository.findByEmailIgnoreCase("test@example.com")).isEmpty();
    }

    @Test
    void testInviteUserWithExistingEmailThrows() {
        User existingUser = saveUser("existing", "existing@example.com");

        assertThatThrownBy(
            () -> userManagementFacade.inviteUser("existing@example.com", PASSWORD, AuthorityConstants.USER))
                .isInstanceOf(EmailAlreadyUsedException.class);

        assertThat(userRepository.findByEmailIgnoreCase("existing@example.com"))
            .map(User::getId)
            .contains(existingUser.getId());

        verify(mailService, never()).sendInvitationEmail(any(), anyString());
    }

    @Test
    void testDeleteUserRequiresAdmin() {
        saveUser("deleted", "deleted@example.com");

        authenticateAs(AuthorityConstants.USER);

        assertThatThrownBy(() -> userManagementFacade.deleteUser("deleted"))
            .isInstanceOf(AuthorizationDeniedException.class);

        assertThat(userRepository.findByLogin("deleted")).isPresent();

        authenticateAs(AuthorityConstants.ADMIN);

        userManagementFacade.deleteUser("deleted");

        assertThat(userRepository.findByLogin("deleted")).isEmpty();
    }

    @Test
    void testInviteUserRequiresAdmin() {
        authenticateAs(AuthorityConstants.USER);

        assertThatThrownBy(
            () -> userManagementFacade.inviteUser("invited@example.com", PASSWORD, AuthorityConstants.USER))
                .isInstanceOf(AuthorizationDeniedException.class);

        assertThat(userRepository.findByEmailIgnoreCase("invited@example.com")).isEmpty();

        authenticateAs(AuthorityConstants.ADMIN);

        userManagementFacade.inviteUser("invited@example.com", PASSWORD, AuthorityConstants.USER);

        assertThat(userRepository.findByEmailIgnoreCase("invited@example.com")).isPresent();
    }

    @Test
    void testFetchUserRequiresAdmin() {
        saveUser("fetched", "fetched@example.com");

        authenticateAs(AuthorityConstants.USER);

        assertThatThrownBy(() -> userManagementFacade.fetchUser("fetched"))
            .isInstanceOf(AuthorizationDeniedException.class);

        authenticateAs(AuthorityConstants.ADMIN);

        Optional<UserWithAuthorities> userWithAuthorities = userManagementFacade.fetchUser("fetched");

        assertThat(userWithAuthorities)
            .map(UserWithAuthorities::user)
            .map(User::getEmail)
            .contains("fetched@example.com");
    }

    @Test
    void testGetUsersRequiresAdmin() {
        saveUser("listed", "listed@example.com");

        authenticateAs(AuthorityConstants.USER);

        assertThatThrownBy(() -> userManagementFacade.getUsers(0, 1000))
            .isInstanceOf(AuthorizationDeniedException.class);

        authenticateAs(AuthorityConstants.ADMIN);

        UsersWithAuthorities usersWithAuthorities = userManagementFacade.getUsers(0, 1000);

        assertThat(usersWithAuthorities.users()
            .getContent())
                .extracting(User::getLogin)
                .contains("listed");
    }

    @Test
    void testUpdateUserRoleRequiresAdmin() {
        saveUser("promoted", "promoted@example.com");

        authenticateAs(AuthorityConstants.USER);

        assertThatThrownBy(() -> userManagementFacade.updateUserRole("promoted", AuthorityConstants.ADMIN))
            .isInstanceOf(AuthorizationDeniedException.class);

        assertThat(getAuthorityNames("promoted")).containsExactly(AuthorityConstants.USER);

        authenticateAs(AuthorityConstants.ADMIN);

        userManagementFacade.updateUserRole("promoted", AuthorityConstants.ADMIN);

        assertThat(getAuthorityNames("promoted")).containsExactly(AuthorityConstants.ADMIN);
    }

    private static void authenticateAs(String authority) {
        SecurityContextHolder.getContext()
            .setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                    "test-" + authority, null, List.of(new SimpleGrantedAuthority(authority))));
    }

    private List<String> getAuthorityNames(String login) {
        User user = userRepository.findByLogin(login)
            .orElseThrow();

        return user.getAuthorityIds()
            .stream()
            .map(authorityId -> authorityRepository.findById(authorityId)
                .map(Authority::getName)
                .orElseThrow())
            .toList();
    }

    private User saveUser(String login, String email) {
        Authority userAuthority = authorityRepository.findByName(AuthorityConstants.USER)
            .orElseThrow();

        User user = new User();

        user.setActivated(true);
        user.setAuthorities(Set.of(userAuthority));
        user.setEmail(email);
        user.setLangKey("en");
        user.setLogin(login);
        user.setPassword(RandomStringUtils.secure()
            .nextAlphanumeric(60));

        return userRepository.save(user);
    }

    @EnableMethodSecurity
    @Import(CacheConfiguration.class)
    @TestConfiguration
    static class UserManagementFacadeIntTestConfiguration {
    }
}
