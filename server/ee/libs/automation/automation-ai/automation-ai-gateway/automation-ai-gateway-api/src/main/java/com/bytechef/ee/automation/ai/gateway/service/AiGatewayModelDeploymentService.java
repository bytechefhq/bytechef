/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayModelDeployment;
import java.util.List;

/**
 * @version ee
 */
public interface AiGatewayModelDeploymentService {

    AiGatewayModelDeployment create(AiGatewayModelDeployment deployment);

    void delete(long id);

    void deleteByModelId(long modelId);

    void deleteByRoutingPolicyId(long routingPolicyId);

    List<AiGatewayModelDeployment> getDeploymentsByRoutingPolicyId(long routingPolicyId);

    AiGatewayModelDeployment update(AiGatewayModelDeployment deployment);
}
