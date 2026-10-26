/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class ProtoValuesTest {

    @Test
    void testMapRoundTrip() {
        Map<String, Object> map = new HashMap<>();

        map.put("count", 5);
        map.put("flag", true);
        map.put("list", Arrays.asList(1, "x"));
        map.put("name", "a");
        map.put("nested", Map.of("k", "v"));
        map.put("nothing", null);
        map.put("ratio", 0.5);

        assertThat(ProtoValues.toMap(ProtoValues.toStruct(map))).isEqualTo(map);
    }

    @Test
    void testMapKeyOrderIsKept() {
        Map<String, Object> nested = new LinkedHashMap<>();

        nested.put("zeta", 1);
        nested.put("alpha", 2);

        Map<String, Object> map = new LinkedHashMap<>();

        map.put("zulu", "z");
        map.put("alpha", "a");
        map.put("mike", nested);

        Map<String, Object> roundTripMap = ProtoValues.toMap(ProtoValues.toStruct(map));

        assertThat(roundTripMap.keySet()).containsExactly("zulu", "alpha", "mike");
        Map<?, ?> roundTripNested = (Map<?, ?>) roundTripMap.get("mike");

        assertThat(List.copyOf(roundTripNested.keySet())).isEqualTo(List.of("zeta", "alpha"));
    }

    @Test
    void testNumbersBeyondDoublePrecisionTravelAsStrings() {
        assertThat(ProtoValues.toObject(ProtoValues.toValue(9_007_199_254_740_993L))).isEqualTo("9007199254740993");
        assertThat(ProtoValues.toObject(ProtoValues.toValue(new BigInteger("123456789012345678901234567890"))))
            .isEqualTo("123456789012345678901234567890");
        assertThat(ProtoValues.toObject(ProtoValues.toValue(new BigDecimal("12345678901234567890.5"))))
            .isEqualTo("12345678901234567890.5");
        assertThat(ProtoValues.toObject(ProtoValues.toValue(new BigDecimal("0.1")))).isEqualTo(0.1);
        assertThat(ProtoValues.toObject(ProtoValues.toValue(9_007_199_254_740_992L))).isEqualTo(9_007_199_254_740_992L);
    }

    @Test
    void testIntegralNumbersComeBackAsIntegerOrLong() {
        assertThat(ProtoValues.toObject(ProtoValues.toValue(5))).isEqualTo(5);
        assertThat(ProtoValues.toObject(ProtoValues.toValue(5_000_000_000L))).isEqualTo(5_000_000_000L);
        assertThat(ProtoValues.toObject(ProtoValues.toValue(2.5))).isEqualTo(2.5);
    }

    @Test
    void testNullMapIsEmptyStruct() {
        assertThat(ProtoValues.toStruct(null)
            .getFieldsCount()).isZero();
    }

    @Test
    void testNullValueRoundTrip() {
        assertThat(ProtoValues.toObject(ProtoValues.toValue(null))).isNull();
    }

    @Test
    void testLocalDateBecomesIsoString() {
        assertThat(ProtoValues.toObject(ProtoValues.toValue(LocalDate.of(2026, 10, 26)))).isEqualTo("2026-10-26");
    }

    @Test
    void testListOfMapsRoundTrip() {
        List<Map<String, Object>> rows = List.of(Map.of("id", 1), Map.of("id", 2));

        assertThat(ProtoValues.toObject(ProtoValues.toValue(rows))).isEqualTo(rows);
    }
}
