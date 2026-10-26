/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.routing;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModelDeployment;
import com.bytechef.ee.platform.ai.model.catalog.domain.AiModel;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CostOptimizedRoutingStrategy implements AiGatewayRoutingStrategy {

    private static final Logger log = LoggerFactory.getLogger(CostOptimizedRoutingStrategy.class);

    @Override
    public AiGatewayModelDeployment selectDeployment(
        List<AiGatewayModelDeployment> deployments, AiGatewayRoutingContext context) {
        return deployments.stream()
            .min(Comparator.comparing(deployment -> {
                AiModel model = context.modelMap()
                    .get(deployment.getModelId());

                if (model == null || model.getInputCostPerMTokens() == null) {
                    log.debug("No pricing data for deployment (modelId={}), deprioritizing in cost routing",
                        deployment.getModelId());

                    return BigDecimal.valueOf(Long.MAX_VALUE);
                }

                return model.getInputCostPerMTokens();
            }))
            .orElse(deployments.getFirst());
    }
}
