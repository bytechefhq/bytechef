/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.usage;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.config.ApplicationProperties;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class DefaultCostEstimatorTest {

    private DefaultCostEstimator defaultCostEstimator;

    @BeforeEach
    void beforeEach() {
        ApplicationProperties applicationProperties = new ApplicationProperties();

        ApplicationProperties.Ai ai = applicationProperties.getAi();

        ApplicationProperties.Ai.Hub hub = ai.getHub();

        ApplicationProperties.Ai.Hub.CostEstimation costEstimation = hub.getCostEstimation();

        Map<String, BigDecimal> toolRates = costEstimation.getToolRates();

        toolRates.put("firecrawl_scrape", new BigDecimal("0.0015"));

        defaultCostEstimator = new DefaultCostEstimator(applicationProperties);
    }

    @Test
    void testEstimateToolCostMultipliesRateByUnitCount() {
        assertThat(defaultCostEstimator.estimateToolCost("firecrawl_scrape", 3)).isEqualTo(new BigDecimal("0.004500"));
    }

    @Test
    void testEstimateToolCostClampsNegativeUnitCount() {
        assertThat(defaultCostEstimator.estimateToolCost("firecrawl_scrape", -2)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void testEstimateToolCostReturnsZeroForUnknownTool() {
        assertThat(defaultCostEstimator.estimateToolCost("openai_image", 1)).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void testEstimateToolCostReturnsZeroForBlankToolName() {
        assertThat(defaultCostEstimator.estimateToolCost(" ", 1)).isEqualTo(BigDecimal.ZERO);
        assertThat(defaultCostEstimator.estimateToolCost(null, 1)).isEqualTo(BigDecimal.ZERO);
    }
}
