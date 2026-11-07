/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.table.remote.client.service;

import com.bytechef.ee.platform.data.table.remote.grpc.proto.DataTableInfoResponse;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.DataTableResponse;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.FetchDataTableRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.ListTablesRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.TableRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.domain.DataTableLinkColumn;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import java.util.List;
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
public class RemoteDataTableServiceClient extends AbstractRemoteDataTableClient implements DataTableService {

    public RemoteDataTableServiceClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        super(grpcChannels, objectMapper);
    }

    @Override
    public void addColumn(long dataTableId, ColumnSpec columnSpec, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long createTable(
        @Nullable Long workspaceId, String name, @Nullable String description, List<ColumnSpec> columnSpecs,
        long environmentId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public boolean dropTable(long dataTableId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long duplicateTable(long dataTableId, String newName, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DataTable> fetchDataTable(@Nullable Long workspaceId, String name) {
        FetchDataTableRequest.Builder builder = FetchDataTableRequest.newBuilder()
            .setName(name);

        if (workspaceId != null) {
            builder.setWorkspaceId(workspaceId);
        }

        DataTableResponse dataTableResponse = call(() -> getStub().fetchDataTable(builder.build()));

        if (!dataTableResponse.hasDataTable()) {
            return Optional.empty();
        }

        return Optional.of(dataTableProtoMapper.toDataTable(dataTableResponse.getDataTable()));
    }

    @Override
    public Optional<DataTableInfo> fetchDataTableInfo(long dataTableId, long environmentId) {
        DataTableInfoResponse dataTableInfoResponse = call(
            () -> getStub().fetchDataTableInfo(
                TableRequest.newBuilder()
                    .setDataTableId(dataTableId)
                    .setEnvironmentId(environmentId)
                    .build()));

        if (!dataTableInfoResponse.hasDataTableInfo()) {
            return Optional.empty();
        }

        return Optional.of(dataTableProtoMapper.toDataTableInfo(dataTableInfoResponse.getDataTableInfo()));
    }

    @Override
    public DataTable getDataTable(long dataTableId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTable> getWorkspaceDataTables(long workspaceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableLinkColumn> getLinksTo(long targetDataTableId, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableInfo> listAllTables(long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<DataTableInfo> listTables(@Nullable Long workspaceId, long environmentId) {
        ListTablesRequest.Builder builder = ListTablesRequest.newBuilder()
            .setEnvironmentId(environmentId);

        if (workspaceId != null) {
            builder.setWorkspaceId(workspaceId);
        }

        return dataTableProtoMapper.toDataTableInfos(
            call(() -> getStub().listTables(builder.build()))
                .getDataTableInfos());
    }

    @Override
    public void removeColumn(long dataTableId, String columnName, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void renameColumn(long dataTableId, String fromColumnName, String toColumnName, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void renameTable(long dataTableId, String newName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateDescription(long dataTableId, @Nullable String description) {
        throw new UnsupportedOperationException();
    }
}
