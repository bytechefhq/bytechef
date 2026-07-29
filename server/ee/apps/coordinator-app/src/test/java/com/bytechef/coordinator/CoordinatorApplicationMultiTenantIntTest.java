/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.coordinator;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.tenant.remote.client.service.RemoteTenantServiceClient;
import com.bytechef.tenant.service.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Pins that coordinator-app starts in multi-tenant mode. The app is deliberately datasource-less, so the
 * DataSource-backed {@code MultiTenantService} cannot be constructed here; without a datasource-free binding the six
 * per-tenant monitors (which take {@code TenantService} as a required constructor argument) fail to resolve and the
 * context does not start at all. The remote client supplies that binding.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(properties = "bytechef.tenant.mode=multi")
@EnableAutoConfiguration
public class CoordinatorApplicationMultiTenantIntTest {

    @Autowired
    private TenantService tenantService;

    @Test
    void testContextLoadsWithRemoteTenantServiceBinding() {
        assertThat(tenantService).isInstanceOf(RemoteTenantServiceClient.class);
    }
}
