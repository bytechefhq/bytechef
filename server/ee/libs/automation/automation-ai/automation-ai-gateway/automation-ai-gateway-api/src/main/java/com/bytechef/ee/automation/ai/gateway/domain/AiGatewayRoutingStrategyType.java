/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

/**
 * @version ee
 */
public enum AiGatewayRoutingStrategyType {

    COST_OPTIMIZED,
    INTELLIGENT_BALANCED,
    INTELLIGENT_COST,
    INTELLIGENT_QUALITY,
    LATENCY_OPTIMIZED,
    PRIORITY_FALLBACK,
    SIMPLE,
    TAG_BASED,
    WEIGHTED_RANDOM;
}
