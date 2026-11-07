/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.table.remote.grpc;

import com.bytechef.ee.remote.grpc.JsonBytes;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort;
import com.google.protobuf.ByteString;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class DataTableProtoMapper {

    private static final TypeReference<List<List<DataTableRowFilter>>> CONDITION_GROUPS_TYPE_REFERENCE =
        new TypeReference<>() {};
    private static final TypeReference<List<DataTableInfo>> DATA_TABLE_INFOS_TYPE_REFERENCE =
        new TypeReference<>() {};
    private static final TypeReference<DataTableRow> DATA_TABLE_ROW_TYPE_REFERENCE = new TypeReference<>() {};
    private static final TypeReference<List<DataTableRow>> DATA_TABLE_ROWS_TYPE_REFERENCE =
        new TypeReference<>() {};
    private static final TypeReference<DataTableRowSort> DATA_TABLE_ROW_SORT_TYPE_REFERENCE =
        new TypeReference<>() {};
    private static final TypeReference<List<Map<String, Object>>> ROW_VALUES_TYPE_REFERENCE =
        new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> VALUES_TYPE_REFERENCE = new TypeReference<>() {};

    private final JsonBytes jsonBytes;

    public DataTableProtoMapper(ObjectMapper objectMapper) {
        this.jsonBytes = new JsonBytes(objectMapper);
    }

    public DataTableRowConditions toDataTableRowConditions(ByteString bytes) {
        List<List<DataTableRowFilter>> groups = jsonBytes.read(bytes, CONDITION_GROUPS_TYPE_REFERENCE);

        if (groups == null || groups.isEmpty()) {
            return DataTableRowConditions.matchAll();
        }

        return DataTableRowConditions.anyOf(groups);
    }

    public List<DataTableInfo> toDataTableInfos(ByteString bytes) {
        List<DataTableInfo> dataTableInfos = jsonBytes.read(bytes, DATA_TABLE_INFOS_TYPE_REFERENCE);

        return dataTableInfos == null ? List.of() : dataTableInfos;
    }

    public DataTableRow toDataTableRow(ByteString bytes) {
        return Objects.requireNonNull(jsonBytes.read(bytes, DATA_TABLE_ROW_TYPE_REFERENCE), "dataTableRow");
    }

    public List<DataTableRow> toDataTableRows(ByteString bytes) {
        List<DataTableRow> dataTableRows = jsonBytes.read(bytes, DATA_TABLE_ROWS_TYPE_REFERENCE);

        return dataTableRows == null ? List.of() : dataTableRows;
    }

    public DataTableRowSort toDataTableRowSort(ByteString bytes) {
        return Objects.requireNonNull(jsonBytes.read(bytes, DATA_TABLE_ROW_SORT_TYPE_REFERENCE), "dataTableRowSort");
    }

    public List<Map<String, Object>> toRowValues(ByteString bytes) {
        List<Map<String, Object>> rowValues = jsonBytes.read(bytes, ROW_VALUES_TYPE_REFERENCE);

        return rowValues == null ? List.of() : rowValues;
    }

    public Map<String, Object> toValues(ByteString bytes) {
        Map<String, Object> values = jsonBytes.read(bytes, VALUES_TYPE_REFERENCE);

        return values == null ? Map.of() : values;
    }

    public ByteString write(Object value) {
        return jsonBytes.write(value);
    }

    public ByteString writeConditions(DataTableRowConditions dataTableRowConditions) {
        return jsonBytes.write(dataTableRowConditions.groups());
    }
}
