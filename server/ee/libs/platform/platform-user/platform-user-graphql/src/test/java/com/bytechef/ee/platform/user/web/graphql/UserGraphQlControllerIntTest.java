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

package com.bytechef.ee.platform.user.web.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.user.web.graphql.config.PlatformUserGraphQlConfigurationSharedMocks;
import com.bytechef.ee.platform.user.web.graphql.config.PlatformUserGraphQlTestConfiguration;
import com.bytechef.platform.mail.MailService;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.service.AuthorityService;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.service.TenantService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    PlatformUserGraphQlTestConfiguration.class,
    UserGraphQlController.class
})
@GraphQlTest(
    controllers = UserGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "bytechef.edition=ee",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@PlatformUserGraphQlConfigurationSharedMocks
public class UserGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private AuthorityService authorityService;

    @Autowired
    private MailService mailService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private UserService userService;

    @Test
    void testDeleteUser() {
        // Given
        doNothing().when(userService)
            .delete(anyString());

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    deleteUser(login: "testuser")
                }
                """)
            .execute()
            .path("deleteUser")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(userService).delete("testuser");
    }

    @Test
    void testGetUser() {
        // Given
        Authority authority = createMockAuthority(1L, "ROLE_ADMIN");
        User mockUser = createMockUser(1L, "john", "john@example.com");

        mockUser.setAuthorities(Set.of(authority));

        when(authorityService.getAuthorities()).thenReturn(List.of(authority));
        when(userService.fetchUserByLogin("john")).thenReturn(Optional.of(mockUser));

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    user(login: "john") {
                        id
                        login
                        email
                        authorities
                    }
                }
                """)
            .execute()
            .path("user.login")
            .entity(String.class)
            .isEqualTo("john")
            .path("user.email")
            .entity(String.class)
            .isEqualTo("john@example.com")
            .path("user.authorities")
            .entityList(String.class)
            .hasSize(1)
            .contains("ROLE_ADMIN");
    }

    @Test
    void testGetUserNotFound() {
        // Given
        Authority authority = createMockAuthority(1L, "ROLE_ADMIN");

        when(authorityService.getAuthorities()).thenReturn(List.of(authority));
        when(userService.fetchUserByLogin("unknown")).thenReturn(Optional.empty());

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    user(login: "unknown") {
                        id
                        login
                    }
                }
                """)
            .execute()
            .path("user")
            .valueIsNull();
    }

    @Test
    void testGetUsers() {
        // Given
        Authority authority = createMockAuthority(1L, "ROLE_USER");
        User user1 = createMockUser(1L, "user1", "user1@example.com");
        User user2 = createMockUser(2L, "user2", "user2@example.com");

        user1.setAuthorities(Set.of(authority));
        user2.setAuthorities(Set.of(authority));

        Page<User> userPage = new PageImpl<>(List.of(user1, user2), PageRequest.of(0, 20), 2);

        when(authorityService.getAuthorities()).thenReturn(List.of(authority));
        when(userService.getAllManagedUsers(any(PageRequest.class))).thenReturn(userPage);

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    users {
                        content {
                            id
                            login
                            email
                        }
                        totalElements
                        totalPages
                        number
                        size
                    }
                }
                """)
            .execute()
            .path("users.content")
            .entityList(Object.class)
            .hasSize(2)
            .path("users.content[0].login")
            .entity(String.class)
            .isEqualTo("user1")
            .path("users.content[1].login")
            .entity(String.class)
            .isEqualTo("user2")
            .path("users.totalElements")
            .entity(Integer.class)
            .isEqualTo(2)
            .path("users.totalPages")
            .entity(Integer.class)
            .isEqualTo(1)
            .path("users.number")
            .entity(Integer.class)
            .isEqualTo(0)
            .path("users.size")
            .entity(Integer.class)
            .isEqualTo(20);
    }

    @Test
    void testGetUsersWithPagination() {
        // Given
        Authority authority = createMockAuthority(1L, "ROLE_USER");
        User user1 = createMockUser(3L, "user3", "user3@example.com");

        user1.setAuthorities(Set.of(authority));

        Page<User> userPage = new PageImpl<>(List.of(user1), PageRequest.of(1, 10), 25);

        when(authorityService.getAuthorities()).thenReturn(List.of(authority));
        when(userService.getAllManagedUsers(PageRequest.of(1, 10))).thenReturn(userPage);

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    users(pageNumber: 1, pageSize: 10) {
                        content {
                            login
                        }
                        totalElements
                        totalPages
                        number
                        size
                    }
                }
                """)
            .execute()
            .path("users.content")
            .entityList(Object.class)
            .hasSize(1)
            .path("users.content[0].login")
            .entity(String.class)
            .isEqualTo("user3")
            .path("users.totalElements")
            .entity(Integer.class)
            .isEqualTo(25)
            .path("users.totalPages")
            .entity(Integer.class)
            .isEqualTo(3)
            .path("users.number")
            .entity(Integer.class)
            .isEqualTo(1)
            .path("users.size")
            .entity(Integer.class)
            .isEqualTo(10);

        verify(userService).getAllManagedUsers(PageRequest.of(1, 10));
    }

    @Test
    void testGetUsersEmpty() {
        // Given
        Authority authority = createMockAuthority(1L, "ROLE_USER");
        Page<User> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        when(authorityService.getAuthorities()).thenReturn(List.of(authority));
        when(userService.getAllManagedUsers(any(PageRequest.class))).thenReturn(emptyPage);

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    users {
                        content {
                            login
                        }
                        totalElements
                        totalPages
                    }
                }
                """)
            .execute()
            .path("users.content")
            .entityList(Object.class)
            .hasSize(0)
            .path("users.totalElements")
            .entity(Integer.class)
            .isEqualTo(0)
            .path("users.totalPages")
            .entity(Integer.class)
            .isEqualTo(0);
    }

    @Test
    void testInviteUser() {
        // Given
        Authority authority = createMockAuthority(1L, "ROLE_USER");
        User mockUser = createMockUser(1L, "newuser", "newuser@example.com");

        when(tenantService.isMultiTenantEnabled()).thenReturn(false);
        when(userService.fetchUserByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(userService.registerUser(any(), anyString())).thenReturn(mockUser);
        when(authorityService.getAuthorities()).thenReturn(List.of(authority));
        doNothing().when(userService)
            .save(any());
        doNothing().when(mailService)
            .sendInvitationEmail(any(), anyString());

        // When & Then - password meets requirements: 8+ chars, uppercase, digit
        this.graphQlTester
            .document("""
                mutation {
                    inviteUser(email: "newuser@example.com", password: "Password123", role: "ROLE_USER")
                }
                """)
            .execute()
            .path("inviteUser")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(mailService).sendInvitationEmail(any(), anyString());
    }

    @Test
    void testInviteUserWithPasswordTooShort() {
        // Given
        when(tenantService.isMultiTenantEnabled()).thenReturn(false);
        when(userService.fetchUserByEmail("test@example.com")).thenReturn(Optional.empty());

        // When & Then - password too short (less than 8 characters)
        this.graphQlTester
            .document("""
                mutation {
                    inviteUser(email: "test@example.com", password: "Pass1", role: "ROLE_USER")
                }
                """)
            .execute()
            .errors()
            .expect(error -> true)
            .verify();

        // Verify registerUser was never called due to password validation failure
        verify(userService, never()).registerUser(any(), anyString());
    }

    @Test
    void testInviteUserWithPasswordMissingUppercase() {
        // Given
        when(tenantService.isMultiTenantEnabled()).thenReturn(false);
        when(userService.fetchUserByEmail("test@example.com")).thenReturn(Optional.empty());

        // When & Then - password missing uppercase letter
        this.graphQlTester
            .document("""
                mutation {
                    inviteUser(email: "test@example.com", password: "password123", role: "ROLE_USER")
                }
                """)
            .execute()
            .errors()
            .expect(error -> true)
            .verify();

        // Verify registerUser was never called due to password validation failure
        verify(userService, never()).registerUser(any(), anyString());
    }

    @Test
    void testInviteUserWithPasswordMissingDigit() {
        // Given
        when(tenantService.isMultiTenantEnabled()).thenReturn(false);
        when(userService.fetchUserByEmail("test@example.com")).thenReturn(Optional.empty());

        // When & Then - password missing digit
        this.graphQlTester
            .document("""
                mutation {
                    inviteUser(email: "test@example.com", password: "PasswordABC", role: "ROLE_USER")
                }
                """)
            .execute()
            .errors()
            .expect(error -> true)
            .verify();

        // Verify registerUser was never called due to password validation failure
        verify(userService, never()).registerUser(any(), anyString());
    }

    @Test
    void testUpdateUser() {
        // Given
        Authority adminAuthority = createMockAuthority(1L, "ROLE_ADMIN");
        Authority userAuthority = createMockAuthority(2L, "ROLE_USER");
        User mockUser = createMockUser(1L, "john", "john@example.com");

        mockUser.setAuthorities(Set.of(userAuthority));

        when(authorityService.getAuthorities()).thenReturn(List.of(adminAuthority, userAuthority));
        when(userService.fetchUserByLogin("john")).thenReturn(Optional.of(mockUser));
        when(userService.update(any())).thenReturn(Optional.of(mockUser));

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    updateUser(login: "john", role: "ROLE_ADMIN") {
                        id
                        login
                        email
                    }
                }
                """)
            .execute()
            .path("updateUser.login")
            .entity(String.class)
            .isEqualTo("john")
            .path("updateUser.email")
            .entity(String.class)
            .isEqualTo("john@example.com");
    }

    private User createMockUser(Long id, String login, String email) {
        User user = new User();

        user.setId(id);
        user.setLogin(login);
        user.setEmail(email);
        user.setActivated(true);

        return user;
    }

    private Authority createMockAuthority(Long id, String name) {
        Authority authority = new Authority();

        authority.setId(id);
        authority.setName(name);

        return authority;
    }
}
