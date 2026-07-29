/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.execution.cost.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Per-execution cost configuration, modeled on Sim's cost formula (base run charge of $0.005/run — 1 credit at
 * $0.005/credit — plus token-based AI usage). {@code baseRunChargeUsd = 0} disables the base charge;
 * {@code enabled = false} disables cost-row creation entirely.
 *
 * @param enabled          whether terminal jobs get a {@code workflow_execution_cost} row
 * @param baseRunChargeUsd flat USD charge applied to every workflow execution
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ConfigurationProperties(prefix = "bytechef.workflow.execution-cost")
public record WorkflowExecutionCostProperties(
    @DefaultValue("true") boolean enabled, @DefaultValue("0.005") BigDecimal baseRunChargeUsd) {
}
