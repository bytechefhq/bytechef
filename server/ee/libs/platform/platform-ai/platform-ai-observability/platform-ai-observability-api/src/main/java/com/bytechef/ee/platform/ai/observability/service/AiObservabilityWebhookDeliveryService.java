/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.service;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityWebhookDelivery;
import java.util.List;

/**
 * @version ee
 */
public interface AiObservabilityWebhookDeliveryService {

    AiObservabilityWebhookDelivery create(AiObservabilityWebhookDelivery delivery);

    List<AiObservabilityWebhookDelivery> getDeliveriesBySubscription(long subscriptionId);

    AiObservabilityWebhookDelivery update(AiObservabilityWebhookDelivery delivery);
}
