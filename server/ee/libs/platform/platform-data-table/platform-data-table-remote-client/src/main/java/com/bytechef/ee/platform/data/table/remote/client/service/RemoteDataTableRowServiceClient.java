/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.table.remote.client.service;

import com.bytechef.ee.platform.data.table.remote.grpc.proto.DataTableRowResponse;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.DeleteRowsRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.FindRowsRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.InsertRowsRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.ListRowsRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.RowIdRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.TableRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.UpdateRowsRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.data.table.execution.domain.CreateStrategy;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort;
import com.bytechef.platform.data.table.execution.domain.ExternalIdPatch;
import com.bytechef.platform.data.table.execution.domain.NewRow;
import com.bytechef.platform.data.table.execution.domain.UpsertResult;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public List<DataTableRow> listRows(long dataTableId, int limit, int offset, long environmentId) {
        return dataTableProtoMapper.toDataTableRows(
            call(
                () -> getStub().listRows(
                    ListRowsRequest.newBuilder()
                        .setDataTableId(dataTableId)
                        .setLimit(limit)
                        .setOffset(offset)
                        .setEnvironmentId(environmentId)
                        .build()))
                            .getRows());
    }

    @Override
    public List<DataTableRow> findRows(
        long dataTableId, DataTableRowConditions conditions, List<DataTableRowSort> sorts, int limit,
        int offset, long environmentId) {
        return findRows(dataTableId, conditions, sorts, limit, offset, List.of(), environmentId);
    }

    @Override
    public List<DataTableRow> findRows(
        long dataTableId, DataTableRowConditions conditions, List<DataTableRowSort> sorts, int limit,
        int offset, List<String> expand, long environmentId) {
        return dataTableProtoMapper.toDataTableRows(
            call(
                () -> getStub().findRows(
                    FindRowsRequest.newBuilder()
                        .setDataTableId(dataTableId)
                        .setConditionGroups(dataTableProtoMapper.writeConditions(conditions))
                        .setSorts(dataTableProtoMapper.write(sorts))
                        .setLimit(limit)
                        .setOffset(offset)
                        .addAllExpand(expand)
                        .setEnvironmentId(environmentId)
                        .build()))
                            .getRows());
    }

    @Override
    public DataTableRow insertRow(long dataTableId, Map<String, Object> values, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableRow> insertRows(long dataTableId, List<Map<String, Object>> rowValues, long environmentId) {
        return dataTableProtoMapper.toDataTableRows(
            call(
                () -> getStub().insertRows(
                    InsertRowsRequest.newBuilder()
                        .setDataTableId(dataTableId)
                        .setRowValues(dataTableProtoMapper.write(rowValues))
                        .setEnvironmentId(environmentId)
                        .build()))
                            .getRows());
    }

    @Override
    public DataTableRow updateRow(long dataTableId, long id, Map<String, Object> values, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableRow> updateRows(
        long dataTableId, DataTableRowConditions conditions, Map<String, Object> values, long environmentId) {
        return dataTableProtoMapper.toDataTableRows(
            call(
                () -> getStub().updateRows(
                    UpdateRowsRequest.newBuilder()
                        .setDataTableId(dataTableId)
                        .setConditionGroups(dataTableProtoMapper.writeConditions(conditions))
                        .setValues(dataTableProtoMapper.write(values))
                        .setEnvironmentId(environmentId)
                        .build()))
                            .getRows());
    }

    @Override
    public boolean deleteRow(long dataTableId, long id, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableRow> deleteAllRows(long dataTableId, long environmentId) {
        return dataTableProtoMapper.toDataTableRows(
            call(
                () -> getStub().deleteAllRows(
                    TableRequest.newBuilder()
                        .setDataTableId(dataTableId)
                        .setEnvironmentId(environmentId)
                        .build()))
                            .getRows());
    }

    @Override
    public List<DataTableRow> deleteRows(
        long dataTableId, DataTableRowConditions conditions, long environmentId) {
        return dataTableProtoMapper.toDataTableRows(
            call(
                () -> getStub().deleteRows(
                    DeleteRowsRequest.newBuilder()
                        .setDataTableId(dataTableId)
                        .setConditionGroups(dataTableProtoMapper.writeConditions(conditions))
                        .setEnvironmentId(environmentId)
                        .build()))
                            .getRows());
    }

    @Override
    public @Nullable DataTableRow getRow(long dataTableId, long id, long environmentId) {
        return getRow(dataTableId, id, List.of(), environmentId);
    }

    @Override
    public @Nullable DataTableRow getRow(long dataTableId, long id, List<String> expand, long environmentId) {
        DataTableRowResponse dataTableRowResponse = call(
            () -> getStub().getRow(
                RowIdRequest.newBuilder()
                    .setDataTableId(dataTableId)
                    .setId(id)
                    .addAllExpand(expand)
                    .setEnvironmentId(environmentId)
                    .build()));

        if (!dataTableRowResponse.hasRow()) {
            return null;
        }

        return dataTableProtoMapper.toDataTableRow(dataTableRowResponse.getRow());
    }

    @Override
    public String exportCsv(long dataTableId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int importCsv(long dataTableId, String csv, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long countRows(long dataTableId, DataTableRowConditions conditions, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DataTableRow> fetchRowByExternalId(long dataTableId, String externalId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow insertRow(
        long dataTableId, Map<String, Object> values, @Nullable String externalId, long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableRow> insertRows(
        long dataTableId, List<NewRow> newRows, CreateStrategy createStrategy, long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow updateRow(
        long dataTableId, long id, Map<String, Object> values, @Nullable ExternalIdPatch externalIdPatch,
        long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public UpsertResult upsertRow(long dataTableId, String externalId, Map<String, Object> values, long environmentId) {
        throw new UnsupportedOperationException();
    }
}
