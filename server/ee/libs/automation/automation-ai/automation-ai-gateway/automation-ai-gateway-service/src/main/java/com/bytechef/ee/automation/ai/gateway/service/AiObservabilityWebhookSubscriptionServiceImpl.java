/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiObservabilityWebhookSubscription;
import com.bytechef.ee.automation.ai.gateway.repository.AiObservabilityWebhookSubscriptionRepository;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.apache.commons.lang3.Validate;
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
class AiObservabilityWebhookSubscriptionServiceImpl implements AiObservabilityWebhookSubscriptionService {

    private final AiObservabilityWebhookSubscriptionRepository aiObservabilityWebhookSubscriptionRepository;

    public AiObservabilityWebhookSubscriptionServiceImpl(
        AiObservabilityWebhookSubscriptionRepository aiObservabilityWebhookSubscriptionRepository) {

        this.aiObservabilityWebhookSubscriptionRepository = aiObservabilityWebhookSubscriptionRepository;
    }

    @Override
    public AiObservabilityWebhookSubscription create(AiObservabilityWebhookSubscription subscription) {
        Validate.notNull(subscription, "subscription must not be null");
        Validate.isTrue(subscription.getId() == null, "subscription id must be null for creation");

        return aiObservabilityWebhookSubscriptionRepository.save(subscription);
    }

    @Override
    public void delete(long id) {
        aiObservabilityWebhookSubscriptionRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AiObservabilityWebhookSubscription getWebhookSubscription(long id) {
        return aiObservabilityWebhookSubscriptionRepository.findById(id)
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "AiObservabilityWebhookSubscription not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityWebhookSubscription> getWebhookSubscriptionsByWorkspace(Long workspaceId) {
        return aiObservabilityWebhookSubscriptionRepository.findAllByWorkspaceId(workspaceId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiObservabilityWebhookSubscription> getEnabledWebhookSubscriptionsByWorkspace(Long workspaceId) {
        return aiObservabilityWebhookSubscriptionRepository.findAllByWorkspaceIdAndEnabled(workspaceId, true);
    }

    @Override
    public AiObservabilityWebhookSubscription update(AiObservabilityWebhookSubscription subscription) {
        Validate.notNull(subscription, "subscription must not be null");
        Validate.notNull(subscription.getId(), "subscription id must not be null for update");

        return aiObservabilityWebhookSubscriptionRepository.save(subscription);
    }
}
