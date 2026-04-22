/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.experiment.domain;

/**
 * Status of an experiment (the whole batch). Persisted as INT ordinal — append-only. Implements {@link LifecycleStatus}
 * so the {@code isTerminal()} rule lives once across both this and {@link AiEvalExperimentRunStatus}.
 *
 * @author Ivica Cardic
 * @version ee
 */
public enum AiEvalExperimentStatus implements LifecycleStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}
