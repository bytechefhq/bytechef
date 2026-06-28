/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ProviderExceptionTest {

    @Test
    void testIsRetryableForTooManyRequests() {
        assertTrue(new ProviderException(429, "Quota exceeded").isRetryable());
    }

    @Test
    void testIsRetryableForServiceUnavailable() {
        assertTrue(new ProviderException(503, "Service unavailable").isRetryable());
    }

    @Test
    void testIsNotRetryableForOtherStatusCodes() {
        assertFalse(new ProviderException(400, "Bad request").isRetryable());
        assertFalse(new ProviderException(401, "Unauthorized").isRetryable());
        assertFalse(new ProviderException("No status code").isRetryable());
    }

    @Test
    void testIsRetryableWhenRetryAfterPresentRegardlessOfStatus() {
        // 409 is not retryable by status, but a Retry-After header makes it so.
        ProviderException providerException = ProviderException.getProviderException(
            409, "Conflict", Map.of("Retry-After", List.of("120")));

        assertTrue(providerException.isRetryable());
    }

    @Test
    void testGetProviderExceptionCapturesRetryAfterHeaderCaseInsensitively() {
        ProviderException providerException = ProviderException.getProviderException(
            503, "unavailable", Map.of("Retry-After", List.of("30")));

        assertEquals("30", providerException.getRetryAfter());
        assertTrue(providerException.isRetryable());

        ProviderException lowerCaseHeader = ProviderException.getProviderException(
            429, "quota", Map.of("retry-after", List.of("7")));

        assertEquals("7", lowerCaseHeader.getRetryAfter());
    }

    @Test
    void testGetProviderExceptionWithoutRetryAfterHeader() {
        ProviderException providerException = ProviderException.getProviderException(
            500, "error", Map.of("Content-Type", List.of("application/json")));

        assertNull(providerException.getRetryAfter());
        assertFalse(providerException.isRetryable());
    }

    @Test
    void testGetProviderExceptionWithNullHeaders() {
        ProviderException providerException = ProviderException.getProviderException(500, "error", null);

        assertNull(providerException.getRetryAfter());
        assertFalse(providerException.isRetryable());
    }

    @Test
    void testGetProviderExceptionIgnoresEmptyRetryAfterValue() {
        ProviderException providerException = ProviderException.getProviderException(
            500, "error", Map.of("Retry-After", List.of()));

        assertNull(providerException.getRetryAfter());
        assertFalse(providerException.isRetryable());
    }

    @Test
    void testGetProviderExceptionPreservesSubtypeForMappedStatus() {
        ProviderException providerException = ProviderException.getProviderException(
            404, "missing", Map.of("Retry-After", List.of("5")));

        assertInstanceOf(ProviderException.NotFoundException.class, providerException);
        assertEquals("5", providerException.getRetryAfter());
        assertTrue(providerException.isRetryable());
    }

    @Test
    void testStaticIsRetryableWalksCauseChain() {
        Exception wrapped = new RuntimeException("outer", new ProviderException(429, "Quota exceeded"));

        assertTrue(ProviderException.isRetryable(wrapped));
        assertFalse(ProviderException.isRetryable(new RuntimeException("plain")));
        assertFalse(ProviderException.isRetryable(null));
    }
}
