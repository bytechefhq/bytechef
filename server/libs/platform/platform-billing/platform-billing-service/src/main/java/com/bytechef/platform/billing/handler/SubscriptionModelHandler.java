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

package com.bytechef.platform.billing.handler;

import com.bytechef.platform.billing.domain.BillingSubscription;
import com.bytechef.platform.billing.domain.BillingSubscription.SubscriptionModel;
import com.bytechef.platform.billing.dto.BillingSubscriptionDTO;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;

/**
 * Billing behavior of one {@link SubscriptionModel}. The facade and the webhook handler look up the subscription and
 * route to the handler registered for its model.
 *
 * @author Matija Petanjek
 */
public interface SubscriptionModelHandler {

    SubscriptionModel getSubscriptionModel();

    String createCheckoutSession(String planName);

    void updateSubscription(BillingSubscription currentSubscription, String newPlanName);

    void cancelSubscription(BillingSubscription currentSubscription);

    void reactivateSubscription(BillingSubscription currentSubscription);

    BillingSubscriptionDTO toBillingSubscriptionDTO(BillingSubscription subscription);

    BillingSubscription handleCheckoutSessionCompleted(Session session);

    BillingSubscription handleSubscriptionUpdated(BillingSubscription subscription, Subscription stripeSubscription);

    BillingSubscription handleSubscriptionDeleted(BillingSubscription subscription);
}
