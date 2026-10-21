/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.tenant.multi.liquibase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.tenant.constant.Tenancy;
import com.bytechef.tenant.service.TenantService;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class MultiTenantLiquibaseChangelogLoaderTest {

    @Test
    void testLoadsChangelogForPendingDeletionTenants() {
        TenantService tenantService = mock(TenantService.class);

        when(tenantService.getTenantIds()).thenReturn(List.of("000001"));
        when(tenantService.getTenantIdsIncludingPendingDeletion()).thenReturn(List.of("000001", "000002"));

        new MultiTenantLiquibaseChangelogLoader(tenantService).afterPropertiesSet();

        verify(tenantService).loadChangelog(List.of("000001", "000002"), Tenancy.MULTITENANT);
    }
}
