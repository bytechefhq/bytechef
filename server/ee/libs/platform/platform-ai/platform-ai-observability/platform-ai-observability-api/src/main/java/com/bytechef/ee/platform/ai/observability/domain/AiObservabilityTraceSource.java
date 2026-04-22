/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.observability.domain;

/**
 * @author Ivica Cardic
 * @version ee
 */
public enum AiObservabilityTraceSource {
    API,
    PLAYGROUND,
    OTLP,
    EXPERIMENT;
}
