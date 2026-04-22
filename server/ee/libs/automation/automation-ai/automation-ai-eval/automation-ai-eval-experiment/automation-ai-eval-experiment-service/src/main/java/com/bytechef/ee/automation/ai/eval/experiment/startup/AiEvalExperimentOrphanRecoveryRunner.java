/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.eval.experiment.startup;

import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperiment;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRun;
import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperimentRunStatus;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentRunService;
import com.bytechef.ee.platform.ai.eval.experiment.service.AiEvalExperimentService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * On application startup, finds experiments stranded by a previous JVM crash and force-fails them. Sweeps two states
 * older than the configured threshold (default {@link #DEFAULT_ORPHAN_THRESHOLD_MINUTES} minutes; overridable via
 * {@code bytechef.ai.gateway.experiment.orphan-recovery.threshold-minutes}):
 * <ul>
 * <li>RUNNING — primary case. The previous JVM crashed mid-execution.</li>
 * <li>PENDING — secondary case. {@code AiEvalExperimentExecutor.execute()} deliberately skips {@code markFinished}
 * under JVM Errors and on delete-during-dispatch races, so PENDING experiments older than the threshold may have had
 * their executor abort before transitioning out of PENDING. Without a PENDING sweep these would be invisible to
 * recovery forever.</li>
 * </ul>
 *
 * <p>
 * <strong>PENDING-vs-async-queue caveat:</strong> the PENDING sweep does not distinguish "executor aborted before
 * transitioning out of PENDING" from "executor task is still queued but has not started". On a busy single-instance
 * deployment a sustained dispatch burst that fills the {@code aiEvalExperimentTaskExecutor} queue (capacity 100, see
 * {@code AiGatewayExperimentAsyncConfiguration}) plus {@code CallerRunsPolicy} slowdown could plausibly leave a row
 * PENDING past the default 10-minute threshold while the task is still queued. If your deployment runs experiments
 * larger than {@code corePoolSize} per minute on average, consider raising
 * {@code bytechef.ai.gateway.experiment.orphan-recovery.threshold-minutes} so the sweep window comfortably exceeds
 * {@code (queue capacity / dispatch rate) × per-experiment latency}, otherwise the sweep will race the executor and
 * force-fail experiments that were merely backlogged.
 *
 * <p>
 * <strong>Multi-instance safety:</strong> this hook runs on every {@code ApplicationReadyEvent} in every JVM. In a
 * multi-instance deployment (e.g., two replicas of {@code ai-gateway-app}), instance B's startup would force-fail any
 * RUNNING experiment from instance A that has merely been long-running — even though it is still in flight on the other
 * node. The recovery hook is therefore <em>opt-in</em>: it is gated by
 * {@code bytechef.ai.gateway.experiment.orphan-recovery.enabled=true}, with no default. Single-instance deployments
 * (the default monolith) should set this to {@code true}; multi-instance deployments should leave it unset on all but
 * one designated node, or wait for the atlas-coordinator migration that will replace this hook with a leader-elected
 * periodic task.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Component
@ConditionalOnEEVersion
@ConditionalOnProperty(
    name = {
        "bytechef.ai.gateway.enabled", "bytechef.ai.gateway.experiment.orphan-recovery.enabled"
    },
    havingValue = "true")
@SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
public class AiEvalExperimentOrphanRecoveryRunner {

    private static final Logger log = LoggerFactory.getLogger(AiEvalExperimentOrphanRecoveryRunner.class);

    /**
     * Default minutes-stale threshold; overridable via
     * {@code bytechef.ai.gateway.experiment.orphan-recovery.threshold-minutes}.
     */
    static final int DEFAULT_ORPHAN_THRESHOLD_MINUTES = 10;

    private static final String ORPHAN_MESSAGE = "Orphaned by JVM restart";

    private final AiEvalExperimentService aiEvalExperimentService;
    private final AiEvalExperimentRunService aiEvalExperimentRunService;
    private final int orphanThresholdMinutes;

    public AiEvalExperimentOrphanRecoveryRunner(
        AiEvalExperimentService aiEvalExperimentService, AiEvalExperimentRunService aiEvalExperimentRunService,
        @Value("${bytechef.ai.gateway.experiment.orphan-recovery.threshold-minutes:"
            + DEFAULT_ORPHAN_THRESHOLD_MINUTES + "}") int orphanThresholdMinutes) {

        if (orphanThresholdMinutes <= 0) {
            throw new IllegalArgumentException(
                "bytechef.ai.gateway.experiment.orphan-recovery.threshold-minutes must be positive, got "
                    + orphanThresholdMinutes);
        }

        this.aiEvalExperimentService = aiEvalExperimentService;
        this.aiEvalExperimentRunService = aiEvalExperimentRunService;
        this.orphanThresholdMinutes = orphanThresholdMinutes;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverOrphanedExperiments() {
        Instant threshold = Instant.now()
            .minusSeconds(orphanThresholdMinutes * 60L);

        // De-duplicate by id; a single experiment cannot have two states, but the two queries are independent
        // and a future implementation could conceivably overlap (e.g., status int allowed to change between
        // calls). LinkedHashMap preserves the order RUNNING-first / PENDING-second so logs stay readable.
        Map<Long, AiEvalExperiment> orphansById = new LinkedHashMap<>();

        // Each query in its own try/catch so a transient DB failure on one query (e.g., a CannotAcquireLockException
        // from Spring) does not silently skip orphan recovery for the entire JVM lifecycle. A partial recovery —
        // PENDING-only or RUNNING-only — is strictly better than leaving every stranded experiment until next
        // restart (which could fail the same way indefinitely).
        try {
            for (AiEvalExperiment running : aiEvalExperimentService.findRunningOlderThan(threshold)) {
                orphansById.put(running.getId(), running);
            }
        } catch (Exception runningQueryException) {
            log.error(
                "Could not query RUNNING orphans on startup; PENDING sweep will still run",
                runningQueryException);
        }

        try {
            for (AiEvalExperiment pending : aiEvalExperimentService.findPendingOlderThan(threshold)) {
                orphansById.putIfAbsent(pending.getId(), pending);
            }
        } catch (Exception pendingQueryException) {
            log.error("Could not query PENDING orphans on startup", pendingQueryException);
        }

        if (orphansById.isEmpty()) {
            log.debug("No orphaned experiments found on startup");

            return;
        }

        log.warn("Recovering {} orphaned experiment(s) after startup", orphansById.size());

        for (AiEvalExperiment orphan : orphansById.values()) {
            recoverOne(orphan);
        }
    }

    private void recoverOne(AiEvalExperiment experiment) {
        try {
            List<AiEvalExperimentRun> runs = aiEvalExperimentRunService.findAllByExperiment(experiment.getId());

            // Each run.fail() is in its own try/catch so one bad run does not abort the recovery loop and
            // leave the experiment stranded in RUNNING (a self-perpetuating orphan that the next startup
            // would re-discover and re-fail-on, infinitely). Mirrors the per-cancellation pattern in
            // AiEvalExperimentExecutor.execute.
            for (AiEvalExperimentRun run : runs) {
                AiEvalExperimentRunStatus status = run.getStatus();

                if (status != AiEvalExperimentRunStatus.PENDING && status != AiEvalExperimentRunStatus.RUNNING) {
                    continue;
                }

                try {
                    aiEvalExperimentRunService.fail(run.getId(), ORPHAN_MESSAGE);
                } catch (Exception runFailureException) {
                    // catch (Exception) — NOT catch (Throwable) — so JVM-wide problems (OutOfMemoryError,
                    // StackOverflowError, ThreadDeath) propagate uncaught past this handler to the outer
                    // catch (Error) below, which logs with experiment-id context and aborts the recovery
                    // loop. The outer block needs the explicit Error catch for its log message; the inner
                    // here only needs to NOT swallow it.
                    log.warn(
                        "Failed to fail run {} during orphan recovery (continuing)",
                        run.getId(), runFailureException);
                }
            }

            // markFinished is in its own try/catch for the same reason as the per-run fail() above: if it
            // throws (DB hiccup, optimistic-lock contention, anything), the experiment stays in RUNNING and
            // the next JVM start re-discovers the same orphan — exactly the self-perpetuating loop the
            // per-run try/catch was added to prevent. The outer catch below would log this but the loop
            // would have already left the row in RUNNING; logging at error here makes the cause explicit
            // and ensures dashboards see the recovery-failure signal distinctly from the per-run failure.
            try {
                aiEvalExperimentService.markFinished(experiment.getId(), true);

                log.warn("Experiment {} recovered from orphaned state", experiment.getId());
            } catch (Exception markFinishedException) {
                log.error(
                    "Orphan recovery loop completed run-level failures for experiment {} but could not " +
                        "transition experiment to FAILED — experiment remains RUNNING and will be " +
                        "re-discovered on the next startup",
                    experiment.getId(), markFinishedException);
            }
        } catch (Error error) {
            // JVM-wide problems must propagate up so recoverOrphanedExperiments stops the loop instead of
            // limping on while the JVM is in distress. Logged on the way out so the caller sees which
            // experiment crashed the recovery pass.
            log.error(
                "JVM-level failure while recovering experiment {} — aborting orphan recovery",
                experiment.getId(), error);

            throw error;
        } catch (Exception exception) {
            log.error("Failed to recover orphaned experiment {}", experiment.getId(), exception);

            // Without this best-effort markFinished, an Exception thrown BEFORE the inner markFinished block
            // (e.g. findAllByExperiment fails on a transient DB hiccup) leaves the experiment in RUNNING. The
            // next JVM start re-discovers the same orphan and re-fails on the same spot — the self-perpetuating
            // orphan loop this best-effort markFinished prevents. Failing fast on markFinished here is correct
            // (the orphan reaper still applies on the next startup if this also fails); the goal is to ensure
            // the happy path completes the lifecycle even when the per-run fan-out hits a snag.
            try {
                aiEvalExperimentService.markFinished(experiment.getId(), true);
            } catch (RuntimeException markFinishedException) {
                log.error(
                    "Outer-catch markFinished also failed for experiment {} — experiment remains RUNNING and " +
                        "will be re-discovered on the next startup",
                    experiment.getId(), markFinishedException);
            }
        }
    }
}
