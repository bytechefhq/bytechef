/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.public_.web.rest;

import com.bytechef.ee.automation.data.table.public_.web.rest.model.ColumnLinkModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.ColumnTypeModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.DataTableColumnModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.DataTableModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.DataTableRowModel;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.LinkDeleteRule;
import com.bytechef.platform.data.table.domain.LinkSpec;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.tag.domain.Tag;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
final class DataTableModelMapper {

    private DataTableModelMapper() {
    }

    static DataTableModel toModel(DataTableInfo dataTableInfo, List<Tag> tags) {
        return new DataTableModel()
            .id(dataTableInfo.id())
            .workspaceId(dataTableInfo.workspaceId())
            .name(dataTableInfo.name())
            .description(dataTableInfo.description())
            .columns(dataTableInfo.columns()
                .stream()
                .map(DataTableModelMapper::toColumnModel)
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

        return toColumnSpec(
            dataTableColumnModel.getName(), dataTableColumnModel.getType(), dataTableColumnModel.getLink());
    }

    static ColumnSpec toColumnSpec(
        String name, ColumnTypeModel columnTypeModel, @Nullable ColumnLinkModel columnLinkModel) {

        ColumnType columnType = ColumnType.valueOf(columnTypeModel.name());

        if (columnType == ColumnType.LINK && columnLinkModel == null) {
            throw new DataTableException(
                "A LINK column requires a link specification", DataTableErrorType.LINK_TARGET_INVALID);
        }

        if (columnType != ColumnType.LINK && columnLinkModel != null) {
            throw new DataTableException(
                "Only a LINK column can have a link specification", DataTableErrorType.LINK_TARGET_INVALID);
        }

        if (columnLinkModel == null) {
            return new ColumnSpec(name, columnType, null);
        }

        ColumnLinkModel.OnDeleteEnum onDeleteEnum = columnLinkModel.getOnDelete() == null
            ? ColumnLinkModel.OnDeleteEnum.BLOCK : columnLinkModel.getOnDelete();

        LinkSpec linkSpec = new LinkSpec(
            columnLinkModel.getTargetDataTableId(), LinkDeleteRule.valueOf(onDeleteEnum.name()),
            columnLinkModel.getDisplayColumnName());

        return new ColumnSpec(name, columnType, linkSpec);
    }

    private static DataTableColumnModel toColumnModel(ColumnSpec columnSpec) {
        DataTableColumnModel dataTableColumnModel = new DataTableColumnModel()
            .name(columnSpec.name())
            .type(ColumnTypeModel.valueOf(columnSpec.type()
                .name()));

        LinkSpec linkSpec = columnSpec.link();

        if (linkSpec != null) {
            dataTableColumnModel.link(
                new ColumnLinkModel()
                    .targetDataTableId(linkSpec.targetDataTableId())
                    .onDelete(ColumnLinkModel.OnDeleteEnum.valueOf(linkSpec.onDelete()
                        .name()))
                    .displayColumnName(linkSpec.displayColumnName()));
        }

        return dataTableColumnModel;
    }

    private static @Nullable Object toJsonValue(@Nullable Object value) {
        if (value instanceof DataTableRow linkedRow) {
            return toModel(linkedRow);
        }

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
