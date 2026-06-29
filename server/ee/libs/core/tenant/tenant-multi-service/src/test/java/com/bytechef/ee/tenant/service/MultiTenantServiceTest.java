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

package com.bytechef.ee.tenant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.bytechef.ee.tenant.repository.TenantRepository;
import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.Tenancy;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseProperties;
import org.springframework.context.ApplicationEventPublisher;

/**
 * @author Ivica Cardic
 */
class MultiTenantServiceTest {

    @AfterEach
    void tearDown() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testLoadChangelogRunsEachTenantWithinItsTenantContext() {
        List<String> capturedTenantIds = new ArrayList<>();

        MultiTenantService multiTenantService = new MultiTenantService(
            mock(DataSource.class), mock(ApplicationEventPublisher.class), mock(LiquibaseProperties.class),
            mock(TenantRepository.class)) {

            @Override
            void loadChangelog(String tenantId, Tenancy tenancy) {
                capturedTenantIds.add(MDC.get("tenantId"));
            }
        };

        multiTenantService.loadChangelog(List.of("000001", "000002"), Tenancy.MULTITENANT);

        // The MDC tenantId observed while each tenant's changelog runs must match the tenant being upgraded,
        // not the default "public" value (see GitHub issue bytechef-cloud#119).

        assertThat(capturedTenantIds).containsExactly("000001", "000002");
    }

    @Test
    void testLoadChangelogRestoresPreviousTenantId() {
        MultiTenantService multiTenantService = new MultiTenantService(
            mock(DataSource.class), mock(ApplicationEventPublisher.class), mock(LiquibaseProperties.class),
            mock(TenantRepository.class)) {

            @Override
            void loadChangelog(String tenantId, Tenancy tenancy) {
                // No-op: skip the real Liquibase run.
                assertThat(tenantId).isNotBlank();
            }
        };

        TenantContext.setCurrentTenantId("000099");

        multiTenantService.loadChangelog(List.of("000001"), Tenancy.MULTITENANT);

        // The tenant ID active before the upgrade must be restored afterward, not reset to the default "public".
        assertThat(TenantContext.getCurrentTenantId()).isEqualTo("000099");
    }
}
