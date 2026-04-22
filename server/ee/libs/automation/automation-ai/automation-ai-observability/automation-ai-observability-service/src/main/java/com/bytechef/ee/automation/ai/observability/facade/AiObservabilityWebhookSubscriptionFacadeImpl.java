/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.facade;

import com.bytechef.ee.automation.ai.observability.service.WorkspaceAiObservabilityWebhookSubscriptionService;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityWebhookSubscription;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AiObservabilityWebhookSubscriptionFacade}. Delegates to the shared
 * {@code WorkspaceAiObservabilityWebhookSubscriptionService} and carries the authorization guard so it is enforced for
 * every caller of the facade rather than only the GraphQL entry point.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class AiObservabilityWebhookSubscriptionFacadeImpl implements AiObservabilityWebhookSubscriptionFacade {

    private final WorkspaceAiObservabilityWebhookSubscriptionService workspaceAiObservabilityWebhookSubscriptionService;

    @SuppressFBWarnings("EI")
    AiObservabilityWebhookSubscriptionFacadeImpl(
        WorkspaceAiObservabilityWebhookSubscriptionService workspaceAiObservabilityWebhookSubscriptionService) {

        this.workspaceAiObservabilityWebhookSubscriptionService = workspaceAiObservabilityWebhookSubscriptionService;
    }

    @Override
    @PreAuthorize("hasPermission(#workspaceId, 'Workspace', 'AI_GATEWAY_VIEW')")
    public List<AiObservabilityWebhookSubscription> getWebhookSubscriptionsByWorkspace(Long workspaceId) {
        return workspaceAiObservabilityWebhookSubscriptionService.getWebhookSubscriptionsByWorkspace(workspaceId);
    }
}
