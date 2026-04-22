/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityWebhookDelivery;
import java.util.List;
import java.util.Map;

/**
 * Service for delivering webhook payloads to subscribed endpoints with HMAC-SHA256 signing and exponential backoff
 * retry (3 attempts).
 *
 * @version ee
 */
public interface AiObservabilityWebhookDeliveryService {

    AiObservabilityWebhookDelivery create(AiObservabilityWebhookDelivery delivery);

    void deliverEvent(Long workspaceId, String eventType, Map<String, Object> payload);

    void deliverTestEvent(long subscriptionId);

    List<AiObservabilityWebhookDelivery> getDeliveriesBySubscription(long subscriptionId);

    AiObservabilityWebhookDelivery update(AiObservabilityWebhookDelivery delivery);
}
