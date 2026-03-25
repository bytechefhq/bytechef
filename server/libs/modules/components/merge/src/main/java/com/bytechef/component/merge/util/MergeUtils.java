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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Ivona Pavela
 */
public class MergeUtils {

    public static List<Map<String, Object>> flatten(Object input) {

        List<Map<String, Object>> rows = new ArrayList<>();
        Set<String> allKeys = new LinkedHashSet<>();

        if (input == null) {
            return List.of();
        }

        if (input instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                rows.add(flattenToRow(item));
            }
        } else {
            rows.add(flattenToRow(input));
        }

        for (Map<String, Object> row : rows) {
            allKeys.addAll(row.keySet());
        }

        for (Map<String, Object> row : rows) {
            for (String key : allKeys) {
                row.putIfAbsent(key, null);
            }
        }

        return rows;
    }

    private static Map<String, Object> flattenToRow(Object input) {
        Map<String, Object> row = new LinkedHashMap<>();
        flattenObject(input, row, "");
        return row;
    }

    private static void flattenObject(Object input, Map<String, Object> row, String prefix) {

        if (input instanceof Map<?, ?> map) {

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                Object value = entry.getValue();

                String newKey = prefix.isEmpty() ? key : prefix + "_" + key;

                if (value instanceof Map<?, ?> nestedMap) {
                    flattenObject(nestedMap, row, newKey);

                } else if (value instanceof Iterable<?> iterable) {

                    row.put(newKey, normalizeList(iterable));

                } else {
                    row.put(newKey, value);
                }
            }

        } else if (input instanceof Iterable<?> iterable) {
            row.put(prefix.isEmpty() ? "value" : prefix, normalizeList(iterable));

        } else {
            row.put(prefix.isEmpty() ? "value" : prefix, input);
        }
    }

    private static List<Object> normalizeList(Iterable<?> iterable) {
        List<Object> list = new ArrayList<>();

        for (Object item : iterable) {
            if (item instanceof Map<?, ?> map) {
                Map<String, Object> flattened = new LinkedHashMap<>();
                flattenObject(map, flattened, "");
                list.add(flattened);

            } else if (item instanceof Iterable<?> nestedIterable) {
                list.add(normalizeList(nestedIterable));

            } else {
                list.add(item);
            }
        }

        return list;
    }
}
