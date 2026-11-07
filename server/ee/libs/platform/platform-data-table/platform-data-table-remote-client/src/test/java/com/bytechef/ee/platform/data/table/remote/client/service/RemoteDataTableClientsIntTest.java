/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.data.table.remote.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.platform.data.table.remote.client.config.DataTableRemoteClientIntTestConfiguration;
import com.bytechef.ee.platform.data.table.remote.client.facade.RemoteDataTableRowFacadeClient;
import com.bytechef.ee.platform.data.table.remote.grpc.DataTableProtoMapper;
import com.bytechef.ee.platform.data.table.remote.grpc.DataTableRemoteServiceGrpcServer;
import com.bytechef.ee.remote.grpc.FixedGrpcChannels;
import com.bytechef.ee.remote.grpc.GrpcRemoteExceptionHandler;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.domain.DataTableWebhookType;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.DataTableWorkspaceResolver;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter.Operation;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort;
import com.bytechef.platform.data.table.execution.domain.DataTableRowSort.Direction;
import com.bytechef.platform.data.table.execution.facade.DataTableRowFacade;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.platform.workflow.WorkflowExecutionId;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.grpc.server.exception.GrpcExceptionHandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@SpringBootTest(classes = DataTableRemoteClientIntTestConfiguration.class)
class RemoteDataTableClientsIntTest {

    private static final long ENVIRONMENT_ID = 0;
    private static final String ORDERS = "orders";
    private static final String WORKFLOW_EXECUTION_ID = WorkflowExecutionId.of(
        PlatformType.AUTOMATION, 1, "workflow-uuid", "trigger_1")
        .toString();

    @Autowired
    private DataTableRowFacade dataTableRowFacade;

    @Autowired
    private DataTableRowService dataTableRowService;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private DataTableWebhookService dataTableWebhookService;

    @Autowired
    private DataTableWorkspaceResolver dataTableWorkspaceResolver;

    @Autowired
    private ObjectMapper objectMapper;

    private long dataTableId;
    private DataTableProtoMapper dataTableProtoMapper;
    private ManagedChannel managedChannel;
    private RemoteDataTableRowFacadeClient remoteDataTableRowFacadeClient;
    private RemoteDataTableRowServiceClient remoteDataTableRowServiceClient;
    private RemoteDataTableServiceClient remoteDataTableServiceClient;
    private RemoteDataTableWebhookServiceClient remoteDataTableWebhookServiceClient;
    private RemoteDataTableWorkspaceResolverClient remoteDataTableWorkspaceResolverClient;
    private Server server;

    @BeforeEach
    void beforeEach() throws IOException {
        dropOrders();

        dataTableId = dataTableService.createTable(
            null, ORDERS, null,
            List.of(
                new ColumnSpec("name", ColumnType.STRING), new ColumnSpec("amount", ColumnType.NUMBER),
                new ColumnSpec("paid", ColumnType.BOOLEAN), new ColumnSpec("due", ColumnType.DATE),
                new ColumnSpec("at", ColumnType.DATE_TIME)),
            ENVIRONMENT_ID);

        dataTableProtoMapper = new DataTableProtoMapper(objectMapper);

        String name = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(name)
            .directExecutor()
            .addService(
                ServerInterceptors.intercept(
                    new DataTableRemoteServiceGrpcServer(
                        dataTableService, dataTableRowFacade, dataTableRowService, dataTableWebhookService,
                        dataTableWorkspaceResolver, dataTableProtoMapper),
                    new GrpcExceptionHandlerInterceptor(new GrpcRemoteExceptionHandler())))
            .build()
            .start();

        managedChannel = InProcessChannelBuilder.forName(name)
            .directExecutor()
            .build();

        FixedGrpcChannels fixedGrpcChannels = new FixedGrpcChannels(managedChannel);

        remoteDataTableRowFacadeClient = new RemoteDataTableRowFacadeClient(fixedGrpcChannels, objectMapper);
        remoteDataTableRowServiceClient = new RemoteDataTableRowServiceClient(fixedGrpcChannels, objectMapper);
        remoteDataTableServiceClient = new RemoteDataTableServiceClient(fixedGrpcChannels, objectMapper);
        remoteDataTableWebhookServiceClient = new RemoteDataTableWebhookServiceClient(
            fixedGrpcChannels, objectMapper);
        remoteDataTableWorkspaceResolverClient = new RemoteDataTableWorkspaceResolverClient(
            fixedGrpcChannels, objectMapper);
    }

    @AfterEach
    void afterEach() {
        managedChannel.shutdownNow();
        server.shutdownNow();

        dropOrders();
    }

