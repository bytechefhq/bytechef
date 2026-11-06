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

        Map<Long, AiEvalExperiment> orphansById = new LinkedHashMap<>();

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

            for (AiEvalExperimentRun run : runs) {
                AiEvalExperimentRunStatus status = run.getStatus();

                if (status != AiEvalExperimentRunStatus.PENDING && status != AiEvalExperimentRunStatus.RUNNING) {
                    continue;
                }

                try {
                    aiEvalExperimentRunService.fail(run.getId(), ORPHAN_MESSAGE);
                } catch (Exception runFailureException) {
                    log.warn(
                        "Failed to fail run {} during orphan recovery (continuing)",
                        run.getId(), runFailureException);
                }
            }

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
            log.error(
                "JVM-level failure while recovering experiment {} — aborting orphan recovery",
                experiment.getId(), error);

            throw error;
        } catch (Exception exception) {
            log.error("Failed to recover orphaned experiment {}", experiment.getId(), exception);

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
