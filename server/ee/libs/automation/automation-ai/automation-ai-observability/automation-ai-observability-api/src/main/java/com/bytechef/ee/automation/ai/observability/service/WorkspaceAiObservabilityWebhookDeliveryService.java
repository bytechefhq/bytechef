/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.service;

import java.util.Map;

/**
 * Workspace-scoped fan-out for webhook deliveries. Routes events through subscriptions belonging to the given
 * workspace; the workspace-agnostic delivery-row CRUD lives on the platform-side
 * {@code AiObservabilityWebhookDeliveryService}.
 *
 * @version ee
 */
public interface WorkspaceAiObservabilityWebhookDeliveryService {

    void deliverEvent(Long workspaceId, String eventType, Map<String, Object> payload);

    void deliverTestEvent(long subscriptionId);
}
