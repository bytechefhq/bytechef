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

package com.bytechef.platform.billing.client;

import com.stripe.model.Event;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;

/**
 * @author Matija Petanjek
 */
public interface StripeClient {

    void cancelAtPeriodEnd(String stripeSubscriptionId, String tenantId);

    void reactivateSubscription(String stripeSubscriptionId, String tenantId);

    String fetchProductDefaultPriceId(String productId);

    void upgradeSubscriptionNow(
        String subscriptionId, String existingFlatItemId, String newFlatPriceId, String planName, String tenantId);

    void scheduleDowngrade(
        String subscriptionId, String existingFlatItemId, String existingMeteredItemId, String newFlatPriceId,
        String newMeteredPriceId, String planName, String tenantId, long currentPeriodEnd);

    void releaseSubscriptionScheduleIfPresent(Subscription subscription);

    String createCustomer(String email, String tenantId);

    Session createCheckoutSession(
        String customerId, String flatPriceId, String usagePriceId, String planName, String successUrl,
        String cancelUrl, String tenantId);

    Price retrievePrice(String priceId);

    Subscription retrieveSubscription(String subscriptionId);

    Event verifyWebhookSignature(String payload, String sigHeader);

    void reportMeterEvent(String customerId, int value, String idempotencyKey);
}
