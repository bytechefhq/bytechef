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

package com.bytechef.automation.data.table.configuration.web.graphql;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.data.table.configuration.domain.DataTableInfo;
import com.bytechef.automation.data.table.configuration.service.DataTableService;
import com.bytechef.automation.data.table.configuration.web.graphql.config.DataTableGraphQlConfigurationSharedMocks;
import com.bytechef.automation.data.table.configuration.web.graphql.config.DataTableGraphQlTestConfiguration;
import com.bytechef.automation.data.table.domain.ColumnSpec;
import com.bytechef.automation.data.table.domain.ColumnType;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    DataTableGraphQlTestConfiguration.class,
    DataTableGraphQlController.class
})
@GraphQlTest(
    controllers = DataTableGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:/graphql/"
    })
@DataTableGraphQlConfigurationSharedMocks
public class DataTableGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private DataTableService dataTableService;

    @Autowired
    private EnvironmentService environmentService;

    @Test
    void testCreateDataTable() {
        // Given
        when(environmentService.getEnvironment(1L)).thenReturn(Environment.DEVELOPMENT);
        doNothing().when(dataTableService)
            .createTable(anyString(), anyString(), any(), anyLong(), anyLong());

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    createDataTable(input: {
                        environmentId: "1",
                        baseName: "orders",
                        description: "Orders table",
                        columns: [
                            { name: "name", type: STRING },
                            { name: "amount", type: NUMBER }
                        ],
                        workspaceId: "100"
                    })
                }
                """)
            .execute()
            .path("createDataTable")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(dataTableService).createTable(eq("orders"), eq("Orders table"), any(), eq(100L), eq(0L));
    }

    @Test
    void testGetDataTables() {
        // Given
        DataTableInfo tableInfo = new DataTableInfo(
            1L, "orders", "Orders table",
            List.of(new ColumnSpec("name", ColumnType.STRING), new ColumnSpec("amount", ColumnType.NUMBER)),
            Instant.now());

        when(environmentService.getEnvironment(1L)).thenReturn(Environment.DEVELOPMENT);
        when(dataTableService.listTables(100L, 0)).thenReturn(List.of(tableInfo));

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    dataTables(environmentId: "1", workspaceId: "100") {
                        id
                        baseName
                        description
                        columns {
                            name
                            type
                        }
                    }
                }
                """)
            .execute()
            .path("dataTables")
            .entityList(Object.class)
            .hasSize(1)
            .path("dataTables[0].baseName")
            .entity(String.class)
            .isEqualTo("orders")
            .path("dataTables[0].description")
            .entity(String.class)
            .isEqualTo("Orders table")
            .path("dataTables[0].columns")
            .entityList(Object.class)
            .hasSize(2);
    }

    @Test
    void testGetDataTablesEmpty() {
        // Given
        when(environmentService.getEnvironment(1L)).thenReturn(Environment.DEVELOPMENT);
        when(dataTableService.listTables(100L, 0)).thenReturn(List.of());

        // When & Then
        this.graphQlTester
            .document("""
                query {
                    dataTables(environmentId: "1", workspaceId: "100") {
                        id
                        baseName
                    }
                }
                """)
            .execute()
            .path("dataTables")
            .entityList(Object.class)
            .hasSize(0);
    }

    @Test
    void testAddDataTableColumn() {
        // Given
        when(environmentService.getEnvironment(1L)).thenReturn(Environment.DEVELOPMENT);
        when(dataTableService.getBaseNameById(10L)).thenReturn("orders");
        doNothing().when(dataTableService)
            .addColumn(anyString(), any(ColumnSpec.class), anyLong());

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    addDataTableColumn(input: {
                        environmentId: "1",
                        tableId: "10",
                        column: { name: "status", type: STRING }
                    })
                }
                """)
            .execute()
            .path("addDataTableColumn")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(dataTableService).addColumn(eq("orders"), any(ColumnSpec.class), eq(0L));
    }

    @Test
    void testDropDataTable() {
        // Given
        when(environmentService.getEnvironment(1L)).thenReturn(Environment.DEVELOPMENT);
        when(dataTableService.getBaseNameById(10L)).thenReturn("orders");
        doNothing().when(dataTableService)
            .dropTable(anyString(), anyLong());

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    dropDataTable(input: {
                        environmentId: "1",
                        tableId: "10"
                    })
                }
                """)
            .execute()
            .path("dropDataTable")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(dataTableService).dropTable("orders", 0);
    }

    @Test
    void testRenameDataTable() {
        // Given
        when(environmentService.getEnvironment(1L)).thenReturn(Environment.DEVELOPMENT);
        when(dataTableService.getBaseNameById(10L)).thenReturn("orders");
        doNothing().when(dataTableService)
            .renameTable(anyString(), anyString(), anyLong());

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    renameDataTable(input: {
                        environmentId: "1",
                        tableId: "10",
                        newBaseName: "sales_orders"
                    })
                }
                """)
            .execute()
            .path("renameDataTable")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(dataTableService).renameTable("orders", "sales_orders", 0);
    }

    @Test
    void testDuplicateDataTable() {
        // Given
        when(environmentService.getEnvironment(1L)).thenReturn(Environment.DEVELOPMENT);
        when(dataTableService.getBaseNameById(10L)).thenReturn("orders");
        doNothing().when(dataTableService)
            .duplicateTable(anyString(), anyString(), anyLong());

        // When & Then
        this.graphQlTester
            .document("""
                mutation {
                    duplicateDataTable(input: {
                        environmentId: "1",
                        tableId: "10",
                        newBaseName: "orders_copy"
                    })
                }
                """)
            .execute()
            .path("duplicateDataTable")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(dataTableService).duplicateTable("orders", "orders_copy", 0);
    }

    @Test
    void testRemoveDataTableColumn() {
        // Given
        when(environmentService.getEnvironment(1L)).thenReturn(Environment.DEVELOPMENT);
        when(dataTableService.getBaseNameById(10L)).thenReturn("orders");
        doNothing().when(dataTableService)
            .removeColumn(anyString(), anyString(), anyLong());

        // When & Then (columnId is base64 encoded "name")
        this.graphQlTester
            .document("""
                mutation {
                    removeDataTableColumn(input: {
                        environmentId: "1",
                        tableId: "10",
                        columnId: "bmFtZQ=="
                    })
                }
                """)
            .execute()
            .path("removeDataTableColumn")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(dataTableService).removeColumn(eq("orders"), eq("name"), eq(0L));
    }

    @Test
    void testRenameDataTableColumn() {
        // Given
        when(environmentService.getEnvironment(1L)).thenReturn(Environment.DEVELOPMENT);
        when(dataTableService.getBaseNameById(10L)).thenReturn("orders");
        doNothing().when(dataTableService)
            .renameColumn(anyString(), anyString(), anyString(), anyLong());

        // When & Then (columnId is base64 encoded "name")
        this.graphQlTester
            .document("""
                mutation {
                    renameDataTableColumn(input: {
                        environmentId: "1",
                        tableId: "10",
                        columnId: "bmFtZQ==",
                        newName: "full_name"
                    })
                }
                """)
            .execute()
            .path("renameDataTableColumn")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(dataTableService).renameColumn("orders", "name", "full_name", 0);
    }
}
