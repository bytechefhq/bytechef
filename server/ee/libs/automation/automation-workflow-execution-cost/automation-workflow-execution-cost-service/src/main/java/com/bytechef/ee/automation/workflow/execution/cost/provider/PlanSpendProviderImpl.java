/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.execution.cost.provider;

import com.bytechef.ee.automation.workflow.execution.cost.service.WorkflowExecutionCostService;
import com.bytechef.platform.plan.domain.PlanBillingPeriod;
import com.bytechef.platform.plan.provider.PlanSpendProvider;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Resolves a tenant's current-period execution spend from the per-execution cost rows for the plan monthly-cost
 * admission gate. The billing period is the current calendar month in UTC. Totals are memoized per tenant for a short
 * interval — admission runs on every async job submission and a spend figure that lags by up to a minute is an
 * acceptable trade for not running a SUM per submission. Lookup failures fail open (report zero) so a cost-table
 * problem can never block job admission on its own.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class PlanSpendProviderImpl implements PlanSpendProvider {

    private static final Logger log = LoggerFactory.getLogger(PlanSpendProviderImpl.class);

    private static final long CACHE_TTL_MILLIS = 60_000;

    private record CachedSpend(BigDecimal spendUsd, long computedAtMillis) {
    }

    private final Map<String, CachedSpend> cachedSpends = new ConcurrentHashMap<>();
    private final WorkflowExecutionCostService workflowExecutionCostService;

    public PlanSpendProviderImpl(WorkflowExecutionCostService workflowExecutionCostService) {
        this.workflowExecutionCostService = workflowExecutionCostService;
    }

    @Override
    public BigDecimal getCurrentPeriodSpendUsd(String tenantId) {
        long currentTimeMillis = System.currentTimeMillis();

        CachedSpend cachedSpend = cachedSpends.get(tenantId);

        if (cachedSpend != null && currentTimeMillis - cachedSpend.computedAtMillis() < CACHE_TTL_MILLIS) {
            return cachedSpend.spendUsd();
        }

        BigDecimal spendUsd;

        try {
            spendUsd = workflowExecutionCostService.sumTotalCostSince(PlanBillingPeriod.currentPeriodStart());
        } catch (RuntimeException exception) {
            log.warn("Failed to resolve current-period spend for tenant {}; admitting (fail-open)", tenantId,
                exception);

            spendUsd = BigDecimal.ZERO;
        }

        cachedSpends.put(tenantId, new CachedSpend(spendUsd, currentTimeMillis));

        return spendUsd;
    }
}
