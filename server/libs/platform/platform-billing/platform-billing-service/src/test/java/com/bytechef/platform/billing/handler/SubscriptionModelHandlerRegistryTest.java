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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.billing.domain.BillingSubscription.SubscriptionModel;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Matija Petanjek
 */
class SubscriptionModelHandlerRegistryTest {

    @Test
    void testGetSubscriptionModelHandlerReturnsRegisteredHandler() {
        SubscriptionModelHandler meteredHandler = handler(SubscriptionModel.METERED);
        SubscriptionModelHandler prepaidHandler = handler(SubscriptionModel.PREPAID);

        SubscriptionModelHandlerRegistry registry =
            new SubscriptionModelHandlerRegistry(List.of(meteredHandler, prepaidHandler));

        assertThat(registry.getSubscriptionModelHandler(SubscriptionModel.METERED)).isSameAs(meteredHandler);
        assertThat(registry.getSubscriptionModelHandler(SubscriptionModel.PREPAID)).isSameAs(prepaidHandler);
    }

    @Test
    void testGetSubscriptionModelHandlerWithMissingModelThrows() {
        SubscriptionModelHandlerRegistry registry =
            new SubscriptionModelHandlerRegistry(List.of(handler(SubscriptionModel.METERED)));

        assertThatThrownBy(() -> registry.getSubscriptionModelHandler(SubscriptionModel.PREPAID))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testDuplicateHandlerForSameModelThrows() {
        List<SubscriptionModelHandler> handlers =
            List.of(handler(SubscriptionModel.PREPAID), handler(SubscriptionModel.PREPAID));

        assertThatThrownBy(() -> new SubscriptionModelHandlerRegistry(handlers))
            .isInstanceOf(IllegalStateException.class);
    }

    private SubscriptionModelHandler handler(SubscriptionModel subscriptionModel) {
        SubscriptionModelHandler handler = mock(SubscriptionModelHandler.class);

        when(handler.getSubscriptionModel()).thenReturn(subscriptionModel);

        return handler;
    }
}
