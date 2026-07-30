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

/**
 * @author Matija Petanjek
 */
public enum StripeWebhookEvent {
    CHECKOUT_SESSION_COMPLETED("checkout.session.completed"),
    CUSTOMER_SUBSCRIPTION_DELETED("customer.subscription.deleted"),
    CUSTOMER_SUBSCRIPTION_UPDATED("customer.subscription.updated");

    private String type;

    StripeWebhookEvent(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
