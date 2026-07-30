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

package com.bytechef.platform.billing.facade;

import com.bytechef.platform.billing.client.StripeClient;
import com.bytechef.tenant.TenantContext;
import com.stripe.model.Event;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Matija Petanjek
 */
@Service
public class BillingWebhookFacadeImpl implements BillingWebhookFacade {

    private final BillingWebhookEventHandler billingWebhookEventHandler;
    private final ObjectMapper objectMapper;
    private final StripeClient stripeClient;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public BillingWebhookFacadeImpl(
        BillingWebhookEventHandler billingWebhookEventHandler, ObjectMapper objectMapper,
        StripeClient stripeClient) {

        this.billingWebhookEventHandler = billingWebhookEventHandler;
        this.objectMapper = objectMapper;
        this.stripeClient = stripeClient;
    }

    @Override
    public void processWebhookEvent(String payload, String stripeSignatureHeader) {
        Event event = stripeClient.verifyWebhookSignature(payload, stripeSignatureHeader);

        String tenantId = extractTenantId(payload, event.getType());

        TenantContext.runWithTenantId(tenantId, () -> billingWebhookEventHandler.handle(event));
    }

    private String extractTenantId(String payload, String eventType) {
        JsonNode dataObject = objectMapper.readTree(payload)
            .path("data")
            .path("object");

        if ("checkout.session.completed".equals(eventType)) {
            String clientReferenceId = dataObject.path("client_reference_id")
                .textValue();

            if (clientReferenceId == null) {
                throw new IllegalStateException(
                    "Missing client_reference_id in checkout.session.completed webhook event");
            }

            return clientReferenceId;
        }

        if ("customer.subscription.updated".equals(eventType) ||
            "customer.subscription.deleted".equals(eventType)) {

            String tenantId = dataObject.path("metadata")
                .path("tenantId")
                .textValue();

            if (tenantId == null) {
                throw new IllegalStateException(
                    "Missing tenantId metadata in " + eventType + " webhook event");
            }

            return tenantId;
        }

        throw new IllegalArgumentException("Unhandled webhook event type: " + eventType);
    }
}
