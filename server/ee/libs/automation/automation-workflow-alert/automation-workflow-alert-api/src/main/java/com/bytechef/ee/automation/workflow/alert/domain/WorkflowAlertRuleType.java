/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.alert.domain;

/**
 * @version ee
 * @author Ivica Cardic
 */
public enum WorkflowAlertRuleType {

    CONSECUTIVE_FAILURES,

    FAILURE_RATE,

    ERROR_COUNT,

    LATENCY_THRESHOLD,

    LATENCY_SPIKE,

    COST_THRESHOLD,

    NO_ACTIVITY
}
