/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.metrics;

import com.bytechef.ee.platform.ai.gateway.event.AiGatewayBudgetExceededEvent;
import com.bytechef.ee.platform.ai.gateway.event.AiGatewayTraceCompletedEvent;

/**
 * Micrometer counters/timers for AI Gateway traffic. Tags are intentionally low-cardinality (model + status); workspace
 * ID is omitted from metric tags to avoid cardinality explosion — use logs or traces for per-workspace breakdowns.
 *
 * @version ee
 */
public interface AiGatewayMetrics {

    void onTraceCompleted(AiGatewayTraceCompletedEvent event);

    void onBudgetExceeded(AiGatewayBudgetExceededEvent event);

    void incrementRateLimitRejection(String rateLimitName);

    void incrementWebhookDelivery(String eventType, boolean success);

    void incrementWebhookDispatchFailure(String eventType);

    void incrementWebhookDeliveryRowDivergence();

    void incrementWebhookSubscriptionEventsParseFailure();

    void incrementPostRequestBudgetFailure();

    void incrementCleanupFailure();

    void incrementRequestLogPersistFailure(String kind, String outcome);
}
