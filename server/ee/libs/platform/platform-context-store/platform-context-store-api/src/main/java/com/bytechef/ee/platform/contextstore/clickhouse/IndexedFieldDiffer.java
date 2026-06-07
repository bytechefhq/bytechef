/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.contextstore.clickhouse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Phase 16 follow-up: pure helper that diffs an entity's old and new {@code indexedFields} maps and produces the
 * minimal list of {@link IndexedFieldChange}s needed to reconcile the ClickHouse table.
 *
 * <p>
 * Output order is deterministic: Adds first (alphabetical), then Drops (alphabetical), then TypeChanges (alphabetical).
 * The grouping matters for ALTER TABLE statement order — adding new columns before dropping old ones keeps the table in
 * a valid intermediate state if a partial failure leaves us between two ALTERs.
 * </p>
 *
 * <p>
 * Field types are normalised to upper-case before comparison (matches {@code ClickHouseTableDdlGenerator}'s
 * case-insensitive type resolution). Field names are NOT case-normalised here — that's the caller's contract upstream
 * of the entity row.
 * </p>
 *
 * @author Ivica Cardic
 * @version ee
 */
public final class IndexedFieldDiffer {

    private IndexedFieldDiffer() {
    }

    public static List<IndexedFieldChange> diff(Map<String, ?> oldFields, Map<String, ?> newFields) {
        Map<String, String> oldNormalised = normalise(oldFields);
        Map<String, String> newNormalised = normalise(newFields);

        List<IndexedFieldChange.Add> adds = new ArrayList<>();
        List<IndexedFieldChange.Drop> drops = new ArrayList<>();
        List<IndexedFieldChange.TypeChange> typeChanges = new ArrayList<>();

        for (Map.Entry<String, String> newEntry : newNormalised.entrySet()) {
            String fieldName = newEntry.getKey();
            String newType = newEntry.getValue();
            String oldType = oldNormalised.get(fieldName);

            if (oldType == null) {
                adds.add(new IndexedFieldChange.Add(fieldName, newType));
            } else if (!oldType.equals(newType)) {
                typeChanges.add(new IndexedFieldChange.TypeChange(fieldName, oldType, newType));
            }
        }

        for (String oldFieldName : oldNormalised.keySet()) {
            if (!newNormalised.containsKey(oldFieldName)) {
                drops.add(new IndexedFieldChange.Drop(oldFieldName));
            }
        }

        List<IndexedFieldChange> result = new ArrayList<>(adds.size() + drops.size() + typeChanges.size());

        result.addAll(adds);
        result.addAll(drops);
        result.addAll(typeChanges);

        return result;
    }

    private static Map<String, String> normalise(Map<String, ?> fields) {
        Map<String, String> normalised = new TreeMap<>();

        if (fields == null) {
            return normalised;
        }

        for (Map.Entry<String, ?> entry : fields.entrySet()) {
            normalised.put(entry.getKey(), String.valueOf(entry.getValue())
                .toUpperCase(Locale.ROOT));
        }

        return normalised;
    }
}
