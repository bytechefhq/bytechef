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

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.bytechef.platform.billing.exception.InvalidWebhookSignatureException;
import com.bytechef.platform.billing.facade.BillingWebhookFacade;
import com.bytechef.platform.billing.web.rest.config.PlatformBillingRestTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;

/**
 * @author Matija Petanjek
 */
@ContextConfiguration(classes = PlatformBillingRestTestConfiguration.class)
@WebMvcTest(value = BillingWebhookApiController.class, properties = "billing.enabled=true")
class BillingWebhookApiControllerIntTest {

    private static final String PAYLOAD = "{\"id\":\"evt_test\"}";
    private static final String STRIPE_SIGNATURE = "t=1,v1=test-signature";

    @MockitoBean
    private BillingWebhookFacade billingWebhookFacade;

    @Autowired
    private MockMvc mockMvc;

    private WebTestClient webTestClient;

    @BeforeEach
    void beforeEach() {
        webTestClient = MockMvcWebTestClient.bindTo(mockMvc)
            .build();
    }

    @Test
    void testHandleWebhookDelegatesToFacadeAndReturnsOk() {
        webTestClient
            .post()
            .uri("/billing/webhook")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Stripe-Signature", STRIPE_SIGNATURE)
            .bodyValue(PAYLOAD)
            .exchange()
            .expectStatus()
            .isOk();

        verify(billingWebhookFacade).processWebhookEvent(PAYLOAD, STRIPE_SIGNATURE);
    }

    @Test
    void testHandleWebhookReturnsBadRequestForInvalidSignature() {
        doThrow(new InvalidWebhookSignatureException("Invalid Stripe signature", null))
            .when(billingWebhookFacade)
            .processWebhookEvent(PAYLOAD, STRIPE_SIGNATURE);

        webTestClient
            .post()
            .uri("/billing/webhook")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Stripe-Signature", STRIPE_SIGNATURE)
            .bodyValue(PAYLOAD)
            .exchange()
            .expectStatus()
            .isBadRequest();
    }
}
