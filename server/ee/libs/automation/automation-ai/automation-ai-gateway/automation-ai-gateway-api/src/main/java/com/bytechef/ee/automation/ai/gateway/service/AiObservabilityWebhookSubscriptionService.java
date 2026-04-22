/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityWebhookSubscription;
import java.util.List;

/**
 * @version ee
 */
public interface AiObservabilityWebhookSubscriptionService {

    AiObservabilityWebhookSubscription create(AiObservabilityWebhookSubscription subscription);

    void delete(long id);

    AiObservabilityWebhookSubscription getWebhookSubscription(long id);

    List<AiObservabilityWebhookSubscription> getWebhookSubscriptionsByWorkspace(Long workspaceId);

    List<AiObservabilityWebhookSubscription> getEnabledWebhookSubscriptionsByWorkspace(Long workspaceId);

    AiObservabilityWebhookSubscription update(AiObservabilityWebhookSubscription subscription);
}
