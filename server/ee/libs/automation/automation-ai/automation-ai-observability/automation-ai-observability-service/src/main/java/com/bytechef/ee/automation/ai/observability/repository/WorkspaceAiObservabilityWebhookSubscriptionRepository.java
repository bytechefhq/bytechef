/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.observability.repository;

import com.bytechef.ee.automation.ai.observability.domain.WorkspaceAiObservabilityWebhookSubscription;
import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityWebhookSubscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * @version ee
 */
public interface WorkspaceAiObservabilityWebhookSubscriptionRepository
    extends ListCrudRepository<WorkspaceAiObservabilityWebhookSubscription, Long> {

    Optional<WorkspaceAiObservabilityWebhookSubscription> findByAiObservabilityWebhookSubscriptionId(
        long aiObservabilityWebhookSubscriptionId);

    @Query("""
        SELECT ai_observability_webhook_subscription.*
        FROM ai_observability_webhook_subscription
        JOIN workspace_ai_observability_webhook_subscription
            ON workspace_ai_observability_webhook_subscription.ai_observability_webhook_subscription_id
                 = ai_observability_webhook_subscription.id
        WHERE workspace_ai_observability_webhook_subscription.workspace_id = :workspaceId
        """)
    List<AiObservabilityWebhookSubscription> findAllSubscriptionsByWorkspaceId(
        @Param("workspaceId") Long workspaceId);

    @Query("""
        SELECT ai_observability_webhook_subscription.*
        FROM ai_observability_webhook_subscription
        JOIN workspace_ai_observability_webhook_subscription
            ON workspace_ai_observability_webhook_subscription.ai_observability_webhook_subscription_id
                 = ai_observability_webhook_subscription.id
        WHERE workspace_ai_observability_webhook_subscription.workspace_id = :workspaceId
          AND ai_observability_webhook_subscription.enabled = :enabled
        """)
    List<AiObservabilityWebhookSubscription> findAllSubscriptionsByWorkspaceIdAndEnabled(
        @Param("workspaceId") Long workspaceId, @Param("enabled") boolean enabled);
}
