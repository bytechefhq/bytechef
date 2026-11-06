/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.facade;

/**
 * @author Ivica Cardic
 * @version ee
 */
public enum RejectionCode {

    REJECTED_BY_MAPPER,

    MAPPER_RESOURCE_DECODE_WARNING,

    REJECTED_NO_SYSTEM,

    REJECTED_NO_STATUS,

    REJECTED_PERSIST_FAILED,

    REJECTED_INTERNAL,

    WORKSPACE_BOUNDARY,

    NOT_FOUND,

    VALIDATION_FAILED,

    PERSIST_FAILED,

    BATCH_INTERNAL_ERROR,

    LISTENER_FAILED
}
