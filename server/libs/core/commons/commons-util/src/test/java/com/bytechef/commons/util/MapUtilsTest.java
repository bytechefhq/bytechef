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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.jackson.config.JacksonConfiguration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jackson.JsonComponentModule;

/**
 * @author Matija Petanjek
 */
public class MapUtilsTest {

    @BeforeAll
    public static void setUp() {
        MapUtils.setObjectMapper(new JacksonConfiguration(new JsonComponentModule()).objectMapper());
    }

    @Test
    public void testContainsPath() {
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", null);

        assertTrue(MapUtils.containsPath(map, "key1"));
        assertTrue(MapUtils.containsPath(map, "key2"));
        assertFalse(MapUtils.containsPath(map, "key3"));
    }

    @Test
    public void testConvertInstantWithTimezoneZ() {
        Map<String, Object> map = new HashMap<>();
        map.put("ts", "2025-12-05T16:55:49Z");

        Instant instant = MapUtils.get(map, "ts", Instant.class);

        assertEquals(Instant.parse("2025-12-05T16:55:49Z"), instant);
    }

    @Test
    public void testConvertInstantWithoutTimezoneFractionalSeconds() {
        Map<String, Object> map = new HashMap<>();
        map.put("ts", "2025-12-05T16:55:49.682110827");

        Instant instant = MapUtils.get(map, "ts", Instant.class);

        Instant expected = LocalDateTime.parse("2025-12-05T16:55:49.682110827")
            .toInstant(ZoneOffset.UTC);

        assertEquals(expected, instant);
    }

    @Test
    public void testConvertInstantWithoutTimezoneMillis() {
        Map<String, Object> map = new HashMap<>();
        map.put("ts", "2025-12-05T16:55:49.123");

        Instant instant = MapUtils.get(map, "ts", Instant.class);

        Instant expected = LocalDateTime.parse("2025-12-05T16:55:49.123")
            .toInstant(ZoneOffset.UTC);

        assertEquals(expected, instant);
    }

    @Test
    public void testConvertInstantWithoutTimezoneNoFraction() {
        Map<String, Object> map = new HashMap<>();
        map.put("ts", "2025-12-05T16:55:49");

        Instant instant = MapUtils.get(map, "ts", Instant.class);

        Instant expected = LocalDateTime.parse("2025-12-05T16:55:49")
            .toInstant(ZoneOffset.UTC);

        assertEquals(expected, instant);
    }
}
