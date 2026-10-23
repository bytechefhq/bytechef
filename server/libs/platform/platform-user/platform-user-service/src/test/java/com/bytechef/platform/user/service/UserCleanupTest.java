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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class UserCleanupTest {

    private final List<String> cleanedTenantIds = new ArrayList<>();

    private TenantService tenantService;
    private UserCleanup userCleanup;
    private UserService userService;

    @BeforeEach
    void setUp() {
        tenantService = mock(TenantService.class);
        userService = mock(UserService.class);

        doAnswer(invocation -> cleanedTenantIds.add(TenantContext.getCurrentTenantId())).when(userService)
            .removeNotActivatedUsers();
        doAnswer(invocation -> cleanedTenantIds.add(TenantContext.getCurrentTenantId())).when(userService)
            .removeOldPersistentTokens();

        userCleanup = new UserCleanup(tenantService, userService);
    }

    @AfterEach
    void tearDown() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testCleansOnlyTheDefaultSchemaInSingleTenantMode() {
        userCleanup.removeNotActivatedUsers();
        userCleanup.removeOldPersistentTokens();

        assertThat(cleanedTenantIds).containsExactly(TenantContext.DEFAULT_TENANT_ID, TenantContext.DEFAULT_TENANT_ID);

        verify(tenantService, never()).getTenantIds();
    }

    @Test
    void testRemovesNotActivatedUsersInTheDefaultSchemaAndEveryTenant() {
        givenTenants("000001", "000002");

        userCleanup.removeNotActivatedUsers();

        assertThat(cleanedTenantIds).containsExactly(TenantContext.DEFAULT_TENANT_ID, "000001", "000002");
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);

        verify(userService, never()).removeOldPersistentTokens();
    }

    @Test
    void testRemovesOldPersistentTokensInTheDefaultSchemaAndEveryTenant() {
        givenTenants("000001", "000002");

        userCleanup.removeOldPersistentTokens();

        assertThat(cleanedTenantIds).containsExactly(TenantContext.DEFAULT_TENANT_ID, "000001", "000002");

        verify(userService, never()).removeNotActivatedUsers();
    }

    @Test
    void testAFailingTenantDoesNotStopTheOthers() {
        givenTenants("000001", "000002");

        doAnswer(invocation -> {
            String tenantId = TenantContext.getCurrentTenantId();

            if ("000001".equals(tenantId)) {
                throw new IllegalStateException("tenant schema unavailable");
            }

            return cleanedTenantIds.add(tenantId);
        }).when(userService)
            .removeOldPersistentTokens();

        userCleanup.removeOldPersistentTokens();

        assertThat(cleanedTenantIds).containsExactly(TenantContext.DEFAULT_TENANT_ID, "000002");
    }

    private void givenTenants(String... tenantIds) {
        when(tenantService.isMultiTenantEnabled()).thenReturn(true);
        when(tenantService.getTenantIds()).thenReturn(List.of(tenantIds));
    }
}
