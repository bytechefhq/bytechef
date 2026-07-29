/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.alert.domain;

/**
 * Alert rule types, Sim-modeled. Persisted as INT ordinal — append-only, never reorder. The {@code threshold} field's
 * unit depends on the type (count, percent, milliseconds, multiplier, or USD — see each constant).
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public enum WorkflowAlertRuleType {

    /** N consecutive failed runs (threshold = count; consecutive counter resets on a completed run). */
    CONSECUTIVE_FAILURES,

    /** Failed-run percentage within the window (threshold = percent 0-100; needs >= 5 runs in window). */
    FAILURE_RATE,

    /** Absolute failed-run count within the window (threshold = count). */
    ERROR_COUNT,

    /** Single run slower than the threshold (threshold = milliseconds). */
    LATENCY_THRESHOLD,

    /** Single run slower than threshold x the rule's rolling average latency (threshold = multiplier, e.g. 2.0). */
    LATENCY_SPIKE,

    /** Single run's total execution cost at or above the threshold (threshold = USD). */
    COST_THRESHOLD,

    /** No matching run within the window (threshold unused; window = maximum silence in minutes). */
    NO_ACTIVITY,

    /**
     * Month-to-date workspace execution spend at or above the threshold percent of the plan's included monthly cost
     * (threshold = percent 0-100; window unused; evaluated by an hourly monitor, not per run).
     */
    USAGE_THRESHOLD
}
