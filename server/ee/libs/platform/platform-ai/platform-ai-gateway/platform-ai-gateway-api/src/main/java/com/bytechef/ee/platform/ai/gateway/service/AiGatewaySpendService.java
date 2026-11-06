/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.service;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewaySpendSummary;
import com.bytechef.ee.platform.ai.llm.usage.Money;
import java.time.Instant;
import java.util.List;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiGatewaySpendService {

    AiGatewaySpendSummary create(AiGatewaySpendSummary summary);

    List<AiGatewaySpendSummary> getSpendSummaries(Instant start, Instant end);

    List<AiGatewaySpendSummary> getSpendSummariesByWorkspaceId(long workspaceId, Instant start, Instant end);

    List<AiGatewaySpendSummary> getSpendSummariesWithoutWorkspace(Instant start, Instant end);

    Money getTotalCostByConnectedUserId(long connectedUserId, Instant start, Instant end);
}
