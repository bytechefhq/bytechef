/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.public_.web.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.ee.automation.data.table.public_.web.rest.config.AutomationDataTablePublicRestTestConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.exception.DataTableStorageLimitExceededException;
import com.bytechef.platform.data.table.execution.domain.CreateStrategy;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter;
import com.bytechef.platform.data.table.execution.domain.DataTableRowFilter.Operation;
import com.bytechef.platform.data.table.execution.domain.ExternalIdPatch;
import com.bytechef.platform.data.table.execution.domain.NewRow;
import com.bytechef.platform.data.table.execution.domain.UpsertResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = AutomationDataTablePublicRestTestConfiguration.class)
@Import({
    DataTableApiSupport.class, DataTableRowApiController.class
})
@TestPropertySource(properties = {
    "bytechef.edition=ee", "openapi.openAPIDefinition.base-path.automation=/api/automation"
})
@WebMvcTest(DataTableRowApiController.class)
class DataTableRowApiControllerIntTest {

    private static final String BASE_PATH = "/api/automation/v1";
    private static final String BATCH_ROWS_PATH = BASE_PATH + "/data-tables/{name}/rows/batch";
    private static final String CLEAR_ROWS_PATH = BASE_PATH + "/data-tables/{name}/rows/clear";
    private static final String EXPORT_ROWS_PATH = BASE_PATH + "/data-tables/{name}/rows/export";
    private static final String IMPORT_ROWS_PATH = BASE_PATH + "/data-tables/{name}/rows/import";
    private static final long PRODUCTION_ENVIRONMENT_ID = Environment.PRODUCTION.ordinal();
    private static final String ROW_BY_EXTERNAL_ID_PATH =
        BASE_PATH + "/data-tables/{name}/rows/by-external-id/{externalId}";
    private static final String ROW_PATH = BASE_PATH + "/data-tables/{name}/rows/{id}";
    private static final String ROWS_PATH = BASE_PATH + "/data-tables/{name}/rows";

    @MockitoBean
    private DataTableService dataTableService;

    @MockitoBean
    private EnvironmentService environmentService;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkspaceDataTableFacade workspaceDataTableFacade;

    @BeforeEach
    void beforeEach() {
        when(environmentService.getEnvironment((String) null)).thenReturn(Environment.PRODUCTION);
    }

    @Test
    void testListRowsParsesFiltersAgainstTheTableColumnsAndCapsPageSize() throws Exception {
        stubOrdersTable();

        when(
            workspaceDataTableFacade.listRows(
                eq(7L), any(DataTableRowConditions.class), anyList(), eq(0), eq(500), eq(PRODUCTION_ENVIRONMENT_ID)))
                    .thenReturn(
                        new PageImpl<>(List.of(new DataTableRow(1L, "k", Map.of("total", new BigDecimal("9.5"))))));

        mockMvc
            .perform(
                get(ROWS_PATH, "orders")
                    .param("filter", "total:GTE:5", "externalId:EQ:k")
                    .param("sort", "total:DESC")
                    .param("pageNumber", "0")
                    .param("pageSize", "9999")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].externalId").value("k"));

        ArgumentCaptor<DataTableRowConditions> conditionsArgumentCaptor =
            ArgumentCaptor.forClass(DataTableRowConditions.class);

        verify(workspaceDataTableFacade).listRows(
            eq(7L), conditionsArgumentCaptor.capture(), anyList(), eq(0), eq(500), eq(PRODUCTION_ENVIRONMENT_ID));

        DataTableRowConditions dataTableRowConditions = conditionsArgumentCaptor.getValue();

        List<DataTableRowFilter> dataTableRowFilters = dataTableRowConditions.groups()
            .getFirst();

        assertEquals(2, dataTableRowFilters.size());
        assertEquals(
            "external_id", dataTableRowFilters.get(1)
                .column());
    }

