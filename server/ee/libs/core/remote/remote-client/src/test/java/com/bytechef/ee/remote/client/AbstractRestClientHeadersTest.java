/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.tenant.TenantContext;
import com.bytechef.tenant.constant.TenantConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AbstractRestClientHeadersTest {

    @AfterEach
    void tearDown() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testHeadersSetsTenantAndServiceToken() {
        DefaultRestClient client = new DefaultRestClient();

        ReflectionTestUtils.setField(client, "serviceToken", "secret-token");

        TenantContext.setCurrentTenantId("public");

        HttpHeaders httpHeaders = new HttpHeaders();

        client.headers()
            .accept(httpHeaders);

        assertThat(httpHeaders.getFirst(TenantConstants.CURRENT_TENANT_ID)).isEqualTo("public");
        assertThat(httpHeaders.getFirst(TenantConstants.INTERNAL_SERVICE_TOKEN)).isEqualTo("secret-token");
    }
}
