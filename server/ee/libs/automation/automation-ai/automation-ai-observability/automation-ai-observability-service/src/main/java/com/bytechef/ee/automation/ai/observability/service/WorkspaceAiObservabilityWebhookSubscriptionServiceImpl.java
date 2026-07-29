/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.service;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityWebhookSubscription;
import com.bytechef.ee.platform.ai.observability.repository.AiObservabilityWebhookSubscriptionRepository;
import com.bytechef.ee.platform.ai.observability.service.AiObservabilityWebhookSubscriptionService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 */
@Service
@Transactional
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI")
class WorkspaceAiObservabilityWebhookSubscriptionServiceImpl
    implements WorkspaceAiObservabilityWebhookSubscriptionService {

    private final AiObservabilityWebhookSubscriptionRepository aiObservabilityWebhookSubscriptionRepository;
    private final AiObservabilityWebhookSubscriptionService aiObservabilityWebhookSubscriptionService;

    public WorkspaceAiObservabilityWebhookSubscriptionServiceImpl(
        AiObservabilityWebhookSubscriptionRepository aiObservabilityWebhookSubscriptionRepository,
        AiObservabilityWebhookSubscriptionService aiObservabilityWebhookSubscriptionService) {

        this.aiObservabilityWebhookSubscriptionRepository = aiObservabilityWebhookSubscriptionRepository;
        this.aiObservabilityWebhookSubscriptionService = aiObservabilityWebhookSubscriptionService;
    }

    @Override
    public AiObservabilityWebhookSubscription createInWorkspace(
        AiObservabilityWebhookSubscription subscription, long workspaceId) {

        subscription.setWorkspaceId(workspaceId);

        return aiObservabilityWebhookSubscriptionService.create(subscription);
    }

    @Override
    public void delete(long id) {
        aiObservabilityWebhookSubscriptionService.delete(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityWebhookSubscription> getEnabledWebhookSubscriptionsByWorkspace(Long workspaceId) {
        return aiObservabilityWebhookSubscriptionRepository.findAllByWorkspaceIdAndEnabled(workspaceId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityWebhookSubscription> getWebhookSubscriptionsByWorkspace(Long workspaceId) {
        return aiObservabilityWebhookSubscriptionRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long subscriptionId) {
        // findById rather than the service's getWebhookSubscription: an unknown id must still yield null (the
        // pre-collapse "no membership row" answer) because callers use this as an authorization probe.
        return aiObservabilityWebhookSubscriptionRepository.findById(subscriptionId)
            .map(AiObservabilityWebhookSubscription::getWorkspaceId)
            .orElse(null);
    }
}
