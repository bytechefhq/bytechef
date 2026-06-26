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

package com.bytechef.automation.configuration.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AutomationPermissionEvaluatorTest {

    private PermissionService permissionService;
    private AutomationPermissionEvaluator evaluator;

    @BeforeEach
    void setUp() {
        permissionService = mock(PermissionService.class);
        evaluator = new AutomationPermissionEvaluator(permissionService);
    }

    @Test
    void testUserSelfDelegatesToIsCurrentUser() {
        when(permissionService.isCurrentUser(7L)).thenReturn(true);

        assertThat(evaluator.hasPermission(null, 7L, "User", "SELF")).isTrue();
    }

    @Test
    void testUserWithNonSelfPermissionReturnsFalse() {
        assertThat(evaluator.hasPermission(null, 7L, "User", "OTHER")).isFalse();

        verifyNoInteractions(permissionService);
    }

    @Test
    void testUnknownTargetTypeReturnsFalse() {
        assertThat(evaluator.hasPermission(null, 1L, "Nonsense", "X")).isFalse();
    }

    @Test
    void testNonNumericTargetIdReturnsFalse() {
        assertThat(evaluator.hasPermission(null, "not-a-number", "User", "SELF")).isFalse();

        verifyNoInteractions(permissionService);
    }

    @Test
    void testTenantAdminObjectFormDelegates() {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        assertThat(evaluator.hasPermission(null, "Tenant", "ADMIN")).isTrue();
    }

    @Test
    void testUnknownObjectTargetReturnsFalse() {
        assertThat(evaluator.hasPermission(null, "Something", "ADMIN")).isFalse();

        verifyNoInteractions(permissionService);
    }

    @Test
    void testSkipChecksShortCircuitsWithoutTouchingPermissionService() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(evaluator.hasPermission(null, 1L, "User", "SELF")).isTrue();
            assertThat(evaluator.hasPermission(null, "Tenant", "ADMIN")).isTrue();

            return null;
        });

        verifyNoInteractions(permissionService);
    }
}
