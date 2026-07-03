/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.usage;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration-driven cost rates consumed by {@link DefaultCostEstimator}.
 *
 * <p>
 * LLM rates are USD per 1M tokens (input + output, separately) keyed by model name. Tool rates are USD per unit of work
 * (typically per call). Unknown models / tools resolve to {@link BigDecimal#ZERO}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ConfigurationProperties(prefix = "bytechef.ai-hub.cost-estimation")
public class CostEstimationProperties {

    private final Map<String, ModelRate> llmRates = new LinkedHashMap<>();
    private final Map<String, BigDecimal> toolRates = new LinkedHashMap<>();

    /**
     * Spring's {@code @ConfigurationProperties} binder populates the returned map in-place, so the live reference must
     * be exposed (a defensive copy would silently drop bound entries).
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Map<String, ModelRate> getLlmRates() {
        return llmRates;
    }

    /**
     * Spring's {@code @ConfigurationProperties} binder populates the returned map in-place, so the live reference must
     * be exposed (a defensive copy would silently drop bound entries).
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Map<String, BigDecimal> getToolRates() {
        return toolRates;
    }

    /**
     * USD per 1,000,000 tokens for one model. {@link #input} is the prompt-side rate; {@link #output} is the
     * completion-side rate.
     */
    public static class ModelRate {

        private BigDecimal input = BigDecimal.ZERO;
        private BigDecimal output = BigDecimal.ZERO;

        public BigDecimal getInput() {
            return input;
        }

        public void setInput(BigDecimal input) {
            this.input = input;
        }

        public BigDecimal getOutput() {
            return output;
        }

        public void setOutput(BigDecimal output) {
            this.output = output;
        }
    }
}
