/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.eval.dataset.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 * @version ee
 */
class AiEvalDatasetTest {

    @Test
    void testConstructorRejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new AiEvalDataset(""));
        assertThrows(IllegalArgumentException.class, () -> new AiEvalDataset("   "));
        assertThrows(IllegalArgumentException.class, () -> new AiEvalDataset(null));
    }

    @Test
    void testConstructorAssignsRequiredFields() {
        AiEvalDataset dataset = new AiEvalDataset("training-set");

        assertEquals("training-set", dataset.getName());
        assertNull(dataset.getDescription());
        assertNull(dataset.getTags());
        assertNull(dataset.getProjectId());
        assertNull(dataset.getArchivedDate());
    }

    @Test
    void testSettersRoundTripValues() {
        AiEvalDataset dataset = new AiEvalDataset("training-set");

        dataset.setDescription("labeled examples for q1 classifier");
        dataset.setTags("[\"classifier\",\"q1\"]");
        dataset.setProjectId(7L);
        dataset.setName("training-set-renamed");

        assertEquals("labeled examples for q1 classifier", dataset.getDescription());
        assertEquals("[\"classifier\",\"q1\"]", dataset.getTags());
        assertEquals(7L, dataset.getProjectId());
        assertEquals("training-set-renamed", dataset.getName());
    }

    @Test
    void testSetNameRejectsBlank() {
        AiEvalDataset dataset = new AiEvalDataset("training-set");

        assertThrows(IllegalArgumentException.class, () -> dataset.setName(""));
        assertThrows(IllegalArgumentException.class, () -> dataset.setName("   "));
        assertThrows(IllegalArgumentException.class, () -> dataset.setName(null));
    }

    @Test
    void testArchiveSetsTimestampAndIsIdempotent() {
        AiEvalDataset dataset = new AiEvalDataset("training-set");

        assertFalse(dataset.isArchived());
        assertNull(dataset.getArchivedDate());

        dataset.archive();

        assertTrue(dataset.isArchived());
        assertNotNull(dataset.getArchivedDate());

        java.time.Instant firstArchive = dataset.getArchivedDate();

        // Second archive call MUST NOT overwrite the original timestamp — preserves audit signal that the
        // dataset was archived once at a specific moment, not on every retry.
        dataset.archive();

        assertEquals(firstArchive, dataset.getArchivedDate());
    }

    @Test
    void testUnarchiveClearsTimestamp() {
        AiEvalDataset dataset = new AiEvalDataset("training-set");

        dataset.archive();

        assertTrue(dataset.isArchived());

        dataset.unarchive();

        assertFalse(dataset.isArchived());
        assertNull(dataset.getArchivedDate());
    }

    @Test
    void testUnarchiveOnUnarchivedIsNoOp() {
        AiEvalDataset dataset = new AiEvalDataset("training-set");

        dataset.unarchive();

        assertFalse(dataset.isArchived());
    }
}
