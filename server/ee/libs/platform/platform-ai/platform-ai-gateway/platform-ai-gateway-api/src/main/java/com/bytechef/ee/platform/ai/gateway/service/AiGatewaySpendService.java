/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewaySpendSummary;
import java.time.Instant;
import java.util.List;

/**
 * CRUD for {@link AiGatewaySpendSummary}. A summary carries its owning workspace in its nullable {@code workspace_id}
 * column; the workspace-facing policy layer is {@code WorkspaceAiGatewaySpendService} in automation.
 *
 * @version ee
 */
public interface AiGatewaySpendService {

    AiGatewaySpendSummary create(AiGatewaySpendSummary summary);

    List<AiGatewaySpendSummary> getSpendSummaries(Instant start, Instant end);

    List<AiGatewaySpendSummary> getSpendSummariesByWorkspaceId(long workspaceId, Instant start, Instant end);
}
