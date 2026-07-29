/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.alert.evaluator;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertRule;
import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertRuleType;
import com.bytechef.ee.automation.workflow.alert.evaluator.WorkflowAlertEvaluator.Breach;
import com.bytechef.ee.automation.workflow.alert.evaluator.WorkflowAlertEvaluator.RunFacts;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

/**
 * Pins the Sim-modeled rule semantics: consecutive-failure reset on success, the >= 5 runs floor for FAILURE_RATE,
 * tumbling-window reset, pre-update EWMA spike baseline, cost/latency thresholds, NO_ACTIVITY silence measurement, and
 * the fixed cooldown.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class WorkflowAlertEvaluatorTest {

    private final Instant now = Instant.parse("2026-07-20T12:00:00Z");

    @Test
    void testConsecutiveFailuresFiresAtThresholdAndResetsOnSuccess() {
        WorkflowAlertRule rule = rule(WorkflowAlertRuleType.CONSECUTIVE_FAILURES, new BigDecimal(3));

        assertThat(WorkflowAlertEvaluator.evaluate(rule, failedRun(), now)).isNull();
        assertThat(WorkflowAlertEvaluator.evaluate(rule, failedRun(), now)).isNull();

        Breach breach = WorkflowAlertEvaluator.evaluate(rule, failedRun(), now);

        assertThat(breach).isNotNull();
        assertThat(breach.triggeredValue()).isEqualByComparingTo(new BigDecimal(3));

        assertThat(WorkflowAlertEvaluator.evaluate(rule, successfulRun(), now)).isNull();
        assertThat(rule.getConsecutiveFailures()).isZero();
    }

    @Test
    void testFailureRateNeedsMinimumRuns() {
        WorkflowAlertRule rule = rule(WorkflowAlertRuleType.FAILURE_RATE, new BigDecimal(50));

        rule.setWindowMinutes(60);

        // 4 straight failures = 100% failure rate, but below the 5-run floor — must stay silent.
        for (int i = 0; i < 4; i++) {
            assertThat(WorkflowAlertEvaluator.evaluate(rule, failedRun(), now)).isNull();
        }

        Breach breach = WorkflowAlertEvaluator.evaluate(rule, failedRun(), now);

        assertThat(breach).isNotNull();
        assertThat(breach.triggeredValue()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void testWindowResetsAfterElapse() {
        WorkflowAlertRule rule = rule(WorkflowAlertRuleType.ERROR_COUNT, BigDecimal.TWO);

        rule.setWindowMinutes(10);

        assertThat(WorkflowAlertEvaluator.evaluate(rule, failedRun(), now)).isNull();

        // 11 minutes later the tumbling window has rolled — the old failure must not count.
        Instant later = now.plus(11, ChronoUnit.MINUTES);

        assertThat(WorkflowAlertEvaluator.evaluate(rule, failedRun(), later)).isNull();
        assertThat(rule.getFailuresInWindow()).isEqualTo(1);

        Breach breach = WorkflowAlertEvaluator.evaluate(rule, failedRun(), later);

        assertThat(breach).isNotNull();
        assertThat(breach.triggeredValue()).isEqualByComparingTo(BigDecimal.TWO);
    }

    @Test
    void testLatencyThresholdComparesSingleRun() {
        WorkflowAlertRule rule = rule(WorkflowAlertRuleType.LATENCY_THRESHOLD, new BigDecimal(1000));

        assertThat(WorkflowAlertEvaluator.evaluate(rule, run(false, 900L, null), now)).isNull();

        Breach breach = WorkflowAlertEvaluator.evaluate(rule, run(false, 1500L, null), now);

        assertThat(breach).isNotNull();
        assertThat(breach.triggeredValue()).isEqualByComparingTo(new BigDecimal(1500));
    }

    @Test
    void testLatencySpikeUsesPreUpdateBaseline() {
        WorkflowAlertRule rule = rule(WorkflowAlertRuleType.LATENCY_SPIKE, BigDecimal.TWO);

        // First run only seeds the baseline.
        assertThat(WorkflowAlertEvaluator.evaluate(rule, run(false, 1000L, null), now)).isNull();
        assertThat(rule.getEwmaLatencyMs()).isEqualByComparingTo(new BigDecimal(1000));

        // 3000 ms > 2 x 1000 ms baseline — fires, and the baseline is checked before the spike is folded in.
        Breach breach = WorkflowAlertEvaluator.evaluate(rule, run(false, 3000L, null), now);

        assertThat(breach).isNotNull();
        assertThat(breach.triggeredValue()).isEqualByComparingTo(new BigDecimal(3000));
        assertThat(rule.getEwmaLatencyMs()).isEqualByComparingTo(new BigDecimal("1400.0"));
    }

    @Test
    void testCostThresholdReadsRunCost() {
        WorkflowAlertRule rule = rule(WorkflowAlertRuleType.COST_THRESHOLD, new BigDecimal("0.10"));

        assertThat(WorkflowAlertEvaluator.evaluate(rule, run(false, 100L, new BigDecimal("0.05")), now)).isNull();
        assertThat(WorkflowAlertEvaluator.evaluate(rule, run(false, 100L, null), now)).isNull();

        Breach breach = WorkflowAlertEvaluator.evaluate(rule, run(false, 100L, new BigDecimal("0.15")), now);

        assertThat(breach).isNotNull();
        assertThat(breach.triggeredValue()).isEqualByComparingTo(new BigDecimal("0.15"));
    }

    @Test
    void testNoActivityFiresAfterWindowOfSilence() {
        WorkflowAlertRule rule = rule(WorkflowAlertRuleType.NO_ACTIVITY, BigDecimal.ZERO);

        rule.setWindowMinutes(30);

        // A run 10 minutes ago keeps the rule quiet.
        WorkflowAlertEvaluator.evaluate(rule, successfulRun(), now.minus(10, ChronoUnit.MINUTES));

        assertThat(WorkflowAlertEvaluator.evaluateNoActivity(rule, now)).isNull();

        Breach breach = WorkflowAlertEvaluator.evaluateNoActivity(rule, now.plus(25, ChronoUnit.MINUTES));

        assertThat(breach).isNotNull();
        assertThat(breach.triggeredValue()).isEqualByComparingTo(new BigDecimal(35));
    }

    @Test
    void testUsageThresholdFiresAtPercentOfPlanCeiling() {
        WorkflowAlertRule rule = rule(WorkflowAlertRuleType.USAGE_THRESHOLD, new BigDecimal(80));

        // $7.50 of a $10 plan = 75% — below the 80% threshold.
        assertThat(
            WorkflowAlertEvaluator.evaluateUsageThreshold(rule, new BigDecimal("7.50"), BigDecimal.TEN)).isNull();

        Breach breach = WorkflowAlertEvaluator.evaluateUsageThreshold(rule, new BigDecimal("8.00"), BigDecimal.TEN);

        assertThat(breach).isNotNull();
        assertThat(breach.triggeredValue()).isEqualByComparingTo(new BigDecimal("80.00"));
    }

    @Test
    void testUsageThresholdNeverFiresOnUnlimitedPlan() {
        WorkflowAlertRule rule = rule(WorkflowAlertRuleType.USAGE_THRESHOLD, new BigDecimal(80));

        assertThat(WorkflowAlertEvaluator.evaluateUsageThreshold(rule, new BigDecimal("1000"), null)).isNull();
        assertThat(
            WorkflowAlertEvaluator.evaluateUsageThreshold(rule, new BigDecimal("1000"), BigDecimal.ZERO)).isNull();
    }

    @Test
    void testCooldownSuppressesRefiring() {
        WorkflowAlertRule rule = rule(WorkflowAlertRuleType.LATENCY_THRESHOLD, new BigDecimal(1000));

        rule.setLastTriggeredDate(now.minus(30, ChronoUnit.MINUTES));

        assertThat(WorkflowAlertEvaluator.isCooldownElapsed(rule, now)).isFalse();

        rule.setLastTriggeredDate(now.minus(61, ChronoUnit.MINUTES));

        assertThat(WorkflowAlertEvaluator.isCooldownElapsed(rule, now)).isTrue();
    }

    private static WorkflowAlertRule rule(WorkflowAlertRuleType ruleType, BigDecimal threshold) {
        WorkflowAlertRule rule = new WorkflowAlertRule();

        rule.setName("test-rule");
        rule.setRuleType(ruleType);
        rule.setThreshold(threshold);

        return rule;
    }

    private static RunFacts failedRun() {
        return run(true, 100L, null);
    }

    private static RunFacts successfulRun() {
        return run(false, 100L, null);
    }

    private static RunFacts run(boolean failed, Long durationMs, BigDecimal totalCost) {
        return new RunFacts(failed, durationMs, totalCost);
    }
}
