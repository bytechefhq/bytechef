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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ValueTagUtilsTest {

    @Test
    public void testTagRoundTripsEveryReconstructedType() {
        List<Object> values = List.of(
            java.time.ZonedDateTime.parse("2026-08-26T00:00:00Z"), LocalDate.of(2026, 8, 26),
            LocalDateTime.of(2026, 8, 26, 0, 0), LocalTime.of(10, 30),
            java.time.OffsetTime.parse("10:30:00+02:00"));

        for (Object value : values) {
            assertEquals(value, ValueTagUtils.untag(ValueTagUtils.tag(value)), String.valueOf(value));
        }
    }

    @Test
    public void testTagNormalizesBeforeTagging() {
        Object tagged = ValueTagUtils.tag(Timestamp.from(Instant.parse("2026-08-26T00:00:00Z")));

        assertEquals(
            Map.of("@bytechefType", "ZONED_DATE_TIME", "@bytechefValue", "2026-08-26T00:00:00Z"), tagged);
        assertEquals(java.time.ZonedDateTime.parse("2026-08-26T00:00:00Z"), ValueTagUtils.untag(tagged));
    }

    @Test
    public void testTagRecursesIntoListsAndMaps() {
        Object tagged = ValueTagUtils.tag(
            List.of(Map.of("APPLYDATE", Timestamp.from(Instant.parse("2026-08-26T00:00:00Z")))));

        assertEquals(
            List.of(Map.of("APPLYDATE", java.time.ZonedDateTime.parse("2026-08-26T00:00:00Z"))),
            ValueTagUtils.untag(tagged));
    }

    @Test
    public void testUntagLeavesUntaggedDataUnchanged() {
        Object legacy = List.of(Map.of("APPLYDATE", "2026-08-26T00:00:00.000Z"));

        assertEquals(legacy, ValueTagUtils.untag(legacy));
    }

    @Test
    public void testUntagTreatsLookalikeMapsAsPlainData() {
        Map<String, Object> unknownType = Map.of("@bytechefType", "NOT_A_TYPE", "@bytechefValue", "x");
        Map<String, Object> extraKey = Map.of(
            "@bytechefType", "ZONED_DATE_TIME", "@bytechefValue", "2026-08-26T00:00:00Z", "extra", 1);
        Map<String, Object> unparseableValue = Map.of(
            "@bytechefType", "ZONED_DATE_TIME", "@bytechefValue", "not-a-date");

        assertEquals(unknownType, ValueTagUtils.untag(unknownType));
        assertEquals(extraKey, ValueTagUtils.untag(extraKey));
        assertEquals(unparseableValue, ValueTagUtils.untag(unparseableValue));
    }

    @Test
    public void testTagRoundTripsEveryNumericType() {
        List<Object> values = List.of(
            new BigDecimal("7.5345"), new BigInteger("123456789012345678901234567890"), Long.valueOf(42L),
            Float.valueOf(9.5f), Short.valueOf((short) 7), Byte.valueOf((byte) 5));

        for (Object value : values) {
            assertEquals(value, ValueTagUtils.untag(ValueTagUtils.tag(value)), String.valueOf(value));
        }
    }

    @Test
    public void testTagPreservesBigDecimalScale() {
        BigDecimal withTrailingZero = new BigDecimal("1.10");
        BigDecimal withoutTrailingZero = new BigDecimal("1.1");

        assertNotEquals(withTrailingZero, withoutTrailingZero);

        Object untaggedWithTrailingZero = ValueTagUtils.untag(ValueTagUtils.tag(withTrailingZero));
        Object untaggedWithoutTrailingZero = ValueTagUtils.untag(ValueTagUtils.tag(withoutTrailingZero));

        assertEquals(withTrailingZero, untaggedWithTrailingZero);
        assertEquals(withoutTrailingZero, untaggedWithoutTrailingZero);
        assertNotEquals(untaggedWithTrailingZero, untaggedWithoutTrailingZero);
    }

    @Test
    public void testTagWritesBigDecimalAsPlainString() {
        Object tagged = ValueTagUtils.tag(new BigDecimal("0.0000001"));

        assertEquals(Map.of("@bytechefType", "BIG_DECIMAL", "@bytechefValue", "0.0000001"), tagged);
    }

    @Test
    public void testTagLeavesIntegerDoubleBooleanAndStringUntagged() {
        assertEquals(42, ValueTagUtils.tag(42));
        assertEquals(9.5, ValueTagUtils.tag(9.5));
        assertEquals(true, ValueTagUtils.tag(true));
        assertEquals("hello", ValueTagUtils.tag("hello"));
    }

    @Test
    public void testTagRecursesIntoListsAndMapsForNumericValues() {
        Object tagged = ValueTagUtils.tag(List.of(Map.of("AMOUNT", new BigDecimal("7.5345"))));

        assertEquals(
            List.of(Map.of("AMOUNT", new BigDecimal("7.5345"))),
            ValueTagUtils.untag(tagged));
    }

    @Test
    public void testUntagReturnsPlainDataForUnparseableNumericValue() {
        Map<String, Object> unparseableValue = Map.of("@bytechefType", "LONG", "@bytechefValue", "not-a-number");

        assertEquals(unparseableValue, ValueTagUtils.untag(unparseableValue));
    }

    @Test
    public void testUntagReturnsNullForNull() {
        assertNull(ValueTagUtils.untag(null));
    }
}
