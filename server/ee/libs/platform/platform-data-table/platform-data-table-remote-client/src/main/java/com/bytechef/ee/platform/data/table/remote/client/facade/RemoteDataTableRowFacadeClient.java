/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.table.remote.client.facade;

import com.bytechef.ee.platform.data.table.remote.client.service.AbstractRemoteDataTableClient;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.UpdateRowRequest;
import com.bytechef.ee.remote.grpc.GrpcChannels;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.data.table.execution.domain.CreateStrategy;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.ExternalIdPatch;
import com.bytechef.platform.data.table.execution.domain.NewRow;
import com.bytechef.platform.data.table.execution.domain.UpsertResult;
import com.bytechef.platform.data.table.execution.facade.DataTableRowFacade;
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
public class RemoteDataTableRowFacadeClient extends AbstractRemoteDataTableClient implements DataTableRowFacade {

    public RemoteDataTableRowFacadeClient(GrpcChannels grpcChannels, ObjectMapper objectMapper) {
        super(grpcChannels, objectMapper);
    }

    @Override
    public int importCsv(long dataTableId, String csv, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow insertRow(long dataTableId, Map<String, Object> values, long environmentId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DataTableRow updateRow(long dataTableId, long id, Map<String, Object> values, long environmentId) {
        return dataTableProtoMapper.toDataTableRow(
            call(
                () -> getStub().updateRow(
                    UpdateRowRequest.newBuilder()
                        .setDataTableId(dataTableId)
                        .setId(id)
                        .setValues(dataTableProtoMapper.write(values))
                        .setEnvironmentId(environmentId)
                        .build()))
                            .getRow());
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
