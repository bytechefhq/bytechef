/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.guest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.component.definition.TypeReference;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class GuestParametersTest {

    private final GuestParameters guestParameters = new GuestParameters(
        Map.of(
            "active", "true",
            "amount", "42.5",
            "count", 7,
            "createdDate", "2026-07-04",
            "createdDateTime", "2026-07-04T10:15:30",
            "items", List.of(Map.of("name", "first"), Map.of("name", "second")),
            "name", "test-name",
            "nested", Map.of("inner", Map.of("value", 13)),
            "tags", List.of("one", "two")));

    @Test
    void testScalarCoercions() {
        assertEquals("test-name", guestParameters.getRequiredString("name"));
        assertEquals(7, guestParameters.getRequiredInteger("count"));
        assertEquals(7L, guestParameters.getRequiredLong("count"));
        assertEquals(42.5, guestParameters.getRequiredDouble("amount"));
        assertTrue(guestParameters.getRequiredBoolean("active"));
        assertEquals(LocalDate.of(2026, 7, 4), guestParameters.getRequiredLocalDate("createdDate"));
        assertEquals(
            LocalDateTime.of(2026, 7, 4, 10, 15, 30), guestParameters.getRequiredLocalDateTime("createdDateTime"));
    }

    @Test
    void testDefaults() {
        assertEquals("fallback", guestParameters.getString("missing", "fallback"));
        assertEquals(99, guestParameters.getInteger("missing", 99));
        assertNull(guestParameters.getString("missing"));
    }

    @Test
    void testRequiredMissingThrows() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class, () -> guestParameters.getRequiredString("missing"));

        assertTrue(exception.getMessage()
            .contains("missing"));
    }

    @Test
    void testListAndMapAccess() {
        assertEquals(List.of("one", "two"), guestParameters.getRequiredList("tags", String.class));

        List<Map<String, String>> items = guestParameters.getRequiredList(
            "items", new TypeReference<Map<String, String>>() {});

        assertEquals("first", items.getFirst()
            .get("name"));

        Map<String, ?> nested = guestParameters.getRequiredMap("nested");

        assertTrue(nested.containsKey("inner"));
    }

    @Test
    void testPathAccess() {
        assertEquals(13, guestParameters.getFromPath("nested.inner.value", Integer.class));
        assertEquals("second", guestParameters.getFromPath("items[1].name", String.class));
        assertTrue(guestParameters.containsPath("nested.inner"));
        assertNull(guestParameters.getFromPath("nested.missing.value", String.class));
    }

    @Test
    void testFileEntryUnsupported() {
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class, () -> guestParameters.getFileEntry("anything"));

        assertTrue(exception.getMessage()
            .contains("java-loader=class-loader"));
    }

    @Test
    void testToMapReturnsBackingValues() {
        Map<String, ?> map = guestParameters.toMap();

        assertEquals("test-name", map.get("name"));
    }
}
