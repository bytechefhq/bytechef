/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.facade.AiGatewayRequestLogFacade;
import com.bytechef.ee.platform.ai.llm.usage.AiLlmUsage;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for querying AI LLM Gateway request logs.
 *
 * <p>
 * Authorization is enforced on {@link AiGatewayRequestLogFacade}, not here.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiGatewayRequestLogGraphQlController {

    private final AiGatewayRequestLogFacade aiGatewayRequestLogFacade;

    @SuppressFBWarnings("EI")
    AiGatewayRequestLogGraphQlController(AiGatewayRequestLogFacade aiGatewayRequestLogFacade) {
        this.aiGatewayRequestLogFacade = aiGatewayRequestLogFacade;
    }

    @QueryMapping
    public List<AiLlmUsage> aiGatewayRequestLogs(
        @Argument long startDate, @Argument long endDate) {

        Instant start = Instant.ofEpochMilli(startDate);
        Instant end = Instant.ofEpochMilli(endDate);

        return aiGatewayRequestLogFacade.getRequestLogs(start, end);
    }

    @QueryMapping
    public List<AiLlmUsage> workspaceAiGatewayRequestLogs(
        @Argument Long workspaceId, @Argument long startDate, @Argument long endDate) {

        Instant start = Instant.ofEpochMilli(startDate);
        Instant end = Instant.ofEpochMilli(endDate);

        return aiGatewayRequestLogFacade.getRequestLogsByWorkspace(workspaceId, start, end);
    }
}
