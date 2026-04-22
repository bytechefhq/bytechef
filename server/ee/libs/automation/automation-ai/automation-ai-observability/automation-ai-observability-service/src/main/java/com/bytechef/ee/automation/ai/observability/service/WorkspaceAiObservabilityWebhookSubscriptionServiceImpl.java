/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.service;

import com.bytechef.ee.automation.ai.observability.domain.WorkspaceAiObservabilityWebhookSubscription;
import com.bytechef.ee.automation.ai.observability.repository.WorkspaceAiObservabilityWebhookSubscriptionRepository;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityWebhookSubscription;
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

    private final AiObservabilityWebhookSubscriptionService aiObservabilityWebhookSubscriptionService;
    private final WorkspaceAiObservabilityWebhookSubscriptionRepository workspaceAiObservabilityWebhookSubscriptionRepository;

    public WorkspaceAiObservabilityWebhookSubscriptionServiceImpl(
        AiObservabilityWebhookSubscriptionService aiObservabilityWebhookSubscriptionService,
        WorkspaceAiObservabilityWebhookSubscriptionRepository workspaceAiObservabilityWebhookSubscriptionRepository) {

        this.aiObservabilityWebhookSubscriptionService = aiObservabilityWebhookSubscriptionService;
        this.workspaceAiObservabilityWebhookSubscriptionRepository =
            workspaceAiObservabilityWebhookSubscriptionRepository;
    }

    @Override
    public AiObservabilityWebhookSubscription createInWorkspace(
        AiObservabilityWebhookSubscription subscription, long workspaceId) {

        AiObservabilityWebhookSubscription saved = aiObservabilityWebhookSubscriptionService.create(subscription);

        workspaceAiObservabilityWebhookSubscriptionRepository.save(
            new WorkspaceAiObservabilityWebhookSubscription(saved.getId(), workspaceId));

        return saved;
    }

    @Override
    public void delete(long id) {
        workspaceAiObservabilityWebhookSubscriptionRepository.findByAiObservabilityWebhookSubscriptionId(id)
            .ifPresent(membership -> workspaceAiObservabilityWebhookSubscriptionRepository.deleteById(
                membership.getId()));

        aiObservabilityWebhookSubscriptionService.delete(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityWebhookSubscription> getEnabledWebhookSubscriptionsByWorkspace(Long workspaceId) {
        return workspaceAiObservabilityWebhookSubscriptionRepository
            .findAllSubscriptionsByWorkspaceIdAndEnabled(workspaceId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityWebhookSubscription> getWebhookSubscriptionsByWorkspace(Long workspaceId) {
        return workspaceAiObservabilityWebhookSubscriptionRepository.findAllSubscriptionsByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getWorkspaceId(long subscriptionId) {
        return workspaceAiObservabilityWebhookSubscriptionRepository
            .findByAiObservabilityWebhookSubscriptionId(subscriptionId)
            .map(WorkspaceAiObservabilityWebhookSubscription::getWorkspaceId)
            .orElse(null);
    }
}
