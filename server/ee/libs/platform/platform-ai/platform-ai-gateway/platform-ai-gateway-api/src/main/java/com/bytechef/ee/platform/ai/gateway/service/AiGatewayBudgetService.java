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
 * CRUD for {@link AiGatewayBudget}. A budget carries its owning workspace in its nullable {@code workspace_id} column;
 * the workspace-facing policy layer is {@code WorkspaceAiGatewayBudgetService} in automation.
 *
 * @version ee
 */
public interface AiGatewayBudgetService {

    AiGatewayBudget create(AiGatewayBudget budget);

    void delete(long id);

    Optional<AiGatewayBudget> fetchBudget(long id);

    Optional<AiGatewayBudget> fetchBudgetByWorkspaceId(long workspaceId);

    AiGatewayBudget getBudget(long id);

    AiGatewayBudget update(AiGatewayBudget budget);
}
