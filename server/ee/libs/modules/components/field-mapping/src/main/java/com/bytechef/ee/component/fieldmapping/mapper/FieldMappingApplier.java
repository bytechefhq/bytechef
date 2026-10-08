/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.fieldmapping.mapper;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class FieldMappingApplier {

    private FieldMappingApplier() {
    }

    public static Object apply(
        FieldMappingDescriptor descriptor, Object data, FieldMappingDirection direction, boolean includeUnmapped) {

        if (data instanceof Map<?, ?> map) {
            return applyToObject(descriptor, castMap(map), direction, includeUnmapped);
        }

        if (data instanceof List<?> list) {
            return list.stream()
                .map(item -> apply(descriptor, item, direction, includeUnmapped))
                .toList();
        }

        throw new IllegalArgumentException(
            "Field mapping data must be an object or an array of objects, got: " +
                (data == null ? "null" : data.getClass()
                    .getSimpleName()));
    }

    private static Map<String, Object> applyToObject(
        FieldMappingDescriptor descriptor, Map<String, ?> source, FieldMappingDirection direction,
        boolean includeUnmapped) {

        List<FieldMappingDescriptor.Mapping> mappings = descriptor.mappings();
        Map<String, Object> result = includeUnmapped ? FieldMappingPaths.deepCopy(source) : new LinkedHashMap<>();

        if (includeUnmapped) {
            for (FieldMappingDescriptor.Mapping mapping : mappings) {
                FieldMappingPaths.removeValue(result, direction.sourcePath(mapping));
            }
        }

        List<FieldMappingDescriptor.Mapping> presentMappings = mappings.stream()
            .filter(mapping -> FieldMappingPaths.containsPath(source, direction.sourcePath(mapping)))
            .sorted(Comparator.comparingInt(mapping -> FieldMappingPaths.depth(direction.destinationPath(mapping))))
            .toList();

        for (FieldMappingDescriptor.Mapping mapping : presentMappings) {
            String sourcePath = direction.sourcePath(mapping);

            FieldMappingPaths.setValue(
                result, direction.destinationPath(mapping), FieldMappingPaths.getValue(source, sourcePath));
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> castMap(Map<?, ?> map) {
        return (Map<String, ?>) map;
    }
}
