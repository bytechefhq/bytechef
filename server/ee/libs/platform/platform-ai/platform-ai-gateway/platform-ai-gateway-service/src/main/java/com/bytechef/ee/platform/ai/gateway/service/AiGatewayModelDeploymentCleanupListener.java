/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import com.bytechef.ee.platform.ai.model.catalog.service.AiModelDeleteListener;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Cascades AI Gateway model deployments when a catalog model row is deleted. Registered as an
 * {@link AiModelDeleteListener} so the catalog module can cascade without depending on the gateway — the dependency
 * points gateway → catalog, per the extraction's design.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@SuppressFBWarnings("EI2")
class AiGatewayModelDeploymentCleanupListener implements AiModelDeleteListener {

    private final AiGatewayModelDeploymentService aiGatewayModelDeploymentService;

    AiGatewayModelDeploymentCleanupListener(AiGatewayModelDeploymentService aiGatewayModelDeploymentService) {
        this.aiGatewayModelDeploymentService = aiGatewayModelDeploymentService;
    }

    @Override
    public void beforeDelete(long modelId) {
        aiGatewayModelDeploymentService.deleteByModelId(modelId);
    }
}
