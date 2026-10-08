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

package com.bytechef.automation.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.security.AutomationMethodSecurityConfiguration;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.platform.security.web.authentication.TestConnectedUserAuthentication;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Ivica Cardic
 */
@SpringBootTest(classes = AutomationMethodSecurityExpressionRootIntTest.Config.class)
class AutomationMethodSecurityExpressionRootIntTest {

    private static final String GRANTED = "granted";

    @Autowired
    private GuardedOperations guardedOperations;

    @Autowired
    private PermissionService permissionService;

    @BeforeEach
    void beforeEach() {
        reset(permissionService);

        authenticate(
            new UsernamePasswordAuthenticationToken(
                "user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void afterEach() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testIsCurrentUserDelegates() {
        when(permissionService.isCurrentUser(7L)).thenReturn(true);

        assertThat(guardedOperations.currentUserOperation(7L)).isEqualTo(GRANTED);

        assertThatThrownBy(() -> guardedOperations.currentUserOperation(8L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testIsTenantAdminDelegates() {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        assertThat(guardedOperations.tenantAdminOperation()).isEqualTo(GRANTED);

        when(permissionService.isTenantAdmin()).thenReturn(false);

        assertThatThrownBy(() -> guardedOperations.tenantAdminOperation())
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testIsResourceOwnerDelegates() {
        when(permissionService.isResourceOwner("ApiKey", 9L)).thenReturn(true);

        assertThat(guardedOperations.resourceOwnerOperation(9L)).isEqualTo(GRANTED);

        assertThatThrownBy(() -> guardedOperations.resourceOwnerOperation(10L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testIsCurrentUserShortCircuitsUnderSkipChecks() throws Throwable {
        assertThat(callSkippingChecks(() -> guardedOperations.currentUserOperation(7L))).isEqualTo(GRANTED);

        verifyNoInteractions(permissionService);
    }

    @Test
    void testIsTenantAdminShortCircuitsUnderSkipChecks() throws Throwable {
        assertThat(callSkippingChecks(guardedOperations::tenantAdminOperation)).isEqualTo(GRANTED);

        verifyNoInteractions(permissionService);
    }

    @Test
    void testIsResourceOwnerShortCircuitsUnderSkipChecks() throws Throwable {
        assertThat(callSkippingChecks(() -> guardedOperations.resourceOwnerOperation(9L))).isEqualTo(GRANTED);

        verifyNoInteractions(permissionService);
    }

    @Test
    void testConnectedUserIsDeniedUnderSkipChecks() throws Throwable {
        authenticate(TestConnectedUserAuthentication.of("external-user"));

        assertThat(callSkippingChecks(guardedOperations::connectedUserOperation)).isEqualTo(GRANTED);

        assertThatThrownBy(() -> callSkippingChecks(() -> guardedOperations.currentUserOperation(7L)))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> callSkippingChecks(guardedOperations::tenantAdminOperation))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> callSkippingChecks(() -> guardedOperations.resourceOwnerOperation(9L)))
            .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(permissionService);
    }

    @Test
    void testConnectedUserIsDeniedWithoutConsultingPermissionService() {
        when(permissionService.isCurrentUser(anyLong())).thenReturn(true);
        when(permissionService.isTenantAdmin()).thenReturn(true);
        when(permissionService.isResourceOwner(anyString(), anyLong())).thenReturn(true);

        authenticate(TestConnectedUserAuthentication.of("external-user"));

        assertThatThrownBy(() -> guardedOperations.currentUserOperation(7L))
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> guardedOperations.tenantAdminOperation())
            .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> guardedOperations.resourceOwnerOperation(9L))
            .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(permissionService);
    }

    @Test
    void testIsCurrentConnectedUserMatchesOnlyTheTokensConnectedUserId() {
        authenticate(new TestConnectedUserAuthentication("external-user", 5L, 2L, true));

        assertThat(guardedOperations.currentConnectedUserOperation(5L)).isEqualTo(GRANTED);

        assertThatThrownBy(() -> guardedOperations.currentConnectedUserOperation(6L))
            .isInstanceOf(AccessDeniedException.class);

        verifyNoInteractions(permissionService);
    }

    @Test
    void testIsCurrentConnectedUserIsNotBypassedUnderSkipChecks() {
        authenticate(new TestConnectedUserAuthentication("external-user", 5L, 2L, true));

        assertThatThrownBy(() -> callSkippingChecks(() -> guardedOperations.currentConnectedUserOperation(6L)))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testIsCurrentConnectedUserIsFalseForAnUnauthenticatedConnectedUserOrOtherPrincipals() {
        authenticate(new TestConnectedUserAuthentication("external-user", 5L, 2L, false));

        assertThatThrownBy(() -> guardedOperations.currentConnectedUserOperation(5L))
            .isInstanceOf(AccessDeniedException.class);

        authenticate(
            new UsernamePasswordAuthenticationToken(
                "user", "n/a", List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThatThrownBy(() -> guardedOperations.currentConnectedUserOperation(5L))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void testIsConnectedUserIsFalseForOtherPrincipals() {
        assertThatThrownBy(() -> guardedOperations.connectedUserOperation())
            .isInstanceOf(AccessDeniedException.class);
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }

    private static <T> T callSkippingChecks(Supplier<T> supplier) throws Throwable {
        return AutomationAuthorizationContext.callSkippingChecks(supplier::get);
    }

    @SpringBootConfiguration
    @EnableMethodSecurity
    @ImportAutoConfiguration(AutomationMethodSecurityConfiguration.class)
    @Import(GuardedOperations.class)
    static class Config {

        @Bean("permissionService")
        PermissionService permissionService() {
            return mock(PermissionService.class);
        }
    }

    static class GuardedOperations {

        private final AtomicInteger invocationCount = new AtomicInteger();

        @PreAuthorize("isTenantAdmin()")
        public String tenantAdminOperation() {
            return grant();
        }

        @PreAuthorize("isCurrentUser(#id)")
        @SuppressWarnings("PMD.UnusedFormalParameter")
        public String currentUserOperation(long id) {
            return grant();
        }

        @PreAuthorize("isResourceOwner(#id, 'ApiKey')")
        @SuppressWarnings("PMD.UnusedFormalParameter")
        public String resourceOwnerOperation(long id) {
            return grant();
        }

        @PreAuthorize("isConnectedUser()")
        public String connectedUserOperation() {
            return grant();
        }

        @PreAuthorize("isCurrentConnectedUser(#connectedUserId)")
        @SuppressWarnings("PMD.UnusedFormalParameter")
        public String currentConnectedUserOperation(long connectedUserId) {
            return grant();
        }

        private String grant() {
            invocationCount.incrementAndGet();

            return GRANTED;
        }
    }
}
