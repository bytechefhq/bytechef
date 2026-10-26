/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.metrics;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGatewayMetrics {
    void incrementBudgetExceeded(String model);

    void incrementRateLimitRejection(String rateLimitName);

    void incrementPostRequestBudgetFailure();

    void incrementRequestLogPersistFailure(String kind, String outcome);
}
