/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.experiment.domain;

/**
 * Status of an individual experiment run (one dataset item replay). Persisted as INT ordinal — append-only. Implements
 * {@link LifecycleStatus} so the {@code isTerminal()} rule lives once across both this and
 * {@link AiEvalExperimentStatus} — open-coded {@code status == COMPLETED || status == FAILED} checks across the
 * codebase should call {@link LifecycleStatus#isTerminal()} instead.
 *
 * @author Ivica Cardic
 * @version ee
 */
public enum AiEvalExperimentRunStatus implements LifecycleStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED
}
