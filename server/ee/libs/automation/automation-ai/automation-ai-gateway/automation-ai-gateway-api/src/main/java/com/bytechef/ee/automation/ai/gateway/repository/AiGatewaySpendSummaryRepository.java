/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.repository;

import com.bytechef.ee.automation.ai.gateway.domain.AiGatewaySpendSummary;
import java.time.Instant;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiGatewaySpendSummaryRepository extends ListCrudRepository<AiGatewaySpendSummary, Long> {

    List<AiGatewaySpendSummary> findAllByPeriodStartBetween(Instant start, Instant end);

    List<AiGatewaySpendSummary> findAllByWorkspaceIdAndPeriodStartBetween(
        Long workspaceId, Instant start, Instant end);
}
