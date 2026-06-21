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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bytechef.platform.user.audit.UserAuditPublisher;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.exception.TotpLockedException;
import com.bytechef.platform.user.repository.AuthorityRepository;
import com.bytechef.platform.user.repository.PersistentTokenRepository;
import com.bytechef.platform.user.repository.UserRepository;
import com.bytechef.tenant.service.TenantService;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTotpLockoutTest {

    private static final String SECRET = "ABCDEFGHIJKLMNOP";
    private static final int MAX_FAILED_ATTEMPTS = 3;

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

    private UserService userService;
    private User user;

    @BeforeEach
    void setUp() {
        CacheManager cacheManager = new ConcurrentMapCacheManager();

        userService = new UserServiceImpl(
            authorityRepository, cacheManager, passwordEncoder, persistentTokenRepository, tenantService,
            userAuditPublisher, userRepository, MAX_FAILED_ATTEMPTS, Duration.ofMinutes(15));

        user = new User();

        user.setLogin("user@localhost.com");
        user.setTotpSecret(SECRET);

        lenient()
            .when(userRepository.save(user))
            .thenReturn(user);
    }

    @Test
    void testInvalidCodeIncrementsFailedAttempts() {
        when(userRepository.findByLogin(anyString())).thenReturn(Optional.of(user));

        boolean valid = userService.verifyTotpCode("user@localhost.com", "000000");

        assertThat(valid).isFalse();
        assertThat(user.getFailedTotpAttempts()).isEqualTo(1);
        assertThat(user.getTotpLockoutUntil()).isNull();
    }

    @Test
    void testReachingThresholdSetsLockout() {
        when(userRepository.findByLogin(anyString())).thenReturn(Optional.of(user));

        for (int i = 0; i < MAX_FAILED_ATTEMPTS; i++) {
            userService.verifyTotpCode("user@localhost.com", "000000");
        }

        assertThat(user.getFailedTotpAttempts()).isEqualTo(MAX_FAILED_ATTEMPTS);
        assertThat(user.getTotpLockoutUntil()).isAfter(Instant.now());
    }

    @Test
    void testVerifyWhileLockedThrowsTotpLockedException() {
        user.setTotpLockoutUntil(Instant.now()
            .plus(10, ChronoUnit.MINUTES));

        when(userRepository.findByLogin(anyString())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.verifyTotpCode("user@localhost.com", "000000"))
            .isInstanceOf(TotpLockedException.class);
    }

    @Test
    void testValidCodeResetsCounters() {
        user.setFailedTotpAttempts(2);

        when(userRepository.findByLogin(anyString())).thenReturn(Optional.of(user));

        boolean valid = userService.verifyTotpCode("user@localhost.com", currentValidCode());

        assertThat(valid).isTrue();
        assertThat(user.getFailedTotpAttempts()).isZero();
        assertThat(user.getTotpLockoutUntil()).isNull();
    }

    private static String currentValidCode() {
        SystemTimeProvider timeProvider = new SystemTimeProvider();

        long counter = timeProvider.getTime() / 30;

        try {
            return new DefaultCodeGenerator().generate(SECRET, counter);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
