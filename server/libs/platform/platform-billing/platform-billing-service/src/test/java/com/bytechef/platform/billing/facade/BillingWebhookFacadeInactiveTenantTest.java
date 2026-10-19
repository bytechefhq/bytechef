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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.platform.billing.client.StripeClient;
import com.bytechef.tenant.service.TenantDeletionService;
import com.stripe.net.Webhook;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.databind.ObjectMapper;

/**
 * @author Ivica Cardic
 */
@SuppressWarnings("unchecked")
class BillingWebhookFacadeInactiveTenantTest {

    private static final String WEBHOOK_SECRET = "whsec_test_webhook_secret";
    private static final String PAYLOAD = """
        {
          "id": "evt_sub_updated",
          "object": "event",
          "api_version": "2026-04-22.dahlia",
          "type": "customer.subscription.updated",
          "data": {
            "object": {
              "id": "sub_test",
              "object": "subscription",
              "metadata": { "planName": "STARTER", "tenantId": "000001" }
            }
          }
        }
        """;

    @Test
    void testEventForInactiveTenantIsAcknowledgedAndDropped() throws Exception {
        BillingWebhookEventHandler billingWebhookEventHandler = mock(BillingWebhookEventHandler.class);
        StripeClient stripeClient = mock(StripeClient.class);

        when(stripeClient.verifyWebhookSignature(any(), any()))
            .thenAnswer(
                invocation -> Webhook.constructEvent(
                    invocation.getArgument(0), invocation.getArgument(1), WEBHOOK_SECRET));

        TenantDeletionService tenantDeletionService = mock(TenantDeletionService.class);

        when(tenantDeletionService.isInactive("000001")).thenReturn(true);

        ObjectProvider<TenantDeletionService> tenantDeletionServiceProvider = mock(ObjectProvider.class);

        when(tenantDeletionServiceProvider.getIfAvailable()).thenReturn(tenantDeletionService);

        BillingWebhookFacadeImpl billingWebhookFacade = new BillingWebhookFacadeImpl(
            billingWebhookEventHandler, new ObjectMapper(), stripeClient, tenantDeletionServiceProvider);

        billingWebhookFacade.processWebhookEvent(PAYLOAD, signPayload());

        verifyNoInteractions(billingWebhookEventHandler);
    }

    private static String signPayload() throws Exception {
        long timestamp = Instant.now()
            .getEpochSecond();

        Mac mac = Mac.getInstance("HmacSHA256");

        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));

        String signature = HexFormat.of()
            .formatHex(mac.doFinal((timestamp + "." + PAYLOAD).getBytes(StandardCharsets.UTF_8)));

        return "t=" + timestamp + ",v1=" + signature;
    }
}
