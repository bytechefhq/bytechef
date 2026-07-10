/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.facade;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.exception.EmailAlreadyUsedException;
import com.bytechef.platform.user.exception.InvalidPasswordException;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.service.TenantService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit test for the {@link UserManagementFacadeImpl} invite orchestration that previously lived in
 * {@code UserGraphQlController}: password validation and email-collision handling run before the user is registered.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class UserManagementFacadeImplTest {

    private AuthorityService authorityService;
    private MailService mailService;
    private TenantService tenantService;
    private UserService userService;
    private UserManagementFacadeImpl userManagementFacade;

    @BeforeEach
    void setUp() {
        authorityService = Mockito.mock(AuthorityService.class);
        mailService = Mockito.mock(MailService.class);
        tenantService = Mockito.mock(TenantService.class);
        userService = Mockito.mock(UserService.class);

        userManagementFacade = new UserManagementFacadeImpl(
            authorityService, mailService, tenantService, userService);
    }

    @Test
    void testInviteUserSendsInvitationEmail() {
        Authority authority = createAuthority("ROLE_USER");
        User user = createUser("newuser", "newuser@example.com");

        when(tenantService.isMultiTenantEnabled()).thenReturn(false);
        when(userService.fetchUserByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(userService.registerUser(any(), anyString())).thenReturn(user);
        when(authorityService.getAuthorities()).thenReturn(List.of(authority));

        userManagementFacade.inviteUser("newuser@example.com", "Password123", "ROLE_USER");

        verify(mailService).sendInvitationEmail(any(), anyString());
    }

    @Test
    void testInviteUserWithPasswordTooShortDoesNotRegister() {
        when(tenantService.isMultiTenantEnabled()).thenReturn(false);
        when(userService.fetchUserByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userManagementFacade.inviteUser("test@example.com", "Pass1", "ROLE_USER"))
            .isInstanceOf(InvalidPasswordException.class);

        verify(userService, never()).registerUser(any(), anyString());
    }

    @Test
    void testInviteUserWithPasswordMissingUppercaseDoesNotRegister() {
        when(tenantService.isMultiTenantEnabled()).thenReturn(false);
        when(userService.fetchUserByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userManagementFacade.inviteUser("test@example.com", "password123", "ROLE_USER"))
            .isInstanceOf(InvalidPasswordException.class);

        verify(userService, never()).registerUser(any(), anyString());
    }

    @Test
    void testInviteUserWithPasswordMissingDigitDoesNotRegister() {
        when(tenantService.isMultiTenantEnabled()).thenReturn(false);
        when(userService.fetchUserByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userManagementFacade.inviteUser("test@example.com", "PasswordABC", "ROLE_USER"))
            .isInstanceOf(InvalidPasswordException.class);

        verify(userService, never()).registerUser(any(), anyString());
    }

    @Test
    void testInviteUserWithExistingEmailThrows() {
        when(tenantService.isMultiTenantEnabled()).thenReturn(false);
        when(userService.fetchUserByEmail("existing@example.com"))
            .thenReturn(Optional.of(createUser("existing", "existing@example.com")));

        assertThatThrownBy(() -> userManagementFacade.inviteUser("existing@example.com", "Password123", "ROLE_USER"))
            .isInstanceOf(EmailAlreadyUsedException.class);

        verify(userService, never()).registerUser(any(), anyString());
    }

    private Authority createAuthority(String name) {
        Authority authority = new Authority();

        authority.setName(name);

        return authority;
    }

    private User createUser(String login, String email) {
        User user = new User();

        user.setLogin(login);
        user.setEmail(email);

        return user;
    }
}
