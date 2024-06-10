/*
 * Copyright 2023-present ByteChef Inc.
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
import static org.mockito.Mockito.when;

import com.bytechef.platform.user.config.UserIntTestConfiguration;
import com.bytechef.platform.user.domain.PersistentToken;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.repository.PersistentTokenRepository;
import com.bytechef.platform.user.repository.UserRepository;
import com.bytechef.platform.user.util.RandomUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link UserService}.
 */
@SpringBootTest(classes = UserIntTestConfiguration.class)
@Transactional
class UserServiceIntTest {

    private static final String DEFAULT_LOGIN = "johndoe";
    private static final String DEFAULT_EMAIL = "johndoe@localhost";
    private static final String DEFAULT_FIRSTNAME = "john";
    private static final String DEFAULT_LASTNAME = "doe";
    private static final String DEFAULT_IMAGEURL = "http://placehold.it/50x50";
    private static final String DEFAULT_LANGKEY = "dummy";

    @Autowired
    private PersistentTokenRepository persistentTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @MockBean
    private DateTimeProvider dateTimeProvider;

    private User user;

    @BeforeEach
    public void init() {
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

        dbUser.setCreatedDate(LocalDateTime.ofInstant(now.minus(4, ChronoUnit.DAYS), ZoneOffset.UTC));

        userRepository.save(user);

        Instant threeDaysAgo = now.minus(3, ChronoUnit.DAYS);

        List<User> users = userRepository.findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(
            threeDaysAgo);

        assertThat(users).isNotEmpty();

        userService.removeNotActivatedUsers();

        users = userRepository.findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(threeDaysAgo);

        assertThat(users).isEmpty();
    }

    @Test
    @Transactional
    void assertThatNotActivatedUsersWithNullActivationKeyCreatedBefore3DaysAreNotDeleted() {
        Instant now = Instant.now();

        when(dateTimeProvider.getNow()).thenReturn(Optional.of(now.minus(4, ChronoUnit.DAYS)));

        user.setActivated(false);

        User dbUser = userRepository.save(user);

        dbUser.setCreatedDate(LocalDateTime.ofInstant(now.minus(4, ChronoUnit.DAYS), ZoneOffset.UTC));

        userRepository.save(user);

        Instant threeDaysAgo = now.minus(3, ChronoUnit.DAYS);

        List<User> users = userRepository.findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(
            threeDaysAgo);

        assertThat(users).isEmpty();

        userService.removeNotActivatedUsers();

        Optional<User> maybeDbUser = userRepository.findById(dbUser.getId());

        assertThat(maybeDbUser).contains(dbUser);
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
}