    @Test
    void testListTablesReturnsTheHostsTables() {
        assertThat(remoteDataTableServiceClient.listTables(null, ENVIRONMENT_ID))
            .isNotEmpty()
            .isEqualTo(dataTableService.listTables(null, ENVIRONMENT_ID));
        assertThat(remoteDataTableServiceClient.listTables(Long.MAX_VALUE, ENVIRONMENT_ID))
            .isEqualTo(dataTableService.listTables(Long.MAX_VALUE, ENVIRONMENT_ID));
    }

    @Test
    void testFetchDataTableAndDataTableInfoMatchTheHost() {
        DataTable dataTable = remoteDataTableServiceClient.fetchDataTable(null, ORDERS)
            .orElseThrow();

        assertThat(dataTable.getId()).isEqualTo(dataTableId);
        assertThat(dataTable.getName()).isEqualTo(ORDERS);
        assertThat(remoteDataTableServiceClient.fetchDataTable(null, "missing")).isEmpty();
        assertThat(remoteDataTableServiceClient.fetchDataTableInfo(dataTableId, ENVIRONMENT_ID))
            .isEqualTo(dataTableService.fetchDataTableInfo(dataTableId, ENVIRONMENT_ID))
            .isPresent();
        assertThat(remoteDataTableServiceClient.fetchDataTableInfo(Long.MAX_VALUE, ENVIRONMENT_ID)).isEmpty();
    }

    @Test
    void testWorkspaceResolverAnswersLikeTheHost() {
        assertThat(remoteDataTableWorkspaceResolverClient.resolveByWorkflowId("workflow-1"))
            .isEqualTo(OptionalLong.of(7L));
        assertThat(remoteDataTableWorkspaceResolverClient.resolveByWorkflowId("missing"))
            .isEqualTo(OptionalLong.empty());
        assertThat(remoteDataTableWorkspaceResolverClient.resolveByJobPrincipalId(3L, PlatformType.AUTOMATION))
            .isEqualTo(OptionalLong.of(8L));
        assertThat(remoteDataTableWorkspaceResolverClient.resolveByJobPrincipalId(3L, PlatformType.EMBEDDED))
            .isEqualTo(OptionalLong.empty());
        assertThat(remoteDataTableWorkspaceResolverClient.resolveByWorkflowUuid("workflow-uuid"))
            .isEqualTo(OptionalLong.of(9L));
        assertThat(remoteDataTableWorkspaceResolverClient.resolveByWorkflowUuid("missing"))
            .isEqualTo(OptionalLong.empty());
    }

    @Test
    void testInsertRowsAndGetRowRoundTripTypedValues() {
        List<DataTableRow> dataTableRows = remoteDataTableRowServiceClient.insertRows(
            dataTableId,
            List.of(
                Map.of(
                    "name", "a", "amount", 12.5, "paid", true, "due", "2026-11-07", "at",
                    "2026-11-07T10:15:30Z"),
                Map.of("name", "b", "amount", 3, "paid", false)),
            ENVIRONMENT_ID);

        assertThat(dataTableRows).hasSize(2);

        for (DataTableRow dataTableRow : dataTableRows) {
            DataTableRow localDataTableRow = Objects.requireNonNull(
                dataTableRowService.getRow(dataTableId, dataTableRow.id(), ENVIRONMENT_ID));

            assertThat(remoteDataTableRowServiceClient.getRow(dataTableId, dataTableRow.id(), ENVIRONMENT_ID))
                .isEqualTo(dataTableProtoMapper.toDataTableRow(dataTableProtoMapper.write(localDataTableRow)));
        }

        DataTableRow firstDataTableRow = Objects.requireNonNull(
            remoteDataTableRowServiceClient.getRow(dataTableId, dataTableRows.getFirst()
                .id(), ENVIRONMENT_ID));

        assertThat(firstDataTableRow.values()).containsEntry("name", "a")
            .containsEntry("paid", true)
            .containsKeys("amount", "due", "at");
        assertThat(firstDataTableRow.values()
            .get("due")).isNotNull();
        assertThat(firstDataTableRow.values()
            .get("at")).isNotNull();
        assertThat(remoteDataTableRowServiceClient.getRow(dataTableId, Long.MAX_VALUE, ENVIRONMENT_ID)).isNull();
    }

    @Test
    void testFindRowsWithGroupedConditionsMatchesTheLocalService() {
        insertRows();

        DataTableRowConditions dataTableRowConditions = DataTableRowConditions.anyOf(
            List.of(
                List.of(new DataTableRowFilter("amount", Operation.GREATER, 10)),
                List.of(new DataTableRowFilter("name", Operation.IN, List.of("b", "c")))));
        List<DataTableRowSort> dataTableRowSorts = List.of(new DataTableRowSort("amount", Direction.DESC));

        List<DataTableRow> remoteDataTableRows = remoteDataTableRowServiceClient.findRows(
            dataTableId, dataTableRowConditions, dataTableRowSorts, 10, 0, ENVIRONMENT_ID);

        assertThat(remoteDataTableRows).hasSize(3)
            .isEqualTo(
                roundTrip(
                    dataTableRowService.findRows(
                        dataTableId, dataTableRowConditions, dataTableRowSorts, 10, 0, ENVIRONMENT_ID)));
        assertThat(
            remoteDataTableRows.stream()
                .map(dataTableRow -> dataTableRow.values()
                    .get("name"))
                .toList()).containsExactly("d", "c", "b");
    }

