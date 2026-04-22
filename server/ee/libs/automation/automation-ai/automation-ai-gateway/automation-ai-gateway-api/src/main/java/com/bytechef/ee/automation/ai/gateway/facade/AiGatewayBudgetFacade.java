/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayBudget;
import java.util.Optional;

/**
 * Facade for managing AI LLM Gateway budgets. Hosts the authorization guards so they apply to every caller of the
 * facade rather than only the GraphQL entry point, and keeps them off the shared services the gateway data plane relies
 * on.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGatewayBudgetFacade {

    Optional<AiGatewayBudget> getBudgetByWorkspaceId(long workspaceId);

    AiGatewayBudget getBudget(long id);

    AiGatewayBudget createInWorkspace(AiGatewayBudget budget, long workspaceId);

    void delete(long id);

    AiGatewayBudget update(AiGatewayBudget budget);
}
