/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.automation.ai.gateway.facade.AiGatewaySpendFacade;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewaySpendSummary;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL controller for querying AI LLM Gateway spend summaries.
 *
 * <p>
 * Authorization is enforced on {@link AiGatewaySpendFacade}, not here.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
@ConditionalOnCoordinator
class AiGatewaySpendGraphQlController {

    private final AiGatewaySpendFacade aiGatewaySpendFacade;

    @SuppressFBWarnings("EI")
    AiGatewaySpendGraphQlController(AiGatewaySpendFacade aiGatewaySpendFacade) {
        this.aiGatewaySpendFacade = aiGatewaySpendFacade;
    }

    @QueryMapping
    public List<AiGatewaySpendSummary> aiGatewaySpendSummaries(
        @Argument long startDate, @Argument long endDate) {

        return aiGatewaySpendFacade.getSpendSummaries(
            Instant.ofEpochMilli(startDate), Instant.ofEpochMilli(endDate));
    }
}
