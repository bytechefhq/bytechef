/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.cache.invalidation;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.nio.charset.StandardCharsets;
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
class JsonCacheValueRedisSerializerTest {

    private final JsonCacheValueRedisSerializer jsonCacheValueRedisSerializer = new JsonCacheValueRedisSerializer();

    @Test
    void testWritesPlainJsonWithoutTypeInformation() {
        byte[] bytes = jsonCacheValueRedisSerializer.serialize(Map.of("id", "webhook-1"));

        assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("{\"id\":\"webhook-1\"}");
    }

    @Test
    void testReadsBackJsonTypes() {
        assertThat(roundTrip(true)).isEqualTo(true);
        assertThat(roundTrip("value")).isEqualTo("value");
        assertThat(roundTrip(List.of(1, "two"))).isEqualTo(List.of(1, "two"));
        assertThat(roundTrip(Map.of("nested", Map.of("count", 2)))).isEqualTo(Map.of("nested", Map.of("count", 2)));
    }

    @Test
    void testNullAndEmptyBytes() {
        assertThat(jsonCacheValueRedisSerializer.serialize(null)).isEmpty();
        assertThat(jsonCacheValueRedisSerializer.deserialize(null)).isNull();
        assertThat(jsonCacheValueRedisSerializer.deserialize(new byte[0])).isNull();
    }

    private Object roundTrip(Object value) {
        return jsonCacheValueRedisSerializer.deserialize(jsonCacheValueRedisSerializer.serialize(value));
    }
}
