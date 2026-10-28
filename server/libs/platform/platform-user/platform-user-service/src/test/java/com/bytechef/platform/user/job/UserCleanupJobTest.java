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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.platform.user.service.UserService;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.service.TenantService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class UserCleanupJobTest {

    private final TenantService tenantService = mock(TenantService.class);
    private final UserService userService = mock(UserService.class);
    private final UserCleanupJob userCleanupJob = new UserCleanupJob(tenantService, userService);

    @Test
    void testRemoveOldPersistentTokensRunsOnceWhenSingleTenant() {
        when(tenantService.isMultiTenantEnabled()).thenReturn(false);

        userCleanupJob.removeOldPersistentTokens();

        verify(userService).removeOldPersistentTokens();
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
}
