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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.exception.QuotaLimitExceededException;
import com.bytechef.platform.plan.domain.PlanLimits;
import com.bytechef.platform.plan.domain.PlanTier;
import com.bytechef.platform.plan.provider.PlanLimitsProvider;
import com.bytechef.platform.ratelimit.PlanLimitRejectionCounter;
import com.bytechef.platform.user.audit.UserAuditPublisher;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.dto.AdminUserDTO;
import com.bytechef.platform.user.repository.AuthorityRepository;
import com.bytechef.platform.user.repository.PersistentTokenRepository;
import com.bytechef.platform.user.repository.UserRepository;
import com.bytechef.tenant.service.TenantService;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Pins the {@code maxMembers} quota gate: an at-limit tenant is rejected with {@link QuotaLimitExceededException}
 * before any row is written, a below-limit tenant proceeds, and a null limit (or absent provider bean) means unlimited
 * — the pre-plan behavior.
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
public class UserServiceMemberQuotaTest {

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

    private ObjectProvider<PlanLimitsProvider> planLimitsProviderObjectProvider;
    private UserService userService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void beforeEach() {
        CacheManager cacheManager = new ConcurrentMapCacheManager();

        ObjectProvider<PlanLimitRejectionCounter> planLimitRejectionCounterObjectProvider =
            (ObjectProvider<PlanLimitRejectionCounter>) mock(ObjectProvider.class);

        planLimitsProviderObjectProvider = (ObjectProvider<PlanLimitsProvider>) mock(ObjectProvider.class);

        userService = new UserServiceImpl(
            authorityRepository, cacheManager, passwordEncoder, persistentTokenRepository,
            planLimitRejectionCounterObjectProvider, planLimitsProviderObjectProvider, tenantService,
            userAuditPublisher, userRepository, 5, Duration.ofMinutes(15));

        lenient()
            .when(passwordEncoder.encode(any()))
            .thenReturn("encoded");
        lenient()
            .when(userRepository.findByLogin(any()))
            .thenReturn(Optional.empty());
        lenient()
            .when(userRepository.findByEmailIgnoreCase(any()))
            .thenReturn(Optional.empty());
        // create() publishes an audit event with the saved user's id, so the save stub must assign one.
        lenient()
            .when(userRepository.save(any(User.class)))
            .thenAnswer(invocation -> {
                User user = invocation.getArgument(0);

                user.setId(42L);

                return user;
            });
    }

    @Test
    public void testCreateRejectedAtMemberLimit() {
        stubMaxMembers(2);

        when(userRepository.count()).thenReturn(2L);

        assertThatThrownBy(() -> userService.create(newUserDTO()))
            .isInstanceOf(QuotaLimitExceededException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testCreateAllowedBelowMemberLimit() {
        stubMaxMembers(2);

        when(userRepository.count()).thenReturn(1L);

        assertThatCode(() -> userService.create(newUserDTO())).doesNotThrowAnyException();

        verify(userRepository).save(any(User.class));
    }

    @Test
    public void testRegisterUserRejectedAtMemberLimit() {
        stubMaxMembers(1);

        when(userRepository.count()).thenReturn(1L);

        assertThatThrownBy(() -> userService.registerUser(newUserDTO(), "password"))
            .isInstanceOf(QuotaLimitExceededException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testNullLimitMeansUnlimited() {
        stubMaxMembers(null);

        assertThatCode(() -> userService.create(newUserDTO())).doesNotThrowAnyException();

        verify(userRepository).save(any(User.class));
        verify(userRepository, never()).count();
    }

    private void stubMaxMembers(Integer maxMembers) {
        PlanLimits planLimits = new PlanLimits(
            PlanTier.FREE, null, null, null, null, PlanLimits.DEFAULT_BURST_MULTIPLIER, null, null, null, null, null,
            null, maxMembers);

        when(planLimitsProviderObjectProvider.getIfAvailable()).thenReturn(tenantId -> planLimits);
    }

    private static AdminUserDTO newUserDTO() {
        AdminUserDTO adminUserDTO = new AdminUserDTO();

        adminUserDTO.setLogin("new.member@localhost.com");
        adminUserDTO.setEmail("new.member@localhost.com");

        return adminUserDTO;
    }
}
