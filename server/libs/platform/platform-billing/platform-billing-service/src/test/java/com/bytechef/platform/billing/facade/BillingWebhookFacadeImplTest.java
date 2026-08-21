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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bytechef.platform.billing.client.StripeClient;
import com.bytechef.platform.billing.exception.InvalidWebhookSignatureException;
import com.bytechef.tenant.TenantContext;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Matija Petanjek
 */
@ExtendWith(MockitoExtension.class)
class BillingWebhookFacadeImplTest {

    private static final String WEBHOOK_SECRET = "whsec_test_webhook_secret";
    private static final String SUBSCRIPTION_ID = "sub_test123";

    @Mock
    private BillingWebhookEventHandler billingWebhookEventHandler;

    @Mock
    private StripeClient stripeClient;

    private BillingWebhookFacadeImpl billingWebhookFacade;

    @BeforeEach
    void setUp() {
        billingWebhookFacade =
            new BillingWebhookFacadeImpl(billingWebhookEventHandler, new ObjectMapper(), stripeClient);

        lenient().doAnswer(invocation -> Webhook.constructEvent(
            invocation.getArgument(0), invocation.getArgument(1), WEBHOOK_SECRET))
            .when(stripeClient)
            .verifyWebhookSignature(any(), any());
    }

    @AfterEach
    void tearDown() {
        TenantContext.resetCurrentTenantId();
    }

    @Test
    void testProcessWebhookEventVerifiesSignatureExactlyOnce() throws Exception {
        String payload = checkoutSessionCompletedPayload("acme");

        billingWebhookFacade.processWebhookEvent(payload, signPayload(payload, WEBHOOK_SECRET));

        verify(stripeClient, times(1)).verifyWebhookSignature(any(), any());
    }

    @Test
    void testProcessWebhookEventHandlesEventWithinResolvedTenantContext() throws Exception {
        String payload = checkoutSessionCompletedPayload("acme");

        String[] tenantIdDuringHandling = new String[1];
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);

        doAnswer(invocation -> {
            tenantIdDuringHandling[0] = TenantContext.getCurrentTenantId();

            return null;
        }).when(billingWebhookEventHandler)
            .handle(eventCaptor.capture());

        billingWebhookFacade.processWebhookEvent(payload, signPayload(payload, WEBHOOK_SECRET));

