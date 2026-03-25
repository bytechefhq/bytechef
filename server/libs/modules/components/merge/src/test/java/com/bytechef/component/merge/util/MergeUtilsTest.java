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

package com.bytechef.component.merge.util;

import static com.bytechef.component.merge.util.MergeUtils.flatten;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivona Pavela
 */
class MergeUtilsTest {

    @Test
    void testFlattenSimpleMap() {
        Map<String, Object> input = new HashMap<>();
        input.put("a", 1);
        input.put("b", "test");

        List<Map<String, Object>> result = flatten(input);

        assertEquals(List.of(Map.of("a", 1, "b", "test")), result);
    }

    @Test
    void testFlattenNestedMap() {
        Map<String, Object> nested = new HashMap<>();
        nested.put("inner", "value");

        Map<String, Object> input = new HashMap<>();
        input.put("a", nested);

        List<Map<String, Object>> result = flatten(input);

        assertEquals(
            List.of(Map.of("a_inner", "value")),
            result);
    }

    @Test
    void testFlattenIterable() {
        List<Object> input = List.of(
            Map.of("a", 1),
            Map.of("a", 2));

        List<Map<String, Object>> result = flatten(input);

        assertEquals(List.of(Map.of("a", 1), Map.of("a", 2)), result);
    }

    @Test
    void testFlattenNestedIterableInsideMap() {
        Map<String, Object> input = new HashMap<>();
        input.put("items", List.of(
            Map.of("a", 1),
            Map.of("a", 2)));

        List<Map<String, Object>> result = flatten(input);

        assertEquals(
            List.of(
                Map.of("items", List.of(
                    Map.of("a", 1),
                    Map.of("a", 2)))),
            result);

    }

    @Test
    void testFlattenPrimitiveValue() {
        List<Map<String, Object>> result = flatten(42);

        assertEquals(List.of(Map.of("value", 42)), result);
    }

    @Test
    void testFlattenNull() {
        List<Map<String, Object>> result = flatten(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void testFlattenMixedStructure() {
        Map<String, Object> inner = new HashMap<>();
        inner.put("b", 2);

        Map<String, Object> input = new HashMap<>();
        input.put("a", inner);
        input.put("c", List.of(
            Map.of("d", 3),
            Map.of("d", 4)));

        List<Map<String, Object>> result = flatten(input);

        assertEquals(List.of(
            Map.of(
                "a_b", 2,
                "c", List.of(
                    Map.of("d", 3),
                    Map.of("d", 4)))),
            result);
    }
}
