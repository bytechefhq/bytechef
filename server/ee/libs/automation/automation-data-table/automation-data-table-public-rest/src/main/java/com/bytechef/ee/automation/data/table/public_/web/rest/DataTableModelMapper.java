/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.public_.web.rest;

import com.bytechef.ee.automation.data.table.public_.web.rest.model.ColumnTypeModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.DataTableColumnModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.DataTableModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.DataTableRowModel;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.tag.domain.Tag;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Maps between the platform's data table domain and the public API's generated models.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
final class DataTableModelMapper {

    private DataTableModelMapper() {
    }

    static DataTableModel toModel(DataTableInfo dataTableInfo, List<Tag> tags) {
        return new DataTableModel()
            .name(dataTableInfo.baseName())
            .description(dataTableInfo.description())
            .columns(dataTableInfo.columns()
                .stream()
                .map(columnSpec -> new DataTableColumnModel()
                    .name(columnSpec.name())
                    .type(ColumnTypeModel.valueOf(columnSpec.type()
                        .name())))
                .toList())
            .tags(tags.stream()
                .map(Tag::getName)
                .toList())
            .lastModifiedDate(
                dataTableInfo.lastModifiedDate() == null
                    ? null
                    : dataTableInfo.lastModifiedDate()
                        .atOffset(ZoneOffset.UTC));
    }

    static DataTableRowModel toModel(DataTableRow dataTableRow) {
        Map<String, Object> values = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : dataTableRow.values()
            .entrySet()) {

            values.put(entry.getKey(), toJsonValue(entry.getValue()));
        }

        return new DataTableRowModel()
            .id(dataTableRow.id())
            .externalId(dataTableRow.externalId())
            .values(values);
    }

    static ColumnSpec toColumnSpec(DataTableColumnModel dataTableColumnModel) {
        DataTableApiSupport.validateColumnName(dataTableColumnModel.getName());

        return new ColumnSpec(
            dataTableColumnModel.getName(), ColumnType.valueOf(dataTableColumnModel.getType()
                .name()));
    }

    /** JDBC hands back java.sql types; the wire contract is ISO text for dates and plain JSON for the rest. */
    private static @Nullable Object toJsonValue(@Nullable Object value) {
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toInstant()
                .toString();
        }

        if (value instanceof java.sql.Date date) {
            return date.toLocalDate()
                .toString();
        }

        return value;
    }
}