        assertThat(tenantIdDuringHandling[0])
            .as("tenant must already be resolved in TenantContext before handle() is invoked, since handle() is "
                + "@Transactional and acquires its database connection as soon as it is entered")
            .isEqualTo("acme");
        assertThat(eventCaptor.getValue()
            .getType()).isEqualTo("checkout.session.completed");
    }

    @Test
    void testProcessWebhookEventResetsTenantContextAfterHandling() throws Exception {
        String payload = checkoutSessionCompletedPayload("acme");

        billingWebhookFacade.processWebhookEvent(payload, signPayload(payload, WEBHOOK_SECRET));

        assertThat(TenantContext.getCurrentTenantId())
            .as("runWithTenantId must restore the previous tenant once handling completes")
            .isEqualTo(TenantContext.DEFAULT_TENANT_ID);

        verify(billingWebhookEventHandler).handle(any());
    }

    @Test
    void testProcessWebhookEventExtractsMetadataTenantIdForSubscriptionUpdated() throws Exception {
        String payload = subscriptionUpdatedPayload(SUBSCRIPTION_ID, "public");

        String[] tenantIdDuringHandling = new String[1];

        doAnswer(invocation -> {
            tenantIdDuringHandling[0] = TenantContext.getCurrentTenantId();

            return null;
        }).when(billingWebhookEventHandler)
            .handle(any());

        billingWebhookFacade.processWebhookEvent(payload, signPayload(payload, WEBHOOK_SECRET));

        assertThat(tenantIdDuringHandling[0]).isEqualTo("public");
    }

    @Test
    void testProcessWebhookEventThrowsWhenTenantIdMetadataMissing() throws Exception {
        String payload = unknownEventPayload();

        assertThatThrownBy(
            () -> billingWebhookFacade.processWebhookEvent(payload, signPayload(payload, WEBHOOK_SECRET)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Missing tenantId metadata in invoice.payment_succeeded webhook event");
    }

    @Test
    void testProcessWebhookEventHandlesUnrecognizedEventTypeWhenTenantIdPresent() throws Exception {
        String payload = unknownEventPayloadWithTenantId("acme");

        String[] tenantIdDuringHandling = new String[1];

        doAnswer(invocation -> {
            tenantIdDuringHandling[0] = TenantContext.getCurrentTenantId();

            return null;
        }).when(billingWebhookEventHandler)
            .handle(any());

        billingWebhookFacade.processWebhookEvent(payload, signPayload(payload, WEBHOOK_SECRET));

        assertThat(tenantIdDuringHandling[0]).isEqualTo("acme");
    }

    @Test
    void testProcessWebhookEventPropagatesInvalidSignature() {
        String payload = subscriptionUpdatedPayload(SUBSCRIPTION_ID, "public");

        doThrow(new InvalidWebhookSignatureException("Invalid Stripe signature", null))
            .when(stripeClient)
            .verifyWebhookSignature(eq(payload), eq("t=1,v1=invalid"));

        assertThatThrownBy(() -> billingWebhookFacade.processWebhookEvent(payload, "t=1,v1=invalid"))
            .isInstanceOf(InvalidWebhookSignatureException.class)
            .hasMessage("Invalid Stripe signature");
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private String checkoutSessionCompletedPayload(String tenantId) {
        return """
            {
              "id": "evt_checkout_completed",
              "object": "event",
              "api_version": "2026-04-22.dahlia",
              "type": "checkout.session.completed",
              "data": {
                "object": {
                  "id": "cs_test_checkout",
                  "object": "checkout.session",
                  "client_reference_id": "%s",
                  "customer": "cus_checkout",
                  "subscription": "sub_checkout_test",
                  "metadata": { "planName": "STARTER", "tenantId": "%s" }
                }
              }
            }
            """.formatted(tenantId, tenantId);
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private String subscriptionUpdatedPayload(String subscriptionId, String tenantId) {
        return """
            {
              "id": "evt_sub_updated",
              "object": "event",
              "api_version": "2026-04-22.dahlia",
              "type": "customer.subscription.updated",
              "data": {
                "object": {
                  "id": "%s",
                  "object": "subscription",
                  "status": "active",
                  "metadata": { "planName": "STARTER", "tenantId": "%s" },
                  "items": { "object": "list", "data": [], "has_more": false, "url": "/v1/subscription_items" }
                }
              }
            }
            """.formatted(subscriptionId, tenantId);
    }

    private String unknownEventPayload() {
        return """
            {
              "id": "evt_invoice_paid",
              "object": "event",
              "api_version": "2026-04-22.dahlia",
              "type": "invoice.payment_succeeded",
              "data": {
                "object": {
                  "id": "in_test123",
                  "object": "invoice"
                }
              }
            }
            """;
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private String unknownEventPayloadWithTenantId(String tenantId) {
        return """
            {
              "id": "evt_invoice_paid",
              "object": "event",
              "api_version": "2026-04-22.dahlia",
              "type": "invoice.payment_succeeded",
              "data": {
                "object": {
                  "id": "in_test123",
                  "object": "invoice",
                  "metadata": { "tenantId": "%s" }
                }
              }
            }
            """.formatted(tenantId);
    }

    private String signPayload(String payload, String secret) throws Exception {
        long timestamp = Instant.now()
            .getEpochSecond();
        String signedPayload = timestamp + "." + payload;

        Mac mac = Mac.getInstance("HmacSHA256");

        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

        String signature = HexFormat.of()
            .formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));

        return "t=" + timestamp + ",v1=" + signature;
    }
}
