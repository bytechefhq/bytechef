/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewayBudget;
import java.util.Optional;

/**
 * @version ee
 */
public interface AiGatewayBudgetService {

    AiGatewayBudget create(AiGatewayBudget budget);

    void delete(long id);

    AiGatewayBudget getBudget(long id);

    Optional<AiGatewayBudget> getBudgetByWorkspaceId(long workspaceId);

    AiGatewayBudget update(AiGatewayBudget budget);
}
