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

package com.bytechef.platform.component.definition.ai.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.component.definition.ai.agent.ClusterElementCallRecord.Status;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ClusterElementCallRecordTest {

    private static final Instant START = Instant.parse("2026-10-23T10:00:00Z");

    @Test
    void testCapsLongValues() {
        String capped = ClusterElementCallRecord.cap("x".repeat(70_000));

        assertTrue(capped.startsWith("x".repeat(65_536)));
        assertTrue(capped.endsWith("\n... [truncated, 70000 characters]"));
        assertEquals("short", ClusterElementCallRecord.cap("short"));
        assertNull(ClusterElementCallRecord.cap(null));
    }

    @Test
    void testMapRoundTrip() {
        ClusterElementCallRecord full = new ClusterElementCallRecord(
            "id-1", "parent-1", "httpClient_1", "httpClient", "get", Status.FAILED, START, START.plusSeconds(1), "in",
            "out", "boom");
        ClusterElementCallRecord minimal = new ClusterElementCallRecord(
            "id-2", null, null, null, null, Status.COMPLETED, null, START, null, null, null);

        assertEquals(full, ClusterElementCallRecord.fromMap(full.toMap()));
        assertEquals(minimal, ClusterElementCallRecord.fromMap(minimal.toMap()));
    }

    @Test
    void testMergesRecordsById() {
        ClusterElementCallRecord existingA = new ClusterElementCallRecord(
            "a", null, "n", "c", "t", Status.STARTED, START, null, "i", null, null);
        ClusterElementCallRecord existingB = new ClusterElementCallRecord(
            "b", null, "n", "c", "t", Status.COMPLETED, START, START, null, null, null);
        ClusterElementCallRecord incomingA = new ClusterElementCallRecord(
            "a", null, null, null, null, Status.COMPLETED, null, START.plusSeconds(1), null, "o", null);
        ClusterElementCallRecord incomingC = new ClusterElementCallRecord(
            "c", null, "n", "c", "t", Status.STARTED, START, null, null, null, null);

        List<ClusterElementCallRecord> merged = ClusterElementCallRecord.merge(
            List.of(existingA, existingB), List.of(incomingA, incomingC));

        assertEquals(
            List.of(
                new ClusterElementCallRecord(
                    "a", null, "n", "c", "t", Status.COMPLETED, START, START.plusSeconds(1), "i", "o", null),
                existingB, incomingC),
            merged);
    }
}
