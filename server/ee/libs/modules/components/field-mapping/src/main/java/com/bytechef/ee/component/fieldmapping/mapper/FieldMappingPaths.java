/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.fieldmapping.mapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class FieldMappingPaths {

    private FieldMappingPaths() {
    }

    static boolean containsPath(Map<String, ?> map, String path) {
        String[] segments = path.split("\\.");
        Map<String, ?> current = map;

        for (int index = 0; index < segments.length; index++) {
            if (!current.containsKey(segments[index])) {
                return false;
            }

            if (index == segments.length - 1) {
                return true;
            }

            if (!(current.get(segments[index]) instanceof Map<?, ?> nested)) {
                return false;
            }

            current = castMap(nested);
        }

        return false;
    }

    static Object getValue(Map<String, ?> map, String path) {
        String[] segments = path.split("\\.");
        Map<String, ?> current = map;

        for (int index = 0; index < segments.length - 1; index++) {
            if (!(current.get(segments[index]) instanceof Map<?, ?> nested)) {
                return null;
            }

            current = castMap(nested);
        }

        return current.get(segments[segments.length - 1]);
    }

    static Map<String, Object> deepCopy(Map<String, ?> map) {
        Map<String, Object> copy = new LinkedHashMap<>();

        for (Map.Entry<String, ?> entry : map.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof Map<?, ?> nested) {
                copy.put(entry.getKey(), deepCopy(castMap(nested)));
            } else {
                copy.put(entry.getKey(), value);
            }
        }

        return copy;
    }

    static int depth(String path) {
        String[] segments = path.split("\\.");

        return segments.length;
    }

    static void removeValue(Map<String, Object> map, String path) {
        removeValue(map, path.split("\\."), 0);
    }

    static void setValue(Map<String, Object> map, String path, Object value) {
        String[] segments = path.split("\\.");
        Map<String, Object> current = map;

        for (int index = 0; index < segments.length - 1; index++) {
            Object existing = current.get(segments[index]);

            if (existing instanceof Map<?, ?> nested) {
                Map<String, Object> copy = new LinkedHashMap<>(castMutableMap(nested));

                current.put(segments[index], copy);

                current = copy;
            } else {
                Map<String, Object> created = new LinkedHashMap<>();

                current.put(segments[index], created);

                current = created;
            }
        }

        String lastSegment = segments[segments.length - 1];

        if (current.get(lastSegment) instanceof Map<?, ?> existingMap && value instanceof Map<?, ?> valueMap) {
            current.put(lastSegment, mergeMaps(castMap(existingMap), castMap(valueMap)));
        } else {
            current.put(lastSegment, value);
        }
    }

    private static Map<String, Object> mergeMaps(Map<String, ?> baseMap, Map<String, ?> overlayMap) {
        Map<String, Object> mergedMap = new LinkedHashMap<>(baseMap);

        for (Map.Entry<String, ?> entry : overlayMap.entrySet()) {
            Object baseValue = mergedMap.get(entry.getKey());
            Object overlayValue = entry.getValue();

            if (baseValue instanceof Map<?, ?> baseNestedMap && overlayValue instanceof Map<?, ?> overlayNestedMap) {
                mergedMap.put(entry.getKey(), mergeMaps(castMap(baseNestedMap), castMap(overlayNestedMap)));
            } else {
                mergedMap.put(entry.getKey(), overlayValue);
            }
        }

        return mergedMap;
    }

    private static void removeValue(Map<String, Object> map, String[] segments, int index) {
        String segment = segments[index];

        if (index == segments.length - 1) {
            map.remove(segment);

            return;
        }

        if (!(map.get(segment) instanceof Map<?, ?> nested)) {
            return;
        }

        Map<String, Object> nestedMap = castMutableMap(nested);

        removeValue(nestedMap, segments, index + 1);

        if (nestedMap.isEmpty()) {
            map.remove(segment);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> castMap(Map<?, ?> map) {
        return (Map<String, ?>) map;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMutableMap(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }
}
