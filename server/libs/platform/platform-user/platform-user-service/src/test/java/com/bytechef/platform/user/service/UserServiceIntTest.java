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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.bytechef.commons.util.LocalDateTimeUtils;
import com.bytechef.commons.util.RandomUtils;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.exception.UserNotActivatedException;
import com.bytechef.platform.user.config.UserIntTestConfiguration;
import com.bytechef.platform.user.domain.Authority;
import com.bytechef.platform.user.domain.PersistentToken;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.repository.AuthorityRepository;
import com.bytechef.platform.user.repository.PersistentTokenRepository;
import com.bytechef.platform.user.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link UserService}.
 */
@SpringBootTest(classes = UserIntTestConfiguration.class)
@Transactional
class UserServiceIntTest {

    private static final String ACTIVATION_KEY = "invitation-key";
    private static final String DEFAULT_LOGIN = "johndoe";
    private static final String DEFAULT_EMAIL = "johndoe@localhost";
    private static final String DEFAULT_FIRSTNAME = "john";
    private static final String DEFAULT_LASTNAME = "doe";
    private static final String DEFAULT_IMAGEURL = "http://placehold.it/50x50";
    private static final String DEFAULT_LANGKEY = "dummy";
    private static final String NEW_PASSWORD = "new-password";

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private PersistentTokenRepository persistentTokenRepository;

    @MockitoSpyBean
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @MockitoBean
    private DateTimeProvider dateTimeProvider;

    private User user;

    @BeforeEach
    public void init() {
        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);

