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

package com.bytechef.platform.billing.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.billing.facade.BillingSubscriptionFacade;
import com.bytechef.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.ResponseEntity;

/**
 * @author Matija Petanjek
 */
class BillingWebhookApiControllerTest {

    private static final String PAYLOAD = "{\"id\":\"evt_test\"}";
    private static final String STRIPE_SIGNATURE = "t=1,v1=test-signature";
    private static final String TENANT_ID = "acme";

    private final BillingSubscriptionFacade billingSubscriptionFacade = mock(BillingSubscriptionFacade.class);

    private final BillingWebhookApiController controller =
        new BillingWebhookApiController(billingSubscriptionFacade);

    @AfterEach
    void tearDown() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testHandleWebhookVerifiesSignatureBeforeReadingPayload() {
        when(billingSubscriptionFacade.extractTenantId(PAYLOAD)).thenReturn(TENANT_ID);

        controller.handleWebhook(STRIPE_SIGNATURE, PAYLOAD);

        InOrder inOrder = inOrder(billingSubscriptionFacade);

        inOrder.verify(billingSubscriptionFacade)
            .verifyWebhookSignature(PAYLOAD, STRIPE_SIGNATURE);
        inOrder.verify(billingSubscriptionFacade)
            .extractTenantId(PAYLOAD);
        inOrder.verify(billingSubscriptionFacade)
            .handleWebhookEvent(PAYLOAD, STRIPE_SIGNATURE);
    }

    @Test
    void testHandleWebhookEstablishesTenantContextBeforeHandlingEvent() {
        when(billingSubscriptionFacade.extractTenantId(PAYLOAD)).thenReturn(TENANT_ID);

        String[] tenantIdDuringHandling = new String[1];

        doAnswer(invocation -> {
            tenantIdDuringHandling[0] = TenantContext.getCurrentTenantId();

            return null;
        }).when(billingSubscriptionFacade)
            .handleWebhookEvent(eq(PAYLOAD), eq(STRIPE_SIGNATURE));

        ResponseEntity<Void> response = controller.handleWebhook(STRIPE_SIGNATURE, PAYLOAD);

        assertThat(tenantIdDuringHandling[0])
            .as("tenant must already be resolved in TenantContext before handleWebhookEvent is invoked, since "
                + "handleWebhookEvent is @Transactional and acquires its database connection as soon as it is "
                + "entered")
            .isEqualTo(TENANT_ID);
        assertThat(response.getStatusCode()
            .is2xxSuccessful()).isTrue();
    }
}
