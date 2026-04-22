/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 * @version ee
 */
class AiEvalDatasetItemTest {

    @Test
    void testConstructorRejectsBlankInput() {
        assertThrows(IllegalArgumentException.class, () -> new AiEvalDatasetItem(1L, 2L, ""));
        assertThrows(IllegalArgumentException.class, () -> new AiEvalDatasetItem(1L, 2L, "   "));
        assertThrows(IllegalArgumentException.class, () -> new AiEvalDatasetItem(1L, 2L, null));
    }

    @Test
    void testConstructorRejectsNullIds() {
        assertThrows(NullPointerException.class, () -> new AiEvalDatasetItem(null, 2L, "{\"q\":\"1+1\"}"));
        assertThrows(NullPointerException.class, () -> new AiEvalDatasetItem(1L, null, "{\"q\":\"1+1\"}"));
    }

    @Test
    void testConstructorAssignsRequiredFields() {
        AiEvalDatasetItem item = new AiEvalDatasetItem(1L, 2L, "{\"q\":\"1+1\"}");

        assertEquals(1L, item.getDatasetId());
        assertEquals(2L, item.getDatasetVersionId());
        assertEquals("{\"q\":\"1+1\"}", item.getInput());
    }

    @Test
    void testSettersRoundTripValues() {
        AiEvalDatasetItem item = new AiEvalDatasetItem(1L, 2L, "{\"q\":\"1+1\"}");

        item.setExpectedOutput("{\"a\":\"2\"}");
        item.setMetadata("{\"source\":\"manual\"}");
        item.setSourceTraceId(99L);

        assertEquals("{\"a\":\"2\"}", item.getExpectedOutput());
        assertEquals("{\"source\":\"manual\"}", item.getMetadata());
        assertEquals(99L, item.getSourceTraceId());
    }

    @Test
    void testContentSettersRejectMutationOnPersistedItem() throws Exception {
        AiEvalDatasetItem item = new AiEvalDatasetItem(1L, 2L, "{\"q\":\"1+1\"}");

        // Simulate a hydrated row (Spring Data JDBC reflective writes).
        java.lang.reflect.Field idField = AiEvalDatasetItem.class.getDeclaredField("id");

        idField.setAccessible(true);
        idField.set(item, 42L);

        IllegalStateException inputError = assertThrows(
            IllegalStateException.class, () -> item.setInput("{\"q\":\"changed\"}"));
        IllegalStateException expectedOutputError = assertThrows(
            IllegalStateException.class, () -> item.setExpectedOutput("{\"a\":\"changed\"}"));
        IllegalStateException metadataError = assertThrows(
            IllegalStateException.class, () -> item.setMetadata("{\"source\":\"changed\"}"));

        assertEquals(true, inputError.getMessage()
            .contains("id=42"));
        assertEquals(true, expectedOutputError.getMessage()
            .contains("id=42"));
        assertEquals(true, metadataError.getMessage()
            .contains("id=42"));
    }

    @Test
    void testSetInputRejectsBlank() {
        AiEvalDatasetItem item = new AiEvalDatasetItem(1L, 2L, "{\"q\":\"1+1\"}");

        assertThrows(IllegalArgumentException.class, () -> item.setInput(""));
        assertThrows(IllegalArgumentException.class, () -> item.setInput("   "));
        assertThrows(IllegalArgumentException.class, () -> item.setInput(null));
    }
}