    @Test
    void testListRowsKeepsACommaSeparatedFilterAsOneTerm() throws Exception {
        stubOrdersTable();

        when(
            workspaceDataTableFacade.listRows(
                eq(7L), any(DataTableRowConditions.class), anyList(), eq(0), eq(50), eq(PRODUCTION_ENVIRONMENT_ID)))
                    .thenReturn(new PageImpl<>(List.of()));

        mockMvc
            .perform(get(ROWS_PATH, "orders").param("filter", "total:IN:1,2"))
            .andExpect(status().isOk());

        ArgumentCaptor<DataTableRowConditions> conditionsArgumentCaptor =
            ArgumentCaptor.forClass(DataTableRowConditions.class);

        verify(workspaceDataTableFacade).listRows(
            eq(7L), conditionsArgumentCaptor.capture(), anyList(), eq(0), eq(50), eq(PRODUCTION_ENVIRONMENT_ID));

        DataTableRowConditions dataTableRowConditions = conditionsArgumentCaptor.getValue();

        List<DataTableRowFilter> dataTableRowFilters = dataTableRowConditions.groups()
            .getFirst();

        assertEquals(1, dataTableRowFilters.size());
        assertEquals(
            Operation.IN, dataTableRowFilters.getFirst()
                .operation());
    }

    @Test
    void testListRowsFilterValueContainingColonsIsPreservedWhole() throws Exception {
        stubOrdersTable();

        when(
            workspaceDataTableFacade.listRows(
                eq(7L), any(DataTableRowConditions.class), anyList(), eq(0), eq(50), eq(PRODUCTION_ENVIRONMENT_ID)))
                    .thenReturn(new PageImpl<>(List.of()));

        mockMvc
            .perform(get(ROWS_PATH, "orders").param("filter", "total:EQ:10:30"))
            .andExpect(status().isOk());

        ArgumentCaptor<DataTableRowConditions> conditionsArgumentCaptor =
            ArgumentCaptor.forClass(DataTableRowConditions.class);

        verify(workspaceDataTableFacade).listRows(
            eq(7L), conditionsArgumentCaptor.capture(), anyList(), eq(0), eq(50), eq(PRODUCTION_ENVIRONMENT_ID));

        DataTableRowConditions dataTableRowConditions = conditionsArgumentCaptor.getValue();

        assertEquals(
            "10:30", dataTableRowConditions.groups()
                .getFirst()
                .getFirst()
                .value());
    }

    @Test
    void testListRowsWithAnInvalidFilterIsBadRequestWithTheFilterInvalidKey() throws Exception {
        stubOrdersTable();

        mockMvc
            .perform(get(ROWS_PATH, "orders").param("filter", "missing:EQ:1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorKey").value(112))
            .andExpect(jsonPath("$.errorKey").value(DataTableErrorType.FILTER_INVALID.getErrorKey()))
            .andExpect(jsonPath("$.entityClass").value("DataTableErrorType"));

        verify(workspaceDataTableFacade, never()).listRows(
            anyLong(), any(DataTableRowConditions.class), anyList(), anyInt(), anyInt(),
            anyLong());
    }

