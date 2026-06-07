/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.clickhouse;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Pins the diff output: empty cases, add-only, drop-only, type-change-only, mixed, no-op, and case-insensitive type
 * comparison.
 *
 * @author Ivica Cardic
 * @version ee
 */
class IndexedFieldDifferTest {

    @Test
    void testEmptyToEmptyReturnsEmpty() {
        assertThat(IndexedFieldDiffer.diff(Map.of(), Map.of())).isEmpty();
    }

    @Test
    void testNullOldReturnsAllAdds() {
        List<IndexedFieldChange> diff = IndexedFieldDiffer.diff(null, Map.of("name", "TEXT", "age", "NUMERIC"));

        assertThat(diff).hasSize(2);
        assertThat(diff).allMatch(change -> change instanceof IndexedFieldChange.Add);
    }

    @Test
    void testEmptyToNonEmptyAddsAllAlphabetically() {
        List<IndexedFieldChange> diff = IndexedFieldDiffer.diff(
            Map.of(),
            Map.of("title", "TEXT", "amount", "NUMERIC", "updated_at", "TIMESTAMP"));

        assertThat(diff).hasSize(3);
        // Alphabetical: amount, title, updated_at
        assertThat(diff.get(0)).isEqualTo(new IndexedFieldChange.Add("amount", "NUMERIC"));
        assertThat(diff.get(1)).isEqualTo(new IndexedFieldChange.Add("title", "TEXT"));
        assertThat(diff.get(2)).isEqualTo(new IndexedFieldChange.Add("updated_at", "TIMESTAMP"));
    }

    @Test
    void testNonEmptyToEmptyDropsAll() {
        List<IndexedFieldChange> diff = IndexedFieldDiffer.diff(
            Map.of("title", "TEXT", "amount", "NUMERIC"), Map.of());

        assertThat(diff).hasSize(2);
        assertThat(diff).allMatch(change -> change instanceof IndexedFieldChange.Drop);
    }

    @Test
    void testTypeChangeOnly() {
        List<IndexedFieldChange> diff = IndexedFieldDiffer.diff(
            Map.of("age", "TEXT"), Map.of("age", "NUMERIC"));

        assertThat(diff).containsExactly(new IndexedFieldChange.TypeChange("age", "TEXT", "NUMERIC"));
    }

    @Test
    void testMixedAddDropTypeChangeRendersInDeclaredOrder() {
        List<IndexedFieldChange> diff = IndexedFieldDiffer.diff(
            Map.of("title", "TEXT", "old_field", "TEXT", "age", "TEXT"),
            Map.of("title", "TEXT", "new_field", "NUMERIC", "age", "NUMERIC"));

        // Expected: Adds (new_field), then Drops (old_field), then TypeChanges (age)
        assertThat(diff).hasSize(3);
        assertThat(diff.get(0)).isEqualTo(new IndexedFieldChange.Add("new_field", "NUMERIC"));
        assertThat(diff.get(1)).isEqualTo(new IndexedFieldChange.Drop("old_field"));
        assertThat(diff.get(2)).isEqualTo(new IndexedFieldChange.TypeChange("age", "TEXT", "NUMERIC"));
    }

    @Test
    void testIdentityDiffIsEmpty() {
        Map<String, String> same = Map.of("title", "TEXT", "amount", "NUMERIC");

        assertThat(IndexedFieldDiffer.diff(same, same)).isEmpty();
    }

    @Test
    void testCaseInsensitiveTypeComparisonNotTreatedAsChange() {
        List<IndexedFieldChange> diff = IndexedFieldDiffer.diff(
            Map.of("name", "text"), Map.of("name", "TEXT"));

        // Both normalise to "TEXT"; no change.
        assertThat(diff).isEmpty();
    }

    @Test
    void testMultipleAddsRenderInAlphabeticalOrder() {
        List<IndexedFieldChange> diff = IndexedFieldDiffer.diff(
            Map.of(), Map.of("zebra", "TEXT", "alpha", "TEXT", "mike", "TEXT"));

        assertThat(diff).extracting(IndexedFieldChange::fieldName)
            .containsExactly("alpha", "mike", "zebra");
    }
}
