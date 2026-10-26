/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayBudget;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGatewayBudgetService {

    AiGatewayBudget create(AiGatewayBudget budget);

    void delete(long id);

    Optional<AiGatewayBudget> fetchBudget(long id);

    Optional<AiGatewayBudget> fetchBudgetByWorkspaceId(long workspaceId);

    AiGatewayBudget getBudget(long id);

    AiGatewayBudget update(AiGatewayBudget budget);
}
