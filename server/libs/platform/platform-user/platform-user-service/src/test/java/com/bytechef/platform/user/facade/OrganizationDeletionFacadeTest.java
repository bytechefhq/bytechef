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

package com.bytechef.platform.user.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.platform.user.domain.User;
import com.bytechef.platform.user.exception.InvalidPasswordException;
import com.bytechef.platform.user.exception.OrganizationDeletionConfirmationException;
import com.bytechef.platform.user.exception.OrganizationDeletionNotAvailableException;
import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.domain.TenantDeletion;
import com.bytechef.tenant.service.TenantDeletionService;
import com.bytechef.tenant.service.TenantService;
import com.bytechef.tenant.teardown.TenantTeardownContributor;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
@SuppressFBWarnings("HARD_CODE_PASSWORD")
class OrganizationDeletionFacadeTest {

    private static final String EMAIL = "admin@localhost.com";
    private static final Instant NOW = Instant.parse("2026-10-19T10:00:00Z");
    private static final String PASSWORD_HASH = "hash";
    private static final String TENANT_ID = "000001";

    @Mock
    private ObjectProvider<Clock> clockProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ObjectProvider<TenantDeletionService> tenantDeletionServiceProvider;

    @Mock
    private TenantDeletionService tenantDeletionService;

    @Mock
    private TenantService tenantService;

    @Mock
    private TenantTeardownContributor firstContributor;

    @Mock
    private TenantTeardownContributor secondContributor;

    @Mock
    private UserService userService;

    private User user;
    private OrganizationDeletionFacade organizationDeletionFacade;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenantId(TENANT_ID);

        user = new User();

        user.setEmail(EMAIL);
        user.setLogin("admin");
        user.setPassword(PASSWORD_HASH);

        lenient()
            .when(clockProvider.getIfAvailable(any()))
            .thenReturn(Clock.fixed(NOW, ZoneOffset.UTC));
        lenient()
            .when(passwordEncoder.matches("secret", PASSWORD_HASH))
            .thenReturn(true);
        lenient()
            .when(tenantDeletionServiceProvider.getIfAvailable())
            .thenReturn(tenantDeletionService);
        lenient()
            .when(tenantService.isMultiTenantEnabled())
            .thenReturn(true);
        lenient()
            .when(userService.getCurrentUser())
            .thenReturn(user);

        organizationDeletionFacade = new OrganizationDeletionFacadeImpl(
            new ApplicationProperties(), passwordEncoder, tenantDeletionServiceProvider,
            List.of(firstContributor, secondContributor), tenantService, userService, clockProvider);
    }

    @AfterEach
    void tearDown() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testRejectsMismatchedConfirmation() {
        assertThatThrownBy(() -> organizationDeletionFacade.requestDeletion("other@localhost.com", "secret"))
            .isInstanceOf(OrganizationDeletionConfirmationException.class);

        verify(tenantDeletionService, never()).markPendingDeletion(anyString(), anyString(), any());
    }

    @Test
    void testRejectsWrongPasswordForLocalAccount() {
        assertThatThrownBy(() -> organizationDeletionFacade.requestDeletion(EMAIL, "wrong"))
            .isInstanceOf(InvalidPasswordException.class);

        verify(tenantDeletionService, never()).markPendingDeletion(anyString(), anyString(), any());
    }

    @Test
    void testExternalProviderAdminConfirmsWithEmailOnly() {
        user.setAuthProvider("github");

        when(tenantDeletionService.markPendingDeletion(anyString(), anyString(), any())).thenReturn(true);

        organizationDeletionFacade.requestDeletion(" ADMIN@localhost.com ", null);

        verify(tenantDeletionService).markPendingDeletion(anyString(), anyString(), any());
    }

    @Test
    void testRejectsWhenNotMultiTenant() {
        when(tenantService.isMultiTenantEnabled()).thenReturn(false);

        assertThatThrownBy(() -> organizationDeletionFacade.requestDeletion(EMAIL, "secret"))
            .isInstanceOf(OrganizationDeletionNotAvailableException.class);
    }

    @Test
    void testRejectsDefaultTenant() {
        TenantContext.setCurrentTenantId(TenantContext.DEFAULT_TENANT_ID);

        assertThatThrownBy(() -> organizationDeletionFacade.requestDeletion(EMAIL, "secret"))
            .isInstanceOf(OrganizationDeletionNotAvailableException.class);
    }

    @Test
    void testMarksPendingThenRunsContributorsInOrder() {
        Instant purgeAfter = NOW.plus(Duration.ofDays(30));

        when(tenantDeletionService.markPendingDeletion(TENANT_ID, "admin", purgeAfter)).thenReturn(true);
        when(firstContributor.onDeletionRequested(TENANT_ID)).thenReturn(List.of());
        when(secondContributor.onDeletionRequested(TENANT_ID)).thenReturn(List.of());

        assertThat(organizationDeletionFacade.requestDeletion(EMAIL, "secret")).isEqualTo(purgeAfter);

        InOrder inOrder = inOrder(tenantDeletionService, firstContributor, secondContributor);

        inOrder.verify(tenantDeletionService)
            .markPendingDeletion(TENANT_ID, "admin", purgeAfter);
        inOrder.verify(firstContributor)
            .onDeletionRequested(TENANT_ID);
        inOrder.verify(secondContributor)
            .onDeletionRequested(TENANT_ID);
    }

    @Test
    void testContributorFailureIsRecordedAndDoesNotStopTheRest() {
        when(tenantDeletionService.markPendingDeletion(anyString(), anyString(), any())).thenReturn(true);
        when(firstContributor.onDeletionRequested(TENANT_ID)).thenThrow(new RuntimeException("boom"));
        when(secondContributor.onDeletionRequested(TENANT_ID)).thenReturn(List.of("x"));

        organizationDeletionFacade.requestDeletion(EMAIL, "secret");

        verify(secondContributor).onDeletionRequested(TENANT_ID);
        verify(tenantDeletionService).recordTeardownFailures(
            TENANT_ID, List.of(firstContributor.getClass()
                .getSimpleName() + ": boom", "x"));
    }

    @Test
    void testSecondRequestIsNoOp() {
        Instant existingPurgeAfter = NOW.plus(Duration.ofDays(5));

        when(tenantDeletionService.markPendingDeletion(anyString(), anyString(), any())).thenReturn(false);
        when(tenantDeletionService.fetchTenantDeletion(TENANT_ID))
            .thenReturn(Optional.of(new TenantDeletion(
                TENANT_ID, TenantDeletion.Status.PENDING_DELETION, "admin", NOW.minus(Duration.ofDays(25)),
                existingPurgeAfter, null, null, null, List.of())));

        assertThat(organizationDeletionFacade.requestDeletion(EMAIL, "secret")).isEqualTo(existingPurgeAfter);

        verify(firstContributor, never()).onDeletionRequested(anyString());
        verify(secondContributor, never()).onDeletionRequested(anyString());
        verify(tenantDeletionService, never()).recordTeardownFailures(anyString(), anyList());
    }
}
