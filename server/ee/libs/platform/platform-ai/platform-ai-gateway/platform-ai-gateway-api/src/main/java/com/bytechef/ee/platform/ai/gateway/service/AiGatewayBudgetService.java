/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayBudget;

/**
 * Workspace-agnostic CRUD for {@link AiGatewayBudget}. Workspace association + by-workspace lookups live on
 * {@code WorkspaceAiGatewayBudgetService} in automation.
 *
 * @version ee
 */
public interface AiGatewayBudgetService {

    AiGatewayBudget create(AiGatewayBudget budget);

    void delete(long id);

    AiGatewayBudget getBudget(long id);

    AiGatewayBudget update(AiGatewayBudget budget);
}
