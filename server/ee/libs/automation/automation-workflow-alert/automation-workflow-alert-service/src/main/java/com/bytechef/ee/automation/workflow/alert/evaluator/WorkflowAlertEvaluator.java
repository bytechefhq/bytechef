/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.workflow.alert.evaluator;

import com.bytechef.ee.automation.workflow.alert.domain.WorkflowAlertRule;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 * @author Ivica Cardic
 */
public final class WorkflowAlertEvaluator {

    static final int MINIMUM_RUNS_FOR_RATE = 5;

    private static final BigDecimal EWMA_ALPHA = new BigDecimal("0.2");
    private static final BigDecimal HUNDRED = new BigDecimal(100);

    private WorkflowAlertEvaluator() {
    }

    public record RunFacts(boolean failed, @Nullable Long durationMs, @Nullable BigDecimal totalCost) {
    }

    public record Breach(BigDecimal triggeredValue, String message) {
    }

    public static @Nullable Breach evaluate(WorkflowAlertRule rule, RunFacts runFacts, Instant now) {
        rule.setLastActivityDate(now);

        return switch (rule.getRuleType()) {
            case CONSECUTIVE_FAILURES -> evaluateConsecutiveFailures(rule, runFacts);
            case FAILURE_RATE -> evaluateFailureRate(rule, runFacts, now);
            case ERROR_COUNT -> evaluateErrorCount(rule, runFacts, now);
            case LATENCY_THRESHOLD -> evaluateLatencyThreshold(rule, runFacts);
            case LATENCY_SPIKE -> evaluateLatencySpike(rule, runFacts);
            case COST_THRESHOLD -> evaluateCostThreshold(rule, runFacts);
            case NO_ACTIVITY -> null;
        };
    }

    private static @Nullable Breach evaluateConsecutiveFailures(WorkflowAlertRule rule, RunFacts runFacts) {
        if (runFacts.failed()) {
            rule.setConsecutiveFailures(rule.getConsecutiveFailures() + 1);
        } else {
            rule.setConsecutiveFailures(0);
        }

        BigDecimal consecutiveFailures = BigDecimal.valueOf(rule.getConsecutiveFailures());

        if (rule.getConsecutiveFailures() > 0 && consecutiveFailures.compareTo(rule.getThreshold()) >= 0) {
            return new Breach(
                consecutiveFailures,
                "%d consecutive failed runs (threshold %s)".formatted(
                    rule.getConsecutiveFailures(), rule.getThreshold()));
        }

        return null;
    }

    private static @Nullable Breach evaluateFailureRate(WorkflowAlertRule rule, RunFacts runFacts, Instant now) {
        rollWindow(rule, runFacts, now);

        if (rule.getRunsInWindow() < MINIMUM_RUNS_FOR_RATE) {
            return null;
        }

        BigDecimal failureRatePercent = BigDecimal.valueOf(rule.getFailuresInWindow())
            .multiply(HUNDRED)
            .divide(BigDecimal.valueOf(rule.getRunsInWindow()), 2, RoundingMode.HALF_UP);

        if (failureRatePercent.compareTo(rule.getThreshold()) >= 0) {
            return new Breach(
                failureRatePercent,
                "%s%% of the last %d runs failed (threshold %s%%)".formatted(
                    failureRatePercent, rule.getRunsInWindow(), rule.getThreshold()));
        }

        return null;
    }

    private static @Nullable Breach evaluateErrorCount(WorkflowAlertRule rule, RunFacts runFacts, Instant now) {
        rollWindow(rule, runFacts, now);

        BigDecimal failuresInWindow = BigDecimal.valueOf(rule.getFailuresInWindow());

        if (rule.getFailuresInWindow() > 0 && failuresInWindow.compareTo(rule.getThreshold()) >= 0) {
            return new Breach(
                failuresInWindow,
                "%d failed runs within %s minutes (threshold %s)".formatted(
                    rule.getFailuresInWindow(), rule.getWindowMinutes(), rule.getThreshold()));
        }

        return null;
    }

