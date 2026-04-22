/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.facade;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewaySpendSummary;
import com.bytechef.ee.platform.ai.gateway.service.AiGatewaySpendService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.security.constant.AuthorityConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link AiGatewaySpendFacade}. Delegates to the shared {@code AiGatewaySpendService} and carries the
 * {@code ADMIN} authorization guard so it is enforced for every caller of the facade.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(prefix = "bytechef.ai.gateway", name = "enabled", havingValue = "true")
class AiGatewaySpendFacadeImpl implements AiGatewaySpendFacade {

    private final AiGatewaySpendService aiGatewaySpendService;

    @SuppressFBWarnings("EI")
    AiGatewaySpendFacadeImpl(AiGatewaySpendService aiGatewaySpendService) {
        this.aiGatewaySpendService = aiGatewaySpendService;
    }

    @Override
    @PreAuthorize("hasAuthority(\"" + AuthorityConstants.ADMIN + "\")")
    public List<AiGatewaySpendSummary> getSpendSummaries(Instant startDate, Instant endDate) {
        return aiGatewaySpendService.getSpendSummaries(startDate, endDate);
    }
}
