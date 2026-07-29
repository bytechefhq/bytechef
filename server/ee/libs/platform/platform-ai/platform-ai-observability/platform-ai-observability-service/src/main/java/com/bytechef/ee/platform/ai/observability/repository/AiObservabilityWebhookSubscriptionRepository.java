/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.repository;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityWebhookSubscription;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiObservabilityWebhookSubscriptionRepository
    extends ListCrudRepository<AiObservabilityWebhookSubscription, Long> {

    /**
     * Subscriptions owned by one workspace. A subscription whose {@code workspace_id} is null belongs to no workspace
     * and is invisible here, which is the intended behavior for a workspace-scoped listing. Same for the finder below.
     */
    List<AiObservabilityWebhookSubscription> findAllByWorkspaceId(Long workspaceId);

    List<AiObservabilityWebhookSubscription> findAllByWorkspaceIdAndEnabled(Long workspaceId, boolean enabled);
}
