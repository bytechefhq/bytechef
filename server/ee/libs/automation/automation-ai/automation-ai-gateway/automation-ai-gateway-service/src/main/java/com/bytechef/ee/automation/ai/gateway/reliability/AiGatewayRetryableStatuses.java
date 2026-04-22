/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.reliability;

import java.util.Set;

/**
 * Transient HTTP 4xx status codes that are safe to retry. Shared between the gateway's chat-completion retry path
 * ({@link AiGatewayRetryHandler}) and webhook delivery ({@code AiObservabilityWebhookDeliveryServiceImpl}) so the
 * allowlist cannot drift between the two call sites.
 *
 * <p>
 * 408 Request Timeout, 425 Too Early, and 429 Too Many Requests are the canonical "try again later" 4xx codes. All
 * other 4xx responses mean the caller has a permanent problem (wrong path, revoked credentials, HMAC mismatch), and
 * retrying just multiplies log noise.
 *
 * @version ee
 */
public final class AiGatewayRetryableStatuses {

    public static final Set<Integer> TRANSIENT_RETRYABLE_4XX = Set.of(408, 425, 429);

    private AiGatewayRetryableStatuses() {
    }
}
