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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("HARD_CODE_PASSWORD")
class UserServiceTest {

    private static final String ACTIVATION_KEY = "invitation-key";
    private static final String ENCODED_PASSWORD = "encoded-new-password";
    private static final String NEW_PASSWORD = "new-password";
    private static final String ORIGINAL_PASSWORD = "original-password-hash";
    private static final String RESET_KEY = "reset-key";

    private PasswordEncoder passwordEncoder;
    private UserRepository userRepository;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        CacheManager cacheManager = mock(CacheManager.class);

        when(cacheManager.getCache(anyString())).thenReturn(mock(Cache.class));

        passwordEncoder = mock(PasswordEncoder.class);

        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn(ENCODED_PASSWORD);

        userRepository = mock(UserRepository.class);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService = new UserServiceImpl(
            mock(AuthorityRepository.class), cacheManager, passwordEncoder, mock(PersistentTokenRepository.class),
            userRepository);
    }

    @Test
    void testActivateInvitationSetsThePasswordAndActivatesTheAccount() {
        User invitee = createInvitee(Instant.now()
            .minus(1, ChronoUnit.HOURS));

        when(userRepository.findByActivationKey(ACTIVATION_KEY)).thenReturn(Optional.of(invitee));

        Optional<User> activatedUser = userService.activateInvitation(ACTIVATION_KEY, NEW_PASSWORD);

        assertThat(activatedUser).containsSame(invitee);
        assertThat(invitee.isActivated()).isTrue();
        assertThat(invitee.getActivationKey()).isNull();
        assertThat(invitee.isInvited()).isFalse();
        assertThat(invitee.getPassword()).isEqualTo(ENCODED_PASSWORD);

        verify(userRepository).save(invitee);
    }

    @Test
    void testActivateInvitationRefusesAPendingSignUp() {
        User user = createInvitee(Instant.now()
            .minus(1, ChronoUnit.HOURS));

        user.setInvited(false);

        when(userRepository.findByActivationKey(ACTIVATION_KEY)).thenReturn(Optional.of(user));

        assertThat(userService.activateInvitation(ACTIVATION_KEY, NEW_PASSWORD)).isEmpty();
        assertThat(user.isActivated()).isFalse();
        assertThat(user.getPassword()).isEqualTo(ORIGINAL_PASSWORD);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testActivateRegistrationRefusesAnInvitationKey() {
        User invitee = createInvitee(Instant.now()
            .minus(1, ChronoUnit.HOURS));

        when(userRepository.findByActivationKey(ACTIVATION_KEY)).thenReturn(Optional.of(invitee));

        assertThat(userService.activateRegistration(ACTIVATION_KEY)).isEmpty();
        assertThat(invitee.isActivated()).isFalse();
        assertThat(invitee.getActivationKey()).isEqualTo(ACTIVATION_KEY);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testActivateRegistrationActivatesAPendingSignUp() {
        User user = createInvitee(Instant.now()
            .minus(1, ChronoUnit.HOURS));

        user.setInvited(false);

        when(userRepository.findByActivationKey(ACTIVATION_KEY)).thenReturn(Optional.of(user));

        assertThat(userService.activateRegistration(ACTIVATION_KEY)).containsSame(user);
        assertThat(user.isActivated()).isTrue();
        assertThat(user.getActivationKey()).isNull();
    }

    @Test
    void testExpiredInvitationIsRefusedAndTheAccountStaysInactive() {
        User invitee = createInvitee(Instant.now()
            .minus(4, ChronoUnit.DAYS));

        when(userRepository.findByActivationKey(ACTIVATION_KEY)).thenReturn(Optional.of(invitee));

        Optional<User> activatedUser = userService.activateInvitation(ACTIVATION_KEY, NEW_PASSWORD);

        assertThat(activatedUser).isEmpty();
        assertThat(invitee.isActivated()).isFalse();
        assertThat(invitee.getPassword()).isEqualTo(ORIGINAL_PASSWORD);

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUsedInvitationCannotBeUsedASecondTime() {
        User invitee = createInvitee(Instant.now()
            .minus(1, ChronoUnit.HOURS));

        when(userRepository.findByActivationKey(ACTIVATION_KEY)).thenAnswer(
            invocation -> ACTIVATION_KEY.equals(invitee.getActivationKey()) ? Optional.of(invitee) : Optional.empty());

        assertThat(userService.activateInvitation(ACTIVATION_KEY, NEW_PASSWORD)).isPresent();
        assertThat(userService.activateInvitation(ACTIVATION_KEY, "attacker-password")).isEmpty();
        assertThat(invitee.getPassword()).isEqualTo(ENCODED_PASSWORD);

        verify(passwordEncoder, never()).encode("attacker-password");
    }

    @Test
    void testExpiredResetKeyIsRefusedAndPasswordIsUnchanged() {
        User user = createUserWithResetKey(Instant.now()
            .minus(25, ChronoUnit.HOURS));

        when(userRepository.findByResetKey(RESET_KEY)).thenReturn(Optional.of(user));

        Optional<User> completedUser = userService.completePasswordReset(NEW_PASSWORD, RESET_KEY);

        assertThat(completedUser).isEmpty();
        assertThat(user.getPassword()).isEqualTo(ORIGINAL_PASSWORD);
        assertThat(user.getResetKey()).isEqualTo(RESET_KEY);

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testFreshResetKeySucceedsOnceAndClearsTheKey() {
        User user = createUserWithResetKey(Instant.now()
            .minus(1, ChronoUnit.HOURS));

        when(userRepository.findByResetKey(RESET_KEY)).thenReturn(Optional.of(user));

        Optional<User> completedUser = userService.completePasswordReset(NEW_PASSWORD, RESET_KEY);

        assertThat(completedUser).containsSame(user);
        assertThat(user.getPassword()).isEqualTo(ENCODED_PASSWORD);
        assertThat(user.getResetKey()).isNull();
        assertThat(user.getResetDate()).isNull();

        verify(userRepository).save(user);
    }

    @Test
    void testUsedResetKeyCannotBeUsedASecondTime() {
        User user = createUserWithResetKey(Instant.now()
            .minus(1, ChronoUnit.HOURS));

        when(userRepository.findByResetKey(RESET_KEY)).thenAnswer(
            invocation -> RESET_KEY.equals(user.getResetKey()) ? Optional.of(user) : Optional.empty());

        assertThat(userService.completePasswordReset(NEW_PASSWORD, RESET_KEY)).isPresent();
        assertThat(userService.completePasswordReset("attacker-password", RESET_KEY)).isEmpty();
        assertThat(user.getPassword()).isEqualTo(ENCODED_PASSWORD);

        verify(passwordEncoder, never()).encode("attacker-password");
    }

    private static User createInvitee(Instant createdDate) {
        User user = new User();

        user.setActivated(false);
        user.setActivationKey(ACTIVATION_KEY);
        user.setCreatedDate(createdDate);
        user.setEmail("invitee@example.com");
        user.setInvited(true);
        user.setLogin("invitee@example.com");
        user.setPassword(ORIGINAL_PASSWORD);

        return user;
    }

    private static User createUserWithResetKey(Instant resetDate) {
        User user = new User();

        user.setActivated(true);
        user.setEmail("member@example.com");
        user.setLogin("member@example.com");
        user.setPassword(ORIGINAL_PASSWORD);
        user.setResetDate(resetDate);
        user.setResetKey(RESET_KEY);

        return user;
    }
}
