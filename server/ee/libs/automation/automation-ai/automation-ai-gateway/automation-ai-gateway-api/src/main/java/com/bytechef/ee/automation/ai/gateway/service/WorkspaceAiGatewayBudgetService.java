/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayBudget;
import java.util.Optional;

/**
 * @version ee
 */
public interface WorkspaceAiGatewayBudgetService {

    AiGatewayBudget createInWorkspace(AiGatewayBudget budget, long workspaceId);

    Optional<AiGatewayBudget> getBudgetByWorkspaceId(long workspaceId);

    Long getWorkspaceId(long budgetId);
}
