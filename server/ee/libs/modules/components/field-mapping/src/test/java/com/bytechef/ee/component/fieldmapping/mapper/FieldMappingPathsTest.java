/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.fieldmapping.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class FieldMappingPathsTest {

    @Test
    void testContainsPathAndGetValueOnTopLevelKey() {
        Map<String, Object> map = Map.of("title", "Dr");

        assertTrue(FieldMappingPaths.containsPath(map, "title"));
        assertEquals("Dr", FieldMappingPaths.getValue(map, "title"));
    }

    @Test
    void testContainsPathIsTrueForPresentNullValue() {
        Map<String, Object> map = new HashMap<>();

        map.put("title", null);

        assertTrue(FieldMappingPaths.containsPath(map, "title"));
        assertNull(FieldMappingPaths.getValue(map, "title"));
    }

    @Test
    void testContainsPathIsFalseForMissingKey() {
        assertFalse(FieldMappingPaths.containsPath(Map.of("title", "Dr"), "email"));
    }

    @Test
    void testNestedReadThroughDottedPath() {
        Map<String, Object> map = Map.of("properties", Map.of("firstname", "Ada"));

        assertTrue(FieldMappingPaths.containsPath(map, "properties.firstname"));
        assertEquals("Ada", FieldMappingPaths.getValue(map, "properties.firstname"));
        assertFalse(FieldMappingPaths.containsPath(map, "properties.lastname"));
        assertFalse(FieldMappingPaths.containsPath(map, "title.firstname"));
    }

    @Test
    void testSetValueCreatesIntermediateMaps() {
        Map<String, Object> map = new LinkedHashMap<>();

        FieldMappingPaths.setValue(map, "properties.firstname", "Ada");
        FieldMappingPaths.setValue(map, "properties.lastname", "Lovelace");
        FieldMappingPaths.setValue(map, "email", "ada@example.com");

        assertEquals(
            Map.of("properties", Map.of("firstname", "Ada", "lastname", "Lovelace"), "email", "ada@example.com"),
            map);
    }

    @Test
    void testSetValueCopiesAnImmutableNestedMapInsteadOfMutatingIt() {
        Map<String, Object> immutableProperties = Map.of("firstname", "Ada");
        Map<String, Object> map = new LinkedHashMap<>();

        map.put("properties", immutableProperties);

        FieldMappingPaths.setValue(map, "properties.lastname", "Lovelace");

        assertEquals(Map.of("firstname", "Ada"), immutableProperties);
        assertEquals(Map.of("firstname", "Ada", "lastname", "Lovelace"), map.get("properties"));
    }
}
