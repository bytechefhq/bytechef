/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.usage;

import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.ee.platform.ai.tool.usage.ToolCostEstimator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnAiHub
public class DefaultCostEstimator implements ToolCostEstimator {

    private static final Logger log = LoggerFactory.getLogger(DefaultCostEstimator.class);

    private static final int COST_SCALE = 6;

    private final CostEstimationProperties properties;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DefaultCostEstimator(CostEstimationProperties properties) {
        this.properties = properties;
    }

    @Override
    public BigDecimal estimateToolCost(String toolName, int unitCount) {
        if (toolName == null || toolName.isBlank()) {
            log.warn("Cannot estimate tool cost: toolName is null/blank; charging 0.0");

            return BigDecimal.ZERO;
        }

        BigDecimal rate = properties.getToolRates()
            .get(toolName);

        if (rate == null) {
            log.warn("No tool cost rate configured for tool '{}'; charging 0.0", toolName);

            return BigDecimal.ZERO;
        }

        return rate.multiply(BigDecimal.valueOf(Math.max(unitCount, 0)))
            .setScale(COST_SCALE, RoundingMode.HALF_UP);
    }
}
