/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.domain;

/**
 * Persisted as INT ordinal on {@link AiEvalExecution#status}; reordering or removing values silently flips the meaning
 * of every historical row. Append new variants only at the end. The append-only contract is pinned by
 * {@code PersistedEnumOrdinalContractTest}.
 *
 * @author Ivica Cardic
 * @version ee
 */
public enum AiEvalExecutionStatus {

    PENDING,
    COMPLETED,
    ERROR
}
