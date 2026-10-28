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

package com.bytechef.platform.billing.handler.prepaid;

import com.stripe.model.LineItem;
import com.stripe.model.checkout.Session;
import java.util.List;

/**
 * Stripe calls used only by the PREPAID subscription model.
 *
 * @author Matija Petanjek
 */
public interface PrepaidStripeClient {

    Session createSubscriptionCheckoutSession(
        String customerId, String flatPriceId, String planName, String successUrl, String cancelUrl,
        String tenantId);

    void scheduleDowngrade(
        String subscriptionId, String newFlatPriceId, String planName, String tenantId, long currentPeriodEnd);

    Session createJobExecutionsPackageCheckoutSession(
        String customerId, String packagePriceId, long quantity, String successUrl, String cancelUrl,
        String tenantId);

    List<LineItem> retrieveCheckoutSessionLineItems(String sessionId);
}
