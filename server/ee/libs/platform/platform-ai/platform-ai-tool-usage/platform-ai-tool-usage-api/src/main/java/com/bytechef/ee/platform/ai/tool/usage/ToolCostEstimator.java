/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.tool.usage;

import java.math.BigDecimal;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface ToolCostEstimator {

    BigDecimal estimateToolCost(String toolName, int unitCount);
}
