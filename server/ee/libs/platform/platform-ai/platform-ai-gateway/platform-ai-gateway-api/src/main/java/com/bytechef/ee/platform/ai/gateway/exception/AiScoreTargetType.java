/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.exception;

/**
 * Identifies the kind of resource an AI-gateway boundary check ran on. Promoted to a top-level enum so the 404-class
 * {@link AiScoreTargetNotFoundException} and the 403-class {@link AiScoreWorkspaceBoundaryException} reference one
 * neutral type — without it, the 404 type would have to depend on the 403 type's nested enum, creating an undesired
 * one-way dependency between two sibling exceptions. Future audit DTOs and log appenders also share this single
 * vocabulary.
 *
 * <p>
 * {@code UNKNOWN} is the fallback when the breach happened before the target type could be resolved.
 *
 * @author Ivica Cardic
 * @version ee
 */
public enum AiScoreTargetType {

    TRACE,
    SPAN,
    EXPERIMENT,
    DATASET,
    DATASET_VERSION,
    DATASET_ITEM,
    WORKSPACE,
    UNKNOWN
}
