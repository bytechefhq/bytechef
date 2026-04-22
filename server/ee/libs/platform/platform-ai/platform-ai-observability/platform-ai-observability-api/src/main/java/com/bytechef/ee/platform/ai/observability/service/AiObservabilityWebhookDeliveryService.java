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
 * Workspace-agnostic delivery-row CRUD. The actual fan-out (signing + retry to subscribed endpoints) lives on the
 * automation-side {@code WorkspaceAiObservabilityWebhookDeliveryService} since it's coupled to gateway events.
 *
 * @version ee
 */
public interface AiObservabilityWebhookDeliveryService {

    AiObservabilityWebhookDelivery create(AiObservabilityWebhookDelivery delivery);

    List<AiObservabilityWebhookDelivery> getDeliveriesBySubscription(long subscriptionId);

    AiObservabilityWebhookDelivery update(AiObservabilityWebhookDelivery delivery);
}
