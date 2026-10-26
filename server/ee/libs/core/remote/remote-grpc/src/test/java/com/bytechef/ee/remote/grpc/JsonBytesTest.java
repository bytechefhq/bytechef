/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class JsonBytesTest {

    private final JsonBytes jsonBytes = new JsonBytes(
        JsonMapper.builder()
            .build());

    @Test
    void testKeepsNumberTypes() {
        Map<String, Object> map = Map.of("count", 5, "id", 9_007_199_254_740_993L, "ratio", 5.0);

        Map<String, Object> readMap = jsonBytes.readMap(jsonBytes.write(map));

        assertThat(readMap).isEqualTo(map);
        assertThat(readMap.get("count")).isInstanceOf(Integer.class);
        assertThat(readMap.get("id")).isInstanceOf(Long.class);
        assertThat(readMap.get("ratio")).isInstanceOf(Double.class);
    }

    @Test
    void testNullAndEmptyStayApart() {
        assertThat(jsonBytes.readMap(jsonBytes.write(null))).isNull();
        assertThat(jsonBytes.readMap(jsonBytes.write(Map.of()))).isEmpty();
    }
}
