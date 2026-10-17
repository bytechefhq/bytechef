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

package com.bytechef.component.definition.approval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ApprovalButtonPayloadTest {

    @Test
    void testEncodeAppendsTheDecisionSuffix() {
        assertEquals("abc123:a", ApprovalButtonPayload.encode("abc123", true));
        assertEquals("abc123:d", ApprovalButtonPayload.encode("abc123", false));
    }

    @Test
    void testParseRoundTripsEncode() {
        assertEquals(
            Optional.of(new ApprovalButtonPayload("abc123", true)),
            ApprovalButtonPayload.parse(ApprovalButtonPayload.encode("abc123", true)));
        assertEquals(
            Optional.of(new ApprovalButtonPayload("abc123", false)),
            ApprovalButtonPayload.parse(ApprovalButtonPayload.encode("abc123", false)));
    }

    @Test
    void testParseKeepsColonsInsideTheShortId() {
        assertEquals(Optional.of(new ApprovalButtonPayload("a:b", true)), ApprovalButtonPayload.parse("a:b:a"));
    }

    @Test
    void testParseRejectsMalformedPayloads() {
        assertTrue(ApprovalButtonPayload.parse(null)
            .isEmpty());
        assertTrue(ApprovalButtonPayload.parse("")
            .isEmpty());
        assertTrue(ApprovalButtonPayload.parse(":a")
            .isEmpty());
        assertTrue(ApprovalButtonPayload.parse("abc123:x")
            .isEmpty());
        assertTrue(ApprovalButtonPayload.parse("abc123")
            .isEmpty());
    }

    @Test
    void testEncodeRejectsAnEmptyShortId() {
        assertThrows(IllegalArgumentException.class, () -> ApprovalButtonPayload.encode("", true));
        assertThrows(NullPointerException.class, () -> ApprovalButtonPayload.encode(null, true));
    }
}
