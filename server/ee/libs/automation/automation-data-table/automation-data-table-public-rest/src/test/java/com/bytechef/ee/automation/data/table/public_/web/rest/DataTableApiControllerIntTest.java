/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.public_.web.rest;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.ee.automation.data.table.public_.web.rest.config.AutomationDataTablePublicRestTestConfiguration;
import com.bytechef.exception.ExecutionException;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.exception.DataTableErrorType;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.tag.domain.Tag;
import jakarta.servlet.ServletException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
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
    DataTableApiController.class, DataTableApiSupport.class
})
@TestPropertySource(properties = {
    "bytechef.edition=ee", "openapi.openAPIDefinition.base-path.automation=/api/automation"
})
@WebMvcTest(DataTableApiController.class)
class DataTableApiControllerIntTest {

    private static final String BASE_PATH = "/api/automation/v1";
    private static final String COLUMN_PATH =
        BASE_PATH + "/workspaces/{workspaceId}/data-tables/{name}/columns/{column}";
    private static final String COLUMNS_PATH = BASE_PATH + "/workspaces/{workspaceId}/data-tables/{name}/columns";
    private static final String DATA_TABLE_PATH = BASE_PATH + "/workspaces/{workspaceId}/data-tables/{name}";
    private static final long PRODUCTION_ENVIRONMENT_ID = Environment.PRODUCTION.ordinal();
    private static final String RENAME_COLUMN_PATH =
        BASE_PATH + "/workspaces/{workspaceId}/data-tables/{name}/columns/{column}/rename";
    private static final long STAGING_ENVIRONMENT_ID = Environment.STAGING.ordinal();
    private static final String WORKSPACE_DATA_TABLES_PATH = BASE_PATH + "/workspaces/{workspaceId}/data-tables";
    private static final long WORKSPACE_ID = 1L;

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
        when(environmentService.getEnvironment("STAGING")).thenReturn(Environment.STAGING);
    }

    @Test
    void testListDataTablesFiltersByTagAndMapsColumns() throws Exception {
        DataTableInfo ordersDataTableInfo = new DataTableInfo(
            7L, "orders", WORKSPACE_ID, "d", List.of(new ColumnSpec("total", ColumnType.NUMBER)), Instant.EPOCH);
        DataTableInfo otherDataTableInfo = new DataTableInfo(8L, "other", WORKSPACE_ID, null, List.of(), Instant.EPOCH);

        when(workspaceDataTableFacade.listTables(1L, STAGING_ENVIRONMENT_ID))
            .thenReturn(List.of(ordersDataTableInfo, otherDataTableInfo));
        when(workspaceDataTableFacade.getTagsByTableId(1L)).thenReturn(Map.of(7L, List.of(new Tag("hot"))));

        mockMvc
            .perform(
                get(WORKSPACE_DATA_TABLES_PATH, 1L)
                    .header("X-Environment", "STAGING")
                    .param("tag", "hot")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("orders"))
            .andExpect(jsonPath("$[0].columns[0].type").value("NUMBER"))
            .andExpect(jsonPath("$[0].tags.length()").value(1))
            .andExpect(jsonPath("$[0].tags[0]").value("hot"));
    }

    @Test
    void testListDataTablesWithNoTagFilterReturnsEveryTable() throws Exception {
        DataTableInfo ordersDataTableInfo =
            new DataTableInfo(7L, "orders", WORKSPACE_ID, "d", List.of(), Instant.EPOCH);
        DataTableInfo otherDataTableInfo = new DataTableInfo(8L, "other", WORKSPACE_ID, null, List.of(), Instant.EPOCH);

        when(workspaceDataTableFacade.listTables(1L, PRODUCTION_ENVIRONMENT_ID))
            .thenReturn(List.of(ordersDataTableInfo, otherDataTableInfo));
        when(workspaceDataTableFacade.getTagsByTableId(1L)).thenReturn(Map.of(7L, List.of(new Tag("hot"))));

        mockMvc
            .perform(get(WORKSPACE_DATA_TABLES_PATH, 1L).accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testAccessDeniedOnAWorkspaceUrlIsRethrown() throws Exception {
        when(workspaceDataTableFacade.getTagsByTableId(1L)).thenThrow(new AccessDeniedException("denied"));

        ServletException servletException = assertThrows(
            ServletException.class,
            () -> mockMvc.perform(get(WORKSPACE_DATA_TABLES_PATH, 1L).accept(MediaType.APPLICATION_JSON)));

        assertInstanceOf(AccessDeniedException.class, servletException.getCause());
    }

    @Test
    void testCreateDataTableValidatesNamesThenCreatesThenReturnsTheTable() throws Exception {
        when(workspaceDataTableFacade.createTable(anyString(), any(), anyList(), eq(1L), anyLong())).thenReturn(7L);
        when(workspaceDataTableFacade.getTable(7L, PRODUCTION_ENVIRONMENT_ID))
            .thenReturn(new DataTableInfo(7L, "orders", WORKSPACE_ID, "d", List.of(), Instant.EPOCH));
        when(workspaceDataTableFacade.getTagsByTableId(1L)).thenReturn(Map.of());

        mockMvc
            .perform(
                post(WORKSPACE_DATA_TABLES_PATH, 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "name": "orders",
                                "description": "d",
                                "columns": [{"name": "total", "type": "NUMBER"}],
                                "tags": ["hot"]
                            }
                            """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.workspaceId").value(1))
            .andExpect(jsonPath("$.name").value("orders"));

        verify(workspaceDataTableFacade).createTable(
            eq("orders"), eq("d"), eq(List.of(new ColumnSpec("total", ColumnType.NUMBER))), eq(1L),
            eq(PRODUCTION_ENVIRONMENT_ID));
        verify(workspaceDataTableFacade).updateTags(
            eq(7L), argThat(
                tags -> tags.size() == 1 && "hot".equals(
                    tags.getFirst()
                        .getName())));
    }

    @Test
    void testCreateDataTableWithoutTagsNeverCallsUpdateTags() throws Exception {
        when(workspaceDataTableFacade.createTable(anyString(), any(), anyList(), eq(1L), anyLong())).thenReturn(7L);
        when(workspaceDataTableFacade.getTable(7L, PRODUCTION_ENVIRONMENT_ID))
            .thenReturn(new DataTableInfo(7L, "orders", WORKSPACE_ID, null, List.of(), Instant.EPOCH));
        when(workspaceDataTableFacade.getTagsByTableId(1L)).thenReturn(Map.of());

        mockMvc
            .perform(
                post(WORKSPACE_DATA_TABLES_PATH, 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"name": "orders", "columns": [{"name": "total", "type": "NUMBER"}]}
                            """))
            .andExpect(status().isCreated());

        verify(workspaceDataTableFacade, never()).updateTags(anyLong(), anyList());
    }

    @Test
    void testCreateDataTableRejectsAReservedColumnBeforeTouchingTheFacade() throws Exception {
        mockMvc
            .perform(
                post(WORKSPACE_DATA_TABLES_PATH, 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"name": "orders", "columns": [{"name": "external_id", "type": "STRING"}]}
                            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorKey").value(DataTableErrorType.COLUMN_NAME_INVALID.getErrorKey()));

        verifyNoInteractions(workspaceDataTableFacade);
    }

    @Test
    void testCreateDataTableRejectsAnInvalidTableNameBeforeTouchingTheFacade() throws Exception {
        mockMvc
            .perform(
                post(WORKSPACE_DATA_TABLES_PATH, 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"name": "dt_orders", "columns": [{"name": "total", "type": "NUMBER"}]}
                            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorKey").value(DataTableErrorType.DATA_TABLE_NAME_INVALID.getErrorKey()));

        verifyNoInteractions(workspaceDataTableFacade);
    }

    @Test
    void testCreateDataTableWithAnExistingNameIsConflict() throws Exception {
        doThrow(new DataTableException("Data table exists", DataTableErrorType.DATA_TABLE_ALREADY_EXISTS))
            .when(workspaceDataTableFacade)
            .createTable(anyString(), any(), anyList(), anyLong(), anyLong());

        mockMvc
            .perform(
                post(WORKSPACE_DATA_TABLES_PATH, 1L)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"name": "orders", "columns": [{"name": "total", "type": "NUMBER"}]}
                            """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorKey").value(DataTableErrorType.DATA_TABLE_ALREADY_EXISTS.getErrorKey()))
            .andExpect(jsonPath("$.entityClass").value("DataTableErrorType"));
    }

    @Test
    void testGetDataTable() throws Exception {
        stubDataTable("orders", 7L);
        when(workspaceDataTableFacade.getTable(7L, PRODUCTION_ENVIRONMENT_ID))
            .thenReturn(
                new DataTableInfo(
                    7L, "orders", WORKSPACE_ID, "d", List.of(new ColumnSpec("total", ColumnType.NUMBER)),
                    Instant.EPOCH));
        when(workspaceDataTableFacade.getTagsByTableId(1L)).thenReturn(Map.of(7L, List.of(new Tag("hot"))));

        mockMvc
            .perform(get(DATA_TABLE_PATH, WORKSPACE_ID, "orders").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(7))
            .andExpect(jsonPath("$.workspaceId").value(1))
            .andExpect(jsonPath("$.name").value("orders"))
            .andExpect(jsonPath("$.columns[0].type").value("NUMBER"))
            .andExpect(jsonPath("$.tags.length()").value(1))
            .andExpect(jsonPath("$.tags[0]").value("hot"));
    }

    @Test
    void testGetDataTableOfAnotherWorkspaceIsNotFound() throws Exception {
        stubDataTable("orders", 7L);

        when(dataTableService.fetchDataTable(2L, "orders")).thenReturn(Optional.empty());

        mockMvc
            .perform(get(DATA_TABLE_PATH, 2L, "orders").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorKey").value(DataTableErrorType.DATA_TABLE_NOT_FOUND.getErrorKey()));

        verify(workspaceDataTableFacade, never()).getTable(anyLong(), anyLong());
    }

    @Test
    void testAccessDeniedOnAnItemUrlIsNotFound() throws Exception {
        stubDataTable("orders", 7L);
        when(workspaceDataTableFacade.getTable(7L, PRODUCTION_ENVIRONMENT_ID))
            .thenThrow(new AccessDeniedException("denied"));

        mockMvc
            .perform(get(DATA_TABLE_PATH, WORKSPACE_ID, "orders").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorKey").value(DataTableErrorType.DATA_TABLE_NOT_FOUND.getErrorKey()));
    }

    @Test
    void testExecutionExceptionWithADataTableKeyIsHandledTheSameWay() throws Exception {
        stubDataTable("orders", 7L);
        when(workspaceDataTableFacade.getTable(7L, PRODUCTION_ENVIRONMENT_ID))
            .thenThrow(new ExecutionException("Data table not found", DataTableErrorType.DATA_TABLE_NOT_FOUND));

        mockMvc
            .perform(get(DATA_TABLE_PATH, WORKSPACE_ID, "orders").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorKey").value(DataTableErrorType.DATA_TABLE_NOT_FOUND.getErrorKey()))
            .andExpect(jsonPath("$.entityClass").value("DataTableErrorType"));
    }

    @Test
    void testUpdateDataTableMissingInTheEnvironmentChangesNothing() throws Exception {
        stubDataTable("orders", 7L);
        when(workspaceDataTableFacade.getTable(7L, PRODUCTION_ENVIRONMENT_ID))
            .thenThrow(new DataTableException("Data table not found", DataTableErrorType.DATA_TABLE_NOT_FOUND));

        mockMvc
            .perform(
                patch(DATA_TABLE_PATH, WORKSPACE_ID, "orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"description": "new", "tags": ["hot"]}
                            """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorKey").value(DataTableErrorType.DATA_TABLE_NOT_FOUND.getErrorKey()));

        verify(workspaceDataTableFacade, never()).updateDescription(anyLong(), anyString());
        verify(workspaceDataTableFacade, never()).updateTags(anyLong(), anyList());
    }

    @Test
    void testUpdateDataTableLeavesOmittedFieldsAlone() throws Exception {
        stubOrdersTable();

        mockMvc
            .perform(
                patch(DATA_TABLE_PATH, WORKSPACE_ID, "orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"description": "new"}
                            """))
            .andExpect(status().isOk());

        verify(workspaceDataTableFacade).updateDescription(7L, "new");
        verify(workspaceDataTableFacade, never()).updateTags(anyLong(), anyList());
    }

    @Test
    void testUpdateDataTableWithOnlyTagsLeavesDescriptionAlone() throws Exception {
        stubOrdersTable();

        mockMvc
            .perform(
                patch(DATA_TABLE_PATH, WORKSPACE_ID, "orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"tags": ["hot"]}
                            """))
            .andExpect(status().isOk());

        verify(workspaceDataTableFacade, never()).updateDescription(anyLong(), anyString());
        verify(workspaceDataTableFacade).updateTags(
            eq(7L), argThat(
                tags -> tags.size() == 1 && "hot".equals(
                    tags.getFirst()
                        .getName())));
    }

    @Test
    void testUpdateDataTableTreatsTagsAsThreeState() throws Exception {
        stubOrdersTable();

        mockMvc
            .perform(
                patch(DATA_TABLE_PATH, WORKSPACE_ID, "orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"description": "new"}
                            """))
            .andExpect(status().isOk());

        verify(workspaceDataTableFacade, never()).updateTags(anyLong(), anyList());

        mockMvc
            .perform(
                patch(DATA_TABLE_PATH, WORKSPACE_ID, "orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"tags": null}
                            """))
            .andExpect(status().isOk());

        verify(workspaceDataTableFacade).updateTags(eq(7L), argThat(List::isEmpty));

        mockMvc
            .perform(
                patch(DATA_TABLE_PATH, WORKSPACE_ID, "orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"tags": ["hot"]}
                            """))
            .andExpect(status().isOk());

        verify(workspaceDataTableFacade).updateTags(
            eq(7L), argThat(
                tags -> tags.size() == 1 && "hot".equals(
                    tags.getFirst()
                        .getName())));
    }

    @Test
    void testDeleteDataTableIsNoContent() throws Exception {
        stubDataTable("orders", 7L);

        mockMvc
            .perform(delete(DATA_TABLE_PATH, WORKSPACE_ID, "orders"))
            .andExpect(status().isNoContent());

        verify(workspaceDataTableFacade).dropTable(7L, PRODUCTION_ENVIRONMENT_ID);
    }

    @Test
    void testColumnOperationsResolveTheTable() throws Exception {
        stubOrdersTable();

        mockMvc
            .perform(
                post(COLUMNS_PATH, WORKSPACE_ID, "orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"name": "qty", "type": "INTEGER"}
                            """))
            .andExpect(status().isCreated());

        mockMvc
            .perform(
                post(RENAME_COLUMN_PATH, WORKSPACE_ID, "orders", "qty")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"newName": "quantity"}
                            """))
            .andExpect(status().isOk());

        mockMvc
            .perform(delete(COLUMN_PATH, WORKSPACE_ID, "orders", "quantity"))
            .andExpect(status().isNoContent());

        verify(workspaceDataTableFacade).addColumn(
            7L, new ColumnSpec("qty", ColumnType.INTEGER), PRODUCTION_ENVIRONMENT_ID);
        verify(workspaceDataTableFacade).renameColumn(7L, "qty", "quantity", PRODUCTION_ENVIRONMENT_ID);
        verify(workspaceDataTableFacade).removeColumn(7L, "quantity", PRODUCTION_ENVIRONMENT_ID);
    }

    @Test
    void testCreateColumnRejectsAReservedNameBeforeTouchingTheFacade() throws Exception {
        mockMvc
            .perform(
                post(COLUMNS_PATH, WORKSPACE_ID, "orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"name": "external_id", "type": "STRING"}
                            """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorKey").value(DataTableErrorType.COLUMN_NAME_INVALID.getErrorKey()));

        verifyNoInteractions(workspaceDataTableFacade);
        verifyNoInteractions(dataTableService);
    }

    @Test
    void testCreateColumnWithAnExistingNameIsConflict() throws Exception {
        stubOrdersTable();

        doThrow(new DataTableException("Column exists", DataTableErrorType.COLUMN_ALREADY_EXISTS))
            .when(workspaceDataTableFacade)
            .addColumn(7L, new ColumnSpec("qty", ColumnType.INTEGER), PRODUCTION_ENVIRONMENT_ID);

        mockMvc
            .perform(
                post(COLUMNS_PATH, WORKSPACE_ID, "orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {"name": "qty", "type": "INTEGER"}
                            """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorKey").value(DataTableErrorType.COLUMN_ALREADY_EXISTS.getErrorKey()));
    }

    @Test
    void testDeleteMissingColumnIsNotFound() throws Exception {
        stubDataTable("orders", 7L);

        doThrow(new DataTableException("Column not found", DataTableErrorType.COLUMN_NOT_FOUND))
            .when(workspaceDataTableFacade)
            .removeColumn(7L, "quantity", PRODUCTION_ENVIRONMENT_ID);

        mockMvc
            .perform(delete(COLUMN_PATH, WORKSPACE_ID, "orders", "quantity"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorKey").value(DataTableErrorType.COLUMN_NOT_FOUND.getErrorKey()));
    }

    private void stubOrdersTable() {
        stubDataTable("orders", 7L);
        when(workspaceDataTableFacade.getTable(7L, PRODUCTION_ENVIRONMENT_ID))
            .thenReturn(new DataTableInfo(7L, "orders", WORKSPACE_ID, "d", List.of(), Instant.EPOCH));
        when(workspaceDataTableFacade.getTagsByTableId(anyLong())).thenReturn(Map.of());
    }

    private void stubDataTable(String name, long dataTableId) {
        when(dataTableService.fetchDataTable(WORKSPACE_ID, name))
            .thenReturn(Optional.of(new DataTable(dataTableId, name)));
    }
}