    @Test
    void testFindRowsWithMatchAllReturnsEveryRow() {
        insertRows();

        assertThat(
            remoteDataTableRowServiceClient.findRows(
                dataTableId, DataTableRowConditions.matchAll(), List.of(), 10, 0, ENVIRONMENT_ID)).hasSize(4);
        assertThat(remoteDataTableRowServiceClient.listRows(dataTableId, 2, 1, ENVIRONMENT_ID))
            .isEqualTo(roundTrip(dataTableRowService.listRows(dataTableId, 2, 1, ENVIRONMENT_ID)));
    }

    @Test
    void testUpdateRowsAndDeleteRowsAffectTheSameRowsAsLocally() {
        List<DataTableRow> dataTableRows = insertRows();

        DataTableRow updatedDataTableRow = remoteDataTableRowFacadeClient.updateRow(
            dataTableId, dataTableRows.getFirst()
                .id(),
            Map.of("paid", true), ENVIRONMENT_ID);

        assertThat(updatedDataTableRow.values()).containsEntry("paid", true);

        List<DataTableRow> updatedDataTableRows = remoteDataTableRowServiceClient.updateRows(
            dataTableId, DataTableRowConditions.of(List.of(new DataTableRowFilter("amount", Operation.LESS, 10))),
            Map.of("name", "small"), ENVIRONMENT_ID);

        assertThat(updatedDataTableRows).hasSize(2);

        List<DataTableRow> deletedDataTableRows = remoteDataTableRowServiceClient.deleteRows(
            dataTableId, DataTableRowConditions.of(List.of(new DataTableRowFilter("name", Operation.EQUALS, "small"))),
            ENVIRONMENT_ID);

        assertThat(deletedDataTableRows).extracting(DataTableRow::id)
            .containsExactlyElementsOf(
                updatedDataTableRows.stream()
                    .map(DataTableRow::id)
                    .toList());
        assertThat(dataTableRowService.listRows(dataTableId, 10, 0, ENVIRONMENT_ID)).hasSize(2);
    }

    @Test
    void testDeleteAllRowsEmptiesTheTable() {
        insertRows();

        assertThat(remoteDataTableRowServiceClient.deleteAllRows(dataTableId, ENVIRONMENT_ID)).hasSize(4);
        assertThat(dataTableRowService.listRows(dataTableId, 10, 0, ENVIRONMENT_ID)).isEmpty();
    }

    @Test
    void testUpdateRowOfAnUnknownColumnIsAnIllegalArgument() {
        assertThatThrownBy(
            () -> remoteDataTableRowServiceClient.findRows(
                dataTableId, DataTableRowConditions.of(List.of(new DataTableRowFilter("missing", Operation.EQUALS, 1))),
                List.of(), 10, 0, ENVIRONMENT_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("missing");
    }

    @Test
    void testAddWebhookThenRemoveWebhook() {
        long webhookId = remoteDataTableWebhookServiceClient.addWebhook(
            dataTableId, WORKFLOW_EXECUTION_ID, "http://localhost:9555/webhooks/" + WORKFLOW_EXECUTION_ID,
            DataTableWebhookType.RECORD_CREATED, ENVIRONMENT_ID);

        assertThat(dataTableWebhookService.listWebhooks(dataTableId, ENVIRONMENT_ID))
            .extracting(DataTableWebhookService.Webhook::id)
            .containsExactly(webhookId);

        remoteDataTableWebhookServiceClient.removeWebhook(webhookId);

        assertThat(dataTableWebhookService.listWebhooks(dataTableId, ENVIRONMENT_ID)).isEmpty();
    }

    private void dropOrders() {
        dataTableService.fetchDataTable(null, ORDERS)
            .ifPresent(
                dataTable -> dataTableService.dropTable(Objects.requireNonNull(dataTable.getId()), ENVIRONMENT_ID));
    }

    private List<DataTableRow> insertRows() {
        return dataTableRowService.insertRows(
            dataTableId,
            List.of(
                Map.of("name", "a", "amount", 5), Map.of("name", "b", "amount", 7),
                Map.of("name", "c", "amount", 11), Map.of("name", "d", "amount", 20)),
            ENVIRONMENT_ID);
    }

    private List<DataTableRow> roundTrip(List<DataTableRow> dataTableRows) {
        return dataTableProtoMapper.toDataTableRows(dataTableProtoMapper.write(dataTableRows));
    }
}
