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

package com.bytechef.platform.user.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.user.config.UserIntTestConfiguration;
import com.bytechef.platform.user.domain.PersistentToken;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.repository.PersistentTokenRepository;
import com.bytechef.platform.user.repository.UserRepository;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = UserIntTestConfiguration.class)
@Transactional
class UserCleanupJobIntTest {

    @Autowired
    private PersistentTokenRepository persistentTokenRepository;

    @Autowired
    private UserCleanupJob userCleanupJob;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private DateTimeProvider dateTimeProvider;

    @MockitoBean
    private TenantService tenantService;

    @MockitoSpyBean
    private UserService userService;

    @BeforeEach
    void beforeEach() {
        persistentTokenRepository.deleteAll();

        when(dateTimeProvider.getNow()).thenReturn(Optional.of(LocalDateTime.now()));
    }

    @Test
    void testRemoveOldPersistentTokensDeletesExpiredTokensWhenSingleTenant() {
        User user = new User();

        user.setActivated(true);
        user.setEmail("cleanup-job@localhost");
        user.setLogin("cleanup-job");
        user.setPassword(RandomStringUtils.randomAlphanumeric(60));

        user = userRepository.save(user);

        LocalDate today = LocalDate.now();

        savePersistentToken(user, "valid-session", today.minusDays(PersistentToken.VALIDITY_DAYS));
        savePersistentToken(user, "expired-session", today.minusDays(PersistentToken.VALIDITY_DAYS + 1));

        when(tenantService.isMultiTenantEnabled()).thenReturn(false);

        userCleanupJob.removeOldPersistentTokens();

        assertThat(persistentTokenRepository.findAllByUserId(user.getId()))
            .extracting(PersistentToken::getSeries)
            .containsExactly("valid-session");

        verify(tenantService, never()).getTenantIds();
    }

    @Test
    void testRemoveOldPersistentTokensRunsInEachTenant() {
        List<String> cleanedTenantIds = new ArrayList<>();

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIds()).thenReturn(List.of("000001", "000002"));

        doAnswer(invocation -> cleanedTenantIds.add(TenantContext.getCurrentTenantId()))
            .when(userService)
            .removeOldPersistentTokens();

        userCleanupJob.removeOldPersistentTokens();

        assertThat(cleanedTenantIds).containsExactly("000001", "000002");
    }

    @Test
    void testRemoveNotActivatedUsersContinuesAfterTenantFailure() {
        List<String> cleanedTenantIds = new ArrayList<>();

        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIds()).thenReturn(List.of("000001", "000002"));

        doAnswer(invocation -> {
            String tenantId = TenantContext.getCurrentTenantId();

            if (tenantId.equals("000001")) {
                throw new IllegalStateException("Schema unavailable");
            }

            cleanedTenantIds.add(tenantId);

            return null;
        })
            .when(userService)
            .removeNotActivatedUsers();

        userCleanupJob.removeNotActivatedUsers();

        assertThat(cleanedTenantIds).containsExactly("000002");
    }

    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    private void savePersistentToken(User user, String series, LocalDate tokenDate) {
        PersistentToken persistentToken = new PersistentToken();

        persistentToken.setNew(true);
        persistentToken.setSeries(series);
        persistentToken.setUser(user);
        persistentToken.setTokenValue(series + "-data");
        persistentToken.setTokenDate(tokenDate);
        persistentToken.setIpAddress("127.0.0.1");
        persistentToken.setUserAgent("Test agent");

        persistentTokenRepository.save(persistentToken);
    }
}
