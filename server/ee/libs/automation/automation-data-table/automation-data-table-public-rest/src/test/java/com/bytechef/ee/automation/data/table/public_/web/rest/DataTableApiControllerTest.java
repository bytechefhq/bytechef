/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.data.table.public_.web.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.ColumnTypeModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.CreateColumnRequestModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.CreateDataTableRequestModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.DataTableColumnModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.DataTableModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.EnvironmentModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.RenameColumnRequestModel;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.UpdateDataTableRequestModel;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.exception.DataTableException;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.tag.domain.Tag;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class DataTableApiControllerTest {

    private final WorkspaceDataTableFacade facade = mock(WorkspaceDataTableFacade.class);
    private final DataTableService dataTableService = mock(DataTableService.class);
    private final EnvironmentService environmentService = mock(EnvironmentService.class);
    private DataTableApiController controller;

    @BeforeEach
    void beforeEach() {
        when(environmentService.getEnvironment((String) null)).thenReturn(Environment.PRODUCTION);
        when(environmentService.getEnvironment("STAGING")).thenReturn(Environment.STAGING);

        controller = new DataTableApiController(facade, new DataTableApiSupport(dataTableService, environmentService));
    }

    @Test
    void testListFiltersByTagAndMapsColumns() {
        DataTableInfo orders = new DataTableInfo(
            7L, "orders", "d", List.of(new ColumnSpec("total", ColumnType.NUMBER)), Instant.EPOCH);
        DataTableInfo other = new DataTableInfo(8L, "other", null, List.of(), Instant.EPOCH);

        when(facade.listTables(1L, Environment.STAGING.ordinal())).thenReturn(List.of(orders, other));
        when(facade.getDataTableTags(1L)).thenReturn(List.of());
        when(facade.getTagsByTableId(1L)).thenReturn(Map.of(7L, List.of(new Tag("hot"))));

        ResponseEntity<List<DataTableModel>> response = controller.listDataTables(1L, EnvironmentModel.STAGING, "hot");

        assertEquals(HttpStatus.OK, response.getStatusCode());

        List<DataTableModel> models = response.getBody();

        assertEquals(1, models.size());
        assertEquals(
            "orders", models.getFirst()
                .getName());
        assertEquals(
            ColumnTypeModel.NUMBER, models.getFirst()
                .getColumns()
                .getFirst()
                .getType());
        assertEquals(
            List.of("hot"), models.getFirst()
                .getTags());
    }

    @Test
    void testListWithNoTagFilterReturnsEveryTable() {
        DataTableInfo orders = new DataTableInfo(7L, "orders", "d", List.of(), Instant.EPOCH);
        DataTableInfo other = new DataTableInfo(8L, "other", null, List.of(), Instant.EPOCH);

        when(facade.listTables(1L, Environment.PRODUCTION.ordinal())).thenReturn(List.of(orders, other));
        when(facade.getTagsByTableId(1L)).thenReturn(Map.of(7L, List.of(new Tag("hot"))));

        List<DataTableModel> models = controller.listDataTables(1L, null, null)
            .getBody();

        assertEquals(2, models.size());
    }

    @Test
    void testCreateValidatesNamesThenCreatesThenReturnsTheTable() {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(facade.getTable(7L, Environment.PRODUCTION.ordinal()))
            .thenReturn(new DataTableInfo(7L, "orders", "d", List.of(), Instant.EPOCH));
        when(facade.getTagsByTableId(1L)).thenReturn(Map.of());

        ResponseEntity<DataTableModel> response = controller.createDataTable(
            1L, new CreateDataTableRequestModel()
                .name("orders")
                .description("d")
                .columns(
                    List.of(
                        new DataTableColumnModel().name("total")
                            .type(ColumnTypeModel.NUMBER)))
                .tags(List.of("hot")),
            null);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        verify(facade).createTable(
            eq("orders"), eq("d"), eq(List.of(new ColumnSpec("total", ColumnType.NUMBER))), eq(1L),
            eq((long) Environment.PRODUCTION.ordinal()));
        verify(facade).updateTags(
            eq(7L), argThat(
                tags -> tags.size() == 1 && "hot".equals(
                    tags.getFirst()
                        .getName())));
    }

    @Test
    void testCreateWithoutTagsNeverCallsUpdateTags() {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(facade.getTable(7L, Environment.PRODUCTION.ordinal()))
            .thenReturn(new DataTableInfo(7L, "orders", null, List.of(), Instant.EPOCH));
        when(facade.getTagsByTableId(1L)).thenReturn(Map.of());

        controller.createDataTable(
            1L, new CreateDataTableRequestModel()
                .name("orders")
                .columns(
                    List.of(
                        new DataTableColumnModel().name("total")
                            .type(ColumnTypeModel.NUMBER))),
            null);

        verify(facade, never()).updateTags(anyLong(), anyList());
    }

    @Test
    void testCreateRejectsAReservedColumnBeforeTouchingTheFacade() {
        assertThrows(
            DataTableException.class,
            () -> controller.createDataTable(
                1L, new CreateDataTableRequestModel()
                    .name("orders")
                    .columns(
                        List.of(
                            new DataTableColumnModel().name("external_id")
                                .type(ColumnTypeModel.STRING))),
                null));

        verifyNoInteractions(facade);
    }

    @Test
    void testCreateRejectsAnInvalidTableNameBeforeTouchingTheFacade() {
        assertThrows(
            DataTableException.class,
            () -> controller.createDataTable(
                1L, new CreateDataTableRequestModel()
                    .name("dt_orders")
                    .columns(
                        List.of(
                            new DataTableColumnModel().name("total")
                                .type(ColumnTypeModel.NUMBER))),
                null));

        verifyNoInteractions(facade);
    }

    @Test
    void testGetDataTable() {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(facade.getTable(7L, Environment.PRODUCTION.ordinal()))
            .thenReturn(new DataTableInfo(7L, "orders", "d", List.of(new ColumnSpec("total", ColumnType.NUMBER)),
                Instant.EPOCH));
        when(facade.getWorkspaceId(7L)).thenReturn(1L);
        when(facade.getTagsByTableId(1L)).thenReturn(Map.of(7L, List.of(new Tag("hot"))));

        ResponseEntity<DataTableModel> response = controller.getDataTable("orders", null);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        DataTableModel model = response.getBody();

        assertEquals("orders", model.getName());
        assertEquals(
            ColumnTypeModel.NUMBER, model.getColumns()
                .getFirst()
                .getType());
        assertEquals(List.of("hot"), model.getTags());
    }

    @Test
    void testUpdateLeavesOmittedFieldsAlone() {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(facade.getTable(7L, Environment.PRODUCTION.ordinal()))
            .thenReturn(new DataTableInfo(7L, "orders", "d", List.of(), Instant.EPOCH));
        when(facade.getTagsByTableId(anyLong())).thenReturn(Map.of());

        ResponseEntity<DataTableModel> response = controller.updateDataTable(
            "orders", new UpdateDataTableRequestModel().description("new"), null);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(facade).updateDescription(7L, "new");
        verify(facade, never()).updateTags(anyLong(), anyList());
    }

    @Test
    void testUpdateWithOnlyTagsLeavesDescriptionAlone() {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(facade.getTable(7L, Environment.PRODUCTION.ordinal()))
            .thenReturn(new DataTableInfo(7L, "orders", "d", List.of(), Instant.EPOCH));
        when(facade.getTagsByTableId(anyLong())).thenReturn(Map.of());

        controller.updateDataTable("orders", new UpdateDataTableRequestModel().tags(List.of("hot")), null);

        verify(facade, never()).updateDescription(anyLong(), anyString());
        verify(facade).updateTags(
            eq(7L), argThat(
                tags -> tags.size() == 1 && "hot".equals(
                    tags.getFirst()
                        .getName())));
    }

    /**
     * The three states of tags on one request, since only the middle one is new and only the pair of the other two
     * makes it meaningful: omitted leaves the tags alone, an explicit null clears them, a list replaces them. The clear
     * must reach updateTags with an empty list -- not skip the call, which is what the old "present and non-empty"
     * guard did and why a table's last tag could not be removed.
     */
    @Test
    void testUpdateTreatsTagsAsThreeState() {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(facade.getTable(7L, Environment.PRODUCTION.ordinal()))
            .thenReturn(new DataTableInfo(7L, "orders", "d", List.of(), Instant.EPOCH));
        when(facade.getTagsByTableId(anyLong())).thenReturn(Map.of());

        controller.updateDataTable("orders", new UpdateDataTableRequestModel().description("new"), null);

        verify(facade, never()).updateTags(anyLong(), anyList());

        UpdateDataTableRequestModel clearRequest = new UpdateDataTableRequestModel();

        clearRequest.setTags(JsonNullable.of(null));

        controller.updateDataTable("orders", clearRequest, null);

        verify(facade).updateTags(eq(7L), argThat(List::isEmpty));

        controller.updateDataTable("orders", new UpdateDataTableRequestModel().tags(List.of("hot")), null);

        verify(facade).updateTags(
            eq(7L), argThat(
                tags -> tags.size() == 1 && "hot".equals(
                    tags.getFirst()
                        .getName())));
    }

    @Test
    void testDeleteIs204() {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);

        assertEquals(
            HttpStatus.NO_CONTENT, controller.deleteDataTable("orders", null)
                .getStatusCode());

        verify(facade).dropTable(7L, Environment.PRODUCTION.ordinal());
    }

    @Test
    void testColumnOperationsResolveTheTableOnce() {
        when(dataTableService.getIdByBaseName("orders")).thenReturn(7L);
        when(facade.getTable(7L, Environment.PRODUCTION.ordinal()))
            .thenReturn(new DataTableInfo(7L, "orders", null, List.of(), Instant.EPOCH));
        when(facade.getTagsByTableId(anyLong())).thenReturn(Map.of());

        ResponseEntity<DataTableModel> createColumnResponse = controller.createColumn(
            "orders", new CreateColumnRequestModel().name("qty")
                .type(ColumnTypeModel.INTEGER),
            null);
        ResponseEntity<DataTableModel> renameColumnResponse = controller.renameColumn(
            "orders", "qty", new RenameColumnRequestModel().newName("quantity"), null);
        ResponseEntity<Void> deleteColumnResponse = controller.deleteColumn("orders", "quantity", null);

        assertEquals(HttpStatus.CREATED, createColumnResponse.getStatusCode());
        assertEquals(HttpStatus.OK, renameColumnResponse.getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, deleteColumnResponse.getStatusCode());

        verify(facade).addColumn(7L, new ColumnSpec("qty", ColumnType.INTEGER), Environment.PRODUCTION.ordinal());
        verify(facade).renameColumn(7L, "qty", "quantity", Environment.PRODUCTION.ordinal());
        verify(facade).removeColumn(7L, "quantity", Environment.PRODUCTION.ordinal());
    }

    @Test
    void testCreateColumnRejectsAReservedNameBeforeTouchingTheFacade() {
        assertThrows(
            DataTableException.class,
            () -> controller.createColumn(
                "orders", new CreateColumnRequestModel().name("external_id")
                    .type(ColumnTypeModel.STRING),
                null));

        verifyNoInteractions(facade);
        verifyNoInteractions(dataTableService);
    }
}
