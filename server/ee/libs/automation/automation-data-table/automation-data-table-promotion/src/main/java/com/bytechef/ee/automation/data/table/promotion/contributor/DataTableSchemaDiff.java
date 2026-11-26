/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.promotion.contributor;

import com.bytechef.ee.automation.promotion.dto.PromotionSchemaChange;
import com.bytechef.ee.automation.promotion.dto.PromotionSchemaChange.Kind;
import com.bytechef.ee.automation.promotion.exception.EnvironmentPromotionErrorType;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.ColumnTypeConversion;
import com.bytechef.platform.data.table.domain.LinkSpec;
import com.bytechef.platform.data.table.domain.ReservedColumns;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class DataTableSchemaDiff {

    private DataTableSchemaDiff() {
    }

    public static List<PromotionSchemaChange> diff(
        String tableName, @Nullable List<ColumnSpec> targetColumns, List<ColumnSpec> sourceColumns) {

        Map<String, ColumnSpec> sourceColumnsByName = columnsByName(tableName, sourceColumns);

        List<PromotionSchemaChange> schemaChanges = new ArrayList<>();

        if (targetColumns == null) {
            schemaChanges.add(new PromotionSchemaChange(tableName, null, Kind.CREATE_TABLE, null, null, false));

            for (ColumnSpec sourceColumn : sourceColumnsByName.values()) {
                schemaChanges.add(addColumn(tableName, sourceColumn));
            }

            return schemaChanges;
        }

        Map<String, ColumnSpec> targetColumnsByName = columnsByName(tableName, targetColumns);

        for (Map.Entry<String, ColumnSpec> targetEntry : targetColumnsByName.entrySet()) {
            if (!sourceColumnsByName.containsKey(targetEntry.getKey())) {
                schemaChanges.add(dropColumn(tableName, targetEntry.getValue()));
            }
        }

        for (Map.Entry<String, ColumnSpec> sourceEntry : sourceColumnsByName.entrySet()) {
            ColumnSpec sourceColumn = sourceEntry.getValue();
            ColumnSpec targetColumn = targetColumnsByName.get(sourceEntry.getKey());

            if (targetColumn == null) {
                schemaChanges.add(addColumn(tableName, sourceColumn));
            } else if (isLink(sourceColumn) && isLink(targetColumn)) {
                if (!sameLink(sourceColumn.link(), targetColumn.link())) {
                    schemaChanges.add(
                        new PromotionSchemaChange(
                            tableName, sourceColumn.name(), Kind.CHANGE_LINK, typeName(targetColumn),
                            typeName(sourceColumn), retargetsLink(sourceColumn.link(), targetColumn.link())));
                }
            } else if (isLink(sourceColumn) || isLink(targetColumn)) {
                schemaChanges.add(dropColumn(tableName, targetColumn));
                schemaChanges.add(addColumn(tableName, sourceColumn));
            } else if (sourceColumn.type() != targetColumn.type()) {
                schemaChanges.add(
                    new PromotionSchemaChange(
                        tableName, sourceColumn.name(), Kind.CHANGE_COLUMN_TYPE, typeName(targetColumn),
                        typeName(sourceColumn), isLossy(targetColumn, sourceColumn)));
            }
        }

        return schemaChanges;
    }

    private static PromotionSchemaChange addColumn(String tableName, ColumnSpec sourceColumn) {
        return new PromotionSchemaChange(tableName, sourceColumn.name(), Kind.ADD_COLUMN, null, typeName(sourceColumn),
            false);
    }

    private static Map<String, ColumnSpec> columnsByName(String tableName, List<ColumnSpec> columnSpecs) {
        Map<String, ColumnSpec> columnsByName = new LinkedHashMap<>();

        for (ColumnSpec columnSpec : columnSpecs) {
            if (ReservedColumns.isReserved(columnSpec.name())) {
                continue;
            }

            String columnName = columnSpec.name();

            ColumnSpec previousColumnSpec = columnsByName.putIfAbsent(columnName.toLowerCase(Locale.ROOT), columnSpec);

            if (previousColumnSpec != null) {
                throw new ConfigurationException(
                    "Data table '%s' has columns '%s' and '%s' whose names differ only by case".formatted(
                        tableName, previousColumnSpec.name(), columnName),
                    EnvironmentPromotionErrorType.SCHEMA_CHANGE_FAILED);
            }
        }

        return columnsByName;
    }

    private static PromotionSchemaChange dropColumn(String tableName, ColumnSpec targetColumn) {
        return new PromotionSchemaChange(
            tableName, targetColumn.name(), Kind.DROP_COLUMN, typeName(targetColumn), null,
            true);
    }

    private static boolean isLossy(ColumnSpec targetColumn, ColumnSpec sourceColumn) {
        return ColumnTypeConversion.of(targetColumn.type(), sourceColumn.type()) == ColumnTypeConversion.LOSSY;
    }

    private static boolean retargetsLink(@Nullable LinkSpec sourceLink, @Nullable LinkSpec targetLink) {
        return sourceLink != null && targetLink != null &&
            sourceLink.targetDataTableId() != targetLink.targetDataTableId();
    }

    private static boolean isLink(ColumnSpec columnSpec) {
        return columnSpec.type() == ColumnType.LINK;
    }

    private static boolean sameLink(@Nullable LinkSpec sourceLink, @Nullable LinkSpec targetLink) {
        if (sourceLink == null || targetLink == null) {
            return sourceLink == targetLink;
        }

        String sourceDisplayColumnName = sourceLink.displayColumnName();
        String targetDisplayColumnName = targetLink.displayColumnName();

        boolean sameDisplayColumn;

        if (sourceDisplayColumnName == null || targetDisplayColumnName == null) {
            sameDisplayColumn = Objects.equals(sourceDisplayColumnName, targetDisplayColumnName);
        } else {
            sameDisplayColumn = sourceDisplayColumnName.equalsIgnoreCase(targetDisplayColumnName);
        }

        return sourceLink.targetDataTableId() == targetLink.targetDataTableId() &&
            sourceLink.onDelete() == targetLink.onDelete() && sameDisplayColumn;
    }

    private static String typeName(ColumnSpec columnSpec) {
        ColumnType columnType = columnSpec.type();

        return columnType.name();
    }
}
