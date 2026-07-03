/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.usage;

import com.bytechef.ee.platform.ai.llm.usage.LlmCostEstimator;
import com.bytechef.ee.platform.ai.tool.usage.ToolCostEstimator;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Bridges AI Hub's per-deployment rate file (see {@link CostEstimationProperties}) into the two shared cost-estimation
 * contracts that the platform-ai usage modules expose. LLM rates are USD per 1,000,000 tokens; tool rates are USD per
 * unit. Unknown models / tools log a single WARN and return {@link BigDecimal#ZERO} so a missing rate never breaks an
 * end-user request.
 *
 * <p>
 * Implements both {@link LlmCostEstimator} (consumed by {@code platform-ai-llm-usage}) and {@link ToolCostEstimator}
 * (consumed by {@code platform-ai-tool-usage}). The shared modules' default fallback beans are gated on
 * {@code @ConditionalOnMissingBean}, so registering this {@code @Component} in a AI Hub deployment automatically
 * replaces both fallbacks with rate-driven estimators.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class DefaultCostEstimator implements LlmCostEstimator, ToolCostEstimator {

    private static final Logger log = LoggerFactory.getLogger(DefaultCostEstimator.class);

    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");

    private static final int COST_SCALE = 6;

    private final CostEstimationProperties properties;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public DefaultCostEstimator(CostEstimationProperties properties) {
        this.properties = properties;
    }

    @Override
    public BigDecimal estimateLlmCost(String model, int inputTokens, int outputTokens) {
        if (model == null || model.isBlank()) {
            log.warn("Cannot estimate LLM cost: model is null/blank; charging 0.0");

            return BigDecimal.ZERO;
        }

        CostEstimationProperties.ModelRate rate = properties.getLlmRates()
            .get(model);

        if (rate == null) {
            log.warn("No LLM cost rate configured for model '{}'; charging 0.0", model);

            return BigDecimal.ZERO;
        }

        BigDecimal inputCost = rate.getInput()
            .multiply(BigDecimal.valueOf(Math.max(inputTokens, 0)))
            .divide(ONE_MILLION, COST_SCALE, RoundingMode.HALF_UP);
        BigDecimal outputCost = rate.getOutput()
            .multiply(BigDecimal.valueOf(Math.max(outputTokens, 0)))
            .divide(ONE_MILLION, COST_SCALE, RoundingMode.HALF_UP);

        return inputCost.add(outputCost);
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
