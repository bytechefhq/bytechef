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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.repository.AuthorityRepository;
import com.bytechef.platform.user.repository.PersistentTokenRepository;
import com.bytechef.platform.user.repository.UserRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Covers the claim key an invitee redeems: invitations reuse the password-reset key, so its expiry and single use are
 * what keep a leaked or stale invitation link from setting someone's password.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("HARD_CODE_PASSWORD")
class UserServiceClaimKeyTest {

    private static final String CLAIM_KEY = "claim-key-1051";
    private static final String ENCODED_PASSWORD = "encoded-new-password";
    private static final String NEW_PASSWORD = "new-password";
    private static final String ORIGINAL_PASSWORD = "original-password-hash";

    private PasswordEncoder passwordEncoder;
    private UserRepository userRepository;
    private UserServiceImpl userService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        CacheManager cacheManager = mock(CacheManager.class);

        when(cacheManager.getCache(anyString())).thenReturn(mock(Cache.class));

        passwordEncoder = mock(PasswordEncoder.class);

        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

        userRepository = mock(UserRepository.class);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService = new UserServiceImpl(
            mock(AuthorityRepository.class), cacheManager, passwordEncoder, mock(PersistentTokenRepository.class),
            userRepository, mock(ObjectProvider.class));
    }

    @Test
    void testExpiredClaimKeyIsRefusedAndPasswordIsUnchanged() {
        User invitee = createInvitee(Instant.now()
            .minus(25, ChronoUnit.HOURS));

        when(userRepository.findByResetKey(CLAIM_KEY)).thenReturn(Optional.of(invitee));

        Optional<User> completedUser = userService.completePasswordReset(NEW_PASSWORD, CLAIM_KEY);

        assertThat(completedUser).isEmpty();
        assertThat(invitee.getPassword()).isEqualTo(ORIGINAL_PASSWORD);
        assertThat(invitee.getResetKey()).isEqualTo(CLAIM_KEY);

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testFreshClaimKeySucceedsOnceAndClearsTheKey() {
        User invitee = createInvitee(Instant.now()
            .minus(1, ChronoUnit.HOURS));

        when(userRepository.findByResetKey(CLAIM_KEY)).thenReturn(Optional.of(invitee));

        Optional<User> completedUser = userService.completePasswordReset(NEW_PASSWORD, CLAIM_KEY);

        assertThat(completedUser).containsSame(invitee);
        assertThat(invitee.getPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(invitee.getResetKey()).isNull();
        assertThat(invitee.getResetDate()).isNull();

        verify(userRepository).save(invitee);
    }

    @Test
    void testUsedClaimKeyCannotBeUsedASecondTime() {
        User invitee = createInvitee(Instant.now()
            .minus(1, ChronoUnit.HOURS));

        // The repository finds the user by the key only while the key is still stored on it.
        when(userRepository.findByResetKey(CLAIM_KEY)).thenAnswer(
            invocation -> CLAIM_KEY.equals(invitee.getResetKey()) ? Optional.of(invitee) : Optional.empty());

        Optional<User> firstCompletion = userService.completePasswordReset(NEW_PASSWORD, CLAIM_KEY);

        assertThat(firstCompletion).isPresent();

        Optional<User> secondCompletion = userService.completePasswordReset("attacker-password", CLAIM_KEY);

        assertThat(secondCompletion).isEmpty();
        assertThat(invitee.getPassword()).isEqualTo(ENCODED_PASSWORD);

        verify(passwordEncoder, never()).encode("attacker-password");
    }

    private static User createInvitee(Instant resetDate) {
        User user = new User();

        user.setEmail("invitee@example.com");
        user.setLogin("invitee@example.com");
        user.setPassword(ORIGINAL_PASSWORD);
        user.setResetDate(resetDate);
        user.setResetKey(CLAIM_KEY);

        return user;
    }
}
