/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.event;

import java.math.BigDecimal;

/**
 * @version ee
 */
public record AiGatewayBudgetExceededEvent(
    Long workspaceId,
    String model,
    BigDecimal currentSpend,
    BigDecimal budgetLimit) {
}
