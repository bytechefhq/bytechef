/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.reliability;

import java.util.Set;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AiGatewayRetryableStatuses {

    public static final Set<Integer> TRANSIENT_RETRYABLE_4XX = Set.of(408, 425, 429);

    private AiGatewayRetryableStatuses() {
    }
}
