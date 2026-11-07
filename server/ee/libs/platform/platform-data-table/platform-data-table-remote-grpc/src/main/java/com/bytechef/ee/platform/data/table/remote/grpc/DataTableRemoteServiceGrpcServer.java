/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.table.remote.grpc;

import com.bytechef.ee.platform.data.table.remote.grpc.proto.AddWebhookRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.AddWebhookResponse;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.DataTableInfoResponse;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.DataTableInfosResponse;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.DataTableRemoteServiceGrpc;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.DataTableResponse;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.DataTableRowResponse;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.DataTableRowsResponse;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.DeleteRowsRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.FetchDataTableRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.FindRowsRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.InsertRowsRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.ListRowsRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.ListTablesRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.RemoveWebhookRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.RemoveWebhookResponse;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.ResolveWorkspaceByJobPrincipalIdRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.ResolveWorkspaceByWorkflowIdRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.ResolveWorkspaceByWorkflowUuidRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.RowIdRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.TableRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.UpdateRowRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.UpdateRowsRequest;
import com.bytechef.ee.platform.data.table.remote.grpc.proto.WorkspaceIdResponse;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.platform.data.table.domain.DataTableWorkspaceResolver;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.facade.DataTableRowFacade;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import org.jspecify.annotations.Nullable;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class DataTableRemoteServiceGrpcServer extends DataTableRemoteServiceGrpc.DataTableRemoteServiceImplBase {

    private final DataTableProtoMapper dataTableProtoMapper;
    private final DataTableRowFacade dataTableRowFacade;
    private final DataTableRowService dataTableRowService;
    private final DataTableService dataTableService;
    private final DataTableWebhookService dataTableWebhookService;
    private final DataTableWorkspaceResolver dataTableWorkspaceResolver;

    @SuppressFBWarnings("EI")
    public DataTableRemoteServiceGrpcServer(
        DataTableService dataTableService, DataTableRowFacade dataTableRowFacade,
        DataTableRowService dataTableRowService, DataTableWebhookService dataTableWebhookService,
        DataTableWorkspaceResolver dataTableWorkspaceResolver, DataTableProtoMapper dataTableProtoMapper) {

        this.dataTableProtoMapper = dataTableProtoMapper;
        this.dataTableRowFacade = dataTableRowFacade;
        this.dataTableRowService = dataTableRowService;
        this.dataTableService = dataTableService;
        this.dataTableWebhookService = dataTableWebhookService;
        this.dataTableWorkspaceResolver = dataTableWorkspaceResolver;
    }

    @Override
    public void fetchDataTable(FetchDataTableRequest request, StreamObserver<DataTableResponse> responseObserver) {
        Long workspaceId = request.hasWorkspaceId() ? request.getWorkspaceId() : null;

        Optional<DataTable> dataTableOptional = dataTableService.fetchDataTable(workspaceId, request.getName());

        DataTableResponse.Builder builder = DataTableResponse.newBuilder();

        dataTableOptional.ifPresent(dataTable -> builder.setDataTable(dataTableProtoMapper.write(dataTable)));

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void fetchDataTableInfo(TableRequest request, StreamObserver<DataTableInfoResponse> responseObserver) {
        Optional<DataTableInfo> dataTableInfoOptional = dataTableService.fetchDataTableInfo(
            request.getDataTableId(), request.getEnvironmentId());

        DataTableInfoResponse.Builder builder = DataTableInfoResponse.newBuilder();

        dataTableInfoOptional.ifPresent(
            dataTableInfo -> builder.setDataTableInfo(dataTableProtoMapper.write(dataTableInfo)));

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void listTables(ListTablesRequest request, StreamObserver<DataTableInfosResponse> responseObserver) {
        Long workspaceId = request.hasWorkspaceId() ? request.getWorkspaceId() : null;

        responseObserver.onNext(
            DataTableInfosResponse.newBuilder()
                .setDataTableInfos(
                    dataTableProtoMapper.write(dataTableService.listTables(workspaceId, request.getEnvironmentId())))
                .build());
        responseObserver.onCompleted();
    }

    @Override
    public void listRows(ListRowsRequest request, StreamObserver<DataTableRowsResponse> responseObserver) {
        reply(
            dataTableRowService.listRows(
                request.getDataTableId(), request.getLimit(), request.getOffset(), request.getEnvironmentId()),
            responseObserver);
    }

    @Override
    public void findRows(FindRowsRequest request, StreamObserver<DataTableRowsResponse> responseObserver) {
        reply(
            dataTableRowService.findRows(
                request.getDataTableId(), dataTableProtoMapper.toDataTableRowConditions(request.getConditionGroups()),
                dataTableProtoMapper.toDataTableRowSorts(request.getSorts()), request.getLimit(), request.getOffset(),
                request.getExpandList(), request.getEnvironmentId()),
            responseObserver);
    }

    @Override
    public void getRow(RowIdRequest request, StreamObserver<DataTableRowResponse> responseObserver) {
        DataTableRow dataTableRow = dataTableRowService.getRow(
            request.getDataTableId(), request.getId(), request.getExpandList(), request.getEnvironmentId());

        reply(dataTableRow, responseObserver);
    }

    @Override
    public void insertRows(InsertRowsRequest request, StreamObserver<DataTableRowsResponse> responseObserver) {
        reply(
            dataTableRowService.insertRows(
                request.getDataTableId(), dataTableProtoMapper.toRowValues(request.getRowValues()),
                request.getEnvironmentId()),
            responseObserver);
    }

    @Override
    public void updateRow(UpdateRowRequest request, StreamObserver<DataTableRowResponse> responseObserver) {
        reply(
            dataTableRowFacade.updateRow(
                request.getDataTableId(), request.getId(), dataTableProtoMapper.toValues(request.getValues()),
                request.getEnvironmentId()),
            responseObserver);
    }

    @Override
    public void updateRows(UpdateRowsRequest request, StreamObserver<DataTableRowsResponse> responseObserver) {
        reply(
            dataTableRowService.updateRows(
                request.getDataTableId(), dataTableProtoMapper.toDataTableRowConditions(request.getConditionGroups()),
                dataTableProtoMapper.toValues(request.getValues()), request.getEnvironmentId()),
            responseObserver);
    }

    @Override
    public void deleteRows(DeleteRowsRequest request, StreamObserver<DataTableRowsResponse> responseObserver) {
        reply(
            dataTableRowService.deleteRows(
                request.getDataTableId(), dataTableProtoMapper.toDataTableRowConditions(request.getConditionGroups()),
                request.getEnvironmentId()),
            responseObserver);
    }

    @Override
    public void deleteAllRows(TableRequest request, StreamObserver<DataTableRowsResponse> responseObserver) {
        reply(
            dataTableRowService.deleteAllRows(request.getDataTableId(), request.getEnvironmentId()),
            responseObserver);
    }

    @Override
    public void addWebhook(AddWebhookRequest request, StreamObserver<AddWebhookResponse> responseObserver) {
        long id = dataTableWebhookService.addWebhook(
            request.getDataTableId(), request.getWorkflowExecutionId(), request.getUrl(),
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

    @Override
    public void resolveWorkspaceByWorkflowId(
        ResolveWorkspaceByWorkflowIdRequest request, StreamObserver<WorkspaceIdResponse> responseObserver) {

        reply(dataTableWorkspaceResolver.resolveByWorkflowId(request.getWorkflowId()), responseObserver);
    }

    @Override
    public void resolveWorkspaceByJobPrincipalId(
        ResolveWorkspaceByJobPrincipalIdRequest request, StreamObserver<WorkspaceIdResponse> responseObserver) {

        reply(
            dataTableWorkspaceResolver.resolveByJobPrincipalId(
                request.getJobPrincipalId(), PlatformType.valueOf(request.getPlatformType())),
            responseObserver);
    }

    @Override
    public void resolveWorkspaceByWorkflowUuid(
        ResolveWorkspaceByWorkflowUuidRequest request, StreamObserver<WorkspaceIdResponse> responseObserver) {

        reply(dataTableWorkspaceResolver.resolveByWorkflowUuid(request.getWorkflowUuid()), responseObserver);
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

    private void reply(OptionalLong workspaceId, StreamObserver<WorkspaceIdResponse> responseObserver) {
        WorkspaceIdResponse.Builder builder = WorkspaceIdResponse.newBuilder();

        workspaceId.ifPresent(builder::setWorkspaceId);

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }
}
