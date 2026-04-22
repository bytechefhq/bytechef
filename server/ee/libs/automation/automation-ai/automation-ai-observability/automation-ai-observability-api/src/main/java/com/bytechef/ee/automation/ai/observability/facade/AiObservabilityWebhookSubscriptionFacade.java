/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.facade;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityWebhookSubscription;
import java.util.List;

/**
 * Facade for querying AI Observability webhook subscriptions by workspace. Hosts the workspace-role authorization guard
 * so it applies to every caller of the facade rather than only the GraphQL entry point, and keeps it off the shared
 * {@code WorkspaceAiObservabilityWebhookSubscriptionService}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiObservabilityWebhookSubscriptionFacade {

    List<AiObservabilityWebhookSubscription> getWebhookSubscriptionsByWorkspace(Long workspaceId);
}
