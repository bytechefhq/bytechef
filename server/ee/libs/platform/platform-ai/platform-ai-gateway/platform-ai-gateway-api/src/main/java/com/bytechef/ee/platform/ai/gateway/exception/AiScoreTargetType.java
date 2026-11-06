/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.exception;

/**
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
