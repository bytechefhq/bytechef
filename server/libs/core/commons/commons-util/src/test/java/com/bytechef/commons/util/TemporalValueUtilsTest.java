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

package com.bytechef.commons.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class TemporalValueUtilsTest {

    private static final ZonedDateTime EXPECTED = ZonedDateTime.parse("2026-08-26T00:00:00Z");

    @Test
    public void testNormalizeInstantBearingTypesToUtcZonedDateTime() {
        assertEquals(EXPECTED, TemporalValueUtils.normalize(Timestamp.from(Instant.parse("2026-08-26T00:00:00Z"))));
        assertEquals(EXPECTED, TemporalValueUtils.normalize(Instant.parse("2026-08-26T00:00:00Z")));
        assertEquals(EXPECTED,
            TemporalValueUtils.normalize(java.util.Date.from(Instant.parse("2026-08-26T00:00:00Z"))));
        assertEquals(EXPECTED, TemporalValueUtils.normalize(OffsetDateTime.parse("2026-08-26T02:00:00+02:00")));
        assertEquals(EXPECTED, TemporalValueUtils.normalize(ZonedDateTime.parse("2026-08-26T02:00:00+02:00")));
    }

    @Test
    public void testNormalizeLeavesValuesThatFixNoInstant() {
        assertEquals(LocalDate.of(2026, 8, 26), TemporalValueUtils.normalize(LocalDate.of(2026, 8, 26)));
        assertEquals(
            LocalDateTime.of(2026, 8, 26, 0, 0), TemporalValueUtils.normalize(LocalDateTime.of(2026, 8, 26, 0, 0)));
        assertEquals(LocalTime.of(10, 30), TemporalValueUtils.normalize(LocalTime.of(10, 30)));
    }

    @Test
    public void testNormalizeMapsSqlDateAndTimeToTheirOwnTypes() {
        assertEquals(LocalDate.of(2026, 8, 26), TemporalValueUtils.normalize(java.sql.Date.valueOf("2026-08-26")));
        assertEquals(LocalTime.of(10, 30), TemporalValueUtils.normalize(java.sql.Time.valueOf("10:30:00")));
    }

    @Test
    public void testNormalizeRecursesIntoListsAndMaps() {
        Object normalized = TemporalValueUtils.normalize(
            List.of(Map.of("APPLYDATE", Timestamp.from(Instant.parse("2026-08-26T00:00:00Z")))));

        assertEquals(List.of(Map.of("APPLYDATE", EXPECTED)), normalized);
    }

    @Test
    public void testNormalizeLeavesNonTemporalValuesUntouched() {
        assertEquals("2026-08-26T00:00:00.000Z", TemporalValueUtils.normalize("2026-08-26T00:00:00.000Z"));
        assertEquals(42, TemporalValueUtils.normalize(42));
        assertNull(TemporalValueUtils.normalize(null));
    }

    @Test
    public void testNormalizeIsIdempotent() {
        Object once = TemporalValueUtils.normalize(Timestamp.from(Instant.parse("2026-08-26T00:00:00Z")));

        assertEquals(once, TemporalValueUtils.normalize(once));
    }

    @Test
    public void testNormalizePreservesTheInstantWhenChangingZone() {
        ZonedDateTime normalized = (ZonedDateTime) TemporalValueUtils.normalize(
            ZonedDateTime.parse("2026-08-26T02:00:00+02:00"));

        assertEquals(ZoneOffset.UTC, normalized.getZone());
        assertEquals(Instant.parse("2026-08-26T00:00:00Z"), normalized.toInstant());
    }
}
