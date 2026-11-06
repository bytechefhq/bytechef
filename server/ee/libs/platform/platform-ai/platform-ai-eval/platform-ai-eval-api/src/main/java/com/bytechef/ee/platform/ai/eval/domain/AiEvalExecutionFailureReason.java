/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.domain;

/**
 * @author Ivica Cardic
 * @version ee
 */
public enum AiEvalExecutionFailureReason {

    UNKNOWN,

    PROVIDER_NOT_FOUND,

    SCORE_CONFIG_NOT_FOUND,

    LLM_RESPONSE_PARSE_FAILED,

    PROVIDER_REQUEST_FAILED,

    PERSISTENCE_FAILED,

    MISSING_MODEL,

    PROVIDER_REQUEST_FAILED_AFTER_RETRIES
}
