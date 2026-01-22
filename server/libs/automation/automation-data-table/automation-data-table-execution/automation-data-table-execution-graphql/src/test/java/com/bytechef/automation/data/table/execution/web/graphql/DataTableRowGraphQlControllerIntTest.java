/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.automation.data.table.execution.web.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.data.table.configuration.service.DataTableService;
import com.bytechef.automation.data.table.execution.domain.DataTableRow;
import com.bytechef.automation.data.table.execution.service.DataTableRowService;
import com.bytechef.automation.data.table.execution.web.graphql.config.DataTableRowGraphQlConfigurationSharedMocks;
import com.bytechef.automation.data.table.execution.web.graphql.config.DataTableRowGraphQlTestConfiguration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    DataTableRowGraphQlTestConfiguration.class,
    DataTableRowGraphQlController.class
})
@GraphQlTest(
    controllers = DataTableRowGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@DataTableRowGraphQlConfigurationSharedMocks
public class DataTableRowGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private DataTableRowService dataTableRowService;

    @Autowired
    private DataTableService dataTableService;

    @Test
    void testGetDataTableRows() {
        // Given
        DataTableRow row1 = new DataTableRow(1L, Map.of("name", "Order 1", "amount", 100));
        DataTableRow row2 = new DataTableRow(2L, Map.of("name", "Order 2", "amount", 200));

        when(dataTableService.getBaseNameById(10L)).thenReturn("orders");
        when(dataTableRowService.listRows("orders", Integer.MAX_VALUE, 0, 1L)).thenReturn(List.of(row1, row2));

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    dataTableRows(environmentId: "1", tableId: "10") {
                        id
                        values
                    }
                }
                """)
            .execute()
            .path("dataTableRows")
            .entityList(Object.class)
            .hasSize(2);
    }

    @Test
    void testGetDataTableRowsEmpty() {
        // Given
        when(dataTableService.getBaseNameById(10L)).thenReturn("orders");
        when(dataTableRowService.listRows("orders", Integer.MAX_VALUE, 0, 1L)).thenReturn(List.of());

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    dataTableRows(environmentId: "1", tableId: "10") {
                        id
                        values
                    }
                }
                """)
            .execute()
            .path("dataTableRows")
            .entityList(Object.class)
            .hasSize(0);
    }

    @Test
    void testGetDataTableRowsPage() {
        // Given
        DataTableRow row1 = new DataTableRow(1L, Map.of("name", "Order 1"));
        DataTableRow row2 = new DataTableRow(2L, Map.of("name", "Order 2"));
        DataTableRow row3 = new DataTableRow(3L, Map.of("name", "Order 3"));

        when(dataTableService.getBaseNameById(10L)).thenReturn("orders");
        when(dataTableRowService.listRows("orders", 3, 0, 1L)).thenReturn(List.of(row1, row2, row3));

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    dataTableRowsPage(environmentId: "1", tableId: "10", limit: 2, offset: 0) {
                        items {
                            id
                            values
                        }
                        hasMore
                        nextOffset
                    }
                }
                """)
            .execute()
            .path("dataTableRowsPage.items")
            .entityList(Object.class)
            .hasSize(2)
            .path("dataTableRowsPage.hasMore")
            .entity(Boolean.class)
            .isEqualTo(true)
            .path("dataTableRowsPage.nextOffset")
            .entity(Integer.class)
            .isEqualTo(2);
    }

    @Test
    void testGetDataTableRowsPageNoMore() {
        // Given
        DataTableRow row1 = new DataTableRow(1L, Map.of("name", "Order 1"));

        when(dataTableService.getBaseNameById(10L)).thenReturn("orders");
        when(dataTableRowService.listRows("orders", 3, 0, 1L)).thenReturn(List.of(row1));

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    dataTableRowsPage(environmentId: "1", tableId: "10", limit: 2, offset: 0) {
                        items {
                            id
                        }
                        hasMore
                        nextOffset
                    }
                }
                """)
            .execute()
            .path("dataTableRowsPage.items")
            .entityList(Object.class)
            .hasSize(1)
            .path("dataTableRowsPage.hasMore")
            .entity(Boolean.class)
            .isEqualTo(false)
            .path("dataTableRowsPage.nextOffset")
            .valueIsNull();
    }

    @Test
    void testInsertDataTableRow() {
        // Given
        DataTableRow insertedRow = new DataTableRow(1L, Map.of("name", "New Order", "amount", 500));

        when(dataTableService.getBaseNameById(10L)).thenReturn("orders");
        when(dataTableRowService.insertRow(eq("orders"), any(), eq(1L))).thenReturn(insertedRow);

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    insertDataTableRow(input: {
                        environmentId: "1",
                        tableId: "10",
                        values: { name: "New Order", amount: 500 }
                    }) {
                        id
                        values
                    }
                }
                """)
            .execute()
            .path("insertDataTableRow.id")
            .entity(String.class)
            .isEqualTo("1");

        verify(dataTableRowService).insertRow(eq("orders"), any(), eq(1L));
    }

    @Test
    void testUpdateDataTableRow() {
        // Given
        DataTableRow updatedRow = new DataTableRow(1L, Map.of("name", "Updated Order", "amount", 750));

        when(dataTableService.getBaseNameById(10L)).thenReturn("orders");
        when(dataTableRowService.updateRow(eq("orders"), eq(1L), any(), eq(1L))).thenReturn(updatedRow);

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    updateDataTableRow(input: {
                        environmentId: "1",
                        tableId: "10",
                        id: "1",
                        values: { name: "Updated Order", amount: 750 }
                    }) {
                        id
                        values
                    }
                }
                """)
            .execute()
            .path("updateDataTableRow.id")
            .entity(String.class)
            .isEqualTo("1");

        verify(dataTableRowService).updateRow(eq("orders"), eq(1L), any(), eq(1L));
    }

    @Test
    void testDeleteDataTableRow() {
        // Given
        when(dataTableService.getBaseNameById(10L)).thenReturn("orders");
        when(dataTableRowService.deleteRow("orders", 1L, 1L)).thenReturn(true);

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    deleteDataTableRow(input: {
                        environmentId: "1",
                        tableId: "10",
                        id: "1"
                    })
                }
                """)
            .execute()
            .path("deleteDataTableRow")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(dataTableRowService).deleteRow("orders", 1L, 1L);
    }

    @Test
    void testDeleteDataTableRowNotFound() {
        // Given
        when(dataTableService.getBaseNameById(10L)).thenReturn("orders");
        when(dataTableRowService.deleteRow("orders", 999L, 1L)).thenReturn(false);

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    deleteDataTableRow(input: {
                        environmentId: "1",
                        tableId: "10",
                        id: "999"
                    })
                }
                """)
            .execute()
            .path("deleteDataTableRow")
            .entity(Boolean.class)
            .isEqualTo(false);
    }

    @Test
    void testExportDataTableCsv() {
        // Given
        String csvContent = "name,amount\nOrder 1,100\nOrder 2,200";

        when(dataTableService.getBaseNameById(10L)).thenReturn("orders");
        when(dataTableRowService.exportCsv("orders", 1L)).thenReturn(csvContent);

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    exportDataTableCsv(environmentId: "1", tableId: "10")
                }
                """)
            .execute()
            .path("exportDataTableCsv")
            .entity(String.class)
            .isEqualTo(csvContent);
    }

    @Test
    void testImportDataTableCsv() {
        // Given
        when(dataTableService.getBaseNameById(10L)).thenReturn("orders");

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    importDataTableCsv(input: {
                        environmentId: "1",
                        tableId: "10",
                        csv: "name,amount\\nImported 1,100\\nImported 2,200"
                    })
                }
                """)
            .execute()
            .path("importDataTableCsv")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(dataTableRowService).importCsv(eq("orders"), anyString(), eq(1L));
    }
}
