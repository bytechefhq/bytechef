/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.domain;

/**
 * @version ee
 */
public enum AiObservabilityAlertMetric {

    ERROR_RATE(false),
    LATENCY_P95(false),
    COST(true),
    TOKEN_USAGE(false),
    REQUEST_VOLUME(false);

    private final boolean monetary;

    AiObservabilityAlertMetric(boolean monetary) {
        this.monetary = monetary;
    }

    public boolean isMonetary() {
        return monetary;
    }
}
