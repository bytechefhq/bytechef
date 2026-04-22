/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.domain;

/**
 * Origin of an {@link AiEvalScore} row. Persisted as INT ordinal — append new values at the end and never reorder or
 * remove. Reordering rebases every previously-written row onto a different source; the {@code safeOrdinalLookup} guard
 * in {@link AiEvalScore#getSource()} surfaces such corruption with the row id but cannot undo the misclassification.
 *
 * @author Ivica Cardic
 * @version ee
 */
public enum AiEvalScoreSource {

    MANUAL,
    API,
    LLM_JUDGE,
    EXTERNAL
}
