/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.fieldmapping.mapper;

import com.bytechef.commons.util.MapUtils;
import java.util.List;
import java.util.Map;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public record FieldMappingDescriptor(String objectType, List<Mapping> mappings) {

    private static final String APPLICATION_FIELD = "applicationField";
    private static final String INTEGRATION_FIELD = "integrationField";
    private static final String MAPPINGS = "mappings";
    private static final String OBJECT_TYPE = "objectType";
    private static final String VALUE = "value";

    public FieldMappingDescriptor {
        mappings = List.copyOf(mappings);
    }

    public static FieldMappingDescriptor of(Map<String, ?> map) {
        List<Object> rawMappings = MapUtils.getList(map, MAPPINGS, Object.class, List.of());

        if (rawMappings.isEmpty()) {
            throw new IllegalArgumentException("Field mapping has no mappings");
        }

        List<Mapping> mappings = rawMappings.stream()
            .map(FieldMappingDescriptor::toMapping)
            .toList();

        return new FieldMappingDescriptor(MapUtils.getString(map, OBJECT_TYPE), mappings);
    }

    private static Mapping toMapping(Object rawMapping) {
        if (!(rawMapping instanceof Map<?, ?> mappingMap)) {
            throw new IllegalArgumentException("Each field mapping entry must be an object, got: " + rawMapping);
        }

        Map<String, ?> mapping = castMap(mappingMap);

        Map<String, ?> applicationField = MapUtils.getRequiredMap(mapping, APPLICATION_FIELD);

        return new Mapping(
            MapUtils.getRequiredString(applicationField, VALUE),
            MapUtils.getRequiredString(mapping, INTEGRATION_FIELD));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> castMap(Map<?, ?> map) {
        return (Map<String, ?>) map;
    }

    public record Mapping(String applicationField, String integrationField) {
    }
}
