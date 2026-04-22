/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.domain;

/**
 * Scope of traces a rule applies to. Persisted as INT ordinal — append-only.
 *
 * <ul>
 * <li>{@code LIVE_TRACE} (0) — live production traces (default; matches pre-experiment behavior).
 * <li>{@code EXPERIMENT_TRACE} (1) — synthetic traces produced by experiment replay.
 * </ul>
 *
 * @author Ivica Cardic
 * @version ee
 */
public enum AiEvalRuleTarget {
    LIVE_TRACE,
    EXPERIMENT_TRACE
}
