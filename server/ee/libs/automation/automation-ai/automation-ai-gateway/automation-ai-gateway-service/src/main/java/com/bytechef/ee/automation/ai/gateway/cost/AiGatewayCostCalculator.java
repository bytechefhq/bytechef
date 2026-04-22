/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.cost;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModel;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
public class AiGatewayCostCalculator {

    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");

    public BigDecimal calculateCost(AiGatewayModel model, int inputTokens, int outputTokens) {
        if (model.getInputCostPerMTokens() == null || model.getOutputCostPerMTokens() == null) {
            throw new IllegalStateException(
                "Model '" + model.getName() + "' (id=" + model.getId() +
                    ") is missing cost configuration. Configure inputCostPerMTokens and " +
                    "outputCostPerMTokens for accurate budget tracking.");
        }

        BigDecimal inputCost = model.getInputCostPerMTokens()
            .multiply(BigDecimal.valueOf(inputTokens))
            .divide(ONE_MILLION, 6, RoundingMode.HALF_UP);

        BigDecimal outputCost = model.getOutputCostPerMTokens()
            .multiply(BigDecimal.valueOf(outputTokens))
            .divide(ONE_MILLION, 6, RoundingMode.HALF_UP);

        return inputCost.add(outputCost);
    }
}
