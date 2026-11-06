/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayConnectedUserSettings;
import java.math.BigDecimal;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 */
public interface AiGatewayConnectedUserSettingsService {

    void assignRoutingPolicy(long connectedUserId, long routingPolicyId);

    long countByRoutingPolicyId(long routingPolicyId);

    void deleteByConnectedUserId(long connectedUserId);

    Optional<AiGatewayConnectedUserSettings> fetchByConnectedUserId(long connectedUserId);

    void unassignRoutingPolicy(long connectedUserId);

    void updateBudgetCap(long connectedUserId, @Nullable BigDecimal budgetCap);
}
