/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.experiment.service;

import com.bytechef.ee.platform.ai.eval.experiment.domain.AiEvalExperiment;
import java.time.Instant;
import java.util.List;

/**
 * Workspace-agnostic CRUD and lifecycle operations for {@link AiEvalExperiment}. Lifecycle transitions
 * ({@link #markRunning} / {@link #markFinished}) are distinct methods rather than free-form updates so the executor
 * cannot accidentally corrupt the status machine. Workspace-scoped queries (by workspace, by-workspace+id,
 * createInWorkspace) live in
 * {@code com.bytechef.ee.automation.ai.eval.experiment.service.WorkspaceAiEvalExperimentService} — mirrors the
 * {@code platform-connection.ConnectionService} / {@code automation-configuration.WorkspaceConnectionService} split.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface AiEvalExperimentService {

    AiEvalExperiment create(AiEvalExperiment experiment);

    AiEvalExperiment getExperiment(long id);

    List<AiEvalExperiment> findAllByDatasetVersion(Long datasetVersionId);

    AiEvalExperiment update(AiEvalExperiment experiment);

    /**
     * Transitions the experiment to RUNNING + sets startedDate. Returns the persisted entity.
     */
    AiEvalExperiment markRunning(long experimentId);

    /**
     * Marks COMPLETED or FAILED depending on whether all runs succeeded. Sets completedDate.
     */
    AiEvalExperiment markFinished(long experimentId, boolean anyRunFailed);

    /**
     * Flags the experiment to stop after its next executor iteration boundary. Status transitions to FAILED
     * asynchronously when the executor observes the flag.
     *
     * @throws IllegalArgumentException if experiment does not exist.
     */
    AiEvalExperiment requestStop(long experimentId);

    /**
     * @return experiments with status = RUNNING and startedDate before the given threshold. Used by the orphan-
     *         recovery startup hook to find experiments stranded by a JVM crash.
     */
    List<AiEvalExperiment> findRunningOlderThan(Instant threshold);

    /**
     * @return experiments with status = PENDING and createdDate before the given threshold. Used by the orphan-
     *         recovery startup hook to find experiments where dispatch crashed before {@code markRunning} could
     *         transition the row out of PENDING (e.g., a JVM Error inside the executor's markRunning catch path, which
     *         deliberately skips {@code markFinished} to avoid cascading DB writes under OOM).
     */
    List<AiEvalExperiment> findPendingOlderThan(Instant threshold);
}
