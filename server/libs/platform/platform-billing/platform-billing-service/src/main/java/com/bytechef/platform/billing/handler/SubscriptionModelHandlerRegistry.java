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

import com.bytechef.platform.billing.domain.BillingSubscription.SubscriptionModel;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author Matija Petanjek
 */
@Component
@ConditionalOnProperty(prefix = "billing", name = "enabled", havingValue = "true")
public class SubscriptionModelHandlerRegistry {

    private final Map<SubscriptionModel, SubscriptionModelHandler> handlers = new EnumMap<>(SubscriptionModel.class);

    public SubscriptionModelHandlerRegistry(List<SubscriptionModelHandler> handlers) {
        for (SubscriptionModelHandler handler : handlers) {
            this.handlers.put(handler.getSubscriptionModel(), handler);
        }
    }

    public SubscriptionModelHandler getSubscriptionModelHandler(SubscriptionModel subscriptionModel) {
        SubscriptionModelHandler handler = handlers.get(subscriptionModel);

        if (handler == null) {
            throw new IllegalArgumentException("No handler registered for subscription model: " + subscriptionModel);
        }

        return handler;
    }
}
