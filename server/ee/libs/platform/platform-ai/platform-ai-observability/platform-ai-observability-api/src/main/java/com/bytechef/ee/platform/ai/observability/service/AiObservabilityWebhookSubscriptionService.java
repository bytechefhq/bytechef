/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.service;

import com.bytechef.ee.platform.ai.observability.domain.AiObservabilityWebhookSubscription;

/**
 * @version ee
 */
public interface AiObservabilityWebhookSubscriptionService {

    AiObservabilityWebhookSubscription create(AiObservabilityWebhookSubscription subscription);

    void delete(long id);

    AiObservabilityWebhookSubscription getWebhookSubscription(long id);

    AiObservabilityWebhookSubscription update(AiObservabilityWebhookSubscription subscription);
}
