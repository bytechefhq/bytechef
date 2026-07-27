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

import com.bytechef.platform.billing.dto.BillingSubscriptionDTO;
import java.util.Optional;

/**
 * @author Matija Petanjek
 */
public interface BillingSubscriptionFacade {

    void cancelSubscription();

    String createCheckoutSession(String planName);

    void reactivateSubscription();

    void updateSubscription(String planName);

    /**
     * Verifies that the webhook payload was genuinely sent by Stripe. Throws if the signature is invalid. Callers must
     * do this before trusting any data read out of {@code payload}, e.g. via {@link #extractTenantId(String)}.
     */
    void verifyWebhookSignature(String payload, String stripeSignatureHeader);

    /**
     * Resolves the tenant a (already signature-verified) webhook event belongs to, without touching the database.
     * Callers must establish this tenant as the current {@code TenantContext} before invoking
     * {@link #handleWebhookEvent(String, String)}, since that method opens a transaction (and therefore acquires a
     * tenant-scoped database connection) as soon as it is entered.
     */
    String extractTenantId(String payload);

    void handleWebhookEvent(String payload, String stripeSignatureHeader);

    Optional<BillingSubscriptionDTO> fetchCurrentSubscription();
}
