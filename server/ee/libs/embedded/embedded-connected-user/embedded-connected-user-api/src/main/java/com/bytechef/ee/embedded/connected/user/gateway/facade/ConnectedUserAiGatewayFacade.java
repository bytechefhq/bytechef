/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.gateway.facade;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 * @author Ivica Cardic
 */
public interface ConnectedUserAiGatewayFacade {

    void assignRoutingPolicy(long connectedUserId, long routingPolicyId);

    AiGatewayProvider createProvider(long connectedUserId, AiGatewayProvider provider);

    void deleteProvider(long connectedUserId, long providerId);

    void unassignRoutingPolicy(long connectedUserId);

    void updateBudgetCap(long connectedUserId, @Nullable BigDecimal budgetCap);
}
