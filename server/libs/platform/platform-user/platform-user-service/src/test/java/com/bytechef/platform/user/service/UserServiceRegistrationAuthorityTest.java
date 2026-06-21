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

package com.bytechef.platform.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.user.audit.UserAuditPublisher;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.repository.AuthorityRepository;
import com.bytechef.platform.user.repository.PersistentTokenRepository;
import com.bytechef.platform.user.repository.UserRepository;
import com.bytechef.tenant.service.TenantService;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Pins the bootstrap-admin policy on user provisioning (gecko T1): on self-registration, ADMIN is granted only for a
 * genuine bootstrap (multi-tenant registrant, or single-tenant first active user) and any later single-tenant
 * registration defaults to the non-privileged ROLE_USER; on social/OIDC provisioning, the new user receives exactly the
 * caller-supplied authority (the OAuth2/OIDC services now pass ROLE_USER).
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class UserServiceRegistrationAuthorityTest {

    private static final long ADMIN_AUTHORITY_ID = 1L;
    private static final long USER_AUTHORITY_ID = 2L;

    @Mock
    private AuthorityRepository authorityRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PersistentTokenRepository persistentTokenRepository;

    @Mock
    private TenantService tenantService;

    @Mock
    private UserAuditPublisher userAuditPublisher;

    @Mock
    private UserRepository userRepository;

    @Captor
    private ArgumentCaptor<User> userArgumentCaptor;

    private UserService userService;

    @BeforeEach
    void setUp() {
        CacheManager cacheManager = new ConcurrentMapCacheManager();

        userService = new UserServiceImpl(
            authorityRepository, cacheManager, passwordEncoder, persistentTokenRepository, tenantService,
            userAuditPublisher, userRepository, 5, Duration.ofMinutes(15));

        lenient()
            .when(userRepository.findByLogin(anyString()))
            .thenReturn(Optional.empty());
        lenient()
            .when(userRepository.findByEmailIgnoreCase(anyString()))
            .thenReturn(Optional.empty());
        lenient()
            .when(passwordEncoder.encode(anyString()))
            .thenReturn("encoded-password");
        lenient()
            .when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> {
                User user = invocation.getArgument(0);

                user.setId(99L);

                return user;
            });
        lenient()
            .when(authorityRepository.findByName(AuthorityConstants.ADMIN))
            .thenReturn(Optional.of(new Authority().id(ADMIN_AUTHORITY_ID)
                .name(AuthorityConstants.ADMIN)));
        lenient()
            .when(authorityRepository.findByName(AuthorityConstants.USER))
            .thenReturn(Optional.of(new Authority().id(USER_AUTHORITY_ID)
                .name(AuthorityConstants.USER)));
    }

    @Test
    void testFirstSingleTenantUserBecomesAdmin() {
        when(tenantService.isMultiTenantEnabled()).thenReturn(false);
        when(userRepository.countAllByActivatedIsTrue()).thenReturn(0L);

        userService.registerUser(newUserDTO("owner", "owner@example.com"), "Password1");

        assertThat(savedAuthorityIds()).containsExactly(ADMIN_AUTHORITY_ID);
    }

    @Test
    void testSubsequentSingleTenantUserBecomesUser() {
        when(tenantService.isMultiTenantEnabled()).thenReturn(false);
        when(userRepository.countAllByActivatedIsTrue()).thenReturn(1L);

        userService.registerUser(newUserDTO("second", "second@example.com"), "Password1");

        assertThat(savedAuthorityIds()).containsExactly(USER_AUTHORITY_ID);
    }

    @Test
    void testMultiTenantRegistrantBecomesAdmin() {
        when(tenantService.isMultiTenantEnabled()).thenReturn(true);

        userService.registerUser(newUserDTO("tenant-owner", "tenant-owner@example.com"), "Password1");

        assertThat(savedAuthorityIds()).containsExactly(ADMIN_AUTHORITY_ID);
    }

    @Test
    void testNewSocialUserGetsPassedAuthority() {
        when(userRepository.findByAuthProviderAndProviderId("GOOGLE", "provider-id")).thenReturn(Optional.empty());

        // The OAuth2/OIDC services pass ROLE_USER for federated logins; the new user must receive exactly that role,
        // not an escalated one.
        userService.findOrCreateSocialUser(
            "social@example.com", "First", "Last", null, "GOOGLE", "provider-id", true, AuthorityConstants.USER);

        assertThat(savedAuthorityIds()).containsExactly(USER_AUTHORITY_ID);
    }

    private List<Long> savedAuthorityIds() {
        verify(userRepository).save(userArgumentCaptor.capture());

        return userArgumentCaptor.getValue()
            .getAuthorityIds();
    }

    private static AdminUserDTO newUserDTO(String login, String email) {
        AdminUserDTO userDTO = new AdminUserDTO();

        userDTO.setLogin(login);
        userDTO.setEmail(email);
        userDTO.setFirstName("First");
        userDTO.setLastName("Last");
        userDTO.setLangKey("en");

        return userDTO;
    }
}
