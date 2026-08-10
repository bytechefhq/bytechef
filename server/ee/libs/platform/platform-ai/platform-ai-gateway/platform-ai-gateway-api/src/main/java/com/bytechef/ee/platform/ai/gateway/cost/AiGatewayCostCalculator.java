/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.cost;

import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import java.math.BigDecimal;

/**
 * @version ee
 */
public interface AiGatewayCostCalculator {

    BigDecimal calculateCost(AiModel model, int inputTokens, int outputTokens);
}
