/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.repository;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewaySpendSummary;
import java.time.Instant;
import java.util.List;
import org.springframework.data.repository.ListCrudRepository;

/**
 * @version ee
 */
public interface AiGatewaySpendSummaryRepository extends ListCrudRepository<AiGatewaySpendSummary, Long> {

    List<AiGatewaySpendSummary> findAllByPeriodStartBetween(Instant start, Instant end);

    /**
     * Returns the spend summaries owned by the given workspace whose period starts inside the range. A summary with a
     * null {@code workspace_id} belongs to no workspace and is therefore never returned here — SQL equality never
     * matches NULL.
     */
    List<AiGatewaySpendSummary> findAllByWorkspaceIdAndPeriodStartBetween(
        long workspaceId, Instant start, Instant end);
}
