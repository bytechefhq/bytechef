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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bytechef.platform.billing.facade.BillingWebhookFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * @author Matija Petanjek
 */
class BillingWebhookApiControllerTest {

    private static final String PAYLOAD = "{\"id\":\"evt_test\"}";
    private static final String STRIPE_SIGNATURE = "t=1,v1=test-signature";

    private final BillingWebhookFacade billingWebhookFacade =
        mock(BillingWebhookFacade.class);

    private final BillingWebhookApiController controller =
        new BillingWebhookApiController(billingWebhookFacade);

    @Test
    void testHandleWebhookDelegatesToFacadeAndReturnsOk() {
        ResponseEntity<Void> response = controller.handleWebhook(STRIPE_SIGNATURE, PAYLOAD);

        verify(billingWebhookFacade).processWebhookEvent(PAYLOAD, STRIPE_SIGNATURE);
        assertThat(response.getStatusCode()
            .is2xxSuccessful()).isTrue();
    }
}
