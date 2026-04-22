/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.experiment.domain;

/**
 * Shared lifecycle contract for the {@link AiEvalExperimentStatus} (whole-batch) and {@link AiEvalExperimentRunStatus}
 * (per-item) enums. Both enums had two divergent {@code isTerminal()} implementations with identical bodies — extracted
 * here as a {@code default} method so the rule lives in exactly one place. Implementing types are kept distinct so the
 * type system continues to prevent passing an experiment status where a run status is expected and vice versa.
 *
 * <p>
 * The default uses {@link Enum#name()} rather than instance comparison so the contract survives an enum reorder
 * (ordinal positions are pinned separately by {@code PersistedEnumOrdinalContractTest}, but {@code isTerminal} should
 * not depend on that pinning). Adding a future status that should also be terminal (e.g., {@code CANCELLED},
 * {@code TIMED_OUT}) only requires updating this method — both enums inherit the change.
 *
 * @author Ivica Cardic
 * @version ee
 */
public interface LifecycleStatus {

    String name();

    /**
     * @return true iff this is a final, no-further-transitions state. Prefer this over open-coded
     *         {@code status == COMPLETED || status == FAILED} checks scattered across callers.
     */
    default boolean isTerminal() {
        String statusName = name();

        return "COMPLETED".equals(statusName) || "FAILED".equals(statusName);
    }
}
