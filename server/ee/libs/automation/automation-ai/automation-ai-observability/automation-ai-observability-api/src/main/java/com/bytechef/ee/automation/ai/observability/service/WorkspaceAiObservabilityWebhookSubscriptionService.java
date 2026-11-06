/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.service;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityWebhookSubscription;
import java.util.List;

/**
 * @version ee
 */
public interface WorkspaceAiObservabilityWebhookSubscriptionService {

    AiObservabilityWebhookSubscription createInWorkspace(
        AiObservabilityWebhookSubscription subscription, long workspaceId);

    void delete(long id);

    List<AiObservabilityWebhookSubscription> getEnabledWebhookSubscriptionsByWorkspace(Long workspaceId);

    List<AiObservabilityWebhookSubscription> getWebhookSubscriptionsByWorkspace(Long workspaceId);

    Long getWorkspaceId(long subscriptionId);
}