    @Test
    void testListRowsPagingReportsTotalElementsDifferentFromContentSize() throws Exception {
        stubOrdersTable();

        when(
            workspaceDataTableFacade.listRows(
                eq(7L), any(DataTableRowConditions.class), anyList(), eq(0), eq(1), eq(PRODUCTION_ENVIRONMENT_ID)))
                    .thenReturn(
                        new PageImpl<>(List.of(new DataTableRow(1L, null, Map.of())), PageRequest.of(0, 1), 5));

        mockMvc
            .perform(
                get(ROWS_PATH, "orders")
                    .param("pageNumber", "0")
                    .param("pageSize", "1")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    void testCreateRowInsertsAndReturnsCreated() throws Exception {
        stubOrdersTable();

        when(workspaceDataTableFacade.insertRow(7L, Map.of("total", "5"), null, PRODUCTION_ENVIRONMENT_ID))
            .thenReturn(new DataTableRow(1L, null, Map.of("total", new BigDecimal("5"))));

        mockMvc
            .perform(
                post(ROWS_PATH, "orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"values": {"total": "5"}}
                            """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.values.total").value(5));

        verify(workspaceDataTableFacade).insertRow(7L, Map.of("total", "5"), null, PRODUCTION_ENVIRONMENT_ID);
    }

    @Test
    void testCreateRowWithAnExternalIdValidatesAndPassesIt() throws Exception {
        stubOrdersTable();

        when(workspaceDataTableFacade.insertRow(7L, Map.of("total", "5"), "k", PRODUCTION_ENVIRONMENT_ID))
            .thenReturn(new DataTableRow(1L, "k", Map.of("total", new BigDecimal("5"))));

        mockMvc
            .perform(
                post(ROWS_PATH, "orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"values": {"total": "5"}, "externalId": "k"}
                            """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.externalId").value("k"));

        verify(workspaceDataTableFacade).insertRow(7L, Map.of("total", "5"), "k", PRODUCTION_ENVIRONMENT_ID);
    }

    @Test
    void testCreateRowValidatesValuesBeforeTheFacade() throws Exception {
        stubOrdersTable();

        mockMvc
            .perform(
                post(ROWS_PATH, "orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"values": {"total": "lots"}}
                            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorKey").value(DataTableErrorType.ROW_VALUE_INVALID.getErrorKey()));

        verify(workspaceDataTableFacade, never()).insertRow(anyLong(), anyMap(), any(), anyLong());
    }

    @Test
    void testCreateRowWithAConflictingExternalIdIsConflict() throws Exception {
        stubOrdersTable();

        when(workspaceDataTableFacade.insertRow(7L, Map.of("total", "5"), "k", PRODUCTION_ENVIRONMENT_ID))
            .thenThrow(new DataTableException("Duplicate externalId", DataTableErrorType.ROW_EXTERNAL_ID_CONFLICT));

        mockMvc
            .perform(
                post(ROWS_PATH, "orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"values": {"total": "5"}, "externalId": "k"}
                            """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorKey").value(DataTableErrorType.ROW_EXTERNAL_ID_CONFLICT.getErrorKey()));
    }

    @Test
    void testCreateRowOverTheStorageLimitIsInsufficientStorage() throws Exception {
        stubOrdersTable();

        when(workspaceDataTableFacade.insertRow(7L, Map.of("total", "5"), null, PRODUCTION_ENVIRONMENT_ID))
            .thenThrow(new DataTableStorageLimitExceededException(10, 5));

        mockMvc
            .perform(
                post(ROWS_PATH, "orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"values": {"total": "5"}}
                            """))
            .andExpect(status().isInsufficientStorage())
            .andExpect(jsonPath("$.errorKey").value(DataTableErrorType.STORAGE_LIMIT_EXCEEDED.getErrorKey()))
            .andExpect(jsonPath("$.entityClass").value("DataTableErrorType"));
    }

    @Test
    void testGetRowReturnsOk() throws Exception {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(workspaceDataTableFacade.getRow(7L, 3L, PRODUCTION_ENVIRONMENT_ID))
            .thenReturn(new DataTableRow(3L, null, Map.of("total", BigDecimal.ONE)));

        mockMvc
            .perform(get(ROW_PATH, "orders", 3L).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    void testUpdateRowDistinguishesAbsentNullAndValueForExternalId() throws Exception {
        stubOrdersTable();

        when(workspaceDataTableFacade.updateRow(eq(7L), eq(3L), anyMap(), any(), eq(PRODUCTION_ENVIRONMENT_ID)))
            .thenReturn(new DataTableRow(3L, null, Map.of()));

        mockMvc
            .perform(
                patch(ROW_PATH, "orders", 3L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"values": {}}
                            """))
            .andExpect(status().isOk());

        mockMvc
            .perform(
                patch(ROW_PATH, "orders", 3L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"values": {}, "externalId": null}
                            """))
            .andExpect(status().isOk());

        mockMvc
            .perform(
                patch(ROW_PATH, "orders", 3L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"values": {}, "externalId": "k"}
                            """))
            .andExpect(status().isOk());

        InOrder inOrder = inOrder(workspaceDataTableFacade);

        inOrder.verify(workspaceDataTableFacade)
            .updateRow(eq(7L), eq(3L), anyMap(), isNull(), eq(PRODUCTION_ENVIRONMENT_ID));
        inOrder.verify(workspaceDataTableFacade)
            .updateRow(eq(7L), eq(3L), anyMap(), eq(new ExternalIdPatch(null)), eq(PRODUCTION_ENVIRONMENT_ID));
        inOrder.verify(workspaceDataTableFacade)
            .updateRow(eq(7L), eq(3L), anyMap(), eq(new ExternalIdPatch("k")), eq(PRODUCTION_ENVIRONMENT_ID));
    }

    @Test
    void testUpdateRowValidatesValuesBeforeTheFacade() throws Exception {
        stubOrdersTable();

        mockMvc
            .perform(
                patch(ROW_PATH, "orders", 3L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"values": {"total": "lots"}}
                            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorKey").value(DataTableErrorType.ROW_VALUE_INVALID.getErrorKey()));

        verify(workspaceDataTableFacade, never()).updateRow(anyLong(), anyLong(), anyMap(), any(), anyLong());
    }

    @Test
    void testDeleteRowReturnsNoContent() throws Exception {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(workspaceDataTableFacade.deleteRow(7L, 3L, PRODUCTION_ENVIRONMENT_ID)).thenReturn(true);

        mockMvc
            .perform(delete(ROW_PATH, "orders", 3L))
            .andExpect(status().isNoContent());

        verify(workspaceDataTableFacade).deleteRow(7L, 3L, PRODUCTION_ENVIRONMENT_ID);
    }

    @Test
    void testDeleteRowFalseIsRowNotFound() throws Exception {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(workspaceDataTableFacade.deleteRow(7L, 3L, PRODUCTION_ENVIRONMENT_ID)).thenReturn(false);

        mockMvc
            .perform(delete(ROW_PATH, "orders", 3L))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorKey").value(DataTableErrorType.ROW_NOT_FOUND.getErrorKey()))
            .andExpect(jsonPath("$.entityClass").value("DataTableErrorType"));
    }

    @Test
    void testGetRowByExternalIdReturnsOk() throws Exception {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(workspaceDataTableFacade.fetchRowByExternalId(7L, "k", PRODUCTION_ENVIRONMENT_ID))
            .thenReturn(Optional.of(new DataTableRow(1L, "k", Map.of())));

        mockMvc
            .perform(get(ROW_BY_EXTERNAL_ID_PATH, "orders", "k").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.externalId").value("k"));
    }

    @Test
    void testGetRowByExternalIdEmptyIsRowNotFound() throws Exception {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(workspaceDataTableFacade.fetchRowByExternalId(7L, "missing", PRODUCTION_ENVIRONMENT_ID))
            .thenReturn(Optional.empty());

        mockMvc
            .perform(get(ROW_BY_EXTERNAL_ID_PATH, "orders", "missing").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorKey").value(DataTableErrorType.ROW_NOT_FOUND.getErrorKey()));
    }

    @Test
    void testUpsertRowAnswersCreatedOnCreateAndOkOnMerge() throws Exception {
        stubOrdersTable();

        when(workspaceDataTableFacade.upsertRow(7L, "k", Map.of(), PRODUCTION_ENVIRONMENT_ID))
            .thenReturn(new UpsertResult(new DataTableRow(1L, "k", Map.of()), true))
            .thenReturn(new UpsertResult(new DataTableRow(1L, "k", Map.of()), false));

        mockMvc
            .perform(
                put(ROW_BY_EXTERNAL_ID_PATH, "orders", "k")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"values": {}}
                            """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.externalId").value("k"));

        mockMvc
            .perform(
                put(ROW_BY_EXTERNAL_ID_PATH, "orders", "k")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"values": {}}
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.externalId").value("k"));
    }

    @Test
    void testUpsertRowValidatesTheExternalIdBeforeTheFacade() throws Exception {
        stubOrdersTable();

        mockMvc
            .perform(
                put(ROW_BY_EXTERNAL_ID_PATH, "orders", " ")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"values": {}}
                            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorKey").value(DataTableErrorType.ROW_VALUE_INVALID.getErrorKey()));

        verify(workspaceDataTableFacade, never()).upsertRow(anyLong(), anyString(), anyMap(), anyLong());
    }

    @Test
    void testDeleteRowByExternalIdReturnsNoContent() throws Exception {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(workspaceDataTableFacade.deleteRowByExternalId(7L, "k", PRODUCTION_ENVIRONMENT_ID)).thenReturn(true);

        mockMvc
            .perform(delete(ROW_BY_EXTERNAL_ID_PATH, "orders", "k"))
            .andExpect(status().isNoContent());

        verify(workspaceDataTableFacade).deleteRowByExternalId(7L, "k", PRODUCTION_ENVIRONMENT_ID);
    }

    @Test
    void testDeleteRowByExternalIdFalseIsRowNotFound() throws Exception {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(workspaceDataTableFacade.deleteRowByExternalId(7L, "missing", PRODUCTION_ENVIRONMENT_ID))
            .thenReturn(false);

        mockMvc
            .perform(delete(ROW_BY_EXTERNAL_ID_PATH, "orders", "missing"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorKey").value(DataTableErrorType.ROW_NOT_FOUND.getErrorKey()));
    }

    @Test
    void testBatchRowsInsertsWithDefaultStrategyAndReturnsOk() throws Exception {
        stubOrdersTable();

        when(
            workspaceDataTableFacade.insertRows(
                eq(7L), anyList(), eq(CreateStrategy.INSERT), eq(PRODUCTION_ENVIRONMENT_ID)))
                    .thenReturn(List.of(new DataTableRow(1L, null, Map.of("total", BigDecimal.ONE))));

        mockMvc
            .perform(
                post(BATCH_ROWS_PATH, "orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"rows": [{"values": {"total": "1"}}]}
                            """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rows.length()").value(1));

        verify(workspaceDataTableFacade).insertRows(
            7L, List.of(new NewRow(Map.of("total", "1"), null)), CreateStrategy.INSERT, PRODUCTION_ENVIRONMENT_ID);
    }

    @Test
    void testBatchRowsUpsertStrategyReachesTheFacade() throws Exception {
        stubOrdersTable();

        when(
            workspaceDataTableFacade.insertRows(
                eq(7L), anyList(), eq(CreateStrategy.UPSERT), eq(PRODUCTION_ENVIRONMENT_ID)))
                    .thenReturn(List.of());

        mockMvc
            .perform(
                post(BATCH_ROWS_PATH, "orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"rows": [{"values": {"total": "1"}, "externalId": "k"}], "createStrategy": "UPSERT"}
                            """))
            .andExpect(status().isOk());

        verify(workspaceDataTableFacade).insertRows(
            7L, List.of(new NewRow(Map.of("total", "1"), "k")), CreateStrategy.UPSERT, PRODUCTION_ENVIRONMENT_ID);
    }

    @Test
    void testBatchRowsRejectsMoreThanAThousandRows() throws Exception {
        stubOrdersTable();

        String rowsJson = LongStream.rangeClosed(1, 1001)
            .mapToObj(rowNumber -> "{\"values\": {}}")
            .collect(Collectors.joining(",", "{\"rows\": [", "]}"));

        mockMvc
            .perform(
                post(BATCH_ROWS_PATH, "orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rowsJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorKey").value(DataTableErrorType.BATCH_TOO_LARGE.getErrorKey()));

        verify(workspaceDataTableFacade, never()).insertRows(anyLong(), anyList(), any(), anyLong());
    }

    @Test
    void testBatchRowsValidatesEachRowBeforeTheFacade() throws Exception {
        stubOrdersTable();

        mockMvc
            .perform(
                post(BATCH_ROWS_PATH, "orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"rows": [{"values": {"total": "lots"}}]}
                            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorKey").value(DataTableErrorType.ROW_VALUE_INVALID.getErrorKey()));

        verify(workspaceDataTableFacade, never()).insertRows(anyLong(), anyList(), any(), anyLong());
    }

    @Test
    void testDeleteRowsRejectsEmptyIds() throws Exception {
        mockMvc
            .perform(delete(ROWS_PATH, "orders").param("ids", ""))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorKey").value(DataTableErrorType.ROW_VALUE_INVALID.getErrorKey()));

        verifyNoInteractions(workspaceDataTableFacade);
    }

    @Test
    void testDeleteRowsRejectsMoreThanAThousandIds() throws Exception {
        String ids = LongStream.rangeClosed(1, 1001)
            .mapToObj(String::valueOf)
            .collect(Collectors.joining(","));

        mockMvc
            .perform(delete(ROWS_PATH, "orders").param("ids", ids))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorKey").value(DataTableErrorType.BATCH_TOO_LARGE.getErrorKey()));

        verifyNoInteractions(workspaceDataTableFacade);
    }

    @Test
    void testDeleteRowsReturnsOkWithTheDeletedIds() throws Exception {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(workspaceDataTableFacade.deleteRows(7L, List.of(1L, 2L), PRODUCTION_ENVIRONMENT_ID))
            .thenReturn(List.of(1L, 2L));

        mockMvc
            .perform(delete(ROWS_PATH, "orders").param("ids", "1,2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.deletedCount").value(2))
            .andExpect(jsonPath("$.deletedIds.length()").value(2))
            .andExpect(jsonPath("$.deletedIds[0]").value(1))
            .andExpect(jsonPath("$.deletedIds[1]").value(2));
    }

    @Test
    void testClearRowsReturnsTheDeletedCount() throws Exception {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(workspaceDataTableFacade.clearRows(7L, PRODUCTION_ENVIRONMENT_ID)).thenReturn(42L);

        mockMvc
            .perform(post(CLEAR_ROWS_PATH, "orders"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.deletedCount").value(42));
    }

    @Test
    void testImportRowsReturnsTheImportedCount() throws Exception {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(workspaceDataTableFacade.importCsv(7L, "total\n1\n", PRODUCTION_ENVIRONMENT_ID)).thenReturn(1);

        mockMvc
            .perform(
                post(IMPORT_ROWS_PATH, "orders")
                    .contentType("text/csv")
                    .content("total\n1\n"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.importedCount").value(1));

        verify(workspaceDataTableFacade).importCsv(7L, "total\n1\n", PRODUCTION_ENVIRONMENT_ID);
    }

    @Test
    void testImportRowsIllegalArgumentIsBadRequest() throws Exception {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(workspaceDataTableFacade.importCsv(7L, "total\nlots\n", PRODUCTION_ENVIRONMENT_ID))
            .thenThrow(new IllegalArgumentException("Invalid CSV"));

        mockMvc
            .perform(
                post(IMPORT_ROWS_PATH, "orders")
                    .contentType("text/csv")
                    .content("total\nlots\n"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorKey").value(DataTableErrorType.ROW_VALUE_INVALID.getErrorKey()))
            .andExpect(jsonPath("$.entityClass").value("DataTableErrorType"))
            .andExpect(jsonPath("$.detail").value("Invalid CSV"));
    }

    @Test
    void testExportRowsSetsCsvHeaders() throws Exception {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(workspaceDataTableFacade.exportCsv(7L, PRODUCTION_ENVIRONMENT_ID)).thenReturn("external_id,total\n,1\n");

        mockMvc
            .perform(get(EXPORT_ROWS_PATH, "orders").accept("text/csv"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8"))
            .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"orders.csv\""))
            .andExpect(content().string("external_id,total\n,1\n"));
    }

    private void stubOrdersTable() {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(workspaceDataTableFacade.getTable(7L, PRODUCTION_ENVIRONMENT_ID))
            .thenReturn(
                new DataTableInfo(
                    7L, "orders", null, List.of(new ColumnSpec("total", ColumnType.NUMBER)), Instant.EPOCH));
    }
}
