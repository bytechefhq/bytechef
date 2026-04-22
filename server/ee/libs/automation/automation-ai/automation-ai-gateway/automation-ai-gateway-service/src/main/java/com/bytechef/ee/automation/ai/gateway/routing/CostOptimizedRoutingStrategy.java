/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.routing;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModel;
import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModelDeployment;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Selects the deployment with the lowest input cost per million tokens. Only input cost is considered as a
 * simplification; for workloads where output tokens are significant, consider a different routing strategy. Deployments
 * without pricing are sorted to the end of the list.
 *
 * @version ee
 */
class CostOptimizedRoutingStrategy implements AiGatewayRoutingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(CostOptimizedRoutingStrategy.class);

    @Override
    public AiGatewayModelDeployment selectDeployment(
        List<AiGatewayModelDeployment> deployments, AiGatewayRoutingContext context) {

        return deployments.stream()
            .min(Comparator.comparing(deployment -> {
                AiGatewayModel model = context.modelMap()
                    .get(deployment.getModelId());

                if (model == null || model.getInputCostPerMTokens() == null) {
                    logger.debug("No pricing data for deployment (modelId={}), deprioritizing in cost routing",
                        deployment.getModelId());

                    return BigDecimal.valueOf(Long.MAX_VALUE);
                }

                return model.getInputCostPerMTokens();
            }))
            .orElse(deployments.getFirst());
    }
}
