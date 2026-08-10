/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class AiGatewayModelDeploymentCleanupListenerTest {

    @Test
    void testBeforeDeleteRemovesDeploymentsForModel() {
        AiGatewayModelDeploymentService aiGatewayModelDeploymentService = mock(AiGatewayModelDeploymentService.class);

        new AiGatewayModelDeploymentCleanupListener(aiGatewayModelDeploymentService).beforeDelete(9L);

        verify(aiGatewayModelDeploymentService).deleteByModelId(9L);
    }
}