            if (cache != null) {
                cache.clear();
            }
        }

        persistentTokenRepository.deleteAll();

        user = new User();

        user.setLogin(DEFAULT_LOGIN);
        user.setPassword(RandomStringUtils.randomAlphanumeric(60));
        user.setActivated(true);
        user.setEmail(DEFAULT_EMAIL);
        user.setFirstName(DEFAULT_FIRSTNAME);
        user.setLastName(DEFAULT_LASTNAME);
        user.setImageUrl(DEFAULT_IMAGEURL);
        user.setLangKey(DEFAULT_LANGKEY);

        when(dateTimeProvider.getNow()).thenReturn(Optional.of(LocalDateTime.now()));
    }

    @Test
    @Transactional
    void testClearActivationKeyClaimsAKeyOnlyOnce() {
        user.setActivated(false);
        user.setActivationKey("invitation-key");
        user.setInvited(true);

        User savedUser = userRepository.save(user);

        long userId = savedUser.getId();

        assertThat(userRepository.clearActivationKey(userId, "invitation-key")).isEqualTo(1);
        assertThat(userRepository.clearActivationKey(userId, "invitation-key")).isZero();
        assertThat(userRepository.findByActivationKey("invitation-key")).isEmpty();
    }

    @Test
    @Transactional
    void testRemoveOldPersistentTokens() {
        userRepository.save(user);

        List<PersistentToken> persistentTokens = persistentTokenRepository.findAllByUserId(user.getId());

        int existingCount = persistentTokens.size();

        LocalDate today = LocalDate.now();

        generateUserToken(user, "1111-1111", today);
        generateUserToken(user, "2222-2222", today.minusDays(32));

        persistentTokens = persistentTokenRepository.findAllByUserId(user.getId());

        assertThat(persistentTokens).hasSize(existingCount + 2);

        userService.removeOldPersistentTokens();

        persistentTokens = persistentTokenRepository.findAllByUserId(user.getId());

        assertThat(persistentTokens).hasSize(existingCount + 1);
    }

    @Test
    @Transactional
    void assertThatUserMustExistToResetPassword() {
        userRepository.save(user);

        Optional<User> maybeUser = userService.requestPasswordReset("invalid.login@localhost");

        assertThat(maybeUser).isNotPresent();

        maybeUser = userService.requestPasswordReset(user.getEmail());

        assertThat(maybeUser).isPresent();
        assertThat(
            maybeUser.orElse(null)
                .getEmail()).isEqualTo(user.getEmail());
        assertThat(
            maybeUser.orElse(null)
                .getResetDate()).isNotNull();
        assertThat(
            maybeUser.orElse(null)
                .getResetKey()).isNotNull();
    }

    @Test
    @Transactional
    void assertThatOnlyActivatedUserCanRequestPasswordReset() {
        user.setActivated(false);

        userRepository.save(user);

        Optional<User> maybeUser = userService.requestPasswordReset(user.getLogin());

        assertThat(maybeUser).isNotPresent();

        userRepository.delete(user);
    }

    @Test
    @Transactional
    void assertThatResetKeyMustNotBeOlderThan24Hours() {
        Instant daysAgo = Instant.now()
            .minus(25, ChronoUnit.HOURS);

        String resetKey = RandomUtils.generateResetKey();

        user.setActivated(true);
        user.setResetDate(daysAgo);
        user.setResetKey(resetKey);

        userRepository.save(user);

        Optional<User> maybeUser = userService.completePasswordReset("johndoe2", user.getResetKey());

        assertThat(maybeUser).isNotPresent();

        userRepository.delete(user);
    }

    @Test
    @Transactional
    void assertThatResetKeyMustBeValid() {
        Instant daysAgo = Instant.now()
            .minus(25, ChronoUnit.HOURS);

        user.setActivated(true);
        user.setResetDate(daysAgo);
        user.setResetKey("1234");

        userRepository.save(user);

        Optional<User> maybeUser = userService.completePasswordReset("johndoe2", user.getResetKey());

        assertThat(maybeUser).isNotPresent();

        userRepository.delete(user);
    }

    @Test
    @Transactional
    void assertThatUserCanResetPassword() {
        String oldPassword = user.getPassword();
        Instant daysAgo = Instant.now()
            .minus(2, ChronoUnit.HOURS);
        String resetKey = RandomUtils.generateResetKey();

        user.setActivated(true);
        user.setResetDate(daysAgo);
        user.setResetKey(resetKey);

        userRepository.save(user);

        Optional<User> maybeUser = userService.completePasswordReset("johndoe2", user.getResetKey());

        assertThat(
            maybeUser).isPresent();
        assertThat(
            maybeUser.orElse(null)
                .getResetDate()).isNull();
        assertThat(
            maybeUser.orElse(null)
                .getResetKey()).isNull();
        assertThat(
            maybeUser.orElse(null)
                .getPassword()).isNotEqualTo(oldPassword);

        userRepository.delete(user);
    }

    @Test
    @Transactional
    void assertThatNotActivatedUsersWithNotNullActivationKeyCreatedBefore3DaysAreDeleted() {
        Instant now = Instant.now();

        when(dateTimeProvider.getNow()).thenReturn(Optional.of(now.minus(4, ChronoUnit.DAYS)));

        user.setActivated(false);
        user.setActivationKey(RandomStringUtils.random(20));

        User dbUser = userRepository.save(user);

        dbUser.setCreatedDate(now.minus(4, ChronoUnit.DAYS));

        userRepository.save(user);

        Instant threeDaysAgo = now.minus(3, ChronoUnit.DAYS);

        List<User> users = userRepository.findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(
            LocalDateTimeUtils.toLocalDateTime(threeDaysAgo));

        assertThat(users).isNotEmpty();

        userService.removeNotActivatedUsers();

        users = userRepository.findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(
            LocalDateTimeUtils.toLocalDateTime(threeDaysAgo));

        assertThat(users).isEmpty();
    }

    @Test
    @Transactional
    void assertThatNotActivatedUsersWithNullActivationKeyCreatedBefore3DaysAreNotDeleted() {
        Instant now = Instant.now();

        when(dateTimeProvider.getNow()).thenReturn(Optional.of(now.minus(4, ChronoUnit.DAYS)));

        user.setActivated(false);

        User dbUser = userRepository.save(user);

        dbUser.setCreatedDate(now.minus(4, ChronoUnit.DAYS));

        userRepository.save(user);

        Instant threeDaysAgo = now.minus(3, ChronoUnit.DAYS);

        List<User> users = userRepository.findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(
            LocalDateTimeUtils.toLocalDateTime(threeDaysAgo));

        assertThat(users).isEmpty();

        userService.removeNotActivatedUsers();

        Optional<User> maybeDbUser = userRepository.findById(dbUser.getId());

        assertThat(maybeDbUser).contains(dbUser);
    }

    @Test
    @Transactional
    void testActivateInvitationSetsThePasswordAndActivatesTheAccount() {
        long inviteeId = saveInvitee(Instant.now()
            .minus(1, ChronoUnit.HOURS), true);

        assertThat(userService.activateInvitation(ACTIVATION_KEY, NEW_PASSWORD)).isPresent();

        User invitee = userRepository.findById(inviteeId)
            .orElseThrow();

        assertThat(invitee.isActivated()).isTrue();
        assertThat(invitee.getActivationKey()).isNull();
        assertThat(invitee.isInvited()).isFalse();
        assertThat(passwordEncoder.matches(NEW_PASSWORD, invitee.getPassword())).isTrue();
    }

    @Test
    @Transactional
    void testActivateInvitationRefusesAKeyAnotherRequestClaimedFirst() {
        long inviteeId = saveInvitee(Instant.now()
            .minus(1, ChronoUnit.HOURS), true);

        String originalPassword = user.getPassword();

        doReturn(0).when(userRepository)
            .clearActivationKey(anyLong(), anyString());

        assertThat(userService.activateInvitation(ACTIVATION_KEY, NEW_PASSWORD)).isEmpty();

        User invitee = userRepository.findById(inviteeId)
            .orElseThrow();

        assertThat(invitee.isActivated()).isFalse();
        assertThat(invitee.getPassword()).isEqualTo(originalPassword);
    }

    @Test
    @Transactional
    void testActivateInvitationRefusesAPendingSignUp() {
        long userId = saveInvitee(Instant.now()
            .minus(1, ChronoUnit.HOURS), false);

        String originalPassword = user.getPassword();

        assertThat(userService.activateInvitation(ACTIVATION_KEY, NEW_PASSWORD)).isEmpty();

        User pendingUser = userRepository.findById(userId)
            .orElseThrow();

        assertThat(pendingUser.isActivated()).isFalse();
        assertThat(pendingUser.getActivationKey()).isEqualTo(ACTIVATION_KEY);
        assertThat(pendingUser.getPassword()).isEqualTo(originalPassword);
    }

    @Test
    @Transactional
    void testActivateRegistrationRefusesAnInvitationKey() {
        long inviteeId = saveInvitee(Instant.now()
            .minus(1, ChronoUnit.HOURS), true);

        assertThat(userService.activateRegistration(ACTIVATION_KEY)).isEmpty();

        User invitee = userRepository.findById(inviteeId)
            .orElseThrow();

        assertThat(invitee.isActivated()).isFalse();
        assertThat(invitee.getActivationKey()).isEqualTo(ACTIVATION_KEY);
    }

    @Test
    @Transactional
    void testActivateRegistrationActivatesAPendingSignUp() {
        long userId = saveInvitee(Instant.now()
            .minus(1, ChronoUnit.HOURS), false);

        assertThat(userService.activateRegistration(ACTIVATION_KEY)).isPresent();

        User activatedUser = userRepository.findById(userId)
            .orElseThrow();

        assertThat(activatedUser.isActivated()).isTrue();
        assertThat(activatedUser.getActivationKey()).isNull();
    }

    @Test
    @Transactional
    void testExpiredInvitationIsRefusedAndTheAccountStaysInactive() {
        long inviteeId = saveInvitee(Instant.now()
            .minus(4, ChronoUnit.DAYS), true);

        String originalPassword = user.getPassword();

        assertThat(userService.activateInvitation(ACTIVATION_KEY, NEW_PASSWORD)).isEmpty();

        User invitee = userRepository.findById(inviteeId)
            .orElseThrow();

        assertThat(invitee.isActivated()).isFalse();
        assertThat(invitee.getActivationKey()).isEqualTo(ACTIVATION_KEY);
        assertThat(invitee.getPassword()).isEqualTo(originalPassword);
    }

    @Test
    @Transactional
    void testUsedInvitationCannotBeUsedASecondTime() {
        long inviteeId = saveInvitee(Instant.now()
            .minus(1, ChronoUnit.HOURS), true);

        assertThat(userService.activateInvitation(ACTIVATION_KEY, NEW_PASSWORD)).isPresent();
        assertThat(userService.activateInvitation(ACTIVATION_KEY, "attacker-password")).isEmpty();

        User invitee = userRepository.findById(inviteeId)
            .orElseThrow();

        assertThat(passwordEncoder.matches(NEW_PASSWORD, invitee.getPassword())).isTrue();
    }

    @Test
    @Transactional
    void testUsedResetKeyCannotBeUsedASecondTime() {
        String resetKey = RandomUtils.generateResetKey();

        user.setResetDate(Instant.now()
            .minus(1, ChronoUnit.HOURS));
        user.setResetKey(resetKey);

        User savedUser = userRepository.save(user);

        long userId = Objects.requireNonNull(savedUser.getId());

        assertThat(userService.completePasswordReset(NEW_PASSWORD, resetKey)).isPresent();
        assertThat(userService.completePasswordReset("attacker-password", resetKey)).isEmpty();

        User resetUser = userRepository.findById(userId)
            .orElseThrow();

        assertThat(passwordEncoder.matches(NEW_PASSWORD, resetUser.getPassword())).isTrue();
    }

    private long saveInvitee(Instant createdDate, boolean invited) {
        when(dateTimeProvider.getNow()).thenReturn(
            Optional.of(LocalDateTime.ofInstant(createdDate, ZoneId.systemDefault())));

        user.setActivated(false);
        user.setActivationKey(ACTIVATION_KEY);
        user.setInvited(invited);

        User savedUser = userRepository.save(user);

        return Objects.requireNonNull(savedUser.getId());
    }

    /**
     * {@code UserRepository.findAllByAuthorityName} is hand-written SQL over three tables, and it runs on every load of
     * the workspace members page to project tenant admins as inherited workspace admins. Both of its consumers mock the
     * repository, so before this test the statement had never executed against a database at all — a wrong join, a
     * mistyped column or a missing {@code activated} predicate would have surfaced only in production.
     */
    @Test
    @Transactional
    void testGetUsersByAuthorityNameReturnsOnlyActivatedAdmins() {
        long adminAuthorityId = authorityId(AuthorityConstants.ADMIN);
        long userAuthorityId = authorityId(AuthorityConstants.USER);

        User activatedAdmin = saveUser("activatedadmin", true, adminAuthorityId);
        User deactivatedAdmin = saveUser("deactivatedadmin", false, adminAuthorityId);
        User activatedMember = saveUser("activatedmember", true, userAuthorityId);

        List<User> admins = userService.getUsersByAuthorityName(AuthorityConstants.ADMIN);

        // The deactivated admin is the case the SQL's `u.activated = true` exists for: showing them as an inherited
        // workspace admin would advertise access nobody has. The ROLE_USER row proves the authority join filters at
        // all.
        assertThat(admins).extracting(User::getId)
            .contains(activatedAdmin.getId())
            .doesNotContain(deactivatedAdmin.getId(), activatedMember.getId());
    }

    private long authorityId(String authorityName) {
        Authority authority = authorityRepository.findByName(authorityName)
            .orElseThrow();

        return authority.getId();
    }

    private User saveUser(String login, boolean activated, long authorityId) {
        User newUser = new User();

        newUser.setActivated(activated);
        newUser.setAuthorityIds(List.of(authorityId));
        newUser.setEmail(login + "@localhost");
        newUser.setFirstName(DEFAULT_FIRSTNAME);
        newUser.setLangKey(DEFAULT_LANGKEY);
        newUser.setLastName(DEFAULT_LASTNAME);
        newUser.setLogin(login);
        newUser.setPassword(RandomStringUtils.randomAlphanumeric(60));

        return userRepository.save(newUser);
    }

    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    private void generateUserToken(User user, String tokenSeries, LocalDate localDate) {
        PersistentToken token = new PersistentToken();

        token.setNew(true);
        token.setSeries(tokenSeries);
        token.setUser(user);
        token.setTokenValue(tokenSeries + "-data");
        token.setTokenDate(localDate);
        token.setIpAddress("127.0.0.1");
        token.setUserAgent("Test agent");

        persistentTokenRepository.save(token);
    }

    @Test
    @Transactional
    void testFindOrCreateSocialUserAcceptsPendingInvitation() {
        user.setActivated(false);
        user.setActivationKey(RandomUtils.generateActivationKey());

        userRepository.save(user);

        User socialUser = userService.findOrCreateSocialUser(
            DEFAULT_EMAIL, "John", "Doe", DEFAULT_IMAGEURL, "GOOGLE", "google-subject", false, "ROLE_ADMIN");

        assertThat(socialUser.isActivated()).isTrue();
        assertThat(socialUser.getActivationKey()).isNull();
        assertThat(socialUser.getAuthProvider()).isEqualTo("GOOGLE");
    }

    @Test
    @Transactional
    void testFindOrCreateSocialUserRefusesDeactivatedUserMatchedByEmail() {
        user.setActivated(false);
        user.setActivationKey(null);

        userRepository.save(user);

        assertThatThrownBy(
            () -> userService.findOrCreateSocialUser(
                DEFAULT_EMAIL, "John", "Doe", DEFAULT_IMAGEURL, "GOOGLE", "google-subject", false, "ROLE_ADMIN"))
                    .isInstanceOf(UserNotActivatedException.class);

        assertThat(userRepository.findByLogin(DEFAULT_LOGIN))
            .hasValueSatisfying(storedUser -> assertThat(storedUser.isActivated()).isFalse());
    }

    @Test
    @Transactional
    void testFindOrCreateSocialUserRefusesDeactivatedUserMatchedByProviderId() {
        user.setActivated(false);
        user.setActivationKey(null);
        user.setAuthProvider("GITHUB");
        user.setProviderId("4242");

        userRepository.save(user);

        assertThatThrownBy(
            () -> userService.findOrCreateSocialUser(
                "other@localhost", "John", "Doe", DEFAULT_IMAGEURL, "GITHUB", "4242", false, "ROLE_ADMIN"))
                    .isInstanceOf(UserNotActivatedException.class);
    }

    @Test
    @Transactional
    void testFindOrCreateSocialUserRefusesUnknownEmailWithoutProvisioning() {
        assertThatThrownBy(
            () -> userService.findOrCreateSocialUser(
                "stranger@localhost", "Str", "Anger", null, "GOOGLE", "stranger-subject", false, "ROLE_ADMIN"))
                    .isInstanceOf(IllegalStateException.class);

        assertThat(userRepository.findByEmailIgnoreCase("stranger@localhost")).isEmpty();
    }

    @Test
    @Transactional
    void testFindOrCreateSocialUserProvisionsUnknownEmail() {
        User socialUser = userService.findOrCreateSocialUser(
            "Newcomer@Localhost", "New", "Comer", null, "GOOGLE", "newcomer-subject", true, "ROLE_ADMIN");

        assertThat(socialUser.getLogin()).isEqualTo("newcomer@localhost");
        assertThat(socialUser.isActivated()).isTrue();
        assertThat(userRepository.findByLogin("newcomer@localhost")).isPresent();
    }
}
