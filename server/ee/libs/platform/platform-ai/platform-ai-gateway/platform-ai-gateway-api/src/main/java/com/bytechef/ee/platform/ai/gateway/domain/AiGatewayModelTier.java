/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.domain;

import java.math.BigDecimal;

/**
 * Cost tier classification for gateway models, ordered by capability ascending (BASIC cheapest, FRONTIER most capable).
 * Thresholds are USD per 1M output tokens, mirroring Merge Gateway's intelligent routing tiers. A model with unknown
 * (null) pricing is treated as the most capable tier so it is never silently selected as the "cheap" option.
 *
 * <p>
 * The threshold spacing (notably the narrow $1.50–$5.00 STANDARD/ADVANCED band) is pinned to Merge Gateway's published
 * tiers, not chosen for even width; do not "rebalance" the bands without re-checking that reference.
 *
 * @version ee
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
