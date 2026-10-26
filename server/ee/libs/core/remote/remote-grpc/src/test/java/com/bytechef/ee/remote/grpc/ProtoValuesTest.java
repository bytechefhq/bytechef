/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
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
