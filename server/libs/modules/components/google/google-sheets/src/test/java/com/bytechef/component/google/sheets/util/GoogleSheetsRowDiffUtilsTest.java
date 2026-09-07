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

package com.bytechef.component.google.sheets.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author Anshul Goel
 */
class GoogleSheetsRowDiffUtilsTest {

    @Test
    void testGetRowHashIgnoresTrailingBlankCells() {
        assertEquals(
            GoogleSheetsRowDiffUtils.getRowHash(List.of("a", "b")),
            GoogleSheetsRowDiffUtils.getRowHash(Arrays.asList("a", "b", "", null)));
    }

    @Test
    void testGetRowHashDistinguishesCellBoundaries() {
        assertNotEquals(GoogleSheetsRowDiffUtils.getRowHash(List.of("ab")),
            GoogleSheetsRowDiffUtils.getRowHash(List.of("a", "b")));
    }

    @Test
    void testIsBlankRow() {
        assertTrue(GoogleSheetsRowDiffUtils.isBlankRow(List.of()));
        assertTrue(GoogleSheetsRowDiffUtils.isBlankRow(Arrays.asList("", null, " ")));
        assertFalse(GoogleSheetsRowDiffUtils.isBlankRow(Arrays.asList("", "a")));
    }

    @Test
    void testGetInsertedRowIndexesOnFirstRun() {
        assertEquals(List.of(0, 1), GoogleSheetsRowDiffUtils.getInsertedRowIndexes(List.of(), List.of("a", "b")));
    }

    @Test
    void testGetInsertedRowIndexesForRowAppendedAtBottom() {
        assertEquals(List.of(2),
            GoogleSheetsRowDiffUtils.getInsertedRowIndexes(List.of("a", "b"), List.of("a", "b", "c")));
    }

    @Test
    void testGetInsertedRowIndexesForRowInsertedInTheMiddle() {
        assertEquals(List.of(1),
            GoogleSheetsRowDiffUtils.getInsertedRowIndexes(List.of("a", "b", "c"), List.of("a", "x", "b", "c")));
    }

    @Test
    void testGetInsertedRowIndexesForEditedRow() {
        assertEquals(List.of(),
            GoogleSheetsRowDiffUtils.getInsertedRowIndexes(List.of("a", "b", "c"), List.of("a", "x", "c")));
    }

    @Test
    void testGetInsertedRowIndexesForDeletedRow() {
        assertEquals(List.of(),
            GoogleSheetsRowDiffUtils.getInsertedRowIndexes(List.of("a", "b", "c"), List.of("a", "c")));
    }

    @Test
    void testGetInsertedRowIndexesReportsOnlySurplusWhenRowIsEditedAndInserted() {
        List<Integer> result = GoogleSheetsRowDiffUtils.getInsertedRowIndexes(
            List.of("a", "b", "c"), List.of("a", "x", "y", "c"));

        assertEquals(1, result.size());
    }

    @Test
    void testGetInsertedRowIndexesFallsBackWhenTooManyRowsDiffer() {
        List<String> knownRowHashes = new ArrayList<>();
        List<String> currentRowHashes = new ArrayList<>();

        for (int index = 0; index < 600; index++) {
            knownRowHashes.add("known" + index);
            currentRowHashes.add("current" + index);
        }

        currentRowHashes.add("current600");

        assertEquals(List.of(600), GoogleSheetsRowDiffUtils.getInsertedRowIndexes(knownRowHashes, currentRowHashes));
    }
}
