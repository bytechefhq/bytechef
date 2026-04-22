/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.ai.gateway.reliability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Locks down the retry allowlist so a future change has to touch this test — drift between the gateway chat-completion
 * path and webhook delivery (both read this set) would silently change retry semantics for production traffic.
 *
 * @version ee
 */
class AiGatewayRetryableStatusesTest {

    @Test
    void testSetIsExactlyThreeCanonicalTransientCodes() {
        assertEquals(Set.of(408, 425, 429), AiGatewayRetryableStatuses.TRANSIENT_RETRYABLE_4XX);
    }

    @Test
    void testRequestTimeoutIsRetryable() {
        assertTrue(AiGatewayRetryableStatuses.TRANSIENT_RETRYABLE_4XX.contains(408));
    }

    @Test
    void testTooEarlyIsRetryable() {
        assertTrue(AiGatewayRetryableStatuses.TRANSIENT_RETRYABLE_4XX.contains(425));
    }

    @Test
    void testTooManyRequestsIsRetryable() {
        assertTrue(AiGatewayRetryableStatuses.TRANSIENT_RETRYABLE_4XX.contains(429));
    }

    @Test
    void testPermanentClientErrorsAreNotRetryable() {
        // 4xx values that indicate the caller has a permanent problem — retrying just multiplies log noise.
        assertFalse(AiGatewayRetryableStatuses.TRANSIENT_RETRYABLE_4XX.contains(400));
        assertFalse(AiGatewayRetryableStatuses.TRANSIENT_RETRYABLE_4XX.contains(401));
        assertFalse(AiGatewayRetryableStatuses.TRANSIENT_RETRYABLE_4XX.contains(403));
        assertFalse(AiGatewayRetryableStatuses.TRANSIENT_RETRYABLE_4XX.contains(404));
        assertFalse(AiGatewayRetryableStatuses.TRANSIENT_RETRYABLE_4XX.contains(422));
    }

    @Test
    void testServerErrorsAreNotListed() {
        // 5xx retry semantics live at the HTTP-client level; this constant is the 4xx allowlist only.
        assertFalse(AiGatewayRetryableStatuses.TRANSIENT_RETRYABLE_4XX.contains(500));
        assertFalse(AiGatewayRetryableStatuses.TRANSIENT_RETRYABLE_4XX.contains(503));
    }
}
