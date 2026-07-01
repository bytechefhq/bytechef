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
import java.util.function.Supplier;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class AutomationMethodSecurityExpressionRootTest {

    private PermissionService permissionService;
    private AutomationMethodSecurityExpressionRoot root;

    @BeforeEach
    void setUp() {
        permissionService = mock(PermissionService.class);

        Supplier<Authentication> authentication = () -> mock(Authentication.class);
        MethodInvocation methodInvocation = mock(MethodInvocation.class);

        root = new AutomationMethodSecurityExpressionRoot(authentication, methodInvocation, permissionService);
    }

    @Test
    void testIsCurrentUserDelegates() {
        when(permissionService.isCurrentUser(7L)).thenReturn(true);

        assertThat(root.isCurrentUser(7L)).isTrue();
    }

    @Test
    void testIsTenantAdminDelegates() {
        when(permissionService.isTenantAdmin()).thenReturn(true);

        assertThat(root.isTenantAdmin()).isTrue();
    }

    @Test
    void testIsResourceOwnerDelegates() {
        when(permissionService.isResourceOwner("ApiKey", 9L)).thenReturn(true);

        assertThat(root.isResourceOwner(9L, "ApiKey")).isTrue();
    }

    @Test
    void testIsCurrentUserShortCircuitsUnderSkipChecks() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(root.isCurrentUser(7L)).isTrue();

            return null;
        });

        verifyNoInteractions(permissionService);
    }

    @Test
    void testIsTenantAdminShortCircuitsUnderSkipChecks() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(root.isTenantAdmin()).isTrue();

            return null;
        });

        verifyNoInteractions(permissionService);
    }

    @Test
    void testIsResourceOwnerShortCircuitsUnderSkipChecks() throws Throwable {
        AutomationAuthorizationContext.callSkippingChecks(() -> {
            assertThat(root.isResourceOwner(9L, "ApiKey")).isTrue();

            return null;
        });

        verifyNoInteractions(permissionService);
    }
}
