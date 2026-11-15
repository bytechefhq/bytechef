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

package com.bytechef.automation.data.table.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.automation.data.table.web.graphql.config.AutomationDataTableGraphQlTestConfiguration;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.ColumnSpec;
import com.bytechef.platform.data.table.domain.ColumnType;
import com.bytechef.platform.data.table.domain.DataTableStorageUsage;
import com.bytechef.platform.data.table.domain.LinkDeleteRule;
import com.bytechef.platform.data.table.domain.LinkSpec;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * @author Ivica Cardic
 */
@ContextConfiguration(classes = {
    AutomationDataTableGraphQlTestConfiguration.class, DataTableGraphQlController.class
})
@GraphQlTest(
    controllers = DataTableGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:graphql/**/"
    })
@MockitoBean(types = {
    DataTableService.class, EnvironmentService.class
})
class DataTableGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private EnvironmentService environmentService;

    @MockitoBean
    private WorkspaceDataTableFacade workspaceDataTableFacade;

    @Test
    void testAddDataTableColumnWithLink() {
        when(environmentService.getEnvironment(1L)).thenReturn(Environment.DEVELOPMENT);

        GraphQlTester.Response response = graphQlTester
            .document("""
                mutation {
                    addDataTableColumn(input: {
                        environmentId: 1, tableId: 5,
                        column: {
                            name: "customer", type: LINK,
                            link: {targetDataTableId: 7, onDelete: CLEAR, displayColumnName: "name"}
                        }
                    })
                }
                """)
            .execute();

        response.path("addDataTableColumn")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(workspaceDataTableFacade).addColumn(
            5L,
            new ColumnSpec("customer", ColumnType.LINK, new LinkSpec(7L, LinkDeleteRule.CLEAR, "name")),
            Environment.DEVELOPMENT.ordinal());
    }

    @Test
    void testAddDataTableColumnWithLinkDefaultsOnDeleteToBlock() {
        when(environmentService.getEnvironment(1L)).thenReturn(Environment.DEVELOPMENT);

        GraphQlTester.Response response = graphQlTester
            .document("""
                mutation {
                    addDataTableColumn(input: {
                        environmentId: 1, tableId: 5,
                        column: {name: "customer", type: LINK, link: {targetDataTableId: 7}}
                    })
                }
                """)
            .execute();

        response.path("addDataTableColumn")
            .entity(Boolean.class)
            .isEqualTo(true);

        verify(workspaceDataTableFacade).addColumn(
            5L, new ColumnSpec("customer", ColumnType.LINK, new LinkSpec(7L, LinkDeleteRule.BLOCK, null)),
            Environment.DEVELOPMENT.ordinal());
    }

    @Test
    void testAddDataTableColumnOfLinkTypeWithoutLinkIsRejected() {
        when(environmentService.getEnvironment(1L)).thenReturn(Environment.DEVELOPMENT);

        GraphQlTester.Response response = graphQlTester
            .document("""
                mutation {
                    addDataTableColumn(input: {
                        environmentId: 1, tableId: 5, column: {name: "customer", type: LINK}
                    })
                }
                """)
            .execute();

        response.errors()
            .satisfy(errors -> assertThat(errors).hasSize(1));

        verify(workspaceDataTableFacade, never()).addColumn(anyLong(), any(ColumnSpec.class), anyLong());
    }

    @Test
    void testAddDataTableColumnWithLinkOnNonLinkTypeIsRejected() {
        when(environmentService.getEnvironment(1L)).thenReturn(Environment.DEVELOPMENT);

        GraphQlTester.Response response = graphQlTester
            .document("""
                mutation {
                    addDataTableColumn(input: {
                        environmentId: 1, tableId: 5,
                        column: {name: "title", type: STRING, link: {targetDataTableId: 7, onDelete: BLOCK}}
                    })
                }
                """)
            .execute();

        response.errors()
            .satisfy(errors -> assertThat(errors).hasSize(1));

        verify(workspaceDataTableFacade, never()).addColumn(anyLong(), any(ColumnSpec.class), anyLong());
    }

    @Test
    void testDataTablesExposeLinkColumns() {
        when(environmentService.getEnvironment(1L)).thenReturn(Environment.DEVELOPMENT);
        when(workspaceDataTableFacade.listTables(3L, Environment.DEVELOPMENT.ordinal()))
            .thenReturn(List.of(
                new DataTableInfo(
                    7L, "customers", 3L, null, List.of(new ColumnSpec("name", ColumnType.STRING)), Instant.now()),
                new DataTableInfo(
                    5L, "orders", 3L, null,
                    List.of(
                        new ColumnSpec("title", ColumnType.STRING),
                        new ColumnSpec("customer", ColumnType.LINK, new LinkSpec(7L, LinkDeleteRule.BLOCK, "name"))),
                    Instant.now())));

        GraphQlTester.Response response = graphQlTester
            .document("""
                query {
                    dataTables(environmentId: 1, workspaceId: 3) {
                        baseName
                        columns {
                            name
                            type
                            link {
                                targetDataTableId
                                targetDataTableName
                                onDelete
                                displayColumnName
                            }
                        }
                    }
                }
                """)
            .execute();

        response.path("dataTables[1].columns[0].link")
            .valueIsNull();
        response.path("dataTables[1].columns[1].type")
            .entity(String.class)
            .isEqualTo("LINK");
        response.path("dataTables[1].columns[1].link.targetDataTableId")
            .entity(Long.class)
            .isEqualTo(7L);
        response.path("dataTables[1].columns[1].link.targetDataTableName")
            .entity(String.class)
            .isEqualTo("customers");
        response.path("dataTables[1].columns[1].link.onDelete")
            .entity(String.class)
            .isEqualTo("BLOCK");
        response.path("dataTables[1].columns[1].link.displayColumnName")
            .entity(String.class)
            .isEqualTo("name");
    }

    @Test
    void testDataTableStorageUsage() {
        when(workspaceDataTableFacade.getStorageUsage())
            .thenReturn(new DataTableStorageUsage(10L, 100L, 10.0, false));

        GraphQlTester.Response response = graphQlTester
            .document("""
                query {
                    dataTableStorageUsage {
                        usedBytes
                        limitBytes
                        percentage
                        unlimited
                    }
                }
                """)
            .execute();

        response.path("dataTableStorageUsage.usedBytes")
            .entity(Long.class)
            .isEqualTo(10L);
        response.path("dataTableStorageUsage.limitBytes")
            .entity(Long.class)
            .isEqualTo(100L);
        response.path("dataTableStorageUsage.percentage")
            .entity(Double.class)
            .isEqualTo(10.0);
        response.path("dataTableStorageUsage.unlimited")
            .entity(Boolean.class)
            .isEqualTo(false);

        verify(workspaceDataTableFacade).getStorageUsage();
    }

    @Test
    void testDataTableStorageUsageUnlimited() {
        when(workspaceDataTableFacade.getStorageUsage())
            .thenReturn(new DataTableStorageUsage(26_214_400L, 0L, 0.0, true));

        GraphQlTester.Response response = graphQlTester
            .document("""
                query {
                    dataTableStorageUsage {
                        usedBytes
                        unlimited
                    }
                }
                """)
            .execute();

        response.path("dataTableStorageUsage.usedBytes")
            .entity(Long.class)
            .isEqualTo(26_214_400L);
        response.path("dataTableStorageUsage.unlimited")
            .entity(Boolean.class)
            .isEqualTo(true);
    }
}
