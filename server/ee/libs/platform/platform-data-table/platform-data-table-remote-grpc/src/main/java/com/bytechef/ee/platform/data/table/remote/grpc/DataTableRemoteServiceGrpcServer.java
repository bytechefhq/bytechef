/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.table.remote.grpc;

import com.bytechef.ee.platform.data.table.remote.grpc.proto.AddWebhookRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.AddWebhookResponse;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.DataTableInfosResponse;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.DataTableRemoteServiceGrpc;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.DataTableRowResponse;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.DataTableRowsResponse;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.DeleteRowsRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.EnvironmentRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.FindRowsRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.InsertRowsRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.ListRowsRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.RemoveWebhookRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.RemoveWebhookResponse;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.RowIdRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.TableRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.UpdateRowRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.UpdateRowsRequest;
import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class DataTableRemoteServiceGrpcServer extends DataTableRemoteServiceGrpc.DataTableRemoteServiceImplBase {

    private final DataTableProtoMapper dataTableProtoMapper;
    private final DataTableRowService dataTableRowService;
    private final DataTableService dataTableService;
    private final DataTableWebhookService dataTableWebhookService;

    @SuppressFBWarnings("EI")
    public DataTableRemoteServiceGrpcServer(
        DataTableService dataTableService, DataTableRowService dataTableRowService,
        DataTableWebhookService dataTableWebhookService, DataTableProtoMapper dataTableProtoMapper) {

        this.dataTableProtoMapper = dataTableProtoMapper;
        this.dataTableRowService = dataTableRowService;
        this.dataTableService = dataTableService;
        this.dataTableWebhookService = dataTableWebhookService;
    }

    @Override
    public void listTables(EnvironmentRequest request, StreamObserver<DataTableInfosResponse> responseObserver) {
        responseObserver.onNext(
            DataTableInfosResponse.newBuilder()
                .setDataTableInfos(
                    dataTableProtoMapper.write(dataTableService.listTables(request.getEnvironmentId())))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void listRows(ListRowsRequest request, StreamObserver<DataTableRowsResponse> responseObserver) {
        reply(
            dataTableRowService.listRows(
                request.getBaseName(), request.getLimit(), request.getOffset(), request.getEnvironmentId()),
            responseObserver);
    }

    @Override
    public void findRows(FindRowsRequest request, StreamObserver<DataTableRowsResponse> responseObserver) {
        DataTableRowSort dataTableRowSort = request.hasSort()
            ? dataTableProtoMapper.toDataTableRowSort(request.getSort())
            : null;

        reply(
            dataTableRowService.findRows(
                request.getBaseName(), dataTableProtoMapper.toDataTableRowConditions(request.getConditionGroups()),
                dataTableRowSort, request.getLimit(), request.getOffset(), request.getEnvironmentId()),
            responseObserver);
    }

    @Override
    public void getRow(RowIdRequest request, StreamObserver<DataTableRowResponse> responseObserver) {
        DataTableRow dataTableRow = dataTableRowService.getRow(
            request.getBaseName(), request.getId(), request.getEnvironmentId());

        reply(dataTableRow, responseObserver);
    }

    @Override
    public void insertRows(InsertRowsRequest request, StreamObserver<DataTableRowsResponse> responseObserver) {
        reply(
            dataTableRowService.insertRows(
                request.getBaseName(), dataTableProtoMapper.toRowValues(request.getRowValues()),
                request.getEnvironmentId()),
            responseObserver);
    }

    @Override
    public void updateRow(UpdateRowRequest request, StreamObserver<DataTableRowResponse> responseObserver) {
        reply(
            dataTableRowService.updateRow(
                request.getBaseName(), request.getId(), dataTableProtoMapper.toValues(request.getValues()),
                request.getEnvironmentId()),
            responseObserver);
    }

    @Override
    public void updateRows(UpdateRowsRequest request, StreamObserver<DataTableRowsResponse> responseObserver) {
        reply(
            dataTableRowService.updateRows(
                request.getBaseName(), dataTableProtoMapper.toDataTableRowConditions(request.getConditionGroups()),
                dataTableProtoMapper.toValues(request.getValues()), request.getEnvironmentId()),
            responseObserver);
    }

    @Override
    public void deleteRows(DeleteRowsRequest request, StreamObserver<DataTableRowsResponse> responseObserver) {
        reply(
            dataTableRowService.deleteRows(
                request.getBaseName(), dataTableProtoMapper.toDataTableRowConditions(request.getConditionGroups()),
                request.getEnvironmentId()),
            responseObserver);
    }

    @Override
    public void deleteAllRows(TableRequest request, StreamObserver<DataTableRowsResponse> responseObserver) {
        reply(dataTableRowService.deleteAllRows(request.getBaseName(), request.getEnvironmentId()), responseObserver);
    }

    @Override
    public void addWebhook(AddWebhookRequest request, StreamObserver<AddWebhookResponse> responseObserver) {
        long id = dataTableWebhookService.addWebhook(
            request.getBaseName(), request.getWorkflowExecutionId(), request.getUrl(),
            DataTableWebhookType.valueOf(request.getType()), request.getEnvironmentId());

        responseObserver.onNext(
            AddWebhookResponse.newBuilder()
                .setId(id)
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void removeWebhook(RemoveWebhookRequest request, StreamObserver<RemoveWebhookResponse> responseObserver) {
        dataTableWebhookService.removeWebhook(request.getId());

        responseObserver.onNext(RemoveWebhookResponse.getDefaultInstance());
        responseObserver.onCompleted();
    }

    private void reply(List<DataTableRow> dataTableRows, StreamObserver<DataTableRowsResponse> responseObserver) {
        responseObserver.onNext(
            DataTableRowsResponse.newBuilder()
                .setRows(dataTableProtoMapper.write(dataTableRows))
                .build());
        responseObserver.onCompleted();
    }

    private void reply(@Nullable DataTableRow dataTableRow, StreamObserver<DataTableRowResponse> responseObserver) {
        DataTableRowResponse.Builder builder = DataTableRowResponse.newBuilder();

        if (dataTableRow != null) {
            builder.setRow(dataTableProtoMapper.write(dataTableRow));
        }

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
