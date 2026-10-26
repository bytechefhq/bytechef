/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.domain;

import java.math.BigDecimal;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum AiGatewayModelTier {

    BASIC,
    EFFICIENT,
    STANDARD,
    ADVANCED,
    FRONTIER;

    private static final BigDecimal EFFICIENT_FLOOR = new BigDecimal("0.10");
    private static final BigDecimal STANDARD_FLOOR = new BigDecimal("1.50");
    private static final BigDecimal ADVANCED_FLOOR = new BigDecimal("2.00");
    private static final BigDecimal FRONTIER_FLOOR = new BigDecimal("5.00");

    public static AiGatewayModelTier classify(BigDecimal outputCostPerMTokens) {
        if (outputCostPerMTokens == null) {
            return FRONTIER;
        }

        if (outputCostPerMTokens.compareTo(FRONTIER_FLOOR) >= 0) {
            return FRONTIER;
        }

        if (outputCostPerMTokens.compareTo(ADVANCED_FLOOR) >= 0) {
            return ADVANCED;
        }

        if (outputCostPerMTokens.compareTo(STANDARD_FLOOR) >= 0) {
            return STANDARD;
        }

        if (outputCostPerMTokens.compareTo(EFFICIENT_FLOOR) >= 0) {
            return EFFICIENT;
        }

        return BASIC;
    }
}
