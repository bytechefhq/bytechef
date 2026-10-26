/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc.interceptor;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.remote.grpc.EchoService;
import com.bytechef.ee.remote.grpc.InProcessGrpc;
import com.bytechef.tenant.TenantContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class TenantContextServerInterceptorTest {

    @AfterEach
    void afterEach() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testTenantIsBoundOnServer() throws Exception {
        try (InProcessGrpc inProcessGrpc = start()) {
            String tenantId = TenantContext.callWithTenantId(
                "000002", () -> EchoService.echoTenant(inProcessGrpc.channel()));

            assertThat(tenantId).isEqualTo("000002");
            assertThat(inProcessGrpc.serverExecutorService()
                .submit(TenantContext::getCurrentTenantId)
                .get()).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
        }
    }

    @Test
    void testNoTenantLeavesContextUnset() throws Exception {
        try (InProcessGrpc inProcessGrpc = start()) {
            assertThat(EchoService.echoTenant(inProcessGrpc.channel())).isEqualTo(TenantContext.DEFAULT_TENANT_ID);
        }
    }

    private static InProcessGrpc start() throws Exception {
        return new InProcessGrpc(
            new EchoService(), List.of(new TenantContextServerInterceptor()),
            List.of(new TenantContextClientInterceptor()));
    }
}
