/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.event;

import java.math.BigDecimal;

/**
 * Published when a workspace's budget threshold is breached during a pre-request budget check. Fires at-most-once per
 * request; downstream listeners (webhook delivery, alerting) decide how to react.
 *
 * @version ee
 */
public record AiGatewayBudgetExceededEvent(
    Long workspaceId,
    String model,
    BigDecimal currentSpend,
    BigDecimal budgetLimit) {
}
