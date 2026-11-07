/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.table.remote.client.service;

import com.bytechef.ee.platform.data.table.remote.grpc.proto.DataTableRowResponse;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.DataTableRowsResponse;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.DeleteRowsRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.FindRowsRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.InsertRowsRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.ListRowsRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.RowIdRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.TableRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.UpdateRowRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.UpdateRowsRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteDataTableRowServiceClient extends AbstractRemoteDataTableClient implements DataTableRowService {

    public RemoteDataTableRowServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        super(grpcChannels, objectMapper);
    }

    @Override
    public boolean deleteRow(String baseName, long id, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableRow> deleteAllRows(String baseName, long environmentId) {
        DataTableRowsResponse dataTableRowsResponse = call(
            () -> getStub().deleteAllRows(
                TableRequest.newBuilder()
                    .setBaseName(baseName)
                    .setEnvironmentId(environmentId)
                    .build()));

        return dataTableProtoMapper.toDataTableRows(dataTableRowsResponse.getRows());
    }

    @Override
    public List<DataTableRow> deleteRows(String baseName, DataTableRowConditions conditions, long environmentId) {
        DataTableRowsResponse dataTableRowsResponse = call(
            () -> getStub().deleteRows(
                DeleteRowsRequest.newBuilder()
                    .setBaseName(baseName)
                    .setConditionGroups(dataTableProtoMapper.writeConditions(conditions))
                    .setEnvironmentId(environmentId)
                    .build()));

        return dataTableProtoMapper.toDataTableRows(dataTableRowsResponse.getRows());
    }

    @Override
    public @Nullable DataTableRow getRow(String baseName, long id, long environmentId) {
        DataTableRowResponse dataTableRowResponse = call(
            () -> getStub().getRow(
                RowIdRequest.newBuilder()
                    .setBaseName(baseName)
                    .setId(id)
                    .setEnvironmentId(environmentId)
                    .build()));

        if (!dataTableRowResponse.hasRow()) {
            return null;
        }

        return dataTableProtoMapper.toDataTableRow(dataTableRowResponse.getRow());
    }

    @Override
    public DataTableRow insertRow(String baseName, Map<String, Object> values, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableRow> insertRows(String baseName, List<Map<String, Object>> rowValues, long environmentId) {
        DataTableRowsResponse dataTableRowsResponse = call(
            () -> getStub().insertRows(
                InsertRowsRequest.newBuilder()
                    .setBaseName(baseName)
                    .setRowValues(dataTableProtoMapper.write(rowValues))
                    .setEnvironmentId(environmentId)
                    .build()));

        return dataTableProtoMapper.toDataTableRows(dataTableRowsResponse.getRows());
    }

    @Override
    public List<DataTableRow> listRows(String baseName, int limit, int offset, long environmentId) {
        DataTableRowsResponse dataTableRowsResponse = call(
            () -> getStub().listRows(
                ListRowsRequest.newBuilder()
                    .setBaseName(baseName)
                    .setLimit(limit)
                    .setOffset(offset)
                    .setEnvironmentId(environmentId)
                    .build()));

        return dataTableProtoMapper.toDataTableRows(dataTableRowsResponse.getRows());
    }

    @Override
    public List<DataTableRow> findRows(
        String baseName, DataTableRowConditions conditions, @Nullable DataTableRowSort sort, int limit, int offset,
        long environmentId) {

        FindRowsRequest.Builder findRowsRequestBuilder = FindRowsRequest.newBuilder()
            .setBaseName(baseName)
            .setConditionGroups(dataTableProtoMapper.writeConditions(conditions))
            .setLimit(limit)
            .setOffset(offset)
            .setEnvironmentId(environmentId);

        if (sort != null) {
            findRowsRequestBuilder.setSort(dataTableProtoMapper.write(sort));
        }

        DataTableRowsResponse dataTableRowsResponse = call(
            () -> getStub().findRows(findRowsRequestBuilder.build()));

        return dataTableProtoMapper.toDataTableRows(dataTableRowsResponse.getRows());
    }

    @Override
    public String exportCsv(String baseName, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void importCsv(String baseName, String csv, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow updateRow(String baseName, long id, Map<String, Object> values, long environmentId) {
        DataTableRowResponse dataTableRowResponse = call(
            () -> getStub().updateRow(
                UpdateRowRequest.newBuilder()
                    .setBaseName(baseName)
                    .setId(id)
                    .setValues(dataTableProtoMapper.write(values))
                    .setEnvironmentId(environmentId)
                    .build()));

        return dataTableProtoMapper.toDataTableRow(dataTableRowResponse.getRow());
    }

    @Override
    public List<DataTableRow> updateRows(
        String baseName, DataTableRowConditions conditions, Map<String, Object> values, long environmentId) {

        DataTableRowsResponse dataTableRowsResponse = call(
            () -> getStub().updateRows(
                UpdateRowsRequest.newBuilder()
                    .setBaseName(baseName)
                    .setConditionGroups(dataTableProtoMapper.writeConditions(conditions))
                    .setValues(dataTableProtoMapper.write(values))
                    .setEnvironmentId(environmentId)
                    .build()));

        return dataTableProtoMapper.toDataTableRows(dataTableRowsResponse.getRows());
    }
}