    private static @Nullable Breach evaluateLatencyThreshold(WorkflowAlertRule rule, RunFacts runFacts) {
        Long durationMs = runFacts.durationMs();

        if (durationMs == null) {
            return null;
        }

        BigDecimal duration = BigDecimal.valueOf(durationMs);

        if (duration.compareTo(rule.getThreshold()) > 0) {
            return new Breach(
                duration, "Run took %d ms (threshold %s ms)".formatted(durationMs, rule.getThreshold()));
        }

        return null;
    }

    private static @Nullable Breach evaluateLatencySpike(WorkflowAlertRule rule, RunFacts runFacts) {
        Long durationMs = runFacts.durationMs();

        if (durationMs == null) {
            return null;
        }

        BigDecimal duration = BigDecimal.valueOf(durationMs);
        BigDecimal ewmaLatencyMs = rule.getEwmaLatencyMs();

        Breach breach = null;

        if (ewmaLatencyMs != null && ewmaLatencyMs.signum() > 0) {
            BigDecimal spikeCeiling = ewmaLatencyMs.multiply(rule.getThreshold());

            if (duration.compareTo(spikeCeiling) > 0) {
                breach = new Breach(
                    duration,
                    "Run took %d ms, more than %sx the rolling average of %s ms".formatted(
                        durationMs, rule.getThreshold(), ewmaLatencyMs.setScale(0, RoundingMode.HALF_UP)));
            }
        }

        BigDecimal updatedEwma = ewmaLatencyMs == null
            ? duration
            : duration.multiply(EWMA_ALPHA)
                .add(
                    ewmaLatencyMs.multiply(
                        BigDecimal.ONE.subtract(EWMA_ALPHA)),
                    MathContext.DECIMAL64);

        rule.setEwmaLatencyMs(updatedEwma);

        return breach;
    }

    private static @Nullable Breach evaluateCostThreshold(WorkflowAlertRule rule, RunFacts runFacts) {
        BigDecimal totalCost = runFacts.totalCost();

        if (totalCost == null) {
            return null;
        }

        if (totalCost.compareTo(rule.getThreshold()) >= 0) {
            return new Breach(
                totalCost, "Run cost %s USD (threshold %s USD)".formatted(totalCost, rule.getThreshold()));
        }

        return null;
    }

    public static @Nullable Breach evaluateNoActivity(WorkflowAlertRule rule, Instant now) {
        Integer windowMinutes = rule.getWindowMinutes();

        if (windowMinutes == null || windowMinutes <= 0) {
            return null;
        }

        Instant lastActivityDate = rule.getLastActivityDate() != null
            ? rule.getLastActivityDate()
            : rule.getCreatedDate();

        if (lastActivityDate == null) {
            return null;
        }

        Duration silence = Duration.between(lastActivityDate, now);

        long silenceMinutes = silence.toMinutes();

        if (silenceMinutes >= windowMinutes) {
            return new Breach(
                BigDecimal.valueOf(silenceMinutes),
                "No matching workflow run for %d minutes (threshold %d minutes)".formatted(
                    silenceMinutes, windowMinutes));
        }

        return null;
    }

    public static boolean isCooldownElapsed(WorkflowAlertRule rule, Instant now) {
        Instant lastTriggeredDate = rule.getLastTriggeredDate();

        return lastTriggeredDate == null ||
            Duration.between(lastTriggeredDate, now)
                .toMinutes() >= rule.getCooldownMinutes();
    }

    private static void rollWindow(WorkflowAlertRule rule, RunFacts runFacts, Instant now) {
        Integer windowMinutes = rule.getWindowMinutes();

        long effectiveWindowMinutes = windowMinutes == null || windowMinutes <= 0 ? 60 : windowMinutes;

        Instant windowStart = rule.getWindowStart();

        if (windowStart == null ||
            Duration.between(windowStart, now)
                .toMinutes() >= effectiveWindowMinutes) {

            rule.setWindowStart(now);
            rule.setRunsInWindow(0);
            rule.setFailuresInWindow(0);
        }

        rule.setRunsInWindow(rule.getRunsInWindow() + 1);

        if (runFacts.failed()) {
            rule.setFailuresInWindow(rule.getFailuresInWindow() + 1);
        }
    }
}
