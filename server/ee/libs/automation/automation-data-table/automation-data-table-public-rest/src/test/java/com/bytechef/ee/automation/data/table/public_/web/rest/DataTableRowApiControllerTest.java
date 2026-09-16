/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.public_.web.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.BatchRowModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.BatchRowsRequestModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.BatchRowsResponseModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.ClearRowsResponseModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.CreateRowRequestModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.CreateStrategyModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.DataTableRowModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.DeleteRowsResponseModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.ImportRowsResponseModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.UpdateRowRequestModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.UpsertRowRequestModel;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.RowFilter;
import com.bytechef.platform.data.table.execution.domain.CreateStrategy;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.ExternalIdPatch;
import com.bytechef.platform.data.table.execution.domain.UpsertResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class DataTableRowApiControllerTest {

    private static final long PRODUCTION_ENVIRONMENT_ID = Environment.PRODUCTION.ordinal();

    private final WorkspaceDataTableFacade facade = mock(WorkspaceDataTableFacade.class);
    private final DataTableService dataTableService = mock(DataTableService.class);
    private final EnvironmentService environmentService = mock(EnvironmentService.class);
    private DataTableRowApiController controller;

    @BeforeEach
    void beforeEach() {
        when(environmentService.getEnvironment((String) null)).thenReturn(Environment.PRODUCTION);

        controller = new DataTableRowApiController(
            facade, new DataTableApiSupport(dataTableService, environmentService));
    }

    private void stubTable() {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(facade.getTable(7L, Environment.PRODUCTION.ordinal())).thenReturn(
            new DataTableInfo(7L, "orders", null, List.of(new ColumnSpec("total", ColumnType.NUMBER)), Instant.EPOCH));
    }

    @Test
    void testListParsesFiltersAgainstTheTableColumnsAndCapsPageSize() {
        stubTable();

        when(facade.listRows(eq(7L), anyList(), anyList(), eq(0), eq(500), eq(PRODUCTION_ENVIRONMENT_ID)))
            .thenReturn(new PageImpl<>(List.of(new DataTableRow(1L, "k", Map.of("total", new BigDecimal("9.5"))))));

        ResponseEntity<Page> response = controller.listRows(
            "orders", null, List.of("total:GTE:5", "externalId:EQ:k"), List.of("total:DESC"), 0, 9_999);

        ArgumentCaptor<List<RowFilter>> filters = ArgumentCaptor.forClass(List.class);

        verify(facade).listRows(eq(7L), filters.capture(), anyList(), eq(0), eq(500), eq(PRODUCTION_ENVIRONMENT_ID));

        Page body = Objects.requireNonNull(response.getBody());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("external_id", filters.getValue()
            .get(1)
            .field());
        assertEquals("k", ((DataTableRowModel) body.getContent()
            .getFirst()).getExternalId());
    }

    @Test
    void testListFilterValueContainingColonsIsPreservedWhole() {
        stubTable();

        when(facade.listRows(eq(7L), anyList(), anyList(), eq(0), eq(50), eq(PRODUCTION_ENVIRONMENT_ID)))
            .thenReturn(new PageImpl<>(List.of()));

        controller.listRows("orders", null, List.of("total:EQ:10:30"), null, null, null);

        ArgumentCaptor<List<RowFilter>> filters = ArgumentCaptor.forClass(List.class);

        verify(facade).listRows(eq(7L), filters.capture(), anyList(), eq(0), eq(50), eq(PRODUCTION_ENVIRONMENT_ID));

        assertEquals("10:30", filters.getValue()
            .getFirst()
            .value());
    }

    @Test
    void testListPagingReportsTotalElementsDifferentFromContentSize() {
        stubTable();

        // A full page (content size == pageSize) so PageImpl's own last-page correction leaves the given total alone.
        when(facade.listRows(eq(7L), anyList(), anyList(), eq(0), eq(1), eq(PRODUCTION_ENVIRONMENT_ID)))
            .thenReturn(
                new PageImpl<>(
                    List.of(new DataTableRow(1L, null, Map.of())), PageRequest.of(0, 1), 5));

        ResponseEntity<Page> response = controller.listRows("orders", null, null, null, 0, 1);
        Page body = Objects.requireNonNull(response.getBody());

        assertEquals(1, body.getContent()
            .size());
        assertEquals(5L, body.getTotalElements());
        assertNotEquals(
            body.getContent()
                .size(),
            body.getTotalElements());
    }

    @Test
    void testCreateInsertsAndReturns201() {
        stubTable();

        when(facade.insertRow(7L, Map.of("total", "5"), null, PRODUCTION_ENVIRONMENT_ID))
            .thenReturn(new DataTableRow(1L, null, Map.of("total", new BigDecimal("5"))));

        ResponseEntity<DataTableRowModel> response = controller.createRow(
            "orders", new CreateRowRequestModel().values(Map.of("total", "5")), null);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(facade).insertRow(7L, Map.of("total", "5"), null, PRODUCTION_ENVIRONMENT_ID);
    }

    @Test
    void testCreateWithAnExternalIdValidatesAndPassesIt() {
        stubTable();

        when(facade.insertRow(7L, Map.of("total", "5"), "k", PRODUCTION_ENVIRONMENT_ID))
            .thenReturn(new DataTableRow(1L, "k", Map.of("total", new BigDecimal("5"))));

        controller.createRow("orders", new CreateRowRequestModel().values(Map.of("total", "5"))
            .externalId("k"), null);

        verify(facade).insertRow(7L, Map.of("total", "5"), "k", PRODUCTION_ENVIRONMENT_ID);
    }

    @Test
    void testCreateValidatesValuesBeforeTheFacade() {
        stubTable();

        assertThrows(DataTableException.class, () -> controller.createRow(
            "orders", new CreateRowRequestModel().values(Map.of("total", "lots")), null));

        verify(facade, never()).insertRow(anyLong(), anyMap(), any(), anyLong());
    }

    @Test
    void testGetRowReturns200() {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(facade.getRow(7L, 3L, PRODUCTION_ENVIRONMENT_ID))
            .thenReturn(new DataTableRow(3L, null, Map.of("total", BigDecimal.ONE)));

        ResponseEntity<DataTableRowModel> response = controller.getRow("orders", 3L, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
            3L, Objects.requireNonNull(response.getBody())
                .getId());
    }

    @Test
    void testUpdateDistinguishesAbsentNullAndValueForExternalId() {
        stubTable();

        when(facade.updateRow(eq(7L), eq(3L), anyMap(), any(), eq(PRODUCTION_ENVIRONMENT_ID)))
            .thenReturn(new DataTableRow(3L, null, Map.of()));

        ResponseEntity<DataTableRowModel> absentResponse = controller.updateRow(
            "orders", 3L, new UpdateRowRequestModel().values(Map.of()), null);
        ResponseEntity<DataTableRowModel> nullResponse = controller.updateRow(
            "orders", 3L, new UpdateRowRequestModel().values(Map.of())
                .externalId(null),
            null);
        ResponseEntity<DataTableRowModel> valueResponse = controller.updateRow(
            "orders", 3L, new UpdateRowRequestModel().values(Map.of())
                .externalId("k"),
            null);

        assertEquals(HttpStatus.OK, absentResponse.getStatusCode());
        assertEquals(HttpStatus.OK, nullResponse.getStatusCode());
        assertEquals(HttpStatus.OK, valueResponse.getStatusCode());

        InOrder inOrder = inOrder(facade);

        inOrder.verify(facade)
            .updateRow(eq(7L), eq(3L), anyMap(), isNull(), eq(PRODUCTION_ENVIRONMENT_ID));
        inOrder.verify(facade)
            .updateRow(eq(7L), eq(3L), anyMap(), eq(new ExternalIdPatch(null)), eq(PRODUCTION_ENVIRONMENT_ID));
        inOrder.verify(facade)
            .updateRow(eq(7L), eq(3L), anyMap(), eq(new ExternalIdPatch("k")), eq(PRODUCTION_ENVIRONMENT_ID));
    }

    @Test
    void testUpdateValidatesValuesBeforeTheFacade() {
        stubTable();

        assertThrows(DataTableException.class, () -> controller.updateRow(
            "orders", 3L, new UpdateRowRequestModel().values(Map.of("total", "lots")), null));

        verify(facade, never()).updateRow(anyLong(), anyLong(), anyMap(), any(), anyLong());
    }

    @Test
    void testDeleteRowReturns204() {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(facade.deleteRow(7L, 3L, PRODUCTION_ENVIRONMENT_ID)).thenReturn(true);

        assertEquals(
            HttpStatus.NO_CONTENT, controller.deleteRow("orders", 3L, null)
                .getStatusCode());
    }

    @Test
    void testDeleteRowFalseIsRowNotFound() {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(facade.deleteRow(7L, 3L, PRODUCTION_ENVIRONMENT_ID)).thenReturn(false);

        DataTableException dataTableException = assertThrows(
            DataTableException.class, () -> controller.deleteRow("orders", 3L, null));

        assertEquals(DataTableErrorType.ROW_NOT_FOUND.getErrorKey(), dataTableException.getErrorKey());
    }

    @Test
    void testGetRowByExternalIdReturns200() {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(facade.fetchRowByExternalId(7L, "k", PRODUCTION_ENVIRONMENT_ID))
            .thenReturn(Optional.of(new DataTableRow(1L, "k", Map.of())));

        ResponseEntity<DataTableRowModel> response = controller.getRowByExternalId("orders", "k", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
            "k", Objects.requireNonNull(response.getBody())
                .getExternalId());
    }

    @Test
    void testGetRowByExternalIdEmptyIsRowNotFound() {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(facade.fetchRowByExternalId(7L, "missing", PRODUCTION_ENVIRONMENT_ID)).thenReturn(Optional.empty());

        DataTableException dataTableException = assertThrows(
            DataTableException.class, () -> controller.getRowByExternalId("orders", "missing", null));

        assertEquals(DataTableErrorType.ROW_NOT_FOUND.getErrorKey(), dataTableException.getErrorKey());
    }

    @Test
    void testUpsertAnswers201OnCreateAnd200OnMerge() {
        stubTable();

        when(facade.upsertRow(7L, "k", Map.of(), PRODUCTION_ENVIRONMENT_ID))
            .thenReturn(new UpsertResult(new DataTableRow(1L, "k", Map.of()), true))
            .thenReturn(new UpsertResult(new DataTableRow(1L, "k", Map.of()), false));

        UpsertRowRequestModel request = new UpsertRowRequestModel().values(Map.of());

        assertEquals(
            HttpStatus.CREATED, controller.upsertRowByExternalId("orders", "k", request, null)
                .getStatusCode());
        assertEquals(
            HttpStatus.OK, controller.upsertRowByExternalId("orders", "k", request, null)
                .getStatusCode());
    }

    @Test
    void testUpsertValidatesTheExternalIdBeforeTheFacade() {
        stubTable();

        assertThrows(DataTableException.class, () -> controller.upsertRowByExternalId(
            "orders", "", new UpsertRowRequestModel().values(Map.of()), null));

        verify(facade, never()).upsertRow(anyLong(), any(), anyMap(), anyLong());
    }

    @Test
    void testDeleteRowByExternalIdReturns204() {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(facade.deleteRowByExternalId(7L, "k", PRODUCTION_ENVIRONMENT_ID)).thenReturn(true);

        assertEquals(
            HttpStatus.NO_CONTENT, controller.deleteRowByExternalId("orders", "k", null)
                .getStatusCode());
    }

    @Test
    void testDeleteRowByExternalIdFalseIsRowNotFound() {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(facade.deleteRowByExternalId(7L, "missing", PRODUCTION_ENVIRONMENT_ID)).thenReturn(false);

        DataTableException dataTableException = assertThrows(
            DataTableException.class, () -> controller.deleteRowByExternalId("orders", "missing", null));

        assertEquals(DataTableErrorType.ROW_NOT_FOUND.getErrorKey(), dataTableException.getErrorKey());
    }

    @Test
    void testBatchRowsInsertsWithDefaultStrategyAndReturns200() {
        stubTable();

        when(facade.insertRows(eq(7L), anyList(), eq(CreateStrategy.INSERT), eq(PRODUCTION_ENVIRONMENT_ID)))
            .thenReturn(List.of(new DataTableRow(1L, null, Map.of("total", BigDecimal.ONE))));

        ResponseEntity<BatchRowsResponseModel> response = controller.batchRows(
            "orders", new BatchRowsRequestModel().rows(
                List.of(new BatchRowModel().values(Map.of("total", "1")))),
            null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
            1, Objects.requireNonNull(response.getBody())
                .getRows()
                .size());
    }

    @Test
    void testBatchRowsUpsertStrategyReachesTheFacade() {
        stubTable();

        when(facade.insertRows(eq(7L), anyList(), eq(CreateStrategy.UPSERT), eq(PRODUCTION_ENVIRONMENT_ID)))
            .thenReturn(List.of());

        controller.batchRows(
            "orders", new BatchRowsRequestModel()
                .rows(List.of(new BatchRowModel().values(Map.of("total", "1"))
                    .externalId("k")))
                .createStrategy(CreateStrategyModel.UPSERT),
            null);

        verify(facade).insertRows(eq(7L), anyList(), eq(CreateStrategy.UPSERT), eq(PRODUCTION_ENVIRONMENT_ID));
    }

    @Test
    void testBatchRejectsMoreThanAThousandRows() {
        stubTable();

        List<BatchRowModel> rows = Collections.nCopies(1001, new BatchRowModel().values(Map.of()));

        DataTableException dataTableException = assertThrows(
            DataTableException.class,
            () -> controller.batchRows("orders", new BatchRowsRequestModel().rows(rows), null));

        assertEquals(DataTableErrorType.BATCH_TOO_LARGE.getErrorKey(), dataTableException.getErrorKey());
        verify(facade, never()).insertRows(anyLong(), anyList(), any(), anyLong());
    }

    @Test
    void testBatchValidatesEachRowBeforeTheFacade() {
        stubTable();

        assertThrows(DataTableException.class, () -> controller.batchRows(
            "orders", new BatchRowsRequestModel().rows(
                List.of(new BatchRowModel().values(Map.of("total", "lots")))),
            null));

        verify(facade, never()).insertRows(anyLong(), anyList(), any(), anyLong());
    }

    @Test
    void testDeleteRowsRejectsEmptyIds() {
        stubTable();

        DataTableException dataTableException = assertThrows(
            DataTableException.class, () -> controller.deleteRows("orders", List.of(), null));

        assertEquals(DataTableErrorType.ROW_VALUE_INVALID.getErrorKey(), dataTableException.getErrorKey());
        verifyNoInteractions(facade);
    }

    @Test
    void testDeleteRowsRejectsMoreThanAThousandIds() {
        stubTable();

        List<Long> ids = LongStream.rangeClosed(1, 1001)
            .boxed()
            .toList();

        DataTableException dataTableException = assertThrows(
            DataTableException.class, () -> controller.deleteRows("orders", ids, null));

        assertEquals(DataTableErrorType.BATCH_TOO_LARGE.getErrorKey(), dataTableException.getErrorKey());
        verifyNoInteractions(facade);
    }

    @Test
    void testDeleteRowsReturns200WithTheDeletedIds() {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(facade.deleteRows(7L, List.of(1L, 2L), PRODUCTION_ENVIRONMENT_ID)).thenReturn(List.of(1L, 2L));

        ResponseEntity<DeleteRowsResponseModel> response = controller.deleteRows("orders", List.of(1L, 2L), null);
        DeleteRowsResponseModel body = Objects.requireNonNull(response.getBody());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, body.getDeletedCount());
        assertEquals(List.of(1L, 2L), body.getDeletedIds());
    }

    @Test
    void testClearRowsReturnsTheDeletedCount() {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(facade.clearRows(7L, PRODUCTION_ENVIRONMENT_ID)).thenReturn(42L);

        ResponseEntity<ClearRowsResponseModel> response = controller.clearRows("orders", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
            42L, Objects.requireNonNull(response.getBody())
                .getDeletedCount());
    }

    @Test
    void testImportRowsReturnsTheImportedCount() {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(facade.importCsv(7L, "total\n1\n", PRODUCTION_ENVIRONMENT_ID)).thenReturn(1);

        ResponseEntity<ImportRowsResponseModel> response = controller.importRows("orders", "total\n1\n", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
            1, Objects.requireNonNull(response.getBody())
                .getImportedCount());
    }

    @Test
    void testExportSetsCsvHeaders() {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(facade.exportCsv(7L, PRODUCTION_ENVIRONMENT_ID)).thenReturn("external_id,total\n,1\n");

        ResponseEntity<String> response = controller.exportRows("orders", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
            "text/csv;charset=UTF-8", response.getHeaders()
                .getFirst(HttpHeaders.CONTENT_TYPE));
        assertEquals(
            "attachment; filename=\"orders.csv\"", response.getHeaders()
                .getFirst(HttpHeaders.CONTENT_DISPOSITION));
    }
}
