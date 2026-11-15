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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.data.table.configuration.domain.DataTableLinkOption;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.automation.data.table.web.graphql.config.AutomationDataTableGraphQlTestConfiguration;
import com.bytechef.platform.data.table.execution.domain.DataTableRow;
import java.util.List;
import java.util.Map;
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
    AutomationDataTableGraphQlTestConfiguration.class, DataTableRowGraphQlController.class
})
@GraphQlTest(
    controllers = DataTableRowGraphQlController.class,
    properties = {
        "bytechef.coordinator.enabled=true",
        "spring.graphql.schema.locations=classpath*:graphql/**/"
    })
class DataTableRowGraphQlControllerIntTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @MockitoBean
    private WorkspaceDataTableFacade workspaceDataTableFacade;

    @Test
    void testDataTableRowsExpand() {
        DataTableRow customerRow = new DataTableRow(7L, "cust-7", Map.of("name", "Ana"));

        when(workspaceDataTableFacade.listRows(5L, Integer.MAX_VALUE, 0, List.of("customer"), 1L))
            .thenReturn(List.of(new DataTableRow(1L, Map.of("title", "Order", "customer", customerRow))));

        GraphQlTester.Response response = graphQlTester
            .document("""
                query {
                    dataTableRows(environmentId: 1, tableId: 5, expand: ["customer"]) {
                        id
                        values
                    }
                }
                """)
            .execute();

        response.path("dataTableRows[0].values.customer.id")
            .entity(Long.class)
            .isEqualTo(7L);
        response.path("dataTableRows[0].values.customer.externalId")
            .entity(String.class)
            .isEqualTo("cust-7");
        response.path("dataTableRows[0].values.customer.values.name")
            .entity(String.class)
            .isEqualTo("Ana");
    }

    @Test
    void testDataTableRowsPageExpand() {
        when(workspaceDataTableFacade.listRows(5L, 11, 0, List.of("customer"), 1L))
            .thenReturn(List.of(new DataTableRow(1L, Map.of("title", "Order"))));

        GraphQlTester.Response response = graphQlTester
            .document("""
                query {
                    dataTableRowsPage(environmentId: 1, tableId: 5, limit: 10, expand: ["customer"]) {
                        items {
                            id
                        }
                        hasMore
                    }
                }
                """)
            .execute();

        response.path("dataTableRowsPage.items[0].id")
            .entity(Long.class)
            .isEqualTo(1L);
        response.path("dataTableRowsPage.hasMore")
            .entity(Boolean.class)
            .isEqualTo(false);
    }

    @Test
    void testDataTableLinkOptions() {
        when(workspaceDataTableFacade.getLinkOptions(5L, "customer", "an", 10, 1L))
            .thenReturn(List.of(new DataTableLinkOption(7L, "Ana"), new DataTableLinkOption(9L, "#9")));

        GraphQlTester.Response response = graphQlTester
            .document("""
                query {
                    dataTableLinkOptions(
                        environmentId: 1, tableId: 5, columnName: "customer", search: "an", limit: 10) {
                        id
                        label
                    }
                }
                """)
            .execute();

        response.path("dataTableLinkOptions[0].id")
            .entity(Long.class)
            .isEqualTo(7L);
        response.path("dataTableLinkOptions[0].label")
            .entity(String.class)
            .isEqualTo("Ana");
        response.path("dataTableLinkOptions[1].label")
            .entity(String.class)
            .isEqualTo("#9");

        verify(workspaceDataTableFacade).getLinkOptions(5L, "customer", "an", 10, 1L);
    }
}
